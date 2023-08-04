/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appli.model.domaine.caisse.service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.springframework.transaction.annotation.Transactional;

import appli.controller.domaine.administration.bean.JourneeBean;
import appli.controller.domaine.caisse.bean.CaisseBean;
import appli.controller.domaine.util_erp.ContextAppli;
import appli.controller.domaine.util_erp.ContextAppli.SOFT_ENVS;
import appli.controller.domaine.util_erp.ContextAppli.TYPE_ECRITURE;
import appli.model.domaine.administration.persistant.EcriturePersistant;
import appli.model.domaine.administration.persistant.EtatFinancePersistant;
import appli.model.domaine.administration.persistant.MailQueuePersistant;
import appli.model.domaine.administration.persistant.UserPersistant;
import appli.model.domaine.administration.service.IEtatFinanceService;
import appli.model.domaine.administration.service.IMailUtilService;
import appli.model.domaine.administration.service.impl.MailUtilService;
import appli.model.domaine.caisse.persistant.JourneeVenteView;
import appli.model.domaine.caisse.service.ICaisseService;
import appli.model.domaine.caisse.service.IJourneeService;
import appli.model.domaine.caisse.validator.JourneeValidator;
import appli.model.domaine.fidelite.dao.IPortefeuille2Service;
import appli.model.domaine.personnel.persistant.ClientPersistant;
import appli.model.domaine.stock.persistant.ArticlePersistant;
import appli.model.domaine.stock.persistant.FamilleCuisinePersistant;
import appli.model.domaine.stock.persistant.FamillePersistant;
import appli.model.domaine.stock.persistant.MouvementArticlePersistant;
import appli.model.domaine.stock.persistant.MouvementPersistant;
import appli.model.domaine.stock.service.IFamilleService;
import appli.model.domaine.stock.service.IMouvementService;
import appli.model.domaine.stock.service.impl.RepartitionBean;
import appli.model.domaine.vente.persistant.CaisseJourneePersistant;
import appli.model.domaine.vente.persistant.CaisseMouvementArticlePersistant;
import appli.model.domaine.vente.persistant.CaisseMouvementOffrePersistant;
import appli.model.domaine.vente.persistant.CaisseMouvementPersistant;
import appli.model.domaine.vente.persistant.CaisseMouvementTracePersistant;
import appli.model.domaine.vente.persistant.CaissePersistant;
import appli.model.domaine.vente.persistant.JourneePersistant;
import framework.controller.ContextGloabalAppli;
import framework.model.beanContext.CompteBancairePersistant;
import framework.model.beanContext.EtablissementPersistant;
import framework.model.common.annotation.validator.WorkModelClassValidator;
import framework.model.common.annotation.validator.WorkModelMethodValidator;
import framework.model.common.service.MessageService;
import framework.model.common.util.BigDecimalUtil;
import framework.model.common.util.BooleanUtil;
import framework.model.common.util.DateUtil;
import framework.model.common.util.DateUtil.TIME_ENUM;
import framework.model.common.util.ServiceUtil;
import framework.model.common.util.StrimUtil;
import framework.model.common.util.StringUtil;
import framework.model.service.GenericJpaService;

@Named
@WorkModelClassValidator(validator=JourneeValidator.class)
public class JourneeService extends GenericJpaService<JourneeBean, Long> implements IJourneeService{
	@Inject
	private IFamilleService familleService;
	@Inject
	private ICaisseService caisseService;
	@Inject
	private IEtatFinanceService etatFinanceService;
	@Inject
	private IMailUtilService mailService;
	@Inject
	private IPortefeuille2Service portefeuilleService2;
	
	@Override
	public JourneePersistant getLastJournee() {
		List<JourneePersistant> listJournee = getQuery("from JourneePersistant journee order by date_journee desc").getResultList();
		return (listJournee.size() > 0 ? listJournee.get(0) : null);
	}
	
	@Override
	public JourneePersistant getLastJourneeClose() {
		List<JourneePersistant> listJournee = getQuery("from JourneePersistant journee where statut_journee='C' order by date_journee desc").getResultList();
		return (listJournee.size() > 0 ? listJournee.get(0) : null);
	}
	
	@Override
	@Transactional
	@WorkModelMethodValidator
	public void cloturerJournee(Long journeeId, boolean isRecloture, BigDecimal soldeCoffre, boolean isFromCaisse) {
		JourneePersistant journeeP = findById(JourneePersistant.class, journeeId);
		journeeP.setStatut_journee(ContextAppli.STATUT_JOURNEE.CLOTURE.getStatut());
		journeeP.setSolde_coffre(soldeCoffre);
		// Mettre à jour infos vue
		setDataJourneeFromView(journeeP);
		
		// Décaisser les frais de livraison
		if(StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("LIVRAISON"))) {
			decaisserLivraison(journeeId);
		}
		// Maj
		EntityManager entityManager = getEntityManager();
		entityManager.merge(journeeP);
		entityManager.flush();
		
		// Ecritures
		ajouterEcrituresJournee(journeeP);
		
		// Envoi mail info
		String mailsShifts = ContextGloabalAppli.getGlobalConfig("MAIL_ALERT_JOUR");
		if(StringUtil.isNotEmpty(mailsShifts)){
			Map<String, String> mapParams = new HashMap<>();
			mapParams.put("1", DateUtil.dateToString(journeeP.getDate_journee()));
			
			BigDecimal netCaisse = BigDecimalUtil.substract(journeeP.getMtt_total_net(), journeeP.getMtt_portefeuille(), journeeP.getMtt_donne_point());
			BigDecimal mttEcart = BigDecimalUtil.substract(
					journeeP.getMtt_cloture_caissier(),
					journeeP.getMtt_ouverture(),
	   				netCaisse);
			
			StringBuilder sb = new StringBuilder();
			sb.append("<table>");
			sb.append("<tr style='background-color:white;'>"
					+ "<td>FOND DE ROULEMENT : </td>"
					+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(journeeP.getMtt_ouverture())+"</td>"
					+ "</tr>");
			
			sb.append("<tr style='background-color:white;'>"
					+ "<td>TOTAL BRUT : </td>"
					+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(journeeP.getMtt_total())+"</td>"
					+ "</tr>");
			
			sb.append("<tr style='background-color:white;'>"
					+ "<td style='font-weight:bold;'>TOTAL NET : </td>"
					+ "<td style='text-align:right;font-weight:bold;'>"+BigDecimalUtil.formatNumber(journeeP.getMtt_total_net())+"</td>"
					+ "</tr>");
			sb.append("<tr><td>TOTAL CLOTURE : </td>"
					+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(journeeP.getMtt_cloture_caissier())+"</td><td></td></tr>");
			sb.append("<tr style='background-color:white;'>"
					+ "<td>ECART : </td>"
					+ "<td style='text-align:right;color:orange;'><b>"+BigDecimalUtil.formatNumber(mttEcart)+"</b></td>"
				+ "</tr>");
			
			if(!BigDecimalUtil.isZero(journeeP.getMtt_livraison())){
				sb.append("<tr style='background-color:white;'>"
						+ "<td>LIVRAISON : </td>"
						+ "<td>"+BigDecimalUtil.formatNumber(journeeP.getMtt_livraison())+"</td>"
					+ "</tr>");
			}
			if(!BigDecimalUtil.isZero(journeeP.getMtt_annule())){
				sb.append("<tr style='background-color:white;'>"
						+ "<td style='color:red;'>ANNULATION CMD : </td>"
						+ "<td style='text-align:right;color:red;'>"+BigDecimalUtil.formatNumber(journeeP.getMtt_annule())+"</td>"
					+ "</tr>");
			}
			if(!BigDecimalUtil.isZero(journeeP.getMtt_annule_ligne())){
				sb.append("<tr style='background-color:white;'>"
						+ "<td style='color:red;'>ANNULATION LIGNE : </td>"
						+ "<td style='text-align:right;color:red;'>"+BigDecimalUtil.formatNumber(journeeP.getMtt_annule_ligne())+"</td>"
					+ "</tr>");
			}
			if(!BigDecimalUtil.isZero(journeeP.getMtt_art_offert())){
				sb.append("<tr style='background-color:white;'>"
						+ "<td>OFFERT : </td>"
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(journeeP.getMtt_art_offert())+"</td>"
					+ "</tr>");
			}
			if(!BigDecimalUtil.isZero(journeeP.getMtt_art_reduction())){
				sb.append("<tr style='background-color:white;'>"
						+ "<td>REDUCTIONS ARTICLE : </td>"
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(journeeP.getMtt_art_reduction())+"</td>"
					+ "</tr>");
			}
			if(!BigDecimalUtil.isZero(journeeP.getMtt_reduction())){
				sb.append("<tr style='background-color:white;'>"
						+ "<td>REDUCTIONS : </td>"
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(journeeP.getMtt_reduction())+"</td>"
					+ "</tr>");
			}
			if(!BigDecimalUtil.isZero(journeeP.getMtt_donne_point())){
				sb.append("<tr style='background-color:white;'>"
						+ "<td>POINTS : </td>"
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(journeeP.getMtt_donne_point())+"</td>"
					+ "</tr>");
			}
			if(!BigDecimalUtil.isZero(journeeP.getMtt_portefeuille())){
				sb.append("<tr style='background-color:white;'>"
						+ "<td>PORTEFEUILLE : </td>"
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(journeeP.getMtt_portefeuille())+"</td>"
					+ "</tr>");
			}	
			
			sb.append("</table>");
			
   			BigDecimal mttEcartEsp = BigDecimalUtil.substract(journeeP.getMtt_cloture_caissier_espece(), journeeP.getMtt_espece(), journeeP.getMtt_ouverture());
			BigDecimal mttEcartCb = BigDecimalUtil.substract(journeeP.getMtt_cloture_caissier_cb(), journeeP.getMtt_cb());
			BigDecimal mttEcartChq = BigDecimalUtil.substract(journeeP.getMtt_cloture_caissier_cheque(), journeeP.getMtt_cheque());
			BigDecimal mttEcartDej = BigDecimalUtil.substract(journeeP.getMtt_cloture_caissier_dej(), journeeP.getMtt_dej());
			
			sb.append("<br>"
					+ "<br>"
					+ "<table>");
			
			
			sb.append("<tr style='background-color:orange;'>"
						+ "<td>MODE</td>"
						+ "<td>CALCUL SYSTEM</td>"
						+ "<td>CLOTURE CAISSIER</td>"
						+ "<td>ECART</td>"
					+ "</tr>");
			if(!BigDecimalUtil.isZero(journeeP.getMtt_espece())){
				sb.append("<tr style='background-color:white;'>"
						+ "<td>ESPECES : </td>"
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(journeeP.getMtt_espece())+"</td>"
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(journeeP.getMtt_cloture_caissier_espece())+"</td>"
						+ "<td style='text-align:right;color:orange;'>"+BigDecimalUtil.formatNumber(mttEcartEsp)+"</td>"
					+ "</tr>");
			}
			if(!BigDecimalUtil.isZero(journeeP.getMtt_cb())){
				sb.append("<tr style='background-color:white;'>"
						+ "<td>CARTE : </td>"
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(journeeP.getMtt_cb())+"</td>"
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(journeeP.getMtt_cloture_caissier_cb())+"</td>"
						+ "<td style='text-align:right;color:orange;'>"+BigDecimalUtil.formatNumber(mttEcartCb)+"</td>"
					+ "</tr>");
			}
			if(!BigDecimalUtil.isZero(journeeP.getMtt_cheque())){
				sb.append("<tr style='background-color:white;'>"
						+ "<td>CHEQUE : </td>"
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(journeeP.getMtt_cheque())+"</td>"
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(journeeP.getMtt_cloture_caissier_cheque())+"</td>"
						+ "<td style='text-align:right;color:orange;'>"+BigDecimalUtil.formatNumber(mttEcartChq)+"</td>"
					+ "</tr>");
			}
			if(!BigDecimalUtil.isZero(journeeP.getMtt_dej())){
				sb.append("<tr style='background-color:white;'>"
						+ "<td>CHEQUE DEJ. : </td>"
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(journeeP.getMtt_dej())+"</td>"
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(journeeP.getMtt_cloture_caissier_dej())+"</td>"
						+ "<td style='text-align:right;color:orange;'>"+BigDecimalUtil.formatNumber(mttEcartDej)+"</td>"
					+ "</tr>");
			}
			
			sb.append("</table>");
			
			
			sb.append("<br>"
					+ "<br>"
					+ "<table>");
				BigDecimalUtil.formatNumberZero(BigDecimalUtil.substract(journeeP.getMtt_cloture_caissier(), journeeP.getMtt_ouverture()));
			
			sb.append("</table>");
			
			mapParams.put("2", sb.toString());
			
			MailQueuePersistant mail = new MailQueuePersistant();
			mail.setDestinataires(mailsShifts);
			mail.setExpediteur_mail(StrimUtil.getGlobalConfigPropertie("mail.sender"));
			mail.setExpediteur_nom("Caisse Manager");
			mail.setDate_mail(new Date());
			mail.setSource("JOURNEE");
			mail.setSujet("Chiffre journée "+DateUtil.dateToString(journeeP.getDate_journee()));
			mail.setMessage(MailUtilService.getMailContent(mapParams, "JOURNEE"));
			
			mail.setDate_mail(new Date());
			mail.setMail_signature("ADMIN");		
			mail.setDate_creation(new Date());
			//
			mailService.addMailToQueue(mail);
		}
	}
	
	@Override
	@Transactional
	public void ajouterEcrituresJournee(JourneePersistant journeeP){	
		List<CaisseJourneePersistant> listCaisseJournee = journeeP.getList_caisse_journee();
		for (CaisseJourneePersistant caisseJourneeP : listCaisseJournee) {
				ajouterEcritureCompte(ContextAppli.MODE_PAIEMENT.CARTE, caisseJourneeP.getMtt_cloture_caissier_cb(), caisseJourneeP);
				ajouterEcritureCompte(ContextAppli.MODE_PAIEMENT.CHEQUE, caisseJourneeP.getMtt_cloture_caissier_cheque(), caisseJourneeP);
				BigDecimal mttClotureNet = BigDecimalUtil.substract(caisseJourneeP.getMtt_cloture_caissier_espece(), caisseJourneeP.getMtt_ouverture());
				ajouterEcritureCompte(ContextAppli.MODE_PAIEMENT.ESPECES, mttClotureNet, caisseJourneeP);
				ajouterEcritureCompte(ContextAppli.MODE_PAIEMENT.DEJ, caisseJourneeP.getMtt_cloture_caissier_dej(), caisseJourneeP);
		}
		
	}

	/**
	 * @param mouvementBean
	 * @return
	 */
	@Transactional
	private void ajouterEcritureCompte(ContextAppli.MODE_PAIEMENT modePaiement, BigDecimal mtt, CaisseJourneePersistant caisseJourneeBean){
		CompteBancairePersistant compteBancaire = getCompteBancaire(modePaiement);
		
		if(compteBancaire == null){
			return;
		}
		String source = "CAISSE_"+modePaiement;
		
		// Purger les anciennes écritures
		getQuery("delete from EcriturePersistant where elementId=:elementId and date_mouvement=:dateMvm and source=:src "
							+ "and opc_banque.id=:compteBancaireId")
				.setParameter("elementId", caisseJourneeBean.getId())
				.setParameter("dateMvm", caisseJourneeBean.getOpc_journee().getDate_journee())
				.setParameter("compteBancaireId", compteBancaire.getId())
				.setParameter("src", source)
				.executeUpdate();
		
		getEntityManager().flush();
		
		if(!BigDecimalUtil.isZero(mtt)){
			  EcriturePersistant ecritureP = new EcriturePersistant();
		      ecritureP.setDate_mouvement(caisseJourneeBean.getOpc_journee().getDate_journee());
		      ecritureP.setElementId(caisseJourneeBean.getId());
		
		      String lib = "Vente caisse *"+caisseJourneeBean.getOpc_caisse().getReference() + "* shift de "+DateUtil.dateToString(caisseJourneeBean.getDate_ouverture(), "HH:mm:ss");
		      ecritureP.setLibelle(lib);
		      ecritureP.setMontant(mtt);
	      
		      ecritureP.setOpc_banque(compteBancaire);
		      
		      ecritureP.setSource(source);
		      ecritureP.setSens("C");
	
		      getEntityManager().merge(ecritureP);
		}
    }
	
    /**
     * 
     * @param modePaiement
     * @return 
     */
	private CompteBancairePersistant getCompteBancaire(ContextAppli.MODE_PAIEMENT modePaiement){
       if(modePaiement == null){
           return null;
       }
       Long compteId = null;
       // Conf du compte
       if(modePaiement.equals(ContextAppli.MODE_PAIEMENT.CARTE)){
           String compte = ContextGloabalAppli.getGlobalConfig(ContextAppli.PARAM_APPLI_ENUM.COMPTE_BANCAIRE_CARTE.toString());
           if(StringUtil.isNotEmpty(compte)){
               compteId = Long.valueOf(compte);
           }
       } else if(modePaiement.equals(ContextAppli.MODE_PAIEMENT.CHEQUE)){
           String compte = ContextGloabalAppli.getGlobalConfig(ContextAppli.PARAM_APPLI_ENUM.COMPTE_BANCAIRE_CHEQUE.toString());
           if(StringUtil.isNotEmpty(compte)){
               compteId = Long.valueOf(compte);
           }
       } else{
    	   String compteEsp = ContextGloabalAppli.getGlobalConfig(ContextAppli.PARAM_APPLI_ENUM.COMPTE_BANCAIRE_ESP_CAISSE.toString());
    	   if(StringUtil.isEmpty(compteEsp)) {
    		   compteEsp = ContextGloabalAppli.getGlobalConfig(ContextAppli.PARAM_APPLI_ENUM.COMPTE_BANCAIRE_CAISSE.toString());
    	   }
           if(StringUtil.isNotEmpty(compteEsp)){
               compteId = Long.valueOf(compteEsp);
           }
       }
       
       return compteId != null ? (CompteBancairePersistant)findById(CompteBancairePersistant.class, compteId) : null;
   } 
		
	@Transactional
	private BigDecimal decaisserLivraison(Long currJourneeId) {
		BigDecimal fraisLivraison = BigDecimalUtil.get(ContextGloabalAppli.getGlobalConfig("FRAIS_LIVRAISON"));
		BigDecimal fraisLivraisonPart = BigDecimalUtil.get(ContextGloabalAppli.getGlobalConfig("FRAIS_LIVRAISON_PART"));
		//
		JourneeVenteView journeeV = (JourneeVenteView) getSingleResult(getQuery("from JourneeVenteView where journee_id=:journeeId")
			.setParameter("journeeId", currJourneeId));
		
		BigDecimal nbrLivraison = BigDecimalUtil.get(journeeV.getNbr_livraison()==null?0:journeeV.getNbr_livraison());
		BigDecimal mttLivraison = BigDecimalUtil.multiply(fraisLivraison, nbrLivraison);
		BigDecimal mttLivraisonDebit = null;
		
		// Retirer la part de la société
		if(!BigDecimalUtil.isZero(fraisLivraisonPart)){
			BigDecimal mttLivraisonPart = BigDecimalUtil.multiply(fraisLivraisonPart, nbrLivraison);
			mttLivraisonDebit = BigDecimalUtil.substract(mttLivraison, mttLivraisonPart);
		}
		
		// Purger les anciennes écritures
		getQuery("delete from EcriturePersistant where elementId=:elementId "
				+ "and source=:source")
			.setParameter("elementId", journeeV.getJournee_id())
			.setParameter("source", "LIVRAISON")
			.executeUpdate();
		
		//
		String compteEsp = ContextGloabalAppli.getGlobalConfig(ContextAppli.PARAM_APPLI_ENUM.COMPTE_BANCAIRE_ESP_CAISSE.toString());
 	   	if(StringUtil.isEmpty(compteEsp)) {
 		   compteEsp = ContextGloabalAppli.getGlobalConfig(ContextAppli.PARAM_APPLI_ENUM.COMPTE_BANCAIRE_CAISSE.toString());
 	   	}
 	   
        if(StringUtil.isNotEmpty(compteEsp) && !BigDecimalUtil.isZero(mttLivraisonDebit)){
            Long compteId = Long.valueOf(compteEsp);
            CompteBancairePersistant compteBancaire = (CompteBancairePersistant)findById(CompteBancairePersistant.class, compteId);
			
            EcriturePersistant ecritureP = new EcriturePersistant();
	        ecritureP.setDate_mouvement(journeeV.getDate_journee());
	        ecritureP.setElementId(journeeV.getJournee_id());
	
	        String lib = "Paiement des livreurs";
	        ecritureP.setLibelle(lib);
	        ecritureP.setMontant(mttLivraisonDebit);
	        
			ecritureP.setOpc_banque(compteBancaire);
	        ecritureP.setSource(TYPE_ECRITURE.LIVRAISON.toString());
	        ecritureP.setSens("D");
	
	        getEntityManager().merge(ecritureP);
	        
	        // Ecritur credit pour la part de la société
//	        if(!BigDecimalUtil.isZero(mttLivraisonPart)){
//		        ecritureP = (EcritureBanquePersistant) getSingleResult(
//						getQuery("from EcritureBanquePersistant where elementId=:elementId and date_mouvement=:dateMvm and source=:src "
//								+ "and opc_banque.id=:compteBancaireId")
//					.setParameter("elementId", journeeV.getJournee_id())
//					.setParameter("dateMvm", journeeV.getDate_journee())
//					.setParameter("compteBancaireId", compteBancaire.getId())
//					.setParameter("src", "LIVRAISON_PART"));
			
//				ecritureP = new EcritureBanquePersistant();
//		        ecritureP.setDate_mouvement(journeeV.getDate_journee());
//		        ecritureP.setElementId(journeeV.getJournee_id());
//		
//		        lib = "Frais livraison part société";
//		        ecritureP.setLibelle(lib);
//		        ecritureP.setMontant(mttLivraison);//mttLivraisonPart);
//		        
//				ecritureP.setOpc_banque(compteBancaire);
//		        ecritureP.setSource("LIVRAISON_PART");
//		        ecritureP.setSens("C");
//		
//		        getEntityManager().merge(ecritureP);
//	        }
	        
	        return mttLivraison;
        }
        
        return null;
	}

	
	@Override
	public void setDataJourneeFromView(JourneePersistant journeeBean) {
		// Alimenter les montans
		JourneeVenteView journeeView = (JourneeVenteView) getSingleResult(getQuery("from JourneeVenteView where journee_id=:journeeId")
					.setParameter("journeeId", journeeBean.getId()));
		
		if(journeeView == null){
			return;
		}
		//
		journeeBean.setNbr_vente(journeeView.getNbr_vente());
		if(journeeBean.getTarif_livraison() == null) {
			BigDecimal fraisLivraison = BigDecimalUtil.get(ContextGloabalAppli.getGlobalConfig("FRAIS_LIVRAISON"));
			journeeBean.setTarif_livraison(fraisLivraison);
		}
		if(journeeBean.getTarif_livraison_part() == null) {
			BigDecimal fraisLivraisonPart = BigDecimalUtil.get(ContextGloabalAppli.getGlobalConfig("FRAIS_LIVRAISON_PART"));
			journeeBean.setTarif_livraison_part(fraisLivraisonPart);
		}
		journeeBean.setMtt_livraison(BigDecimalUtil.multiply(journeeBean.getTarif_livraison(), BigDecimalUtil.get((journeeView.getNbr_livraison()==null?0:journeeView.getNbr_livraison()))));
		
		journeeBean.setNbr_livraison(journeeView.getNbr_livraison());
		
		
		//--------------------------------------------------------------------
		BigDecimal mtt_cheque = null, mtt_espece = null, mtt_dej = null;
		BigDecimal mtt_portefeuille = null, mtt_cb = null, mtt_donne_point = null;
		BigDecimal mtt_ouverture = null, mtt_annule = null, mtt_annule_ligne = null;
		BigDecimal mtt_reduction = null, mtt_art_offert = null, mtt_art_reduc = null, mtt_total = null;
		BigDecimal mtt_total_net = null;
		BigDecimal mtt_cloture_caissier = null, mtt_cloture_caissier_cb = null, mtt_cloture_caissier_cheque = null, mtt_cloture_caissier_dej = null,  
				mtt_cloture_caissier_espece = null; 
		
		
		List<CaisseJourneePersistant> listDataShift = getJourneeCaisseView(journeeBean.getId());
		if(listDataShift != null){
			for (CaisseJourneePersistant data : listDataShift) {
				mtt_cheque = BigDecimalUtil.add(mtt_cheque, data.getMtt_cheque());
				mtt_espece = BigDecimalUtil.add(mtt_espece, data.getMtt_espece());
				mtt_dej = BigDecimalUtil.add(mtt_dej, data.getMtt_dej());
				mtt_portefeuille = BigDecimalUtil.add(mtt_portefeuille, data.getMtt_portefeuille());
				mtt_cb = BigDecimalUtil.add(mtt_cb, data.getMtt_cb());
				mtt_donne_point = BigDecimalUtil.add(mtt_donne_point, data.getMtt_donne_point());
				
				mtt_ouverture = BigDecimalUtil.add(mtt_ouverture, data.getMtt_ouverture());
				mtt_annule = BigDecimalUtil.add(mtt_annule, data.getMtt_annule());
				mtt_annule_ligne = BigDecimalUtil.add(mtt_annule_ligne, data.getMtt_annule_ligne());
				mtt_reduction = BigDecimalUtil.add(mtt_reduction, data.getMtt_reduction());
				mtt_art_offert = BigDecimalUtil.add(mtt_art_offert, data.getMtt_art_offert());
				mtt_art_reduc = BigDecimalUtil.add(mtt_art_reduc, data.getMtt_art_reduction());
				
				mtt_total = BigDecimalUtil.add(mtt_total, data.getMtt_total());
				mtt_total_net = BigDecimalUtil.add(mtt_total_net, data.getMtt_total_net());
				
				BigDecimal mttClotureCaisserEsp = data.getMtt_cloture_caissier_espece();
				BigDecimal mttClotureCaisserCheq = data.getMtt_cloture_caissier_cheque();
				BigDecimal mttClotureCaisserDej = data.getMtt_cloture_caissier_dej();
				BigDecimal mttClotureCaisserCb = data.getMtt_cloture_caissier_cb();
				BigDecimal mttClotureCaissier = BigDecimalUtil.add(mttClotureCaisserEsp, mttClotureCaisserCheq, mttClotureCaisserDej, mttClotureCaisserCb);
				
				mtt_cloture_caissier = BigDecimalUtil.add(mtt_cloture_caissier, mttClotureCaissier);
				
				mtt_cloture_caissier_cb = BigDecimalUtil.add(mtt_cloture_caissier_cb, data.getMtt_cloture_caissier_cb());
				mtt_cloture_caissier_cheque = BigDecimalUtil.add(mtt_cloture_caissier_cheque, data.getMtt_cloture_caissier_cheque());
				mtt_cloture_caissier_dej = BigDecimalUtil.add(mtt_cloture_caissier_dej, data.getMtt_cloture_caissier_dej());
				mtt_cloture_caissier_espece = BigDecimalUtil.add(mtt_cloture_caissier_espece, data.getMtt_cloture_caissier_espece());
			}
		}
		journeeBean.setMtt_cheque(mtt_cheque);
		journeeBean.setMtt_espece(mtt_espece);
		journeeBean.setMtt_dej(mtt_dej);
		journeeBean.setMtt_portefeuille(mtt_portefeuille);
		journeeBean.setMtt_cb(mtt_cb);
		journeeBean.setMtt_donne_point(mtt_donne_point);
		
		journeeBean.setMtt_ouverture(mtt_ouverture);
		journeeBean.setMtt_annule(mtt_annule);
		journeeBean.setMtt_annule_ligne(mtt_annule_ligne);
		journeeBean.setMtt_reduction(mtt_reduction);
		journeeBean.setMtt_art_offert(mtt_art_offert);
		journeeBean.setMtt_art_reduction(mtt_art_reduc);
		
		journeeBean.setMtt_total(mtt_total);
		journeeBean.setMtt_total_net(mtt_total_net);
		
		journeeBean.setMtt_cloture_caissier(mtt_cloture_caissier);
		journeeBean.setMtt_cloture_caissier_cb(mtt_cloture_caissier_cb);
		journeeBean.setMtt_cloture_caissier_cheque(mtt_cloture_caissier_cheque);
		journeeBean.setMtt_cloture_caissier_dej(mtt_cloture_caissier_dej);
		journeeBean.setMtt_cloture_caissier_espece(mtt_cloture_caissier_espece);
		
		
		//--------------------------------------------------------------------
		BigDecimal montansAchat = getMontantAchatNonRestau(journeeBean.getId());
		journeeBean.setMtt_total_achat(montansAchat);
	}
	
	@Override
	@Transactional
	@WorkModelMethodValidator
	public void create(JourneeBean e) {
		if(e == null){
			e = new JourneeBean();
			Date date_journee = new Date();
			e.setDate_journee(date_journee);
		}
		
		if(e.getDate_journee() != null) {
			Date date_journee = DateUtil.setDetailDate(e.getDate_journee(), TIME_ENUM.HOUR, 13);
			e.setDate_journee(date_journee);
		}
		
		e.setOpc_user(ContextAppli.getUserBean());
		e.setStatut_journee(ContextAppli.STATUT_JOURNEE.OUVERTE.getStatut());
		
		super.create(e);
	}

	@Override
	@Transactional
	@WorkModelMethodValidator
	public void ouvrirJournee(JourneeBean viewBean) {
		create(viewBean);
	}

	@Override
	@Transactional
	@WorkModelMethodValidator
	public void reOuvrirJournee(JourneeBean viewBean) {
		JourneePersistant jp = findById(JourneePersistant.class, viewBean.getId());
		jp.setStatut_journee("O");
		getEntityManager().merge(jp);
	}
	
	@Override
	public JourneePersistant getJourneeView(Long journeeId) {
		JourneePersistant journee = null;
		if(journeeId != null){
			journee = (JourneePersistant) getQuery("from JourneePersistant where id=:journeeId")
					.setParameter("journeeId", journeeId)
					.getSingleResult();
		}
		if(journee != null && "O".equals(journee.getStatut_journee())) {
			setDataJourneeFromView(journee);
		}
		return journee;
	}

	@Override
	@Transactional
	public void updateBasicInfos(Long mvmCaisseId, String type_cmd, String mode_paiement, Long clientId, Long livreurId) {
		List<EtatFinancePersistant> listEtat = getQuery("from EtatFinancePersistant order by date_etat desc").getResultList();
		CaisseMouvementPersistant cmP = (CaisseMouvementPersistant) findById(CaisseMouvementPersistant.class, mvmCaisseId);
		
		if(!listEtat.isEmpty() && listEtat.get(0).getDate_etat().compareTo(cmP.getDate_vente())>=0) {
			MessageService.addBannerMessage("Le mois du mouvement est un mois clos. Il n'est pas possible de mettre à jour cette commande.");
			return;
		}
		
		cmP.setType_commande(StringUtil.isEmpty(type_cmd) ? null : type_cmd);
		cmP.setMode_paiement(StringUtil.isEmpty(mode_paiement) ? null : mode_paiement);
		
		if(StringUtil.isNotEmpty(mode_paiement)){
			cmP.setLast_statut(ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.VALIDE.toString());
			
			// Effacer les montants
			cmP.setMtt_donne_point(null);
			cmP.setMtt_portefeuille(null);
			cmP.setMtt_donne_dej(null);
			cmP.setMtt_donne_cb(null);
			cmP.setMtt_donne_cheque(null);
			cmP.setMtt_donne(null);
			// Re-attribuer
			if(ContextAppli.MODE_PAIEMENT.CARTE.toString().equals(mode_paiement)){
				cmP.setMtt_donne_cb(cmP.getMtt_commande_net());
			} else if(ContextAppli.MODE_PAIEMENT.CHEQUE.toString().equals(mode_paiement)){
				cmP.setMtt_donne_cheque(cmP.getMtt_commande_net());	
			} else if(ContextAppli.MODE_PAIEMENT.ESPECES.toString().equals(mode_paiement)){
				cmP.setMtt_donne(cmP.getMtt_commande_net());
			} else if(ContextAppli.MODE_PAIEMENT.DEJ.toString().equals(mode_paiement)){
				cmP.setMtt_donne_dej(cmP.getMtt_commande_net());	
			} else if("POINTS".equals(mode_paiement)){
				cmP.setMtt_donne_point(cmP.getMtt_commande_net());	
			} else if("RESERVE".equals(mode_paiement)){
				cmP.setMtt_portefeuille(cmP.getMtt_commande_net());	
			}

		} else{
			cmP.setLast_statut(ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.TEMP.toString());
		}
		
		// Maj portefeuille
		if(cmP.getOpc_client() != null) {
			portefeuilleService2.majSoldePortefeuilleMvm(cmP.getOpc_client().getId(), "CLI");			
		}
		if(clientId != null){
			portefeuilleService2.majSoldePortefeuilleMvm(clientId, "CLI");
		}
		
		if(clientId != null){
			cmP.setOpc_client(findById(ClientPersistant.class, clientId));
		}
		if(livreurId != null){
			cmP.setOpc_livreurU(findById(UserPersistant.class, livreurId));
		}
		//
		getEntityManager().merge(cmP);
	}

	private BigDecimal getMontantAchatNonRestau(Long journeeId){
		JourneePersistant journeeP = findById(journeeId);
		List<CaisseJourneePersistant> listCaisseJournee = journeeP.getList_caisse_journee();
		
		if(listCaisseJournee == null){
			return BigDecimalUtil.ZERO;
		}
		
		BigDecimal mttTotalAchatAll = null;
		for (CaisseJourneePersistant cjP : listCaisseJournee) {
			List<CaisseMouvementPersistant> listMvm = cjP.getList_caisse_mouvement();
			if(listMvm == null) {
				continue;
			}
			
			for (CaisseMouvementPersistant cmP : listMvm) {
				if(StringUtil.isEmpty(cmP.getMvm_stock_ids())){
					continue;
				}
				String[] mvmIds = StringUtil.getArrayFromStringDelim(cmP.getMvm_stock_ids(), ";");
				for (String mvmId : mvmIds) {
					if(StringUtil.isEmpty(mvmId)){
						continue;
					}
					MouvementPersistant mvmP = findById(MouvementPersistant.class, Long.valueOf(mvmId));
					if(mvmP == null) {
						continue;
					}
					List<MouvementArticlePersistant> listMvmArt = mvmP.getList_article();
					
					for (MouvementArticlePersistant cmaP : listMvmArt) {
						ArticlePersistant opc_article = cmaP.getOpc_article();
						if(opc_article != null){
							BigDecimal mttAchat = opc_article.getPrixAchatUnitaireTTC();
							if(BigDecimalUtil.isZero(mttAchat)){
								mttAchat = opc_article.getPrixAchatUnitaireHT();
							}
							if(!BigDecimalUtil.isZero(mttAchat)){
								BigDecimal mttAchatQte = BigDecimalUtil.multiply(cmaP.getQuantite(), mttAchat);
								mttTotalAchatAll = BigDecimalUtil.add(mttTotalAchatAll, mttAchatQte);
							}
						}
					}
				}
			}
		}
		
		return mttTotalAchatAll;
	}
	/**
	 * @param journeeId
	 * @return
	 */
//	private BigDecimal getMontantAchatRestau(Long journeeId){
//		JourneePersistant journeeP = findById(journeeId);
//		List<CaisseJourneePersistant> listCaisseJournee = journeeP.getList_caisse_journee();
//		
//		if(listCaisseJournee == null){
//			return BigDecimalUtil.ZERO;
//		}
//		
//		 String oldMenuIdxId = null;
//		BigDecimal mttTotalAchatAll = null;
//		for (CaisseJourneePersistant cjP : listCaisseJournee) {
//			List<CaisseMouvementPersistant> listMvm = cjP.getList_caisse_mouvement();
//			
//			for (CaisseMouvementPersistant cmP : listMvm) {
//				List<CaisseMouvementArticlePersistant> listMvmArt = cmP.getList_article();
//				
//				for (CaisseMouvementArticlePersistant cmaP : listMvmArt) {
//					if(BooleanUtil.isTrue(cmaP.getIs_annule())){
//						continue;
//					}
//					
//					 // Ajouter les articles de destockages paramétrés dans les menu
//		            if(cmaP.getOpc_menu() != null && (oldMenuIdxId == null || !oldMenuIdxId.equals(cmaP.getMenu_idx()))){
//		                MenuCompositionPersistant mvp = (MenuCompositionPersistant) findById(MenuCompositionPersistant.class, cmaP.getOpc_menu().getId());
//		                
//		                for (MenuCompositionDetailPersistant menuCompoDetP : mvp.getList_composition()) {
//		                    ArticlePersistant opc_article_destock = menuCompoDetP.getOpc_article_destock();
//		                    
//		                    BigDecimal pAchat = null;
//		                    if(opc_article_destock != null){
//			                    pAchat = opc_article_destock.getPrixAchatUnitaireTTC();
//								if(BigDecimalUtil.isZero(pAchat)){
//									pAchat = opc_article_destock.getPrixAchatUnitaireHT();
//								}
//		                    }
//							if(opc_article_destock != null && !BigDecimalUtil.isZero(pAchat)){
//		                        BigDecimal mttAchat = BigDecimalUtil.multiply(menuCompoDetP.getQuantite(), pAchat);// Unité achat / vente ----------------------
//		                        mttTotalAchatAll = BigDecimalUtil.add(mttTotalAchatAll, mttAchat);
//		                    }
//		                }
//		            }
//					
//					if(cmaP.getOpc_article() != null){
//						// Calcul prix achat article
//						List<ArticleDetailPersistant> listComposants = cmaP.getOpc_article().getList_article();
//						for (ArticleDetailPersistant artDetP : listComposants) {
//							ArticlePersistant opc_article_composant = artDetP.getOpc_article_composant();
//							if(!BigDecimalUtil.isZero(opc_article_composant.getPrix_achat_ttc())){
//								BigDecimal pAchat = opc_article_composant.getPrixAchatUnitaireTTC();
//								if(BigDecimalUtil.isZero(pAchat)){
//									pAchat = opc_article_composant.getPrixAchatUnitaireHT();
//								}
//								BigDecimal mttAchatQte = BigDecimalUtil.multiply(artDetP.getQuantite(), pAchat);
//								mttTotalAchatAll = BigDecimalUtil.add(mttTotalAchatAll, mttAchatQte);
//							}
//						}
//					}
//				}
//			}
//		}
//		
//		return mttTotalAchatAll;
//	}
	
	@Override
	public Map<String, Map<String, RepartitionBean>> getChiffresServeurLivreurCaissier(Date dateDebut, Date dateFin, Long journeeId){
		List<JourneePersistant> listJournee = new ArrayList<>();
		if(journeeId != null) {
			JourneePersistant journeeP = (JourneePersistant) findById(JourneePersistant.class, journeeId);
			listJournee.add(journeeP);
		} else {			
			listJournee = getQuery("from JourneePersistant journee "
					+ "where journee.date_journee>=:dateDebut and journee.date_journee<=:dateFin "
					+ "order by journee.date_journee desc")
				.setParameter("dateDebut", dateDebut)
				.setParameter("dateFin", dateFin)
				.getResultList();
		}
		
		Map<String, Map<String, RepartitionBean>> mapData = new HashMap<String, Map<String, RepartitionBean>>();
		Map<String, RepartitionBean> mapLivreur = new LinkedHashMap<>();
		Map<String, RepartitionBean> mapEmploye = new LinkedHashMap<>();
		Map<String, RepartitionBean> mapServeur = new LinkedHashMap<>();

		for (JourneePersistant journeeP : listJournee) {
			List<CaisseJourneePersistant> listCaisseJ = journeeP.getList_caisse_journee();
			//
			for (CaisseJourneePersistant caisseJourneePersistant : listCaisseJ) {
				List<CaisseMouvementPersistant> list_caisse_mouvement = caisseJourneePersistant.getList_caisse_mouvement();
				if(list_caisse_mouvement == null){
					continue;
				}
				//
				for(CaisseMouvementPersistant mvmP : list_caisse_mouvement){
					if(BooleanUtil.isTrue(mvmP.getIs_annule())){
						continue;
					}
					
					UserPersistant opc_serveur = mvmP.getOpc_serveur();
					if(opc_serveur != null){// Serveur
						String key = opc_serveur.getLogin();
						if(opc_serveur.getOpc_employe()!=null){
							key = opc_serveur.getOpc_employe().getNom()+" "+StringUtil.getValueOrEmpty(opc_serveur.getOpc_employe().getPrenom());
						}
						RepartitionBean repBean = mapServeur.get(key);
						if(repBean == null){
							repBean = new RepartitionBean();
							repBean.setLibelle(key);
							mapServeur.put(key, repBean);
						}
						repBean.setMontant(BigDecimalUtil.add(repBean.getMontant(), mvmP.getMtt_commande_net()));
						repBean.setNbrCmd(repBean.getNbrCmd()+1);
					}
					if(mvmP.getOpc_livreurU() != null){// Livreur
						String key = mvmP.getOpc_livreurU().getLogin();
						RepartitionBean repBean = mapLivreur.get(key);
						if(repBean == null){
							repBean = new RepartitionBean();
							repBean.setLibelle(key);
							mapLivreur.put(key, repBean);
						}
						repBean.setMontant(BigDecimalUtil.add(repBean.getMontant(), mvmP.getMtt_commande_net()));
						repBean.setNbrCmd(repBean.getNbrCmd()+1);
					}
					
					
					// Marge et net -----------------
					if(mvmP.getOpc_user() != null){
						List<CaisseMouvementArticlePersistant> list_article = mvmP.getList_article();
						for(CaisseMouvementArticlePersistant det : list_article){
							if(BooleanUtil.isTrue(det.getIs_annule()) || det.getOpc_article() == null){
								continue;
							}
							if(!BigDecimalUtil.isZero(det.getOpc_article().getTaux_marge_caissier())) {
								
							}
						}
					}
					// Marge et net -----------------
					UserPersistant opc_user = mvmP.getOpc_user();
					if(opc_user != null){
						String key = opc_user.getLogin();
						if(opc_user.getOpc_employe()!=null){
							key = opc_user.getOpc_employe().getNom()+" "+StringUtil.getValueOrEmpty(opc_user.getOpc_employe().getPrenom());
						}
						RepartitionBean repBean = mapEmploye.get(key);
						if(repBean == null){
							repBean = new RepartitionBean();
							repBean.setLibelle(key);
							repBean.setElementId(opc_user.getId());
							mapEmploye.put(key, repBean);
						}
						repBean.setMontant(BigDecimalUtil.add(repBean.getMontant(), mvmP.getMtt_commande_net()));
						repBean.setMontantMargeCaissier(BigDecimalUtil.add(repBean.getMontantMargeCaissier(), mvmP.getMtt_marge_caissier()));
						repBean.setNbrCmd(repBean.getNbrCmd()+1);
					}
				}
			}
		}
		
		mapData.put("data_employe", mapEmploye);
		mapData.put("data_livreur", mapLivreur);
		mapData.put("data_serveur", mapServeur);
			
		return mapData;
	}
	
	@Override
	public Date[] getMinMaxDate() {
		Object[] minMax = (Object[]) getSingleResult(getQuery("select min(date_journee) as min_date, "
				+ "max(date_journee) as max_date "
				+ "from JourneePersistant"));
		
		Date dateDebut = (Date) minMax[0];
		Date dateFin = (Date) minMax[1];
		
		return (minMax == null)	 ? new Date[2] : new Date[] {dateDebut, dateFin};
	}
	
	@Override
	public JourneePersistant getJourneeByDate(Date dateJournee) {
		Calendar cal = DateUtil.getCalendar(dateJournee);
		
		return (JourneePersistant) getSingleResult(getQuery("from JourneePersistant "
				+ "where day(date_journee)=:jour "
				+ "and month(date_journee)=:mois "
				+ "and year(date_journee)=:annee")
				.setParameter("jour", cal.get(Calendar.DAY_OF_MONTH))
				.setParameter("mois", cal.get(Calendar.MONTH)+1)
				.setParameter("annee", cal.get(Calendar.YEAR)));
	}
	
	@Override
	public JourneePersistant getJourneeOrLastByDate(Date dateJournee) {
		JourneePersistant journeeP = getJourneeByDate(dateJournee);
		if(journeeP == null){
			dateJournee = DateUtil.getEndOfDay(dateJournee);
			
			journeeP = (JourneePersistant) getSingleResult(getQuery("from JourneePersistant "
					+ "where date_journee<=:dateJournee "
					+ "order by date_journee desc")
					.setParameter("dateJournee", dateJournee)
					.setMaxResults(1));
		}
		return journeeP;
	}
	@Override
	public JourneePersistant getJourneeOrNextByDate(Date dateJournee) {
		IMouvementService mouvementService = ServiceUtil.getBusinessBean(IMouvementService.class);
		JourneePersistant journeeP = getJourneeByDate(dateJournee);
		if(journeeP == null){
			dateJournee = DateUtil.getStartOfDay(dateJournee);
			
			journeeP = (JourneePersistant) mouvementService.getSingleResult(mouvementService.getQuery("from JourneePersistant "
					+ "where date_journee>=:dateJournee "
					+ "order by date_journee asc")
					.setParameter("dateJournee", dateJournee)
					.setMaxResults(1));
		}
		return journeeP;
	}
	@Override
	public JourneePersistant getJourneeOrPreviousByDate(Date dateJournee) {
		IMouvementService mouvementService = ServiceUtil.getBusinessBean(IMouvementService.class);
		JourneePersistant journeeP = getJourneeByDate(dateJournee);
		if(journeeP == null){
			dateJournee = DateUtil.getEndOfDay(dateJournee);
			
			journeeP = (JourneePersistant) mouvementService.getSingleResult(mouvementService.getQuery("from JourneePersistant "
					+ "where date_journee<=:dateJournee "
					+ "order by date_journee desc")
					.setParameter("dateJournee", dateJournee)
					.setMaxResults(1));
		}
		return journeeP;
	}
	
	@Override
	@Transactional
	@WorkModelMethodValidator
	public void majDateJournee(Long journeeId, Date date_journee) {
		JourneePersistant journeePer = (JourneePersistant) findById(JourneePersistant.class, journeeId);
		if( !etatFinanceService.isMoisClos(journeePer.getDate_journee()) ){
			date_journee = DateUtil.setDetailDate(date_journee, TIME_ENUM.HOUR, 13);
			journeePer.setDate_journee(date_journee);
		
			getEntityManager().merge(journeePer);
		} else {
			MessageService.addBannerMessage("Cette journée appartient à un mois clos.");
		}
	}

	@SuppressWarnings("unchecked")
	private Map getRepartitionVenteArticleNonRestau(JourneePersistant journeeIdStart, JourneePersistant journeeIdEnd, Long familleIncludeId){
		Set<Long> familleIncludeIds = new HashSet<Long>();
		if(familleIncludeId != null && !familleIncludeId.toString().equals("-999")) { 
			List<FamillePersistant> familleAll = familleService.getFamilleEnfants("ST", familleIncludeId, true);
			for (FamillePersistant famillePersistant : familleAll) {
				familleIncludeIds.add(famillePersistant.getId());
			}
			familleIncludeIds.add(familleIncludeId);
		}
		
		// Articles
		Query query = getNativeQuery("select det.elementId, sum(det.quantite) as qte, sum(det.mtt_total), "
				+ "det.libelle, fam.libelle as famille "
				+ "from caisse_mouvement_article det "
				+ "inner join caisse_mouvement mvm on det.mvm_caisse_id=mvm.id "
				+ "inner join caisse_journee cj on mvm.caisse_journee_id=cj.id "
				+ "inner join journee jr on cj.journee_id=jr.id "
				+ "inner join article art on det.article_id=art.id "
				+ "left join famille fam on art.famille_stock_id=fam.id "
				+ "where det.article_id is not null and "
				+ "jr.id>=:journeeStartId and jr.id<=:journeeEndId "
				+(familleIncludeIds.size() > 0 ? "and fam.id in (:familleIncludeId) ":" ")
				+ "and (det.is_annule is null or det.is_annule=0) "
				+ "group by det.elementId "
				+ "order by fam.b_left, qte desc");
		
		query.setParameter("journeeStartId", journeeIdStart.getId())
			 .setParameter("journeeEndId", journeeIdEnd.getId());
		
		if(familleIncludeIds.size() > 0){
			query.setParameter("familleIncludeId", familleIncludeIds);
		}
		List<Object[]> listVenteArticle = query.getResultList();
		
		Map<Long, RepartitionBean> mapArticle = new LinkedHashMap<>();
		for (Object[] data : listVenteArticle) {
			String libelle = (String) data[3];
			Long elementId = ((BigInteger) data[0]).longValue();
			
			RepartitionBean repMenu = new RepartitionBean();
			repMenu.setLibelle(libelle);
			repMenu.setFamille((String) data[4]);
			repMenu.setElementId(elementId);
			repMenu.setQuantite(BigDecimalUtil.get(""+data[1]));
			repMenu.setMontant((BigDecimal)data[2]);
			
			mapArticle.put(elementId, repMenu);
		}
		
		//---------------------------------------------------------------------------------------------
		// Enlever les offres
		query = getNativeQuery("select sum(IFNULL(mvm.mtt_reduction, 0)) "
					+ "from caisse_mouvement mvm "
					+ "inner join caisse_journee cj on mvm.caisse_journee_id=cj.id "
					+ "inner join journee jr on cj.journee_id=jr.id "
					+ "where (mvm.is_annule is null or mvm.is_annule=0) and "
					+ "jr.id>=:journeeStartId and jr.id<=:journeeEndId");
		
		query.setParameter("journeeStartId", journeeIdStart)
			 .setParameter("journeeEndId", journeeIdEnd);
		
		Object offreData = getSingleResult(query);
		
		Map mapRetour = new HashMap<>();
		mapRetour.put("ARTS", mapArticle);
		mapRetour.put("OFFRE", offreData);
		
		return mapRetour ;
	}

	/**
	 * Tri
	 * @param mapArtRecap
	 */
	private Map<Long, RepartitionBean> sortMap(Map<Long, RepartitionBean> mapArtRecap) {
		List <Entry<Long, RepartitionBean>> capitalList = new LinkedList(mapArtRecap.entrySet());
	    Collections.sort(capitalList, new Comparator<Entry<Long, RepartitionBean>>() {
			@Override
			public int compare(Entry<Long, RepartitionBean> o1, Entry<Long, RepartitionBean> o2) {
				return o1.getValue().getFamille().compareTo(o2.getValue().getFamille());
			}
		});
	    Map<Long, RepartitionBean> result = new LinkedHashMap<>();
	    for (Map.Entry<Long, RepartitionBean> entry : capitalList) {
	      result.put(entry.getKey(), entry.getValue());
	    }
	    
	    return result;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Map getRepartitionVenteArticleRestau(JourneePersistant journeeStart, JourneePersistant journeeEnd, Long familleIncludeId, Long caisseFilterId, boolean isFromRaz){
		EtablissementPersistant restauP = (EtablissementPersistant) findById(EtablissementPersistant.class, ContextAppli.getEtablissementBean().getId());
		String[] familleExclude = StringUtil.getArrayFromStringDelim(restauP.getVente_familles(), ";");
		String[] menuEquiv = StringUtil.getArrayFromStringDelim(restauP.getVente_menus_art(), ";");
		Map<Long, List<Long>> mapMenuArtEquiv = new HashMap<>();
		
		if(menuEquiv != null){
			for (String mnuArt : menuEquiv) {
				String[] menuArtArray = StringUtil.getArrayFromStringDelim(mnuArt, ":");
				Long menuId = Long.valueOf(menuArtArray[0]);
				List<Long> artArray = new ArrayList<Long>();
				String[] arts = StringUtil.getArrayFromStringDelim(menuArtArray[1], "-");
				//
				for (String art : arts) {
					artArray.add(Long.valueOf(art));
				}
				mapMenuArtEquiv.put(menuId, artArray);
			}
		}
		Set<Long> famExcludeIds = new HashSet<>();
		if(familleExclude != null){
			for(String fam : familleExclude){
				famExcludeIds.add(Long.valueOf(fam));
			}
		}
		
		boolean isMenuFilter = (familleIncludeId != null && familleIncludeId.toString().equals("-999"));
		boolean isAllFamille = (familleIncludeId != null && familleIncludeId.toString().equals("-888"));
		//		
		Set<Long> familleIncludeIds = new HashSet<Long>();
		if(familleIncludeId != null && !isMenuFilter && !isAllFamille) { 
			List<FamillePersistant> familleAll = familleService.getFamilleEnfants("CU", familleIncludeId, true);
			for (FamillePersistant famillePersistant : familleAll) {
				familleIncludeIds.add(famillePersistant.getId());
			}
			familleIncludeIds.add(familleIncludeId);
		}
		
		// ------------------------------------------------------------------------------------
		String req = "from CaisseMouvementArticlePersistant det "
				+ "where "
				+ "det.opc_mouvement_caisse.opc_caisse_journee.opc_journee.id>=:journeeStartId "
				+ "and det.opc_mouvement_caisse.opc_caisse_journee.opc_journee.id<=:journeeEndId"
				+ (caisseFilterId!=null ? " and det.opc_mouvement_caisse.caisse_cuisine like concat('%;','"+caisseFilterId+":',det.id,';%') " : "")
				+ " and (det.is_annule is null or det.is_annule=0) "
				+ " and (det.opc_mouvement_caisse.is_annule is null or det.opc_mouvement_caisse.is_annule=0) "
				+(familleIncludeIds.size() > 0 ? "and det.opc_article.opc_famille_cuisine.id in (:familleIncludeId) ":"")
				+(famExcludeIds.size() > 0 ? "and det.opc_article.opc_famille_cuisine.id not in (:excludeIds) ":"")
				+(isMenuFilter ? "and (det.is_menu is not null and det.is_menu=1) ":"")
				+(isAllFamille ? "and (det.is_menu is null and menu_idx is null) ":"")
				+ " order by det.libelle";// : " order by det.opc_article.opc_famille_cuisine.b_left");
		
		Query queryAll = getQuery(req);
		
		if(familleIncludeIds.size() > 0){
			queryAll.setParameter("familleIncludeId", familleIncludeIds);
		}
		if(famExcludeIds.size() > 0){
			queryAll.setParameter("excludeIds", famExcludeIds);
		}
		
		queryAll.setParameter("journeeStartId", journeeStart.getId())
				.setParameter("journeeEndId", journeeEnd.getId());
		
		Map<Long, RepartitionBean> mapMenuRecap = new LinkedHashMap<>();
		Map<Long, RepartitionBean> mapMenuArtRecap = new LinkedHashMap<>();
		Map<Long, RepartitionBean> mapArtRecap = new LinkedHashMap<>();
		BigDecimal mttRecharge = null;		
		List<CaisseMouvementArticlePersistant> listALlVente = queryAll.getResultList();
		
		//
		BigDecimal ttl = null;
		BigDecimal ttlDet = null;
		for (CaisseMouvementArticlePersistant caisseMvmArtP : listALlVente) {
			ttl = BigDecimalUtil.add(ttl, caisseMvmArtP.getMtt_total());
			// Le menu ---------------------------------------------------------------------------------
			if(BooleanUtil.isTrue(caisseMvmArtP.getIs_menu()) && caisseMvmArtP.getOpc_menu() != null){
				String libelle = caisseMvmArtP.getOpc_menu().getLibelle();
				Long mnuId = caisseMvmArtP.getOpc_menu().getId();
				RepartitionBean repMenu = mapMenuRecap.get(mnuId);
				if(repMenu == null){
					repMenu = new RepartitionBean();
					repMenu.setElementId(mnuId);
					repMenu.setLibelle(libelle);
					mapMenuRecap.put( mnuId, repMenu);
				}
				repMenu.setQuantite(BigDecimalUtil.add(repMenu.getQuantite(), BigDecimalUtil.get(1)));
				repMenu.setMontant(BigDecimalUtil.add(repMenu.getMontant(), caisseMvmArtP.getMtt_total()));
				
				ttlDet = BigDecimalUtil.add(ttlDet, caisseMvmArtP.getMtt_total());
			} else if(caisseMvmArtP.getOpc_article() != null){
				FamilleCuisinePersistant opc_famille_cuisine = caisseMvmArtP.getOpc_article().getOpc_famille_cuisine();
				Long artId = caisseMvmArtP.getOpc_article().getId();
				
				String lib = caisseMvmArtP.getOpc_article().getCode()+"-"+caisseMvmArtP.getOpc_article().getLibelle();
				if("GEN".equals(caisseMvmArtP.getOpc_article().getCode())){
					lib = caisseMvmArtP.getLibelle();
					artId = Long.valueOf(lib.hashCode());
				}
				
				//
				if(StringUtil.isNotEmpty(caisseMvmArtP.getMenu_idx())){
					RepartitionBean repArtMenu = mapMenuArtRecap.get(artId);
					if(repArtMenu == null){
						repArtMenu = new RepartitionBean();
						repArtMenu.setElementId(artId);
						repArtMenu.setFamille(opc_famille_cuisine.getCode()+"-"+opc_famille_cuisine.getLibelle());
						repArtMenu.setLibelle(lib);
						mapMenuArtRecap.put(artId, repArtMenu);
					}
					repArtMenu.setQuantite(BigDecimalUtil.add(repArtMenu.getQuantite(), caisseMvmArtP.getQuantite()));
					repArtMenu.setMontant(BigDecimalUtil.add(repArtMenu.getMontant(), caisseMvmArtP.getMtt_total()));
					ttlDet = BigDecimalUtil.add(ttlDet, caisseMvmArtP.getMtt_total());
				} else{
					RepartitionBean repArt = mapArtRecap.get(artId);
					if(repArt == null){
						repArt = new RepartitionBean();
						repArt.setElementId(artId);
						repArt.setLibelle(lib);
						repArt.setFamille((opc_famille_cuisine != null ? (opc_famille_cuisine.getCode()+"-"+opc_famille_cuisine.getLibelle()) : "-----"));
						mapArtRecap.put(artId, repArt);
					}
					repArt.setQuantite(BigDecimalUtil.add(repArt.getQuantite(), caisseMvmArtP.getQuantite()));
					repArt.setMontant(BigDecimalUtil.add(repArt.getMontant(), caisseMvmArtP.getMtt_total()));
					ttlDet = BigDecimalUtil.add(ttlDet, caisseMvmArtP.getMtt_total());
				}
			} else if(!BigDecimalUtil.isZero(caisseMvmArtP.getMtt_total())){
				if("RECHARGE_PF".equals(caisseMvmArtP.getType_ligne())){
					mttRecharge = BigDecimalUtil.add(mttRecharge, caisseMvmArtP.getMtt_total());
					ttlDet = BigDecimalUtil.add(ttlDet, caisseMvmArtP.getMtt_total());
				} else{
					ttlDet = BigDecimalUtil.add(ttlDet, caisseMvmArtP.getMtt_total());
				}
			}
		}
		//---------------------------------------------------------------------------------------------
		
		Query queryAllMvm = getQuery("from CaisseMouvementPersistant mvm "
				+ "where "
				+ "mvm.opc_caisse_journee.opc_journee.id>=:journeeStartId "
				+ " and mvm.opc_caisse_journee.opc_journee.id<=:journeeEndId "
				+ " and (mvm.is_annule is null or mvm.is_annule=0) "
				+ " order by mvm.id");
		
		queryAllMvm.setParameter("journeeStartId", journeeStart.getId())
				   .setParameter("journeeEndId", journeeEnd.getId());

		BigDecimal livraisonData = null;
		BigDecimal mttOffre = null;
		BigDecimal mttReduction = null;
		BigDecimal mttArtReduction = null;
		BigDecimal mttVente = null;
		BigDecimal mttVenteNet = null;
		
		List<CaisseMouvementPersistant> listAllMvmVente = queryAllMvm.getResultList();
		for (CaisseMouvementPersistant caisseMvmP : listAllMvmVente) {
			livraisonData = BigDecimalUtil.add(livraisonData, caisseMvmP.getMtt_livraison_ttl());
			mttReduction = BigDecimalUtil.add(mttOffre, caisseMvmP.getMtt_reduction());
			mttOffre = BigDecimalUtil.add(mttOffre, caisseMvmP.getMtt_art_offert());
			mttArtReduction = BigDecimalUtil.add(mttArtReduction, caisseMvmP.getMtt_art_reduction());
			mttVenteNet = BigDecimalUtil.add(mttVenteNet, caisseMvmP.getMtt_commande_net());
			mttVente = BigDecimalUtil.add(mttVente, caisseMvmP.getMtt_commande());
		}
	    
		mapArtRecap = sortMap(mapArtRecap);
		mapMenuArtRecap = sortMap(mapMenuArtRecap);
		
		Map mapRetour = new HashMap<>();
		mapRetour.put("MENU", mapMenuRecap);
		mapRetour.put("MENU_ARTS", mapMenuArtRecap);
		mapRetour.put("ARTS", mapArtRecap);
		mapRetour.put("OFFRE", mttOffre);
		mapRetour.put("REDUCTION", mttReduction);
		mapRetour.put("REDUCTION ART", mttArtReduction);
		mapRetour.put("LIVRAISON", livraisonData);
		mapRetour.put("RECHARGE", mttRecharge);
		
		mapRetour.put("VENTE_NET", mttVenteNet);
		mapRetour.put("VENTE", mttVente);
		
		return mapRetour ;
	}

	@Override
	public Map getRepartitionVenteArticle(JourneePersistant journeeIdStart, JourneePersistant journeeIdEnd, Long familleIncludeId, boolean isFromRaz) {
		boolean isRestau = SOFT_ENVS.restau.toString().equals(StrimUtil.getGlobalConfigPropertie("context.soft"));
		if(isRestau){
			return getRepartitionVenteArticleRestau(journeeIdStart, journeeIdEnd, familleIncludeId, null, isFromRaz);
		} else{
			return getRepartitionVenteArticleNonRestau(journeeIdStart, journeeIdEnd, familleIncludeId);
		}
	}

	@Override
	public List<CaisseMouvementTracePersistant> getMouvementAnnomalie(Long journeeId) {
		return getQuery("from CaisseMouvementTracePersistant as trace "
				+ "where trace.id_origine is null or trace.id_origine not in (select mvm.id from CaisseMouvementPersistant mvm where mvm.opc_caisse_journee.opc_journee.id=:journeeId) " 
			+ " and trace.journee_id=:journeeId "
			+ "order by trace.id desc")
				.setParameter("journeeId", journeeId)
				.getResultList();
	}

	@Override
	public Map<String, Object> getRepartitionVenteStock(Long journeeId, Date dateDebut, Date dateFin) {
		if(journeeId == null){
			dateDebut = DateUtil.getStartOfDay(dateDebut);
			dateFin = DateUtil.getEndOfDay(dateFin);
		}
		// Les menus
		Query queryMvmStock = getQuery("select mvm.mvm_stock_ids from CaisseMouvementPersistant mvm "
				+ "where mvm.mvm_stock_ids is not null and "
				+ (journeeId == null ? 
						 "mvm.date_vente>=:dateDebut and mvm.date_vente<=:dateFin "
						: "mvm.opc_caisse_journee.opc_journee.id=:journeeId "
				  )
				+ " and (mvm.is_annule is null or mvm.is_annule=0) ");
		
		if(journeeId == null){
			queryMvmStock.setParameter("dateDebut", dateDebut);
			queryMvmStock.setParameter("dateFin", dateFin);
		} else{
			queryMvmStock.setParameter("journeeId", journeeId);
		}
		List<String> listVenteStc = queryMvmStock.getResultList();
		
		Set<Long> setIds = new HashSet();
		for (String mvmStockIds : listVenteStc) {
			String[] stockMvmIds = StringUtil.getArrayFromStringDelim(mvmStockIds, ";");
			for (String stId : stockMvmIds) {
				if(StringUtil.isNotEmpty(stId)) {
					setIds.add(Long.valueOf(stId));
				}
			}
		}
		
		// Détail
		List<MouvementArticlePersistant> listDetail = new ArrayList<>();
		if(setIds.size() > 0){
			listDetail = getQuery("from MouvementArticlePersistant det "
				+ "where det.opc_mouvement.id in (:ids) "
				+ "order by det.opc_article.opc_famille_stock.b_left")
			.setParameter("ids", setIds)
			.getResultList();
		}
		
		Map<Long, RepartitionBean> mapArtRep = new LinkedHashMap<>();
		
		// On cumul les montants
		for(MouvementArticlePersistant mvmArtP : listDetail){
			ArticlePersistant opc_article = mvmArtP.getOpc_article();
			String libelle = opc_article.getLibelle();
			Long artId = opc_article.getId();
			RepartitionBean repRep = mapArtRep.get(artId);
			if(repRep == null){
				repRep = new RepartitionBean();
				repRep.setElementId(artId);
				repRep.setLibelle(libelle);
				repRep.setFamille(opc_article.getOpc_famille_stock().getCode()+"-"+opc_article.getOpc_famille_stock().getLibelle());
				//
				mapArtRep.put( artId, repRep);
			}
			repRep.setQuantite(BigDecimalUtil.add(repRep.getQuantite(), mvmArtP.getQuantite()));
			repRep.setMontant(BigDecimalUtil.add(repRep.getMontant(), BigDecimalUtil.multiply(mvmArtP.getQuantite(), opc_article.getPrix_achat_ttc())));
		}
		
		Map mapRetour = new HashMap();
		mapRetour.put("ARTICLE", mapArtRep);
		
		return mapRetour ;
	}

	@Override
	public Map<Long, CaisseMouvementPersistant> getMapMvmStockCaisse(Long jrnId) {
		List<CaisseMouvementPersistant> listMvm = getQuery("from CaisseMouvementPersistant where opc_caisse_journee.opc_journee.id=:jrnId")
				.setParameter("jrnId", jrnId)
				.getResultList();
		
		Map<Long, CaisseMouvementPersistant> mapData = new HashMap<>();
		for (CaisseMouvementPersistant caisseMouvementP : listMvm) {
			if(StringUtil.isEmpty(caisseMouvementP.getMvm_stock_ids())) {
				continue;
			}
			String[] stockMvmIds = StringUtil.getArrayFromStringDelim(caisseMouvementP.getMvm_stock_ids(), ";");
			for (String stckId : stockMvmIds) {
				if(StringUtil.isEmpty(stckId)) {
					continue;
				}
				mapData.put(Long.valueOf(stckId), caisseMouvementP);
			}
		}
		return mapData;
	}

	@Override
	public List<JourneePersistant> getListournee(Date debut, Date fin) {
		List<JourneePersistant> listJournee = getQuery("from JourneePersistant journee "
				+ "where date_journee>=:dtDebut and date_journee<=:dtFin "
				+ "order by date_journee desc")
				.setParameter("dtDebut", debut)
				.setParameter("dtFin", fin)
				.getResultList();
		
		return listJournee;
	}
	
	@Override
	public List<CaisseJourneePersistant> getJourneeCaisseView(Long journeeId) {
		String requete = "from CaisseJourneePersistant where opc_journee.id=:journeeId order by opc_caisse.reference, id";
	    List<CaisseJourneePersistant> listData = getQuery(requete).
	    												setParameter("journeeId", journeeId).getResultList();
	    
	    for (CaisseJourneePersistant cjP : listData) {
			if(cjP.getStatut_caisse().equals("O")) {
				caisseService.setDataJourneeCaisseFromView(cjP);
			}
		}
	    
		return listData;
	}
	
	@Override
	public Map<String, Map> getRepartitionVenteArticleParPosteCuisine(JourneePersistant journeeStartId, JourneePersistant journeeEndId, Long familleIncludeId){
		EtablissementPersistant restauP = (EtablissementPersistant) findById(EtablissementPersistant.class, ContextAppli.getEtablissementBean().getId());
		String[] familleExclude = StringUtil.getArrayFromStringDelim(restauP.getVente_familles(), ";");
		String[] menuEquiv = StringUtil.getArrayFromStringDelim(restauP.getVente_menus_art(), ";");
		Map<Long, List<Long>> mapMenuArtEquiv = new HashMap<>();
		List<CaisseBean> listEcranCuisine = caisseService.getListCaisseCuisineActive();
		Map<String, Map> mapResult = new LinkedHashMap<>();
		//
		if(menuEquiv != null){
			for (String mnuArt : menuEquiv) {
				String[] menuArtArray = StringUtil.getArrayFromStringDelim(mnuArt, ":");
				Long menuId = Long.valueOf(menuArtArray[0]);
				List<Long> artArray = new ArrayList<Long>();
				String[] arts = StringUtil.getArrayFromStringDelim(menuArtArray[1], "-");
				//
				for (String art : arts) {
					artArray.add(Long.valueOf(art));
				}
				mapMenuArtEquiv.put(menuId, artArray);
			}
		}
		Set<Long> famExcludeIds = new HashSet<>();
		if(familleExclude != null){
			for(String fam : familleExclude){
				famExcludeIds.add(Long.valueOf(fam));
			}
		}
		
		//		
		Set<Long> familleIncludeIds = new HashSet<Long>();
		if(familleIncludeId != null && !familleIncludeId.toString().equals("-999")) { 
			List<FamillePersistant> familleAll = familleService.getFamilleEnfants("CU", familleIncludeId, true);
			for (FamillePersistant famillePersistant : familleAll) {
				familleIncludeIds.add(famillePersistant.getId());
			}
			familleIncludeIds.add(familleIncludeId);
		}
		
//		dateDebut = DateUtil.getStartOfDay(dateDebut);
//		dateFin = DateUtil.getEndOfDay(dateFin);
		
		for(CaissePersistant caisseB : listEcranCuisine){
			mapResult.put(caisseB.getReference(), getRepartitionVenteArticleRestau(journeeStartId, journeeEndId, familleIncludeId, caisseB.getId(), false));
		}
		
		return mapResult ;
	}
	
	@Override
	public BigDecimal getMontantRechargePortefeuille(Long journeeId) {
		// Total des recharges sur la date
		String req = "select sum(mtt_total) from CaisseMouvementArticlePersistant caiMvm "
				+ "where caiMvm.type_ligne=:typeLigne "
			    + "and (caiMvm.opc_mouvement_caisse.is_annule is null or caiMvm.opc_mouvement_caisse.is_annule=0) "
				+ "and (caiMvm.is_annule is null or caiMvm.is_annule=0) "
				+ "and caiMvm.opc_mouvement_caisse.opc_caisse_journee.opc_journee.id=:journeeId "
				+ "order by caiMvm.opc_mouvement_caisse.id desc";
		BigDecimal totalRechargePeriode = BigDecimalUtil.get(""+
				getSingleResult(getQuery(req)
						.setParameter("journeeId", journeeId)
						.setParameter("typeLigne", ContextAppli.TYPE_LIGNE_COMMANDE.RECHARGE_PF.toString())));
		
		return totalRechargePeriode;
	}
	
	@Override
	@Transactional
	public String recalculChiffresMvmJournee(Long journeeId) {
		List<CaisseMouvementPersistant> listMvmJrn = getQuery("from CaisseMouvementPersistant "
				+ "where opc_caisse_journee.opc_journee.id=:jrnId "
				+ "order by opc_user.login")
			.setParameter("jrnId", journeeId)
			.getResultList();
		
		EntityManager em = getEntityManager();
		JourneePersistant journeeP = caisseService.findById(JourneePersistant.class, journeeId);
		
		StringBuilder sb = new StringBuilder("RAPPORT CORRECTIF DE LA JOURNEE : "+DateUtil.dateToString(journeeP.getDate_journee())+"\n");
		sb.append("--------------------------------------------------------------\n");
		Map<String, BigDecimal> mapTtl = new LinkedHashMap<>();
		//
		for (CaisseMouvementPersistant caisseMvmP : listMvmJrn) {
			BigDecimal oldMttNet = caisseMvmP.getMtt_commande_net();
			recalculChiffresMvmJournee(caisseMvmP);
			BigDecimal newMttNet = caisseMvmP.getMtt_commande_net();
			
	        if(newMttNet.compareTo(oldMttNet) != 0) {
	        	String loginUser = caisseMvmP.getOpc_user().getLogin();
				BigDecimal ecartMtt = BigDecimalUtil.substract(newMttNet, oldMttNet);
				sb.append("Caissier : "+loginUser+" => Commande "+caisseMvmP.getRef_commande()+" : Avant : "+BigDecimalUtil.formatNumber(oldMttNet) 
	        							+ " ---- Après : "+BigDecimalUtil.formatNumber(newMttNet)+" ---- Ecart : "+BigDecimalUtil.formatNumber(ecartMtt)+"\n");
	        	
	        	mapTtl.put(loginUser, BigDecimalUtil.add(mapTtl.get(loginUser), ecartMtt));
	        	// Merge
	        	em.merge(caisseMvmP);
	        }
		}
		
		if(mapTtl.size() > 0) {
			sb.append("--------------------------------------------------------------\n");
			BigDecimal ttl = null;
			for(String user : mapTtl.keySet()) {
				sb.append("TOTAL "+user.toUpperCase()+" => "+BigDecimalUtil.formatNumber(mapTtl.get(user))+"\n");
				ttl = BigDecimalUtil.add(ttl, mapTtl.get(user));
			}
			sb.append("--------------------------------------------------------------\n");
			sb.append("TOTAL ECART => "+BigDecimalUtil.formatNumber(ttl)+"\n");
			sb.append("--------------------------------------------------------------\n");
		}
		
		return sb.toString();
	}
	
	public static void recalculChiffresMvmJournee(CaisseMouvementPersistant caisseMvmP) {
		String startegieArrondi = ContextGloabalAppli.getGlobalConfig("AROUNDI_PRIX_VENTE");
		// Maj total commande
		BigDecimal totalCommande = BigDecimalUtil.ZERO;
		BigDecimal totalCommandeHorsReduc = BigDecimalUtil.ZERO;
		BigDecimal totalReduc = BigDecimalUtil.ZERO;
		
		for (CaisseMouvementArticlePersistant currMvmP : caisseMvmP.getList_article()) {
			if(BooleanUtil.isTrue(currMvmP.getIs_annule())) {
				continue;
			}
			
			if (BooleanUtil.isTrue(currMvmP.getIs_offert())) {
				totalCommandeHorsReduc = BigDecimalUtil.add(totalCommandeHorsReduc, currMvmP.getMtt_total());
				totalReduc = BigDecimalUtil.add(totalReduc, currMvmP.getMtt_total());
			} else {
				totalCommande = BigDecimalUtil.add(totalCommande, currMvmP.getMtt_total());
				totalCommandeHorsReduc = BigDecimalUtil.add(totalCommandeHorsReduc, currMvmP.getMtt_total());
			}
		}
		if(caisseMvmP.getList_offre() != null) {
			for (CaisseMouvementOffrePersistant currMvmP : caisseMvmP.getList_offre()) {
				if(BooleanUtil.isTrue(currMvmP.getIs_annule())) {
					continue;
				}
				totalCommande = BigDecimalUtil.substract(totalCommande, currMvmP.getMtt_reduction());
				totalReduc = BigDecimalUtil.add(totalReduc, currMvmP.getMtt_reduction());
			}
		}

		// Montant commande hors réduction
        if(StringUtil.isNotEmpty(startegieArrondi)){
        	String[] strategy = StringUtil.getArrayFromStringDelim(startegieArrondi, "_");
        	int scale = Integer.valueOf(strategy[0]);
        	int round = Integer.valueOf(strategy[1]);
        	
        	totalCommande = totalCommande.setScale(scale, round);
        }
        
        caisseMvmP.setMtt_commande_net(totalCommande);
        caisseMvmP.setMtt_commande(totalCommandeHorsReduc);
        caisseMvmP.setMtt_reduction(totalReduc);
        
        if(BooleanUtil.isTrue(caisseMvmP.getIs_retour())){
        	caisseMvmP.negateMtt();
        }
	}
}

