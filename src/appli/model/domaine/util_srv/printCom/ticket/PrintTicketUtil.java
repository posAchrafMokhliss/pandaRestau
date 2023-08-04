/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appli.model.domaine.util_srv.printCom.ticket;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.krysalis.barcode4j.BarcodeGenerator;
import org.krysalis.barcode4j.BarcodeUtil;
import org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider;

import appli.controller.domaine.util_erp.ContextAppli;
import appli.controller.domaine.util_erp.ContextAppli.PARAM_APPLI_ENUM;
import appli.controller.domaine.util_erp.ContextAppli.SOFT_ENVS;
import appli.controller.domaine.util_erp.ContextAppli.TYPE_LIGNE_COMMANDE;
import appli.model.domaine.administration.persistant.UserPersistant;
import appli.model.domaine.fidelite.dao.IPortefeuille2Service;
import appli.model.domaine.fidelite.persistant.CarteFideliteClientPersistant;
import appli.model.domaine.personnel.persistant.ClientPersistant;
import appli.model.domaine.personnel.persistant.EmployePersistant;
import appli.model.domaine.vente.persistant.CaisseMouvementArticlePersistant;
import appli.model.domaine.vente.persistant.CaisseMouvementOffrePersistant;
import appli.model.domaine.vente.persistant.CaisseMouvementPersistant;
import appli.model.domaine.vente.persistant.CaissePersistant;
import framework.controller.ContextGloabalAppli;
import framework.model.beanContext.EtablissementPersistant;
import framework.model.common.service.MessageService;
import framework.model.common.util.BigDecimalUtil;
import framework.model.common.util.BooleanUtil;
import framework.model.common.util.ServiceUtil;
import framework.model.common.util.StrimUtil;
import framework.model.common.util.StringUtil;
import framework.model.util.FileUtil;
import framework.model.util.printGen.PrintCommunUtil;
import framework.model.util.printGen.PrintPosBean;
import framework.model.util.printGen.PrintPosDetailBean;

public class PrintTicketUtil {
    private PrintPosBean printBean;
    
    public PrintTicketUtil(CaisseMouvementPersistant caisseMvm, CarteFideliteClientPersistant dataFidelite) {
    	this.printBean = new PrintPosBean();
    	List<PrintPosDetailBean> listDataToPrint = buildMapData(caisseMvm, dataFidelite);
    	this.printBean.setListDetail(listDataToPrint);
    	
		EtablissementPersistant etablissementB = ContextAppli.getEtablissementBean();
		boolean isPrintLogo = StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("LOGO_TICKET"));
		Long restauId = etablissementB.getId();
		String startChemin = etablissementB.getId()+"/"+"restau/"+restauId;
		Map<String, byte[]> mapFilesLogo = FileUtil.getListFilesByte(startChemin);  
		startChemin = etablissementB.getId()+"/"+"paramTICK/"+restauId;
		Map<String, byte[]> mapFilesPub = FileUtil.getListFilesByte(startChemin);
		
		BigDecimal heightTicket = null;
        if(caisseMvm.getList_article() != null){
	        heightTicket = BigDecimalUtil.get(caisseMvm.getList_article().size());
	        
	        // Grand ticket ------------- ***
	        if(caisseMvm.getList_article().size() > 50) {
	        	heightTicket = BigDecimalUtil.get(""+(caisseMvm.getList_article().size()*1.7));
	        }
	        
	        if(isPrintLogo && mapFilesLogo.size() > 0){
	        	heightTicket = BigDecimalUtil.add(heightTicket, BigDecimalUtil.get(40));
	        }
	        // Ajouter les groupes
	        if(caisseMvm.getMax_idx_client() != null && caisseMvm.getMax_idx_client() > 2){
	        	heightTicket = BigDecimalUtil.add(heightTicket, BigDecimalUtil.get(caisseMvm.getMax_idx_client()*6));
	        }
	        
	        if(mapFilesPub.size() > 0){
	        	heightTicket = BigDecimalUtil.add(heightTicket, BigDecimalUtil.get(40));
	        }
        }
        this.printBean.setTicketHeight(heightTicket);
        
        CaissePersistant caisseBean = (CaissePersistant) MessageService.getGlobalMap().get("CURRENT_CAISSE");
    	if(caisseBean == null && caisseMvm.getOpc_caisse_journee() != null) {
    		caisseBean = caisseMvm.getOpc_caisse_journee().getOpc_caisse();
    	}
    	
    	if(caisseBean == null){
    		return;
    	}
    	
    	int nbr_ticket = caisseBean.getNbr_ticket()==null?1:caisseBean.getNbr_ticket();
		this.printBean.setNbrTicket(nbr_ticket);
    	this.printBean.setPrinters(caisseBean.getImprimantes());
    }
    
    public PrintPosBean getPrintPosBean(){
    	return this.printBean;
    }
    
    /**
     * @return
     */
    private List<PrintPosDetailBean> buildMapData(CaisseMouvementPersistant mouvement, 
    		CarteFideliteClientPersistant dataFidelite) {
    	boolean isCloseSaisieQte = StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("CLAC_NBR_SHOW"));
    	EtablissementPersistant restaurantP = ContextAppli.getEtablissementBean();
    	boolean isPrintLogo = StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("LOGO_TICKET"));
    	
    	Font smallTxt = new Font("Roman", Font.PLAIN, StringUtil.isEmpty(ContextGloabalAppli.getGlobalConfig("TICKET_FONT_SMALL")) ? 7 : Integer.valueOf(ContextGloabalAppli.getGlobalConfig("TICKET_FONT_SMALL")));
    	Font bigTxt = new Font("Roman", Font.PLAIN, StringUtil.isEmpty(ContextGloabalAppli.getGlobalConfig("TICKET_FONT_BIG")) ? 8 : Integer.valueOf(ContextGloabalAppli.getGlobalConfig("TICKET_FONT_SMALL")));
    	Font smallTxtB = new Font("Roman", Font.BOLD, StringUtil.isEmpty(ContextGloabalAppli.getGlobalConfig("TICKET_FONT_SMALL")) ? 7 : Integer.valueOf(ContextGloabalAppli.getGlobalConfig("TICKET_FONT_SMALL")));
    	Font bigTxtB = new Font("Roman", Font.BOLD, StringUtil.isEmpty(ContextGloabalAppli.getGlobalConfig("TICKET_FONT_BIG")) ? 8 : Integer.valueOf(ContextGloabalAppli.getGlobalConfig("TICKET_FONT_SMALL")));
    	
    	int ecartEntete = StringUtil.isEmpty(ContextGloabalAppli.getGlobalConfig("ECART_ENTETE_TICKET")) ? 10 : Integer.valueOf(ContextGloabalAppli.getGlobalConfig("ECART_ENTETE_TICKET"));
    	int carcRetourLigne = StringUtil.isEmpty(ContextGloabalAppli.getGlobalConfig("BACKLINE_TICKET")) ? 50 : Integer.valueOf(ContextGloabalAppli.getGlobalConfig("BACKLINE_TICKET"));
    	
    	this.printBean.setMaxLineLength(carcRetourLigne);
    	
		Long restauId = restaurantP.getId();
		String startCheminLogo = restaurantP.getId()+"/restau/"+restauId;
		Map<String, byte[]> mapFilesLogo = FileUtil.getListFilesByte(startCheminLogo);  
		String startCheminPub = restaurantP.getId()+"/paramTICK/"+restauId;
		Map<String, byte[]> mapFilesPub = FileUtil.getListFilesByte(startCheminPub);
		
    	Map<String, String> mapConfig = (Map<String, String>) MessageService.getGlobalMap().get("GLOBAL_CONFIG");
    	if(mapConfig == null){
    		mapConfig = new HashMap<String, String>();
    	}
    	
        List<CaisseMouvementArticlePersistant> listMvm = (mouvement.getListEncaisse()!=null && mouvement.getListEncaisse().size()>0) ? mouvement.getListEncaisse():mouvement.getList_article();
        Map<String, String> ENTETE_TEXT = new LinkedHashMap<>();
        if (StringUtil.isNotEmpty(mapConfig.get(PARAM_APPLI_ENUM.TEXT_ENTETE_TICKET_1.toString()))) {
        	ENTETE_TEXT.put("ENT1", mapConfig.get(PARAM_APPLI_ENUM.TEXT_ENTETE_TICKET_1.toString()));
        }
        if (StringUtil.isNotEmpty(mapConfig.get(PARAM_APPLI_ENUM.TEXT_ENTETE_TICKET_2.toString()))) {
        	ENTETE_TEXT.put("ENT2", mapConfig.get(PARAM_APPLI_ENUM.TEXT_ENTETE_TICKET_2.toString()));
        }

        if (StringUtil.isTrue(mapConfig.get(PARAM_APPLI_ENUM.ADRESSE_ETABLISSEMENT.toString()))  
                    && StringUtil.isNotEmpty(restaurantP.getAdresse())){
        	ENTETE_TEXT.put("ADR", restaurantP.getAdresse());
        }
        if (StringUtil.isTrue(mapConfig.get(PARAM_APPLI_ENUM.ICE.toString()))  
                && StringUtil.isNotEmpty(restaurantP.getNumero_ice())) {
        	ENTETE_TEXT.put("ICE", "ICE : "+restaurantP.getNumero_ice());
        }

         if (StringUtil.isTrue(mapConfig.get(PARAM_APPLI_ENUM.INFORMATION_CONTACT_PHONE.toString()))  
                && StringUtil.isNotEmpty(restaurantP.getTelephone())){
        	 ENTETE_TEXT.put("PHONE", "Téléphone : "+restaurantP.getTelephone());
        }

        boolean isPrintCom = StringUtil.isFalseOrNull(mapConfig.get("COM_ARTICLE")); 
         
        if (StringUtil.isTrue(mapConfig.get(PARAM_APPLI_ENUM.INFORMATION_CONTACT_MAIL.toString()))  
                && StringUtil.isNotEmpty(restaurantP.getMail())){
        	ENTETE_TEXT.put("MAIL", "Mail : "+restaurantP.getMail());
        }

        boolean isRestau = SOFT_ENVS.restau.toString().equals(StrimUtil.getGlobalConfigPropertie("context.soft"));
        boolean isShowNum = StringUtil.isTrue(mapConfig.get(PARAM_APPLI_ENUM.NUM_ARTICLE.toString()));
        List<PrintPosDetailBean> listPrintLinrs = new ArrayList<>();
        
        try {
            /* Draw Header */
            int y = ecartEntete; // Décalage par rapport au logo

            // Entête
            if(ENTETE_TEXT.size() > 0){
	            for (String entete : ENTETE_TEXT.keySet()) {
	            	String value = ENTETE_TEXT.get(entete);
	            	
	                if("ENT1".equals(entete)){
	                	listPrintLinrs.add(new PrintPosDetailBean(value, 0, y, PrintCommunUtil.CUSTOM_FONT_11_B, "C"));
	                } else if("ENT2".equals(entete)){
	                	listPrintLinrs.add(new PrintPosDetailBean(value, 0, y, PrintCommunUtil.CUSTOM_FONT_9_B, "C"));
	                } else if("ADR".equals(entete)){
	                	listPrintLinrs.add(new PrintPosDetailBean(value, 0, y, PrintCommunUtil.CUSTOM_FONT_8, "C"));
	                } else if("ICE".equals(entete)){
	                	listPrintLinrs.add(new PrintPosDetailBean(value, 0, y, PrintCommunUtil.CUSTOM_FONT_8, "C"));
	                } else if("PHONE".equals(entete)){
	                	listPrintLinrs.add(new PrintPosDetailBean(value, 0, y, PrintCommunUtil.CUSTOM_FONT_8_B, "C"));
	                } else if("MAIL".equals(entete)){
	                	listPrintLinrs.add(new PrintPosDetailBean(value, 0, y, PrintCommunUtil.CUSTOM_FONT_8, "C"));
	                }
	                y = y + 10;
	            }
    			y = y + 5;
    		}
            // Remettre la police
            // Logo image --------------------------------------------------------------
    		if(isPrintLogo && mapFilesLogo != null && mapFilesLogo.size() > 0){
				try {
					restauId = restaurantP.getId();
					File file = new File(StrimUtil.BASE_FILES_PATH+"/"+startCheminLogo+"/"+mapFilesLogo.keySet().iterator().next());
					BufferedImage read = ImageIO.read(file);
					int xImg = 10; // print start at 100 on x axies
					int imagewidth = read.getWidth();
					int imageheight = read.getHeight();
					// Max 200
					if(imagewidth > 200){
						Dimension imgSize = new Dimension(read.getWidth(), read.getHeight());
						Dimension boundary = new Dimension(200, 200);
						
						Dimension ratioSize = PrintCommunUtil.getScaledDimension(imgSize, boundary);
						
						imagewidth = ratioSize.width;
						imageheight = ratioSize.height;
					}
					xImg = ((PrintCommunUtil.PAPER_WIDTH - imagewidth) / 2)+15;
					//
					listPrintLinrs.add(new PrintPosDetailBean(file, xImg, y, imagewidth, imageheight, "C"));
					y = y + imageheight + 5;
				} catch (IOException e) {
					e.printStackTrace();
				}
				y = y + 10;
    		}
    		// ----------------------------------------------------------------------------
    		Date dateEnc = new Date();
    		if(mouvement.getDate_encais() != null) {
    			dateEnc = mouvement.getDate_encais();
    		}
    		String infosH = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(dateEnc);
    		if(mouvement.getOpc_user() != null){
    			String caissier = "";
				EmployePersistant opc_employe = mouvement.getOpc_user().getOpc_employe();
				if(opc_employe != null) {
					caissier = opc_employe.getPrenom();
				} else {
					caissier = mouvement.getOpc_user().getLogin();
				}
				
    			infosH += "           CAISSIER : " +caissier;
            }
            listPrintLinrs.add(new PrintPosDetailBean(infosH, 10, y, smallTxt));
            y = y + 10;

            // Table
            Map<String, Integer> nbrCouvertTable = new HashMap<>();
            List<CaisseMouvementArticlePersistant> listArt = mouvement.getList_article();
           for(CaisseMouvementArticlePersistant art : listArt) {
           	if(art.getNbr_couvert() != null){
           		nbrCouvertTable.put(art.getRef_table(), art.getNbr_couvert());
           	 }
           	}
           String refTables = mouvement.getRefTablesDetail();
           String couverts = nbrCouvertTable.get(refTables)!=null?" ("+nbrCouvertTable.get(refTables)+" couverts)":"";

            if(StringUtil.isNotEmpty(refTables)){
            	 listPrintLinrs.add(new PrintPosDetailBean(refTables+couverts, 10, y, smallTxt));
                 y = y + 10;
            }
           

            // Serveur
            if(mouvement.getOpc_serveur() != null){
            	String serveur = mouvement.getOpc_serveur().getLogin();
				
                listPrintLinrs.add(new PrintPosDetailBean("SERVEUR : " + 
                		serveur, 10, y, smallTxt));
                y = y + 10;
            }
            
            // Marquer commande non terminée
            String[] tempStatus = {
            		ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.TEMP.toString(), 
            		ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.ANNUL.toString()
            	};
            
            if(mouvement.getLast_statut() != null && StringUtil.contains(mouvement.getLast_statut(), tempStatus)){
                String statutCmd = "** COMMANDE NON VALIDEE **";
                //
            	if(ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.ANNUL.toString().equals(mouvement.getLast_statut())){
                	statutCmd = "** COMMANDE ANNULLEE **";
            	} else if(StringUtil.isEmpty(mouvement.getMode_paiement())){
            		statutCmd = "** COMMANDE NON REGLEE **";
            	}
            	y = y + 10;
                listPrintLinrs.add(new PrintPosDetailBean(statutCmd, 0, y, PrintCommunUtil.CUSTOM_FONT_12_B, "C"));
                y = y + 15;
            } else{
            	boolean isOffert = (BigDecimalUtil.isZero(mouvement.getMtt_commande_net()) && !BigDecimalUtil.isZero(mouvement.getMtt_reduction()));
            	 if(isOffert){
             		String statutCmd = "** COMMANDE OFFERTE **";
             		y = y + 10;
             		listPrintLinrs.add(new PrintPosDetailBean(statutCmd, 0, y, PrintCommunUtil.CUSTOM_FONT_12_B, "C"));
             		y = y + 15;
            	 }
            }
            
            String refCmd = (mouvement.getRef_commande().length()>12 ? mouvement.getRef_commande().substring(12) : mouvement.getRef_commande());
            
			if(isRestau){
            	y = y + 10;
            	listPrintLinrs.add(new PrintPosDetailBean("COMMANDE : *** " + refCmd+" ***", 10, y, new Font("Arial", Font.PLAIN, 13), "C"));
            	y = y + 15;
            } else{
            	y = y + 5;
            	listPrintLinrs.add(new PrintPosDetailBean("COMMANDE : " + refCmd, 10, y, new Font("Arial", Font.PLAIN, 10), "L"));
            	y = y + 10;
            }
            
            // Numéro token commande
            if(StringUtil.isNotEmpty(mouvement.getNum_token_cmd())){     
                listPrintLinrs.add(new PrintPosDetailBean("COASTER CALL : " + mouvement.getNum_token_cmd(), 10, y, smallTxt));
                y = y + 10;
            }

            if(!ContextAppli.TYPE_COMMANDE.L.toString().equals(mouvement.getType_commande())){
                // Code WIFI
                if(StringUtil.isNotEmpty(mapConfig.get(PARAM_APPLI_ENUM.WIFI.toString()))){     
                    listPrintLinrs.add(new PrintPosDetailBean("** CODE WIFI : " + mapConfig.get(PARAM_APPLI_ENUM.WIFI.toString()), 10, y, smallTxt));
                    y = y + 10;
                }
            }
            
        	UserPersistant livreur = mouvement.getOpc_livreurU();
        	if(livreur != null){
        		String strCli = "LIVREUR : " + 
        				StringUtil.firstCharToUpperCase(livreur.getLogin());
				listPrintLinrs.add(new PrintPosDetailBean(strCli, 10, y, smallTxt));
				y = y + 10;
        	}
        	
			ClientPersistant opc_client = mouvement.getOpc_client();
			if(opc_client != null){
				String strCli = "CLIENT : " + opc_client.getNom()+" "+StringUtil.getValueOrEmpty(opc_client.getPrenom());
				listPrintLinrs.add(new PrintPosDetailBean(strCli, 10, y, smallTxt));
				if (StringUtil.isNotEmpty(opc_client.getAdresse_rue())) {
					y = y + 10;
					listPrintLinrs.add(new PrintPosDetailBean(opc_client.getAdresse_rue(), 10, y, PrintCommunUtil.CUSTOM_FONT_9_B));
				}
				if (StringUtil.isNotEmpty(opc_client.getAdresse_compl())) {
					y = y + 10;
					listPrintLinrs.add(new PrintPosDetailBean(opc_client.getAdresse_compl(), 10, y, PrintCommunUtil.CUSTOM_FONT_9_B));
				}
				if (StringUtil.isNotEmpty(opc_client.getVilleStr())) {
					y = y + 10;
					listPrintLinrs.add(new PrintPosDetailBean(" - " + opc_client.getVilleStr(), 10, y, PrintCommunUtil.CUSTOM_FONT_9_B));
				}
				if(StringUtil.isNotEmpty(opc_client.getTelephone())){
					y = y + 10;
					listPrintLinrs.add(new PrintPosDetailBean("Tél 1 : "+opc_client.getTelephone(), 10, y, PrintCommunUtil.CUSTOM_FONT_9_B));
				}
				if(StringUtil.isNotEmpty(opc_client.getTelephone2())){
					y = y + 10;
					listPrintLinrs.add(new PrintPosDetailBean("Tél 2 : "+opc_client.getTelephone2(), 10, y, PrintCommunUtil.CUSTOM_FONT_9_B));
				}
			    y = y + 10;
			}
            
            // Type de commande
	        if(isRestau){
	            String typeCmd = "";
	            if(StringUtil.isNotEmpty(mouvement.getType_commande())){
		            if(ContextAppli.TYPE_COMMANDE.E.toString().equals(mouvement.getType_commande())){
		                typeCmd = "A EMPORTER";
		            } else if(ContextAppli.TYPE_COMMANDE.P.toString().equals(mouvement.getType_commande())){
		                 typeCmd = "SUR PLACE";
		            } else{
		                typeCmd = "LIVRAISON";
		            }
	            }
	            
	            y = y + 5;
	            listPrintLinrs.add(new PrintPosDetailBean(typeCmd, 10, y, PrintCommunUtil.CUSTOM_FONT_9_B, "C"));
	            y = y + 10;
            }
            
            // Text publicité ****************************************************************
            if (StringUtil.isNotEmpty(restaurantP.getTitre_publicite())) {
                y = y + 10;
                listPrintLinrs.add(new PrintPosDetailBean(restaurantP.getTitre_publicite(), 0, y, smallTxt, "C"));
            }
            
            if (StringUtil.isNotEmpty(restaurantP.getMsg_publicite())) {
            	String[] lines = restaurantP.getMsg_publicite().split("\\r?\\n");
                for (String line : lines) {
                    y = y + 10;
                    listPrintLinrs.add(new PrintPosDetailBean(line, 0, y, smallTxt, "C"));
                }
                y = y + 20;
            }
            
            // Image publicité --------------------------------------------------------------
    		if(mapFilesPub != null && mapFilesPub.size() > 0){
    			y = y + 10;
                // Image
				try {
					File file = new File(StrimUtil.BASE_FILES_PATH+"/"+startCheminPub+"/"+mapFilesPub.keySet().iterator().next());
					BufferedImage read = ImageIO.read(file);
					int xImg = 10; // print start at 100 on x axies
					int imagewidth = read.getWidth();
					int imageheight = read.getHeight();
					// Max 200
					if(imagewidth > 200){
						Dimension imgSize = new Dimension(read.getWidth(), read.getHeight());
						Dimension boundary = new Dimension(200, 200);
						
						Dimension ratioSize = PrintCommunUtil.getScaledDimension(imgSize, boundary);
						
						imagewidth = ratioSize.width;
						imageheight = ratioSize.height;
					}
					xImg = ((PrintCommunUtil.PAPER_WIDTH - imagewidth) / 2)+15;
					//
					listPrintLinrs.add(new PrintPosDetailBean(file, xImg, y, imagewidth, imageheight, "C"));
					y = y + imageheight + 5;
				} catch (IOException e) {
					e.printStackTrace();
				}
				y = y + 10;
    		}
            //--------------------------------------- FIN PUBLICITE ----------------------------------------
            listPrintLinrs.add(new PrintPosDetailBean(0, y, 220, y));
            y = y + 10;

            String[] colNames = {"Article", "Qte", "Montant"};
            int[] colonnePosition = {5, 150, 170};

            for (int i = 0; i < colNames.length; i++) {
                listPrintLinrs.add(new PrintPosDetailBean(colNames[i], colonnePosition[i], y, smallTxt));
            }

            y = y + 10;
            listPrintLinrs.add(new PrintPosDetailBean(0, y, 220, y));

            //---------- On collecte les taux de TVA -----------------------
            int X_MTT_START = 200;
            int X_QTE_START = (isCloseSaisieQte ? 165 : 155);

            int nbrNiveau = 0;
            Integer idxArticle = 0;
            BigDecimal sousTotal = null;
            CaisseMouvementArticlePersistant ligneLivraison = null;
            
            // Recenser les clients
            List<Integer> listIdxClient = new ArrayList<>();
            for(CaisseMouvementArticlePersistant caisseMvmP : listMvm){
            	if(BooleanUtil.isTrue(caisseMvmP.getIs_annule())){
            		continue;
            	}
            	if(!listIdxClient.contains(caisseMvmP.getIdx_client()) && caisseMvmP.getIdx_client() != null){
            		listIdxClient.add(caisseMvmP.getIdx_client());
            	}
            }
            Collections.sort(listIdxClient);
            
            if(listIdxClient != null && listIdxClient.size()>0 && listIdxClient.get(listIdxClient.size()-1) > mouvement.getMax_idx_client()){
            	mouvement.setMax_idx_client(listIdxClient.get(listIdxClient.size()-1));
        	}
            
            // Ventiler les offres paramétrées pour être ventillée ---------------------
            boolean isVentil = false;
            BigDecimal tauxReductVentil = null;
            if(mouvement.getList_offre() != null){
		    	for(CaisseMouvementOffrePersistant offre : mouvement.getList_offre()){
		    		if(BooleanUtil.isTrue(offre.getOpc_offre().getIs_ventil())){
		    			BigDecimal taux = offre.getOpc_offre().getTaux_reduction();
		    			tauxReductVentil = BigDecimalUtil.add(tauxReductVentil, taux);
		    			isVentil = true;
		    		}
		    	}
            }
	    	// --------------------------------------------------------------------------
            
            // Les articles -------------------------------------------
            BigDecimal _MTT_TOTAL_CALCULE = BigDecimalUtil.ZERO;
            BigDecimal _MTT_TOTAL_CALCULE_HD = BigDecimalUtil.ZERO;// Hors devise
            BigDecimal _MTT_TOTAL_CALCULE_D = BigDecimalUtil.ZERO;// Devise
          //---------Gestion devise-------------------------
        	boolean isDevise = !BigDecimalUtil.isZero(BigDecimalUtil.get(ContextGloabalAppli.getGlobalConfig("DEVISE_TAUX")));
        	BigDecimal tauxDevise = (isDevise ? BigDecimalUtil.get(ContextGloabalAppli.getGlobalConfig("DEVISE_TAUX")) : null);
        	//----------------------------------
        	
	    	BigDecimal totalIfVentil = null;
            for(int i=1; i<=mouvement.getMax_idx_client(); i++){
            	if(!listIdxClient.contains(i)){
            		continue;
            	}
            	//
            	if(listIdxClient.size() > 1 && i != listIdxClient.get(0)){
 	    		    y = y + 10;
 	    		   listPrintLinrs.add(new PrintPosDetailBean(0, y, 220, y));
 	    		    y = y + 10;
 	    		    listPrintLinrs.add(new PrintPosDetailBean("SOUS TOTAL GROUPE "+(i-1), 0, y, smallTxt));
 	                listPrintLinrs.add(new PrintPosDetailBean(BigDecimalUtil.formatNumber(sousTotal), X_MTT_START, y, smallTxt, "R"));
 	                y = y + 10;
            	}
            	sousTotal = null;
            	
            	
            	if(listIdxClient.size()>1){
            		 y = y + 10;
            		 listPrintLinrs.add(new PrintPosDetailBean("GROUPE "+i, 0, y, smallTxt));
            		 y = y + 10;
            		 listPrintLinrs.add(new PrintPosDetailBean(0, y, 220, y));
            		 y = y + 10;
            	}
            	
            	Integer NBR_NIVEAU_TICKET = StringUtil.isNotEmpty(mapConfig.get("NBR_NIVEAU_TICKET"))?Integer.valueOf(mapConfig.get("NBR_NIVEAU_TICKET")):null;
            	
            	// Détail des articles
                for (CaisseMouvementArticlePersistant detail : listMvm) {
                    if((detail.getIs_annule() != null && detail.getIs_annule()) || (detail.getIdx_client()!=null && detail.getIdx_client()!=i)){
                        continue;
                    }
                    boolean isDeviseLine = (BooleanUtil.isTrue(detail.getIs_devise()) || BooleanUtil.isTrue(mouvement.getIs_devise()));
                    //
					if("LIVRAISON".equals(detail.getType_ligne())){
						ligneLivraison = detail;
						continue;
					}
					BigDecimal prix = detail.getMtt_total();			    	
			    	//
					if(!BigDecimalUtil.isZero(tauxReductVentil) && !BigDecimalUtil.isZero(prix)){
						BigDecimal mttOffee = BigDecimalUtil.divide(BigDecimalUtil.multiply(prix, tauxReductVentil), BigDecimalUtil.get(100));
						prix = BigDecimalUtil.add(prix, mttOffee.negate()).setScale(0, RoundingMode.UP);// Arondir sans virgule
					}
					//
					totalIfVentil = BigDecimalUtil.add(totalIfVentil, prix);
					
                    sousTotal = BigDecimalUtil.add(sousTotal, prix);
                    
                    String article = detail.getLibelle();
                    String type = detail.getType_ligne();
                    
                    boolean isArtMenu = type.equals(ContextAppli.TYPE_LIGNE_COMMANDE.ART_MENU.toString());
					boolean isArt = type.equals(ContextAppli.TYPE_LIGNE_COMMANDE.ART.toString());

                    int startLine = 0;
                    Font font = bigTxt;// Par défaut
                    // MENUS ----------------------------------
                    
                    if(isShowNum) {
	                    if(type.equals(TYPE_LIGNE_COMMANDE.MENU.toString()) && (detail.getLevel() == null || detail.getLevel() > 1)) {
	                        idxArticle++;
	                        article = idxArticle + "-" + article;
	                    } else if(type.equals(TYPE_LIGNE_COMMANDE.ART.toString())){
	                        idxArticle++;
	                        article = idxArticle + "-" + article;
	                    }
                    }
                    
                    if(type.equals(ContextAppli.TYPE_LIGNE_COMMANDE.MENU.toString())) {
                    	font = bigTxtB;
                        startLine = 0;
                        nbrNiveau++;
                    } else if(type.equals(ContextAppli.TYPE_LIGNE_COMMANDE.GROUPE_MENU.toString())){
                    	font = smallTxtB;
                        startLine = 10;
                        nbrNiveau++;
                    } else if(detail.getMenu_idx() == null && type.equals(ContextAppli.TYPE_LIGNE_COMMANDE.GROUPE_FAMILLE.toString())){
                    	font = smallTxtB;
                        startLine = (detail.getLevel()!=null?detail.getLevel()*3:0);
                        nbrNiveau++;
                    } else if(detail.getMenu_idx() != null && type.equals(ContextAppli.TYPE_LIGNE_COMMANDE.GROUPE_FAMILLE.toString())){
                    	font = smallTxtB;
                        startLine = (detail.getLevel()!=null?detail.getLevel()*3:0);
                        nbrNiveau++;
                    } else if(isArtMenu) {
					    startLine = 20;
					    nbrNiveau = 0;
					} else if(isArt){
					    startLine = 10;
					    nbrNiveau = 0;
					} 

                    boolean isArticle = (isArt || isArtMenu);
                    boolean isToAdd = ((isArticle || NBR_NIVEAU_TICKET == null || nbrNiveau <= NBR_NIVEAU_TICKET) && !detail.getLibelle().startsWith("#"))
                    				|| !BigDecimalUtil.isZero(prix) ;
                    
                    if(isToAdd){
	                    BigDecimal quantite = detail.getQuantite();
	                    String prixStr = BigDecimalUtil.formatNumber(prix);
	                    String qte = "";
	                    
	                    if(quantite != null) {
		                    if(isCloseSaisieQte) {
		                    	qte = ""+quantite.intValue();
		                    } else {
		                    	qte = BigDecimalUtil.formatNumber(quantite);
		                    }
	                    }
	                    
	                    y = y + 5; // shifting drawing line
	
	                    // Afficher l'article et gérer le retour à la ligne
                    	y = y + 5;
                    	
                    	if(isDeviseLine) {
                    		article = article + "["+BigDecimalUtil.formatNumber(BigDecimalUtil.divide(BigDecimalUtil.get(prixStr), tauxDevise))+"€]";
                    	}
                    	
                    	boolean isOffert = BooleanUtil.isTrue(detail.getIs_offert());
						if(isOffert){
                    		article = article+"[**"+prixStr+"]";
                    	}
                    	
                    	//------------------------------------------------------------------------
                    	int MAX_STR_LENGTH = this.printBean.getMaxLineLength();
                    	int nbrLigne = Math.abs(article.length()/MAX_STR_LENGTH)+1 ;
                        if(nbrLigne > 1){
                        	for(int j=0; j<nbrLigne; j++){ 
                        		if(j > 0){
                        			y = y + 10;
                        		}
                        		int endLine = (j*MAX_STR_LENGTH)+MAX_STR_LENGTH > article.length() ? article.length() : (j*MAX_STR_LENGTH)+MAX_STR_LENGTH;
                        		String txtTrun = article.substring(j*MAX_STR_LENGTH, endLine);
                        		//
                        		listPrintLinrs.add(new PrintPosDetailBean(txtTrun, startLine+(j==0?0:2), y, font));
                        	}
                        } else{
                        	listPrintLinrs.add(new PrintPosDetailBean(article, startLine, y, font));
                        }
                        // ---------------------------------------------------------------------
	                    boolean isPrix = StringUtil.isNotEmpty(prixStr) && BigDecimalUtil.ZERO.compareTo(prix) != 0;
	                    //
	                    if(isPrix || (!isArticle && StringUtil.isNotEmpty(qte) && !qte.equals("1,00") && !qte.equals("1") && !qte.equals("0")) || isArticle){
	                    	listPrintLinrs.add(new PrintPosDetailBean(qte, X_QTE_START, y, font, "R"));
	                    }
						if(isPrix){
							String finalPr = (isOffert ? "Offert" : prixStr);
							listPrintLinrs.add(new PrintPosDetailBean(finalPr, X_MTT_START, y, font, "R"));	
		                    
							// Securite total
		                    _MTT_TOTAL_CALCULE = BigDecimalUtil.add(_MTT_TOTAL_CALCULE, BigDecimalUtil.get(isOffert ? "0" : prixStr));
		                    
		                    if(isDeviseLine) {
		                    	_MTT_TOTAL_CALCULE_D = BigDecimalUtil.add(_MTT_TOTAL_CALCULE_D, BigDecimalUtil.divide(BigDecimalUtil.get(isOffert ? "0" : prixStr), tauxDevise));
		                    } else {
		                    	_MTT_TOTAL_CALCULE_HD = BigDecimalUtil.add(_MTT_TOTAL_CALCULE_HD, BigDecimalUtil.get(isOffert ? "0" : prixStr));
		                    }
	                    }
	                    
	                    if(isPrintCom && StringUtil.isNotEmpty(detail.getCommentaire())){
	                        y = y + 7; // shifting drawing line
	                        listPrintLinrs.add(new PrintPosDetailBean("**...."+detail.getCommentaire(), 0, y, font));
	                    }
                    }
                }
            }
            
            if(listIdxClient.size() > 1){
            	y = y + 10;
            	listPrintLinrs.add(new PrintPosDetailBean(0, y, 220, y));
	            y = y + 10;
            	listPrintLinrs.add(new PrintPosDetailBean("SOUS TOTAL GROUPE "+mouvement.getMax_idx_client(), 0, y, smallTxt));
                listPrintLinrs.add(new PrintPosDetailBean(BigDecimalUtil.formatNumber(sousTotal), X_MTT_START, y, smallTxt, "R"));
                y = y + 10;
            }
            
            // Ligne livraison
            if(ligneLivraison != null) {
            	y = y + 10;
            	// Aligner à droire
                listPrintLinrs.add(new PrintPosDetailBean(ligneLivraison.getLibelle(), 0, y, bigTxtB));
                listPrintLinrs.add(new PrintPosDetailBean("", X_QTE_START, y, bigTxtB));
               
                String mttLivraison = BigDecimalUtil.formatNumber(ligneLivraison.getMtt_total());
				listPrintLinrs.add(new PrintPosDetailBean(mttLivraison, X_MTT_START, y, bigTxtB, "R"));
            }

             // Les offres -------------------------------------------
            boolean isReduction = !BigDecimalUtil.isZero(mouvement.getMtt_reduction());
            if(isReduction){
                y = y + 10; // shifting drawing line
                BigDecimal mttReducN = mouvement.getMtt_reduction().negate();
                String lib = mttReducN.compareTo(BigDecimalUtil.ZERO) < 0 ? "Réduction" :"Majoration";
                listPrintLinrs.add(new PrintPosDetailBean("** "+lib, 0, y, smallTxt));
                
                String qte = ""+mouvement.getList_offre().size();
                
				String mtt = BigDecimalUtil.formatNumber(mttReducN);

                listPrintLinrs.add(new PrintPosDetailBean(qte, X_QTE_START, y, smallTxt, "R"));
                listPrintLinrs.add(new PrintPosDetailBean(mtt, X_MTT_START, y, smallTxt, "R"));
            }

            // Ligne séparateur
            y = y + 10;
            listPrintLinrs.add(new PrintPosDetailBean(0, y, 220, y));
            
            // Ligne total -------------------------------------
            if(isDevise && !BigDecimalUtil.isZero(_MTT_TOTAL_CALCULE_D)) {// Devise
                y = y + 15;
                PrintPosDetailBean printPosDetailBean = new PrintPosDetailBean("TOTAL DHS TTC", 60, y, PrintCommunUtil.CUSTOM_FONT_11_B);
                printPosDetailBean.setIsBackground(true);
    			listPrintLinrs.add(printPosDetailBean);
    			listPrintLinrs.add(new PrintPosDetailBean("", 138, y, bigTxtB));
    			
    			printPosDetailBean = new PrintPosDetailBean(BigDecimalUtil.formatNumber(_MTT_TOTAL_CALCULE_HD), X_MTT_START, y, PrintCommunUtil.CUSTOM_FONT_11_B, "R");
	        	printPosDetailBean.setIsBackground(true);
	        	listPrintLinrs.add(printPosDetailBean);
    			
    			
                y = y + 15;
                printPosDetailBean = new PrintPosDetailBean("TOTAL € TTC", 60, y, PrintCommunUtil.CUSTOM_FONT_11_B);
                printPosDetailBean.setIsBackground(true);
    			listPrintLinrs.add(printPosDetailBean);
    			listPrintLinrs.add(new PrintPosDetailBean("", 138, y, bigTxtB));
    			printPosDetailBean = new PrintPosDetailBean(BigDecimalUtil.formatNumber(_MTT_TOTAL_CALCULE_D), X_MTT_START, y, PrintCommunUtil.CUSTOM_FONT_11_B, "R");
	        	printPosDetailBean.setIsBackground(true);
	        	listPrintLinrs.add(printPosDetailBean);
	        	
            } else {
            	y = y + 15;
                PrintPosDetailBean printPosDetailBean = new PrintPosDetailBean("TOTAL TTC", 60, y, PrintCommunUtil.CUSTOM_FONT_11_B);
                printPosDetailBean.setIsBackground(true);
    			listPrintLinrs.add(printPosDetailBean);
    			
                listPrintLinrs.add(new PrintPosDetailBean("", 138, y, bigTxtB));
	            BigDecimal mttNetCmd = null;
	            
	            boolean isEncaissPartiel = (mouvement.getListEncaisse() != null && mouvement.getListEncaisse().size()>0);
	        	if(isEncaissPartiel){
	    			for (CaisseMouvementArticlePersistant cmd : mouvement.getListEncaisse()) {
	    				if(!BooleanUtil.isTrue(cmd.getIs_annule()) && !BooleanUtil.isTrue(cmd.getIs_offert())){
	    					mttNetCmd = BigDecimalUtil.add(mttNetCmd, cmd.getMtt_total());
	    				}
	    	    	}
	        	} else{
	        		mttNetCmd = mouvement.getMtt_commande_net();
	        	}
	        	
	        	String mtt = BigDecimalUtil.formatNumber(mttNetCmd);
	        	
	        	// Securité car déphasage parfois total avec celui de la commande
	        	if(_MTT_TOTAL_CALCULE.compareTo(mttNetCmd) > 0 
	        			&& !isEncaissPartiel
	        			&& !isReduction){
	        		mtt = BigDecimalUtil.formatNumber(_MTT_TOTAL_CALCULE);
	        	}
	        	
	        	if(BooleanUtil.isTrue(mouvement.getIs_retour()) && BigDecimalUtil.get(mtt).compareTo(BigDecimalUtil.ZERO) > 0) {
	        		mtt = BigDecimalUtil.formatNumber(BigDecimalUtil.get(mtt).negate());
	        	}
	            
	        	printPosDetailBean = new PrintPosDetailBean((isVentil ? BigDecimalUtil.formatNumber(totalIfVentil) : mtt), X_MTT_START, y, PrintCommunUtil.CUSTOM_FONT_11_B, "R");
	        	printPosDetailBean.setIsBackground(true);
	        	listPrintLinrs.add(printPosDetailBean);
	        
        	            
	            // Montant donnée
	            if(mouvement.getMtt_donne() != null){
	                y = y + 10;
	                listPrintLinrs.add(new PrintPosDetailBean("DONNÉE", 60, y, smallTxt));
	                listPrintLinrs.add(new PrintPosDetailBean("", 138, y, smallTxt));
	                
	                if(!BigDecimalUtil.isZero(mouvement.getMtt_donne_cb())) {// Carte
	                	 mtt = BigDecimalUtil.formatNumber(mouvement.getMtt_donne_cb());
	                     listPrintLinrs.add(new PrintPosDetailBean("Carte : "+mtt, X_MTT_START, y, smallTxt, "R"));
	                     y = y + 10;
	                }
	                if(!BigDecimalUtil.isZero(mouvement.getMtt_donne_cheque())) {//Chèque
	                	 mtt = BigDecimalUtil.formatNumber(mouvement.getMtt_donne_cheque());
	                     listPrintLinrs.add(new PrintPosDetailBean("Chèque : "+mtt, X_MTT_START, y, smallTxt, "R"));
	                     y = y + 10;
	                }
	                if(!BigDecimalUtil.isZero(mouvement.getMtt_donne_dej())) {// Déj
	                	 mtt = BigDecimalUtil.formatNumber(mouvement.getMtt_donne_dej());
	                     listPrintLinrs.add(new PrintPosDetailBean("Chèque déj. : "+mtt, X_MTT_START, y, smallTxt, "R"));
	                     y = y + 10;
	                }
	                if(!BigDecimalUtil.isZero(mouvement.getMtt_donne_point())) {// Point
	                	 mtt = BigDecimalUtil.formatNumber(mouvement.getMtt_donne_point());
	                     listPrintLinrs.add(new PrintPosDetailBean("Points : "+mtt, X_MTT_START, y, smallTxt, "R"));
	                     y = y + 10;
	                }
	                if(!BigDecimalUtil.isZero(mouvement.getMtt_portefeuille())) {// Portefeuille
	                	 mtt = BigDecimalUtil.formatNumber(mouvement.getMtt_portefeuille());
	                     listPrintLinrs.add(new PrintPosDetailBean("Portefeuille : "+mtt, X_MTT_START, y, smallTxt, "R"));
	                     y = y + 10;
	                }
	                if(!BigDecimalUtil.isZero(mouvement.getMtt_donne())) {// Espéces
	                	 mtt = BigDecimalUtil.formatNumber(mouvement.getMtt_donne());
	                     listPrintLinrs.add(new PrintPosDetailBean("Espèces : "+mtt, X_MTT_START, y, smallTxt, "R"));
	                     y = y + 10;
	                }
	                //--------------------------------------------------------------------------------------
	                // A rendre
	                mtt = BigDecimalUtil.formatNumber(mouvement.getMtt_a_rendre()); 
	                BigDecimal mttArendre = (isVentil ? BigDecimalUtil.substract(mouvement.getMtt_donne_all(), totalIfVentil) : mouvement.getMtt_a_rendre());
	                
	                if(mttArendre != null && mttArendre.compareTo(mouvement.getMtt_donne_all()) < 0){
	                	 // Montant à rendre
	                    y = y + 10;
	                    listPrintLinrs.add(new PrintPosDetailBean("A RENDRE", 60, y, smallTxt));
	                    listPrintLinrs.add(new PrintPosDetailBean("", 138, y, smallTxt));
	                    
	                	listPrintLinrs.add(new PrintPosDetailBean(BigDecimalUtil.formatNumber(mttArendre), X_MTT_START, y, smallTxt, "R"));
	                }
	            }

	            y = y + 10;
	        	PrintPosDetailBean pdB = new PrintPosDetailBean(0, y, 220, y);
	        	pdB.setType("LP");
	        	listPrintLinrs.add(pdB);
	        	y = y + 15; 
	        	
	            // Mode paiement
	            String paiementMode = StringUtil.getValueOrEmpty(mouvement.getMode_paiement());
	            paiementMode = paiementMode.replaceAll("RESERVE", "PORTEFEUILLE");
				listPrintLinrs.add(new PrintPosDetailBean("PAIEMENT : " +paiementMode , 0, y, smallTxt));
	            
	            // Ligne TVA
	            if(StringUtil.isNotEmpty(mapConfig.get(PARAM_APPLI_ENUM.TVA_VENTE.toString()))){
	            	// Formule = [prix TTC / (1 + taux)] * taux
	                BigDecimal tauxTva = BigDecimalUtil.get(mapConfig.get(PARAM_APPLI_ENUM.TVA_VENTE.toString()));
	                BigDecimal mttTva = BigDecimalUtil.multiply(
	                			BigDecimalUtil.divide(mttNetCmd, BigDecimalUtil.get("1"+tauxTva)), 
	                		tauxTva);
	                y = y + 10;
	                listPrintLinrs.add(new PrintPosDetailBean("TVA ("+tauxTva+"%) : "+BigDecimalUtil.formatNumber(mttTva), 0, y, smallTxt));
	            }
            }
            if(dataFidelite != null){
            	// Points utilisés
            	if(!BigDecimalUtil.isZero(mouvement.getMtt_donne_point())){
            		y = y + 10;
            		
            		String st = "** Points utilisés : "+BigDecimalUtil.formatNumber(mouvement.getNbr_donne_point())+" points" + 
    				" / " + BigDecimalUtil.formatNumber(mouvement.getMtt_donne_point()) + " Dhs";
            		listPrintLinrs.add(new PrintPosDetailBean(st, 0, y, smallTxt));
            	}
            	
            	int nbrPointRestant = BigDecimalUtil.divide(dataFidelite.getMtt_total(), dataFidelite.getOpc_carte_fidelite().getMtt_palier()).intValue();
            	BigDecimal mttRestant = dataFidelite.getMtt_total();
            	// Solde points
            	if(mouvement.getId() == null){// Si maj alors pas besoin de soustraire le montant en cours
            		nbrPointRestant = (nbrPointRestant - (mouvement.getNbr_donne_point()!=null?mouvement.getNbr_donne_point():0));
                	mttRestant = BigDecimalUtil.substract(mttRestant, mouvement.getMtt_donne_point());
            	}
            	
            	String st = "** Solde points : "+BigDecimalUtil.formatNumber(nbrPointRestant)+" points" + 
            				" / " + BigDecimalUtil.formatNumber(mttRestant) + " Dhs";
            	 y = y + 10;
            	listPrintLinrs.add(new PrintPosDetailBean(st, 0, y, smallTxt));
            }
            
            if(mouvement.getOpc_client() != null && !BigDecimalUtil.isZero(mouvement.getOpc_client().getSolde_portefeuille())){
            	IPortefeuille2Service portefeuille2Service = ServiceUtil.getBusinessBean(IPortefeuille2Service.class);
            	BigDecimal[] soldeChargeP = portefeuille2Service.getSoldePortefeuilleDetail(mouvement.getOpc_client().getId(), "CLI");
            	BigDecimal solde = null;
            	if(mouvement.getId() == null) {
            		solde = BigDecimalUtil.substract(soldeChargeP[0], BigDecimalUtil.add(mouvement.getMtt_portefeuille(), soldeChargeP[1]));	
            	} else {
            		solde = BigDecimalUtil.substract(soldeChargeP[0], soldeChargeP[1]);
            	}
            	
            	// Solde points
            	String st = "** Solde portefeuille : "+BigDecimalUtil.formatNumber(solde)+" Dhs";
            	 y = y + 10;
            	listPrintLinrs.add(new PrintPosDetailBean(st, 0, y, smallTxt));
            }
            
            // Pied de page --------------------------------------
            String PIED_TEXT = mapConfig.get(PARAM_APPLI_ENUM.TEXT_PIED_TICKET.toString());
            if (StringUtil.isNotEmpty(PIED_TEXT)) {
            	y = y + 10;
            	PrintPosDetailBean pdB = new PrintPosDetailBean(0, y, 220, y);
            	pdB.setType("LP");
            	listPrintLinrs.add(pdB);
            	y = y + 10; 
                listPrintLinrs.add(new PrintPosDetailBean(PIED_TEXT, 0, y, PrintCommunUtil.CUSTOM_FONT_9, "C"));
            }
            
            // Date impression
            y = y + 8;
            String infosDtP = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
            listPrintLinrs.add(new PrintPosDetailBean(infosDtP, 0, y, new Font("Monospaced", Font.PLAIN, 5), "C"));
            y = y + 5;
            
            // ------------------------------------------------------------------------------- CODE BARRE TEST
            boolean isPrintCodeBarre = StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("CODE_BARRE_TICKET"));
            if(isPrintCodeBarre){
	    		y = y + 5;
	    		// Code Barre
	    		generateAndPrintCodeBarreEan13(mouvement);
				try {
					restauId = restaurantP.getId();
					File file = new File(StrimUtil._GET_PATH("temp/codeBarre")+"/"+mouvement.getCode_barre()+".jpg");
					BufferedImage read = ImageIO.read(file);
					if(read != null){
						int xImg = 10; // print start at 100 on x axies
						int imagewidth = read.getWidth();
						int imageheight = read.getHeight();
						// Max 200
						if(imagewidth > 100){
							imagewidth = 130;
							imageheight = 35;
						}
						xImg = ((PrintCommunUtil.PAPER_WIDTH - imagewidth) / 2)+5;
						//
						listPrintLinrs.add(new PrintPosDetailBean(file, xImg, y, imagewidth, imageheight, "C"));
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
            }
            
            // end of the reciept
        } catch (Exception r) {
            r.printStackTrace();
        }

        return listPrintLinrs;
    }

    /**
     * Ouvrir le terroire uniquement
     */
//     public static void openCashDrawer(String imprimantes) {
//    	 String[] listImprimante = StringUtil.getArrayFromStringDelim(imprimantes, "|");
//    	 if(listImprimante == null || listImprimante.length == 0) {
//     		return;
//     	 }
//         String defaultImprimante = listImprimante[0];
//         
//        try {
//            ByteArrayOutputStream commandSet = new ByteArrayOutputStream();
//            final byte[] openCD = {27, 112, 0, 60, 120};
//            commandSet.write(openCD);
//            DocPrintJob job = null;
//            // Imprimante par défaut
//            try{
//                AttributeSet attrSet = new HashPrintServiceAttributeSet(new PrinterName(defaultImprimante, null));
//                PrintService[] lookupPrintServices = PrintServiceLookup.lookupPrintServices(null, attrSet);
//                
//                if(lookupPrintServices.length == 0){
//                	return;
//                }
//				job = lookupPrintServices[0].createPrintJob();
//            } catch (Exception ex) {
//                Logger.getLogger(PrintTicketUtil.class.getName()).log(Level.SEVERE, null, ex);
//            }
//            //
//            if(job == null){
//                job = PrintServiceLookup.lookupDefaultPrintService().createPrintJob();
//            }
//            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
//            Doc doc = new SimpleDoc(commandSet.toByteArray(), flavor, null);
//
//            job.print(doc, null);
//        } catch (PrintException | IOException ex) {
//            Logger.getLogger(PrintTicketUtil.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }     
     
     private void generateAndPrintCodeBarreEan13(CaisseMouvementPersistant mouvement) {     	
 		int xsFontStr =  5;
		String path = StrimUtil._GET_PATH("temp/codeBarre");
 		
 		Font smallFont = new Font("Roman", Font.PLAIN, xsFontStr);
		try {
			if(!new File(path).exists()){
				new File(path).mkdirs();
			}
			
			String codeStr = mouvement.getCode_barre();
			if(codeStr != null && codeStr.length() < 10){
				codeStr = "417"+codeStr;
				int codeCtrl = CHECK_SUM(codeStr);
				codeStr = codeStr + codeCtrl;
			}
			
			BarcodeUtil util = BarcodeUtil.getInstance();
		    BarcodeGenerator gen = util.createBarcodeGenerator(buildCfg("ean-13"));

		    OutputStream fout = new FileOutputStream(path + "/"+mouvement.getCode_barre()+".jpg");
		    int resolution = 400;
		    BitmapCanvasProvider canvas = new BitmapCanvasProvider(
		        fout, "image/jpeg", resolution, BufferedImage.TYPE_BYTE_BINARY, false, 0);
		    
		    gen.generateBarcode(canvas, codeStr);
		    canvas.finish();
		    fout.close();//Important
		    
        	// Ajouter du text à l'image
        	String pathImg = StrimUtil._GET_PATH("temp/codeBarre")+"/"+mouvement.getCode_barre()+".jpg";
           	BufferedImage ORIGINAL = ImageIO.read(new File(pathImg));
           	
           	// Resize ----------------------------------------------------------------------------------
           	GraphicsConfiguration config = 
           	        GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
           	
           	BufferedImage ALTERED = config.createCompatibleImage(
                     ORIGINAL.getWidth(), 
                     ORIGINAL.getHeight() + 20);
           	// --------------------------------------------------------------------------------
           	
           	Graphics g = ALTERED.createGraphics();
           	// Ajouter un espace de 20 px en haut de l'image
           	g.setColor(Color.WHITE);
           	g.fillRect(0, 0, ALTERED.getWidth(), 20);
           	g.drawImage(ORIGINAL, 0, 20, null);
           	//-----------------------------------------------------
           	g.setColor(Color.BLACK);
            g.setFont(smallFont);
            g.dispose();

            ImageIO.write(ALTERED, "jpg", new File(pathImg));
		} catch (Exception ex) {
			ex.printStackTrace();
//			throw new RuntimeException(ex);
		}
 	}
     
    private int CHECK_SUM (String Input) {
 		int evens = 0; //initialize evens variable
 		int odds = 0; //initialize odds variable
 		int checkSum = 0; //initialize the checkSum
 		for (int i = 0; i < Input.length(); i++) {
 			//check if number is odd or even
 			if ((int)Input.charAt(i) % 2 == 0) { // check that the character at position "i" is divisible by 2 which means it's even
 				evens += (int)Input.charAt(i);// then add it to the evens
 			} else {
 				odds += (int)Input.charAt(i); // else add it to the odds
 			}
 		}
 		odds = odds * 3; //multiply odds by three
 		int total = odds + evens; //sum odds and evens
 		if (total % 10 == 0){ //if total is divisible by ten, special case
 			checkSum = 0;//checksum is zero
 		} else { //total is not divisible by ten
 			checkSum = 10 - (total % 10); //subtract the ones digit from 10 to find the checksum
 		}
 		return checkSum;
 	}
     
     private Configuration buildCfg(String type) {
 	    DefaultConfiguration cfg = new DefaultConfiguration("barcode");

 	    //Bar code type
 	    DefaultConfiguration child = new DefaultConfiguration(type);
 	      cfg.addChild(child);
 	    
 	      //Human readable text position
 	      DefaultConfiguration attr = new DefaultConfiguration("human-readable");
 	      DefaultConfiguration subAttr = new DefaultConfiguration("placement");
 	        subAttr.setValue("bottom");
 	        attr.addChild(subAttr);
 	        
 	        child.addChild(attr);
 	        
 	        return cfg;
 	  }
}
