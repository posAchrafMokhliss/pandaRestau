package appli.controller.domaine.caisse.action;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hibernate.criterion.Order;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import appli.controller.domaine.administration.bean.UserBean;
import appli.controller.domaine.administration.bean.ValTypeEnumBean;
import appli.controller.domaine.caisse.ContextAppliCaisse;
import appli.controller.domaine.caisse.bean.CaisseBean;
import appli.controller.domaine.caisse.bean.CaisseJourneeBean;
import appli.controller.domaine.caisse.bean.CmdBean;
import appli.controller.domaine.caisse.bean.MenuCmdBean;
import appli.controller.domaine.fidelite.bean.CarteFideliteClientBean;
import appli.controller.domaine.personnel.bean.ClientBean;
import appli.controller.domaine.stock.bean.ArticleStockInfoBean;
import appli.controller.domaine.stock.bean.ChargeDiversBean;
import appli.controller.domaine.util_erp.ContextAppli;
import appli.controller.domaine.util_erp.ContextAppli.SOFT_ENVS;
import appli.controller.domaine.util_erp.ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM;
import appli.controller.domaine.util_erp.ContextAppli.TYPE_LIGNE_COMMANDE;
import appli.controller.domaine.util_erp.ContextAppli.TYPE_NOTIFICATION;
import appli.model.domaine.administration.persistant.AgencementPersistant;
import appli.model.domaine.administration.persistant.ParametragePersistant;
import appli.model.domaine.administration.persistant.UserPersistant;
import appli.model.domaine.administration.persistant.ValTypeEnumPersistant;
import appli.model.domaine.administration.service.IClientService;
import appli.model.domaine.administration.service.IParametrageService;
import appli.model.domaine.administration.service.IUserService;
import appli.model.domaine.administration.service.IValTypeEnumService;
import appli.model.domaine.caisse.persistant.ArticleStockCaisseInfoPersistant;
import appli.model.domaine.caisse.service.IArticle2Service;
import appli.model.domaine.caisse.service.ICaisseMouvementService;
import appli.model.domaine.caisse.service.ICaisseService;
import appli.model.domaine.caisse.service.ICaisseWebService;
import appli.model.domaine.caisse.service.IJourneeService;
import appli.model.domaine.caisse.service.ITicketCaisseService;
import appli.model.domaine.caisse.service.PrintCuisineUtil;
import appli.model.domaine.caisse.service.impl.JourneeService;
import appli.model.domaine.caisse.service.impl.NotificationQueuService;
import appli.model.domaine.fidelite.dao.IPortefeuille2Service;
import appli.model.domaine.fidelite.persistant.CarteFideliteClientPersistant;
import appli.model.domaine.fidelite.persistant.CarteFidelitePersistant;
import appli.model.domaine.fidelite.service.ICarteFideliteClientService;
import appli.model.domaine.fidelite.service.ICarteFideliteService;
import appli.model.domaine.habilitation.persistant.ProfilePersistant;
import appli.model.domaine.personnel.persistant.ClientPersistant;
import appli.model.domaine.personnel.persistant.EmployePersistant;
import appli.model.domaine.personnel.persistant.OffrePersistant;
import appli.model.domaine.personnel.persistant.SocieteLivrPersistant;
import appli.model.domaine.personnel.service.ISocieteLivrService;
import appli.model.domaine.stock.persistant.ArticleClientPrixPersistant;
import appli.model.domaine.stock.persistant.ArticleDetailPersistant;
import appli.model.domaine.stock.persistant.ArticlePersistant;
import appli.model.domaine.stock.persistant.ArticleStockInfoPersistant;
import appli.model.domaine.stock.persistant.FamillePersistant;
import appli.model.domaine.stock.service.IArticleService;
import appli.model.domaine.stock.service.IFamilleService;
import appli.model.domaine.stock.service.IMouvementService;
import appli.model.domaine.stock.service.impl.FactureVentePDF;
import appli.model.domaine.util_srv.printCom.ticket.PrintTicketCaisseCustomUtil;
import appli.model.domaine.util_srv.printCom.ticket.PrintTicketUtil;
import appli.model.domaine.util_srv.printCom.ticket.PrintTicketWifi;
import appli.model.domaine.vente.persistant.CaisseJourneePersistant;
import appli.model.domaine.vente.persistant.CaisseMouvementArticlePersistant;
import appli.model.domaine.vente.persistant.CaisseMouvementOffrePersistant;
import appli.model.domaine.vente.persistant.CaisseMouvementPersistant;
import appli.model.domaine.vente.persistant.CaissePersistant;
import appli.model.domaine.vente.persistant.JourneePersistant;
import appli.model.domaine.vente.persistant.TicketCaisseConfPersistant;
import framework.component.complex.table.RequestTableBean;
import framework.controller.ActionBase;
import framework.controller.ActionUtil;
import framework.controller.ContextGloabalAppli;
import framework.controller.ControllerUtil;
import framework.controller.annotation.WorkForward;
import framework.controller.bean.PagerBean;
import framework.model.beanContext.AbonnementBean;
import framework.model.beanContext.DataValuesPersistant;
import framework.model.beanContext.EtablissementPersistant;
import framework.model.beanContext.VillePersistant;
import framework.model.common.constante.ProjectConstante;
import framework.model.common.constante.ProjectConstante.MSG_TYPE;
import framework.model.common.service.MessageService;
import framework.model.common.util.BigDecimalUtil;
import framework.model.common.util.BooleanUtil;
import framework.model.common.util.ControllerBeanUtil;
import framework.model.common.util.DateUtil;
import framework.model.common.util.EncryptionEtsUtil;
import framework.model.common.util.EncryptionUtil;
import framework.model.common.util.NumericUtil;
import framework.model.common.util.ReflectUtil;
import framework.model.common.util.ServiceUtil;
import framework.model.common.util.StrimUtil;
import framework.model.common.util.StringUtil;
import framework.model.util.ModelConstante;
import framework.model.util.audit.ReplicationGenerationEventListener;
import framework.model.util.printGen.PrintPosBean;

public class CaisseWebBaseAction extends ActionBase {
	public static Integer NBR_ELEMENTS = 16; 
	@Inject
	private ITicketCaisseService ticketCaisseService;
	@Inject
	private IArticleService articleService;
	@Inject
	private IValTypeEnumService valTypeEnum;
	@Inject
	private IValTypeEnumService valEnumService;
	@Inject
	private ICaisseWebService caisseWebService;
	@Inject
	private ICarteFideliteClientService carteClientService;
	@Inject
	private IPortefeuille2Service portefeuilleService2;
	@Inject
	private ICaisseService caisseService;
	@Inject
	private IFamilleService familleService;
	@Inject
	private IUserService userService;
	@Inject
	private IClientService clientService;
	@Inject
	private ISocieteLivrService societeLivrsService;
	@Inject
	private ICaisseMouvementService caisseMvmtService;
	@Inject
	private ICarteFideliteService carteFideliteService;
	@Inject
	private IArticle2Service articleService2;
	@Inject
	private IParametrageService paramService;
	@Inject
	private IJourneeService journeeService;
	@Inject
	private IMouvementService mouvementService;
	
	public String getCommande_detail_path(ActionUtil httpUtil){
		return httpUtil.getUserAttribute("PATH_JSP_CAISSE")+"/commande-detail.jsp";
	}
	public String getDetail_choix_path(ActionUtil httpUtil){
		return httpUtil.getUserAttribute("PATH_JSP_CAISSE")+"/detail-choix.jsp";
	}
	public String getRight_bloc_path(ActionUtil httpUtil){
		if(httpUtil.getParameter("sens") == null){
			return httpUtil.getUserAttribute("PATH_JSP_CAISSE")+"/caisse-right-bloc.jsp";
		} else{
			if("FAV".equals(httpUtil.getParameter("src"))){
				return httpUtil.getUserAttribute("PATH_JSP_CAISSE")+"/pager-favoris-include.jsp";
			} else{
				return httpUtil.getUserAttribute("PATH_JSP_CAISSE")+"/pager-famille-include.jsp";				
			}
		}
	}
	public String getPaiement_path(ActionUtil httpUtil){
		return "/domaine/caisse/"+httpUtil.getUserAttribute("PATH_JSP_CM")+"/paiement-edit.jsp";
	}
	
	public void work_init(ActionUtil httpUtil) {
		httpUtil.setFormReadOnly();
	}

	/**
	 * @param httpUtil
	 */
//	@SuppressWarnings("unchecked")
//	public void loadFamille(ActionUtil httpUtil) {
//		
//		Long famId = httpUtil.getWorkIdLong();
//		FamillePersistant familleParent = (FamillePersistant) familleService.getGenriqueDao().findById(famId);			
//
//		// Purge infos steps
//		httpUtil.removeUserAttribute("STEP_MNU");
//		httpUtil.removeUserAttribute("LIST_SOUS_MENU");
//				
//		// Données de la session
//		manageDataSession(httpUtil);
//		//
//		httpUtil.removeUserAttribute("CURRENT_MENU_COMPOSITION");
//		httpUtil.removeUserAttribute("CURRENT_MENU_NUM");
//
//		List<FamillePersistant> listSousFamille = familleService2.getFamilleEnfants(famId, ContextAppliCaisse.getCaisseBean().getId(), pagerBeanFam);
//		((List<String>) httpUtil.getUserAttribute("HISTORIQUE_NAV")).add("FAM_" + famId);
//		List<ArticlePersistant> listArticleActifs = articleService.getListArticleActifs(familleParent.getId());
//		
//		httpUtil.setRequestAttribute("listFamille", listSousFamille);
//		httpUtil.setRequestAttribute("listArticle", listArticleActifs);
//
//		// Pour le mobile
//		httpUtil.setRequestAttribute("isLoadEvent", true);
//		
//		httpUtil.setDynamicUrl(getDetail_choix_path(httpUtil));
//	}
	
	/**---------------------------------------------------------------------------------------------------------------*/
	@SuppressWarnings({ "unchecked" })
	public void familleEvent(ActionUtil httpUtil) {
		boolean isRestau = SOFT_ENVS.restau.toString().equals(StrimUtil.getGlobalConfigPropertie("context.soft"));
		Long famId = httpUtil.getWorkIdLong();
		
		if (famId == null) {
			httpUtil.setDynamicUrl(getDetail_choix_path(httpUtil));
			return;
		}
		
		FamillePersistant familleParent = familleService.findById(FamillePersistant.class, famId);			

		if (familleParent == null) {
			httpUtil.setDynamicUrl(getDetail_choix_path(httpUtil));
			return;
		}
		boolean isTopFam = httpUtil.getParameter("is_top") != null;
		
		// Purge infos steps ---->Restau------------------
		httpUtil.removeUserAttribute("STEP_MNU");
		httpUtil.removeUserAttribute("LIST_SOUS_MENU");
		httpUtil.removeUserAttribute("CURRENT_MENU_COMPOSITION");
		httpUtil.removeUserAttribute("CURRENT_MENU_NUM");
		// ------------------------------------------------
		
		List<String> listNav = (List<String>) httpUtil.getUserAttribute("HISTORIQUE_NAV");
		if (listNav == null) {
			listNav = new ArrayList<>();
			httpUtil.setUserAttribute("HISTORIQUE_NAV", listNav);
		}
		
		if(isTopFam){
			listNav.clear();
		}
		
		listNav.add("FAM_" + famId);
		
		boolean isMobile = ContextAppli.APPLI_ENV.cais_mob.toString().equals(httpUtil.getUserAttribute("CURRENT_ENV"));
		List<FamillePersistant> listSousFamille = familleService.getFamilleEnfantsOnLevel((isRestau?"CU":"ST"), famId, true);
		List<ArticlePersistant> listArticleActifs = articleService.getListArticleActifs(famId, null, isMobile);
		
		httpUtil.setRequestAttribute("listFamille", listSousFamille);
		httpUtil.setRequestAttribute("listArticle", listArticleActifs);

		// AJouter controle stock si il est paramétré
		addCtrlStock(httpUtil, listArticleActifs);
		
		httpUtil.setDynamicUrl(getDetail_choix_path(httpUtil));
	}
	
	/** ------------------------------------------------ historique ----------------------------------------*/ 
	public void initHistorique(ActionUtil httpUtil) {
		boolean isShowCmdEncaissee = StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("SHOW_HISTO_ENCAISSE"));
		boolean isRestau = SOFT_ENVS.restau.toString().equals(StrimUtil.getGlobalConfigPropertie("context.soft"));
		String[][] listStatut = new String[ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.values().length][2];
        int i = 0;
		for (ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM st : ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.values()) {
        	listStatut[i][0] = st.toString();
        	listStatut[i][1] = ContextAppli.getLibelleStatut(st.toString());
        	i++;
        }
		
		if(!isRestau){
			listStatut = new String[3][2];
			listStatut[0] = new String[]{STATUT_CAISSE_MOUVEMENT_ENUM.ANNUL.toString(), STATUT_CAISSE_MOUVEMENT_ENUM.ANNUL.getLibelle()};
			listStatut[1] = new String[]{STATUT_CAISSE_MOUVEMENT_ENUM.TEMP.toString(), STATUT_CAISSE_MOUVEMENT_ENUM.TEMP.getLibelle()};
			listStatut[2] = new String[]{STATUT_CAISSE_MOUVEMENT_ENUM.VALIDE.toString(), STATUT_CAISSE_MOUVEMENT_ENUM.VALIDE.getLibelle()};
		} else{
			httpUtil.setRequestAttribute("listTypeCmd", new String[][]{{"P", "Sur place"}, {"E", "A emporter"}, {"L", "Livraison"}});			
		}
		
		httpUtil.setRequestAttribute("listServeur", userService.getListUserActifsByProfile("SERVEUR"));
		
		// Type commande
		String refCmdRep = httpUtil.getParameter("ref_rep_cmd");
		httpUtil.setRequestAttribute("curr_ref_rep", refCmdRep);

        String type_cmd = httpUtil.getParameter("type_cmd");
		httpUtil.setRequestAttribute("curr_typeCmd", type_cmd);

		String statutSt = httpUtil.getParameter("statut_cmd");		
		httpUtil.setRequestAttribute("curr_statut", statutSt);
        httpUtil.setRequestAttribute("listStatut", listStatut);
        
        Long serveurId = null;
        String currServeurId = httpUtil.getParameter("serveur.id");
        if(StringUtil.isNotEmpty(currServeurId)) {
			String decrypted  = EncryptionUtil.decrypt(currServeurId);
			
			if(NumericUtil.isLong(decrypted)) {// Car n'est pas decrypé correctement dans la pagination
				serveurId = Long.valueOf(decrypted);
			} else {
				serveurId = Long.valueOf(currServeurId);				
			}
		}
        httpUtil.setRequestAttribute("currServeurId", currServeurId);
        
		// Si reprise
		if(StringUtil.isTrue(httpUtil.getParameter("isrp"))) {
			httpUtil.setUserAttribute("PLAN_MODE", "REP");
		} else if(StringUtil.isFalse(httpUtil.getParameter("isrp"))){
			httpUtil.setUserAttribute("PLAN_MODE", "STD");
		}
		
		boolean isReprise = "REP".equals(httpUtil.getUserAttribute("PLAN_MODE"));
		boolean isFiltreCaisseConnexion = StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("CAISS_SELECT_DEFAUT"));
		
		Long caisseId = null;
		String caisseIdSt = httpUtil.getParameter("caisse.id");
		if(StringUtil.isNotEmpty(caisseIdSt)) {
			String decrypted  = EncryptionUtil.decrypt(caisseIdSt);
			
			if(NumericUtil.isLong(decrypted)) {// Car n'est pas decrypé correctement dans la pagination
				caisseId = Long.valueOf(decrypted);
			} else {
				caisseId = Long.valueOf(caisseIdSt);				
			}
		}
		
		if(caisseId == null && isFiltreCaisseConnexion) {
			caisseId = ContextAppliCaisse.getCaisseBean().getId();
		}
		
		JourneePersistant journeeP = ContextAppliCaisse.getJourneeBean();
		
		httpUtil.setFormReadOnly(false);
		
		httpUtil.setRequestAttribute("listCaisse", caisseService.getListCaisseActive(ContextAppli.TYPE_CAISSE_ENUM.CAISSE.toString(), true));
		RequestTableBean cplxTable = getTableBean(httpUtil, "list_mouvement");
		
		Map<String, Object> formCriterion = cplxTable.getFormBean().getFormCriterion();
		formCriterion.put("journeeId", journeeP.getId());
		//
		httpUtil.setRequestAttribute("currCaisseId", caisseId);
		//
		
		String statutTemp = ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.TEMP.toString();
		String statutAnnul = ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.ANNUL.toString();
		
		String req = "select mouvement from CaisseMouvementPersistant mouvement "
				+ "left join mouvement.opc_user.opc_profile profile1 "
				+ "left join mouvement.opc_user.opc_profile2 profile2 "
				+ "left join mouvement.opc_user.opc_profile3 profile3 "
				+ "where mouvement.opc_caisse_journee.opc_journee.id='[journeeId]' ";
		
		// Si serveur ou caissier avec paramètrage alors on filtre les commande
		boolean isFilterCaissier = StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("CAISS_FILTER_COM"));
		boolean isCaissier = ContextAppli.getUserBean().isInProfile("CAISSIER");
		boolean isServeur = ContextAppli.getUserBean().isInProfile("SERVEUR");
		
		boolean isPassed = false;
		if(isCaissier && isFilterCaissier){
			req = req + " and ("
					+ "	mouvement.opc_user.id='{userId}' "
					+ "OR ("
						+ "(profile1.code='SERVEUR' OR profile2.code='SERVEUR' OR profile3.code='SERVEUR') "
						+ "and profile1.code!='CAISSIER' and profile2.code!='CAISSIER' and profile3.code!='CAISSIER' "
					+ ") "
					+ ") ";
			formCriterion.put("userId", ContextAppli.getUserBean().getId());
			isPassed = true;
		}
		
		if(!isPassed && !isCaissier && isServeur){
			req = req + " and mouvement.opc_serveur.id='{serveurId}' ";
			formCriterion.put("serveurId", ContextAppli.getUserBean().getId());
		}
		
		if(StringUtil.isNotEmpty(refCmdRep)) {
			req = req + " and mouvement.ref_commande like '[refCmdRep]' ";
			formCriterion.put("refCmdRep", "%"+refCmdRep+"%");
		}
		
		if(caisseId != null){
			req = req +  "and mouvement.opc_caisse_journee.opc_caisse.id='[caisseId]' ";
			formCriterion.put("caisseId", caisseId);
		}
		if(serveurId != null){
			req = req +  "and mouvement.opc_serveur.id='[serveurId]' ";
			formCriterion.put("serveurId", serveurId);
		}
		
		if(isReprise) {
			CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
			if(CURRENT_COMMANDE != null && CURRENT_COMMANDE.getId() != null) {
				req = req + "and mouvement.id!="+CURRENT_COMMANDE.getId()+" ";
			}
			//
			req = req + "and (mouvement.last_statut = '"+statutTemp+"' or (mouvement.last_statut != '"+statutAnnul+"' and mouvement.mode_paiement is null)) ";// temporaire ou non payée (cas restau ou caisse autonome)
		}
		
		if(StringUtil.isNotEmpty(statutSt)){// Filtre statut depuis historique
			req = req + "and mouvement.last_statut='"+statutSt+"' ";
		}
		if(StringUtil.isNotEmpty(type_cmd)){
			req = req + "and mouvement.type_commande='"+type_cmd+"' ";
		}
		
		if(!isShowCmdEncaissee) {
			req = req + "and mouvement.mode_paiement is null ";
		}
		
		req = req + "order by mouvement.id desc";
		
		List<CaisseMouvementPersistant> listMouvement = (List<CaisseMouvementPersistant>) familleService.findByCriteria(cplxTable, req, false);
		httpUtil.setRequestAttribute("listMouvement", listMouvement);
		
		httpUtil.setDynamicUrl("/domaine/caisse/"+httpUtil.getUserAttribute("PATH_JSP_CM")+"/historique-list.jsp");
	}
	public void selectHistorique(ActionUtil httpUtil) {
		Long mvmId = httpUtil.getWorkIdLong();
		httpUtil.setRequestAttribute("caisseMouvement", familleService.findById(CaisseMouvementPersistant.class, mvmId));
		httpUtil.setDynamicUrl("/domaine/caisse/"+httpUtil.getUserAttribute("PATH_JSP_CM")+"/historique-edit.jsp");
	}
	public void editTrHistorique(ActionUtil httpUtil) {
		Long mvmId = httpUtil.getLongParameter("art");
		httpUtil.setRequestAttribute("caisseMouvement", familleService.findById(CaisseMouvementPersistant.class, mvmId));
		httpUtil.setDynamicUrl("/domaine/caisse/"+httpUtil.getUserAttribute("PATH_JSP_CM")+"/historique-tr.jsp");
	}
	
	/** ------------------------------------------------ Shift ----------------------------------------*/ 
	public void managerEmbededPrinter(ActionUtil httpUtil) {
		boolean isEmbedPrint = httpUtil.getUserAttribute("IS_EMBDED_MOBILE_PRINTER") != null;
		if(isEmbedPrint) {
			httpUtil.removeUserAttribute("IS_EMBDED_MOBILE_PRINTER");
		} else {
			httpUtil.setUserAttribute("IS_EMBDED_MOBILE_PRINTER", true);
		}
		httpUtil.writeResponse("MSG_CUSTOM:Les options sont mises à jour.");
		return;
	}
	public void init_opts(ActionUtil httpUtil) {
		ParametragePersistant paramPWifi = paramService.getParameterByCode("WIFI");
		Long caisseId = ContextAppliCaisse.getCaisseBean().getId();
		ParametragePersistant paramPHFamille = paramService.getParameterByCode("HAUTEUR_BLOC_FAMILLE", caisseId);
		ParametragePersistant paramPVeille = paramService.getParameterByCode("AFFICHER_IMAGE_VEILLE", caisseId);
		ParametragePersistant paramPPage = paramService.getParameterByCode("NBR_ELEMENT_PAGE_FAM", caisseId);
		
		httpUtil.setRequestAttribute("paramWifi", paramPWifi);
		httpUtil.setRequestAttribute("paramHauteur", paramPHFamille);
		httpUtil.setRequestAttribute("paramVeille", paramPVeille);
		httpUtil.setRequestAttribute("paramPagger", paramPPage);
		
		httpUtil.setFormReadOnly(false);
		
		if(httpUtil.getParameter("isSub") != null){
			String code_wifi = httpUtil.getParameter("code_wifi");
			String hauteur_fam = httpUtil.getParameter("hauteur_fam");
			String param_veille = httpUtil.getParameter("param_veille");
			String param_nbr_fam = httpUtil.getParameter("param_nbr_fam");
			
			if(StringUtil.isEmpty(hauteur_fam)) {
				hauteur_fam = "413";
			}
			if(StringUtil.isEmpty(param_nbr_fam)) {
				param_nbr_fam = "20";
			}
			
			paramPHFamille.setOpc_terminal(ContextAppliCaisse.getCaisseBean());
			paramPVeille.setOpc_terminal(ContextAppliCaisse.getCaisseBean());
			paramPPage.setOpc_terminal(ContextAppliCaisse.getCaisseBean());
			
			if(paramPWifi != null) {
				paramPWifi.setValeur(code_wifi);
			}
			paramPHFamille.setValeur(hauteur_fam);
			paramPVeille.setValeur(param_veille);
			paramPPage.setValeur(param_nbr_fam);
			
			paramService.mergeParams(paramPWifi, paramPHFamille, paramPVeille, paramPPage);
			
			httpUtil.writeResponse("MSG_CUSTOM:Les options sont mises à jour.");
			return;
		}
		httpUtil.setDynamicUrl("/domaine/caisse/"+httpUtil.getUserAttribute("PATH_JSP_CM")+"/options-caisse.jsp");
	}
	public void printCodeWifi(ActionUtil httpUtil) {
		PrintTicketWifi pw = new PrintTicketWifi(ContextAppliCaisse.getCaisseBean());
		if(printData(httpUtil, pw.getPrintPosBean())) {
			return;
		}
		
		boolean isAsync = printData(httpUtil, pw.getPrintPosBean());
		if(isAsync) {
			forwardToPriterJsp(httpUtil);
		} else {
			httpUtil.writeResponse("MSG_CUSTOM:Code wifi imprimé.");
		}
	}
	
	public void initShift(ActionUtil httpUtil) {
		JourneePersistant currentJournee = ContextAppliCaisse.getJourneeBean();
		if(currentJournee == null) {
			MessageService.addGrowlMessage("Journée fermée", "Aucune journée ouverte n'a été trouvée.");
			return;
		}
		
		CaissePersistant currentCaisse = ContextAppliCaisse.getCaisseBean();
		CaisseJourneePersistant currentJourneeP = caisseService.getJourneCaisseOuverte(currentCaisse.getId());
		
		String tp = "clo";
		if(currentJourneeP != null && currentJourneeP.getStatut_caisse().equals(ContextAppli.STATUT_JOURNEE.OUVERTE.getStatut()) 
				&& currentJourneeP.getOpc_journee().getStatut_journee().equals(ContextAppli.STATUT_JOURNEE.OUVERTE.getStatut())){
			tp = "ouv";
			
			boolean isAutoPassation = StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("SHIFT_PASSASION"));
			if(isAutoPassation){
				if(caisseService.getListCmdTemp(currentJourneeP.getId()).size() > 0 
						|| caisseService.getListCmdNonPaye(currentJourneeP.getId()).size() > 0){
					httpUtil.setRequestAttribute("isPass", true);
					httpUtil.setRequestAttribute("listUser", userService.getListUserActifsByProfile("CAISSIER"));
				}
			}
		}
		httpUtil.setFormReadOnly(false);
		
		httpUtil.setRequestAttribute("tp", tp);
		
		boolean isServeurProfil = ContextAppli.getUserBean().isInProfile("SERVEUR");
		if(isServeurProfil){
			httpUtil.setDynamicUrl("/domaine/caisse/"+httpUtil.getUserAttribute("PATH_JSP_CM")+"/gestion-shift-livreur-edit.jsp");
		} else{
			httpUtil.setDynamicUrl("/domaine/caisse/"+httpUtil.getUserAttribute("PATH_JSP_CM")+"/gestion-shift-edit.jsp");			
		}
	}
	
	@WorkForward(useBean=true, useFormValidator=true, bean=CaisseJourneeBean.class)
	public void ouvrirCloreShift(ActionUtil httpUtil) {
		String tp = httpUtil.getParameter("tp");
		Map params = (Map)httpUtil.getRequest().getAttribute(ProjectConstante.WORK_PARAMS);
		CaisseJourneeBean caisseJBean = ControllerBeanUtil.mapToBean(CaisseJourneeBean.class, params); 
		CaissePersistant currentCaisse = ContextAppliCaisse.getCaisseBean();
		CaisseJourneePersistant currentCaisseJourneeP = caisseService.getJourneCaisseOuverte(currentCaisse.getId());
		CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
		//
		if(tp.equals("clo")) {
	         if(currentCaisseJourneeP != null){
	        	 MessageService.addBannerMessage("Le shift est déjà ouvert.");
	             return;
	         }
	         caisseService.ouvrirCaisse(currentCaisse.getId(), caisseJBean.getMtt_cloture_caissier_espece());
		} else {
			boolean isPassasionSub = StringUtil.isTrue(httpUtil.getParameter("isPass"));
			if(!isPassasionSub){
				if(CURRENT_COMMANDE != null && CURRENT_COMMANDE.getList_article().size() > 0) {
					MessageService.addBannerMessage("Veuillez valider ou annuler la commande en cours avant de continuer.");
			        return;
				}
			} 
			
			if(!currentCaisseJourneeP.getStatut_caisse().equals(ContextAppli.STATUT_JOURNEE.OUVERTE.getStatut())) {
				MessageService.addBannerMessage("Le shift est déjà clos.");
				return;
			}
		
			boolean isServeurProfil = ContextAppli.getUserBean().isInProfile("SERVEUR");
			if(!isServeurProfil){
				BigDecimal total =  BigDecimalUtil.add(caisseJBean.getMtt_cloture_caissier_espece(), 
	                    caisseJBean.getMtt_cloture_caissier_cb(), 
	                    caisseJBean.getMtt_cloture_caissier_cheque(),
	                    caisseJBean.getMtt_cloture_caissier_dej());
				
				BigDecimal mttClotureCaissier = (caisseJBean.getMtt_cloture_caissier() == null ? BigDecimalUtil.ZERO : caisseJBean.getMtt_cloture_caissier());
				if(total.compareTo(mttClotureCaissier) != 0){
					MessageService.addBannerMessage("Le montant total ne correspond pas au total des montants saisis.");
				    return;
				}
				// Commande en attente
				if(!isPassasionSub){
					List<CaisseMouvementPersistant> listCmdEnAttente = caisseWebService.getListMouvementTemp(currentCaisseJourneeP.getOpc_journee().getId(), currentCaisse.getId(), null);
					if (listCmdEnAttente != null && listCmdEnAttente.size() > 0) {
						MessageService.addBannerMessage("Vous devez valider ou supprimer les commandes en cours ou en attente avant de clore le shift.");
					    return;
					}
				} else{
					if(StringUtil.isEmpty(httpUtil.getLongParameter("userPass.id"))){
						MessageService.addBannerMessage("Merci de sélectionner le prochain caissier.");
						return;
					}
				}
			}
			
			// Si Mode passasion alors on passe les mouvements vers le nouveau shift
			if(isPassasionSub){
				caisseService.gererPassasionShift(httpUtil.getLongParameter("userPass.id"), 
						currentCaisseJourneeP, 
						BigDecimalUtil.get(httpUtil.getParameter("mttOuvertureCaissier")), false);
			}
			
			//
			caisseService.cloturerDefinitive(
					currentCaisseJourneeP, false,
					caisseJBean.getMtt_cloture_caissier_espece(), 
					caisseJBean.getMtt_cloture_caissier_cb(), 
					caisseJBean.getMtt_cloture_caissier_cheque(), 
					caisseJBean.getMtt_cloture_caissier_dej(), false, isPassasionSub);
			
			//purger les images des codes-barre utilisé dans les tickets de commande
			for (CaisseMouvementPersistant caisseMnmtP : currentCaisseJourneeP.getList_caisse_mouvement()) {
				caisseMvmtService.deleteImageCodeBarre(caisseMnmtP);
			}
			
			// Deconnecter la caisse
			HttpServletRequest request = httpUtil.getRequest();
			HttpSession session = request.getSession(false);
			if(session != null){
				session.invalidate();
			}
		}
		// Mettre la nouvelle journée caisse dans la session
		CaisseJourneePersistant caisseJourneeP = (CaisseJourneePersistant) caisseService.findAll(CaisseJourneePersistant.class, Order.desc("id")).get(0);
		MessageService.getGlobalMap().put("CURRENT_JOURNEE_CAISSE", caisseJourneeP);
		//
		httpUtil.writeResponse("REDIRECT:");
	}
	
	/** ------------------------------------------------Paiement ----------------------------------------*/
	public void ouvrirTiroirCaisse(ActionUtil httpUtil) { 
		caisseService.updateNbrOuvertureCaisse(ContextAppliCaisse.getJourneeCaisseBean().getId());
		//
		boolean isAsync = openDash(httpUtil, ContextAppliCaisse.getCaisseBean().getImprimantes());
		if(!isAsync) {
			httpUtil.writeResponse("");
		}
	}
	
	/**
	 * @param httpUtil
	 */
	public void majTotalMontantCommande(ActionUtil httpUtil, CaisseMouvementPersistant CURRENT_COMMANDE) {
		JourneeService.recalculChiffresMvmJournee(CURRENT_COMMANDE);
		// Maj afficheur
		if(StringUtil.isNotEmpty(httpUtil.getUserAttribute("CURRENT_ART_TRACK"))){
			sendDataToScreen(httpUtil, "T");
		}
	}
	
	/**
	 * @param httpUtil
	 */
	public void init_add_depense(ActionUtil httpUtil) {
		
		Map<String, String> mapType = new LinkedHashMap<>();
		if(StringUtil.isTrueOrNull(ContextGloabalAppli.getGlobalConfig("RIGHT_ACHAT_ART_LIBRE"))){
			mapType.put("V", "Vente/Recette");
		}
		if(StringUtil.isTrueOrNull(ContextGloabalAppli.getGlobalConfig("RIGHT_VENTE_ART_LIBRE"))){
			mapType.put("A", "Achat/Dépense");
		}
		int i = 0;
		String[][] dataArray = new String[mapType.size()][2];
		for (String key : mapType.keySet()) {
			dataArray[i][0] = key;
			dataArray[i][1] = mapType.get(key);
			i++;
		}
		httpUtil.setRequestAttribute("typeCharge", dataArray);
		
		String[][] typeLibPredef = {{"GARANTIE", "RETOUR GARANTIE"}, {"POURBOIR", "POURBOIR"}};
		httpUtil.setRequestAttribute("typeLibPredef", typeLibPredef);
		
		httpUtil.setFormReadOnly(false);
		httpUtil.setDynamicUrl("/domaine/caisse/normal/depense_add.jsp");
	}
	
	public void init_fav(ActionUtil httpUtil) {
		PagerBean pagerBean = ControllerUtil.managePager(httpUtil.getRequest(), "FAV");
		httpUtil.setRequestAttribute("listArticleFavoris", caisseService.getFavorisCaisse(pagerBean));
		
		httpUtil.setDynamicUrl(ControllerUtil.getUserAttribute("PATH_JSP_CAISSE", httpUtil.getRequest())+"/pager-favoris-include.jsp");
	}
	/**
	 * @param httpUtil
	 */
	public void init_add_retour(ActionUtil httpUtil) {
//		Long workId = httpUtil.getWorkIdLong();
//		if(workId != null) {
//			MouvementPersistant mvmP = caisseService.findById(MouvementPersistant.class, workId);
//			MouvementBean mvmBean = ServiceUtil.persistantToBean(MouvementBean.class, mvmP);
//			CaisseMouvementPersistant caisseMvmP = caisseMvmtService.getCommandeByReference(mvmBean.getRetour_ref_cmd());
//			httpUtil.setRequestAttribute("caisseMouvement_id", caisseMvmP.getId());
//			httpUtil.setViewBean(mvmBean);
//			
//			if(StringUtil.isEmpty(httpUtil.getParameter("isUpd"))){
//				httpUtil.setFormReadOnly(true);
//				httpUtil.setRequestAttribute("isEdit", true);
//				List<ArticlePersistant> listArticle = articleService.getListArticleNonStock(true);
//				httpUtil.setRequestAttribute("listArticle", listArticle);
//			} else {
//				httpUtil.setRequestAttribute("ref_commande", caisseMvmP.getRef_commande());
//				CaisseMouvementBean viewBean = caisseMvmtService.findById(caisseMvmP.getId());
//				httpUtil.setRequestAttribute("caisseMouvement", viewBean);
//				httpUtil.setRequestAttribute("isCodeFounded", true);
//				httpUtil.setRequestAttribute("isUpd", true);
//			}
//		}
		httpUtil.setFormReadOnly(false);
		
//		httpUtil.setRequestAttribute("editFromPaiement", true);
		httpUtil.setDynamicUrl("/domaine/caisse/normal/retour_cmd_add.add.jsp");
	}
	/**
	 * @param httpUtil
	 */
	public void init_add_article(ActionUtil httpUtil) {
//		boolean isRestau = SOFT_ENVS.restau.toString().equals(StrimUtil.getGlobalConfigPropertie("context.soft"));
		List<FamillePersistant> listFamille = familleService.getListeFamille("ST", true, true);
		httpUtil.setRequestAttribute("listeFaimlle", listFamille);
		List<ValTypeEnumBean> listValeurs = valEnumService.getListValeursByType(ModelConstante.ENUM_UNITE);
		httpUtil.setRequestAttribute("listeUnite", listValeurs);
		List<ValTypeEnumBean> listTva = valEnumService.getListValeursByType(ModelConstante.ENUM_TVA);
		httpUtil.setRequestAttribute("listeTva", listTva);
		
		httpUtil.setFormReadOnly(false);
		
		httpUtil.setDynamicUrl("/domaine/caisse/normal/composant_add.jsp");
	}
	
	/**
	 * @param httpUtil
	 */
    public void restituerInfosCommande(ActionUtil httpUtil) {
    	CaisseMouvementPersistant currCmd = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
    	//
    	if(currCmd != null && currCmd.getList_article().size() > 0) {
    		MessageService.addGrowlMessage("", "<h3>Vous devez d'abord annuler ou mettre en attente la commande en cours.</h3>");
    		return;
    	}
    	
        // Reset infos
        resetInfosSession(httpUtil);
        
    	Long mvmId = httpUtil.getWorkIdLong();
    	CaisseMouvementPersistant caisseMvm = (CaisseMouvementPersistant) familleService.findById(CaisseMouvementPersistant.class, mvmId);
    	
    	if(caisseMvm != null) {//-----------------LOCK----------------------
			UserPersistant userLock = caisseMvm.getOpc_user_lock();
    		if(userLock != null) {// && userLock.getId() != ContextAppli.getUserBean().getId()) {
    			MessageService.addGrowlMessage("", "<h3>Cette commande est déjà reprise par ** "+userLock.getLogin()+" **.</h3>");
    			return;
    		}
    		
    		caisseMvm.setOpc_user_lock(ContextAppli.getUserBean());
    		caisseService.mergeEntity(caisseMvm);
    	}
    	//---------------------------------------------------------------------
    	
    	httpUtil.setUserAttribute("CURRENT_COMMANDE", caisseMvm);
    	
    	// Restituer la table + client
    	Integer idxClient = null;
    	String refTable = null;
    	for(CaisseMouvementArticlePersistant caisseMvmP : caisseMvm.getList_article()){
    		if(BooleanUtil.isTrue(caisseMvmP.getIs_annule())){
    			continue;
    		}
    		if(StringUtil.isNotEmpty(caisseMvmP.getIdx_client())){
    			idxClient = caisseMvmP.getIdx_client();
    			refTable = caisseMvmP.getRef_table();
    		}
    	}
    	
    	httpUtil.setUserAttribute("CURRENT_TABLE_REF", refTable);
    	httpUtil.setUserAttribute("CURRENT_IDX_CLIENT", idxClient);
    	
    	httpUtil.addJavaScript("$('#home_lnk').trigger('click');");
    	httpUtil.addJavaScript("$('#close_modal').trigger('click');");
    	
    	httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
    }
    
    /**
     * @param httpUtil
     */
	public void print(ActionUtil httpUtil) {
		Long mvmId = httpUtil.getLongParameter("mvm");
		if(mvmId == null){
			mvmId = httpUtil.getWorkIdLong();
		}
		String cliIdx = httpUtil.getParameter("cli");
				
		
		if(mvmId == null) {
			httpUtil.writeResponse("ok");
			return;
		}
		
		CaisseMouvementPersistant mvmP = (CaisseMouvementPersistant) caisseService.findById(CaisseMouvementPersistant.class, mvmId);
		mvmP.getList_article().size();// Pour forcer le rechargement des lists
		mvmP.getList_offre().size();
		//
		CarteFideliteClientPersistant carteClientP = null;
		//
		if(mvmP.getOpc_client() != null){
			boolean isPoints =  StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("POINTS"));
			boolean isPortefeuille =  StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("PORTEFEUILLE"));
			//			
			carteClientP = isPoints ? carteClientService.getCarteClientActive(mvmP.getOpc_client().getId()) : null;
		}
		
		if(StringUtil.isNotEmpty(cliIdx)){
			Integer cliIdxInt = Integer.valueOf(cliIdx);
			List<CaisseMouvementArticlePersistant> listCli = new ArrayList<>();
			for(CaisseMouvementArticlePersistant det : mvmP.getList_article()){
				if(!BooleanUtil.isTrue(det.getIs_annule()) && det.getIdx_client().equals(cliIdxInt)){
					listCli.add(det);
				}
			}
			mvmP.setListEncaisse(listCli);
		}

    	if(ContextAppliCaisse.getCaisseBean() == null){
    		MessageService.getGlobalMap().put("CURRENT_CAISSE", mvmP.getOpc_caisse_journee().getOpc_caisse());
    	}
		PrintTicketUtil pu = new PrintTicketUtil(mvmP, carteClientP);
		boolean isAsync = printData(httpUtil, pu.getPrintPosBean());
		if(!isAsync) {
			httpUtil.writeResponse("");
		} else {
			forwardToPriterJsp(httpUtil);
		}
	}
	
	public void printTicketCaissePersonalise(ActionUtil httpUtil) {
		
		CaisseMouvementPersistant currCmd = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
		
		Integer clientIdx = httpUtil.getIntParameter("cli");
		List<TicketCaisseConfPersistant> listTicketCaisseConf = (List<TicketCaisseConfPersistant>)ticketCaisseService.findAll(TicketCaisseConfPersistant.class);
		
		PrintTicketCaisseCustomUtil pu = new PrintTicketCaisseCustomUtil(listTicketCaisseConf, currCmd, clientIdx);
		boolean isAsync = printData(httpUtil, pu.getPrintPosBean());
		if(!isAsync) {
			httpUtil.writeResponse("");
		} else {
			forwardToPriterJsp(httpUtil);
		}
	}
	
	/** ------------------------------------------------ Personne ----------------------------------------*/ 
	public void initPersonne(ActionUtil httpUtil) {
		
		MessageService.getGlobalMap().put("NO_ETS", true);
		httpUtil.setRequestAttribute("listVille", familleService.getListData(VillePersistant.class, "opc_region.libelle, libelle"));
		MessageService.getGlobalMap().remove("NO_ETS");
		
		httpUtil.setRequestAttribute("list_mode_paiement", new String[][]{{"CARTE", "CARTE"}, {"CHEQUE", "CHEQUE"}, {"CHEQUE. DEJ", "CHEQUE. DEJ"}, {"ESPECES", "ESPECES"}});
		httpUtil.setRequestAttribute("liste_carte", familleService.findAll(CarteFidelitePersistant.class, Order.asc("libelle")));
		
		String type = httpUtil.getParameter("tp");
		CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
		
		String filterVal = httpUtil.getParameter("tbl_filter");
		filterVal = StringUtil.isNotEmpty(filterVal) ? filterVal.toUpperCase() : filterVal;
		
		if(StringUtil.isEmpty(type)) {
			if(StringUtil.isTrueOrNull(ContextGloabalAppli.getGlobalConfig("CAISSE_CLIENT"))) {
				type = "cli";
			} else if(StringUtil.isTrueOrNull(ContextGloabalAppli.getGlobalConfig("CAISSE_EMPLOYE"))) {
				type = "empl";
			} else if(StringUtil.isTrueOrNull(ContextGloabalAppli.getGlobalConfig("CAISSE_LIVR"))) {
				type = "livr";
			} else if(StringUtil.isTrueOrNull(ContextGloabalAppli.getGlobalConfig("CAISSE_SOC_LIVR"))) {
				type = "socLivr";
			} else if(StringUtil.isTrueOrNull(ContextGloabalAppli.getGlobalConfig("CAISSE_EMPL_SERV"))) {
				type = "serv";
			}
		}
		
		httpUtil.setUserAttribute("typePers", type);
		type = (String)httpUtil.getUserAttribute("typePers");
		
		if("empl".equals(type)) {
			String req = "from EmployePersistant employe where employe.date_sortie is null ";
			
			if(CURRENT_COMMANDE != null && CURRENT_COMMANDE.getOpc_employe() != null) {
				req = req + " and employe.id!="+CURRENT_COMMANDE.getOpc_employe().getId();
			}
			
			if(StringUtil.isNotEmpty(filterVal)) {
				req = req + " and (numero like '"+filterVal+"%' "
						+ "or cin like '"+filterVal+"%' "
						+ "or upper(nom) like '"+filterVal+"%' "
						+ "or telephone like '"+filterVal+"%' "
						+ "or upper(prenom) like '"+filterVal+"%')";
			}
			
			req = req + " order by employe.numero, employe.nom, employe.prenom";
			
			RequestTableBean cplxTable = getTableBean(httpUtil, "list_employe");
			List<EmployePersistant> listEmploye = (List<EmployePersistant>) familleService.findByCriteria(cplxTable, req);
			
			httpUtil.setRequestAttribute("listEmploye", listEmploye);
		} else if("socLivr".equals(type)){
			String req = "from SocieteLivrPersistant societeLivr "
					+ "where (societeLivr.is_disable is null or societeLivr.is_disable=0) ";
			
			if(CURRENT_COMMANDE != null && CURRENT_COMMANDE.getOpc_employe() != null) {
				req = req + " and societeLivr.id!="+CURRENT_COMMANDE.getOpc_societe_livr().getId();
			}
			
			if(StringUtil.isNotEmpty(filterVal)) {
				req = req + " and upper(societeLivr.nom) like '"+filterVal+"%' ";
			}
			
			req = req + " order by societeLivr.nom";
			
			RequestTableBean cplxTable = getTableBean(httpUtil, "list_societeLiv");
			List<SocieteLivrPersistant> listSocieteLivr = (List<SocieteLivrPersistant>) familleService.findByCriteria(cplxTable, req);
			
			httpUtil.setRequestAttribute("listSocieteLivr", listSocieteLivr);
		} else if("serv".equals(type)) {
			String req = "select user from UserPersistant user "
					+ "left join user.opc_profile2 profile2 "
					+ "left join user.opc_profile3 profile3 "
					+ "where (user.is_desactive is null or user.is_desactive=0) "
					+ "and ("
					+ "		user.opc_profile.code='SERVEUR' "
					+ "		OR profile2.code='SERVEUR' "
					+ "		OR profile3.code='SERVEUR' "
					+ ") ";
			
			if(CURRENT_COMMANDE != null && CURRENT_COMMANDE.getOpc_serveur() != null) {
				req = req + " and user.id!="+CURRENT_COMMANDE.getOpc_serveur().getId();
			}
			
			if(StringUtil.isNotEmpty(filterVal)) {
				req = req + " and user.login like '"+filterVal+"%' ";
			}
			req = req + " order by user.login";
			
			RequestTableBean cplxTable = getTableBean(httpUtil, "list_serveur");
			List<UserPersistant> listServeur = (List<UserPersistant>) familleService.findByCriteria(cplxTable, req);
			
			httpUtil.setRequestAttribute("listServeur", listServeur);
		} else if("livr".equals(type)) {
			String req = "select user from UserPersistant user "
					+ "left join user.opc_profile2 profile2 "
					+ "left join user.opc_profile3 profile3 "
					+ "where (user.is_desactive is null or user.is_desactive=0) "
					+ "and ("
					+ " upper(user.opc_profile.code)='LIVREUR' "
					+ "	or upper(user.opc_profile.libelle)='LIVREUR' "
					+ " or upper(profile2.code)='LIVREUR' "
					+ " or upper(profile3.code)='LIVREUR' "
					+ ") "
					+ "and user.opc_employe is not null";
			
			if(CURRENT_COMMANDE != null && CURRENT_COMMANDE.getOpc_livreurU() != null) {
				req = req + " and user.id!="+CURRENT_COMMANDE.getOpc_livreurU().getId();
			}
			
			if(StringUtil.isNotEmpty(filterVal)) {
				req = req + " and user.login like '"+filterVal+"%' ";
			}
			req = req + " order by user.login";
			
			RequestTableBean cplxTable = getTableBean(httpUtil, "list_livreur");
			List<UserPersistant> listServeur = (List<UserPersistant>) familleService.findByCriteria(cplxTable, req);
			
			httpUtil.setRequestAttribute("listLivreur", listServeur);
		} else {
			httpUtil.setRequestAttribute("listDataValueForm", familleService.loadDataForm(null, "CLIENT"));
			
			String req = "from ClientPersistant client where (client.is_disable is null or client.is_disable=0) ";
			if(CURRENT_COMMANDE != null && CURRENT_COMMANDE.getOpc_client() != null) {
				req = req + " and client.id!="+CURRENT_COMMANDE.getOpc_client().getId();
			}
			if(StringUtil.isNotEmpty(filterVal)) {
				req = req + " and (numero like '"+filterVal+"%' "
						+ "or upper(cin) like '"+filterVal+"%' "
						+ "or upper(nom) like '"+filterVal+"%' "
						+ "or upper(prenom) like '"+filterVal+"%' "
						+ "or telephone like '"+filterVal+"%' "
						+ "or telephone2 like '"+filterVal+"%' "
					+ ")";
			}
			req = req + " order by client.nom, client.numero, client.id";
			//
			RequestTableBean cplxTable = getTableBean(httpUtil, "list_client");
			List<ClientPersistant> listClient = (List<ClientPersistant>) familleService.findByCriteria(cplxTable, req);
			
			//
			httpUtil.setRequestAttribute("listClient", listClient);
		}
        
		httpUtil.setRequestAttribute("filterVal", filterVal);
		
		if(type == null){
			if(StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("CAISSE_CLIENT"))){
				type = "cli";
			} else if(StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("CAISSE_EMPLOYE"))){
				type = "empl";
			} else if(StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("CAISSE_SOC_LIVR"))){
				type = "socLivr";
			} else if(StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("CAISSE_EMPL_SERV"))){
				type = "serv";
			} else if(StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("CAISSE_LIVR"))){
				type = "livr";
			} 
		}
		
		httpUtil.setRequestAttribute("tp", type);
		
		httpUtil.setDynamicUrl("/domaine/caisse/"+httpUtil.getUserAttribute("PATH_JSP_CM")+"/personne-list.jsp");
	}
	
	/**
	 * @param httpUtil
	 */
	public void selectModeLivraison(ActionUtil httpUtil) {
		String modeCmd = httpUtil.getParameter("mdL");
		CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
		
		CURRENT_COMMANDE.setType_commande(modeCmd);
		
		// Garantie
		ajouterFraisGarantie(httpUtil);
		
		httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
	}
	
	public void ajouterFraisGarantie(ActionUtil httpUtil) {
		CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
		List<CaisseMouvementArticlePersistant> listDetaimMvm = CURRENT_COMMANDE.getList_article();
		boolean isPassed = false;
		
		for (Iterator<CaisseMouvementArticlePersistant> iterator = CURRENT_COMMANDE.getList_article().iterator(); iterator.hasNext();) {
        	CaisseMouvementArticlePersistant cmd = iterator.next();
    		if("GAR".equals(cmd.getCode())) {
    			iterator.remove();
    			isPassed = true;
    		}
    	}
		
		// Gestion de la garantie ---------------------------------
	    String tpCmd = CURRENT_COMMANDE.getType_commande();
		if(ContextAppli.TYPE_COMMANDE.E.toString().equals(tpCmd)
	    				|| ContextAppli.TYPE_COMMANDE.L.toString().equals(tpCmd)){
	    	List<CaisseMouvementArticlePersistant> listDetaimMvmAdd = new ArrayList<>();
	    	// 
	    	for (CaisseMouvementArticlePersistant caisseMouvementArtP : listDetaimMvm) {
	    		ArticlePersistant artP = caisseMouvementArtP.getOpc_article();
				if(artP == null || BigDecimalUtil.isZero(artP.getMtt_garantie())){
	    			continue;
	    		}
				FamillePersistant opc_famille_cuisine = (ContextAppli.IS_RESTAU_ENV() ? artP.getOpc_famille_cuisine() : artP.getOpc_famille_stock());
		    	CaisseMouvementArticlePersistant cmdPGar = new CaisseMouvementArticlePersistant();
		    	cmdPGar.setCode("GAR");
	            cmdPGar.setLibelle("** Garantie pour : "+artP.getLibelle());
	            cmdPGar.setType_ligne(TYPE_LIGNE_COMMANDE.GARANTIE.toString());
				cmdPGar.setParent_code(caisseMouvementArtP.getParent_code());
	            cmdPGar.setElementId(99+opc_famille_cuisine.getId()+artP.getId());// ElementId
	            cmdPGar.setOpc_mouvement_caisse(CURRENT_COMMANDE);
	            cmdPGar.setMtt_total(artP.getMtt_garantie());
	            cmdPGar.setQuantite(BigDecimalUtil.get(1));
	            cmdPGar.setIdx_client(caisseMouvementArtP.getIdx_client()); 
	            cmdPGar.setRef_table(caisseMouvementArtP.getRef_table());
	            //
	            listDetaimMvmAdd.add(cmdPGar);
	            
	            isPassed = true;
	    	}
	    	for (CaisseMouvementArticlePersistant caisseMouvementArtP : listDetaimMvmAdd) {
	    		listDetaimMvm.add(caisseMouvementArtP);
	    	}
    	}
	    if(isPassed) {
	    	// Sort
			sortAndAddCommandeLigne(httpUtil, CURRENT_COMMANDE);
			// Maj total de la commande
			majTotalMontantCommande(httpUtil, CURRENT_COMMANDE);
    	}
	}
	
	@WorkForward(bean=ClientBean.class, useBean=true, useFormValidator=true)
	public void addClientFromCaisse(ActionUtil httpUtil) {
		IClientService clientService = (IClientService) ServiceUtil.getBusinessBean(IClientService.class);
		Map params = (Map)httpUtil.getRequest().getAttribute(ProjectConstante.WORK_PARAMS);
		ClientBean clientBeanIhm = ControllerBeanUtil.mapToBean(ClientBean.class, params);
		Long clientId = httpUtil.getWorkIdLong();
		Long carte_id = httpUtil.getLongParameter("carte_id");
		boolean isUpdate = (clientId != null);
		
		if(!isUpdate) {
			clientBeanIhm.setNumero(clientService.generateNum());
		}
		boolean isCarte = StringUtil.isNotEmpty(httpUtil.getRequest().getParameter("carte_id"));
		
		List<DataValuesPersistant> listDataValues = (List<DataValuesPersistant>) httpUtil.buildListBeanFromMap("data_value", DataValuesPersistant.class, 
				"eaiid", "data_value");
		//
		if(!isUpdate) {
			clientService.create(clientBeanIhm);
			clientId = clientBeanIhm.getId();
		} else {
			ClientBean clientBean = clientService.findById(clientId);
			clientBean.setCin(clientBeanIhm.getCin());
			clientBean.setNom(clientBeanIhm.getNom());
			clientBean.setPrenom(clientBeanIhm.getPrenom());
			clientBean.setAdresse_rue(clientBeanIhm.getAdresse_rue());
			clientBean.setAdresse_compl(clientBeanIhm.getAdresse_compl());
			clientBean.setOpc_ville(clientBeanIhm.getOpc_ville());
			clientBean.setMail(clientBeanIhm.getMail());
			clientBean.setTelephone(clientBeanIhm.getTelephone());
			clientBean.setIs_solde_neg(clientBeanIhm.getIs_solde_neg());
			clientBean.setIs_portefeuille(clientBeanIhm.getIs_portefeuille());
			//
			clientBeanIhm = clientService.update(clientBean); 
		}
		familleService.deleteDataForm(clientId, "CLIENT");
		familleService.mergeDataForm(listDataValues, clientId, "CLIENT");
		
		CarteFidelitePersistant carteP = null;
		if(isCarte){
			carteP = carteClientService.findById(CarteFidelitePersistant.class, carte_id);
		}
		
		if(isCarte){
			CarteFideliteClientPersistant cbfP = carteClientService.getCarteClientActive(clientId);
			if(cbfP == null){
				CarteFidelitePersistant cfP = (carteP==null ? carteFideliteService.getCarteOrCarteParDefaut(null) : carteP);
				
				cbfP = new CarteFideliteClientBean();
				String codeBarre = clientBeanIhm.getId()+"_"+new Random(1000).nextInt();
				cbfP.setCode_barre(codeBarre);
				cbfP.setDate_debut(new Date());
				cbfP.setOpc_carte_fidelite(cfP);
				cbfP.setOpc_client(clientBeanIhm);
				//
				cbfP = carteClientService.merge((CarteFideliteClientBean) cbfP);
			}
		}	
					
		//
		if(!isUpdate) {
			CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
			if(CURRENT_COMMANDE != null && clientId != null) {
		        ClientPersistant opcClient = (ClientPersistant) clientService.findById(ClientPersistant.class, clientId);
		        CURRENT_COMMANDE.setOpc_client(opcClient);
			}
		}
		//
		if(!isUpdate){
			//httpUtil.writeResponse("MSG_CUSTOM:Le client est ajouté.");
			MessageService.addGrowlMessage(MSG_TYPE.SUCCES, "", "Le client est ajouté.");
		} else{
			//httpUtil.writeResponse("MSG_CUSTOM:La fiche client est mise à jour.");
			MessageService.addGrowlMessage(MSG_TYPE.SUCCES, "", "La fiche client est mise à jour.");
		}
		
		if(!isUpdate) {
			httpUtil.setUserAttribute("empSelectId", clientId);
			httpUtil.setDynamicUrl("caisse-web.caisseWeb.selectPersonne");
		} else {
			httpUtil.addJavaScript("$('#close_modal').trigger('click');");
			httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
		}
		
	}
	
	public void selectPersonne(ActionUtil httpUtil) {
		String type = (String)httpUtil.getUserAttribute("typePers");
		if(type == null){
			type = "cli";
			httpUtil.setUserAttribute("typePers", type);
		}
		
		Long elementId = httpUtil.getWorkIdLong();
		if(elementId == null){
			elementId = (Long) httpUtil.getUserAttribute("empSelectId");
		}
		CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
		if("empl".equals(type)) {
			EmployePersistant opcEmploye = (EmployePersistant) familleService.findById(EmployePersistant.class, elementId);
	        CURRENT_COMMANDE.setOpc_employe(opcEmploye);
	        CURRENT_COMMANDE.setOpc_client(null);
		} else if("socLivr".equals(type)) {
			SocieteLivrPersistant opcSocieteLivr = (SocieteLivrPersistant) familleService.findById(SocieteLivrPersistant.class, elementId);
		    CURRENT_COMMANDE.setOpc_societe_livr(opcSocieteLivr);
		    CURRENT_COMMANDE.setOpc_livreurU(null);

		    manageModeLivraison(httpUtil, false);
		    
		} else if("serv".equals(type)) {
			UserPersistant userServeur = familleService.findById(UserPersistant.class, elementId);
		    CURRENT_COMMANDE.setOpc_serveur(userServeur);
		} else if("livr".equals(type)) {
			UserPersistant userLivreur = familleService.findById(UserPersistant.class, elementId);
	        CURRENT_COMMANDE.setOpc_livreurU(userLivreur);
	        CURRENT_COMMANDE.setOpc_societe_livr(null);
	        
	        manageModeLivraison(httpUtil, false);
	        
		} else {
	        ClientPersistant opcClient = (ClientPersistant) familleService.findById(ClientPersistant.class, elementId);
	        CURRENT_COMMANDE.setOpc_client(opcClient);
	        CURRENT_COMMANDE.setOpc_employe(null);
		}
		
		manageOffreTarif(httpUtil);
		
		boolean isCloseOnSelect =  StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("CLOSE_SELECT_PERS"));
		
		if(isCloseOnSelect){
			httpUtil.addJavaScript("$('#close_modal').trigger('click');");
		} else{
			MessageService.addGrowlMessage(MSG_TYPE.SUCCES, "", "L'élément est sélectionné avec succès");
		}
		httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
	}
	
	 public void deleteRow(ActionUtil httpUtil, 
			 CaisseMouvementPersistant currMvm, 
			 String code, 
			 String typeLigne, 
			 Long elementId, 
			 String parentCode, 
			 String menuIdx, 
			 Integer clientIdx,
			 String refTable,
			 boolean isForceDelete) {
		   List<CaisseMouvementArticlePersistant> listDetail = currMvm.getList_article();
	       List<CaisseMouvementOffrePersistant> listOffres = currMvm.getList_offre();
	       Iterator<CaisseMouvementOffrePersistant> listOffresIter = listOffres.iterator();
	       Iterator<CaisseMouvementArticlePersistant> listDetailIter = listDetail.iterator();
	       
	       boolean isSaved = (!isForceDelete && (currMvm.getId() == null ? false : true));
	       
	       // Si suppresion offre ------------
	       if(typeLigne.equals(TYPE_LIGNE_COMMANDE.OFFRE.toString())){
	    	   while (listOffresIter.hasNext()) {
	    		   CaisseMouvementOffrePersistant offre = listOffresIter.next();
	               if(offre.getOpc_offre().getId().equals(elementId) && !BooleanUtil.isTrue(offre.getIs_annule())){
	                   if(isSaved) {
	                	   offre.setIs_annule(true);
	                   } else {
	                	   listOffresIter.remove();
	                   }
	                   break;
	               }
	           }
	       } 
	       // Si suppresion menu ------------
	       else if(typeLigne.equals(TYPE_LIGNE_COMMANDE.MENU.toString())){
	    	   while (listDetailIter.hasNext()) {
	    		   CaisseMouvementArticlePersistant cmd = listDetailIter.next();
	    		   //
	               if(cmd.getIdx_client().equals(clientIdx)
	            		   && (""+cmd.getRef_table()).equals(""+refTable)
	            		   && cmd.getMenu_idx() != null 
	            		   && cmd.getMenu_idx().equals(menuIdx) 
	            		   && !BooleanUtil.isTrue(cmd.getIs_annule())){
	            	   if(isSaved) {
	            		   cmd.setIs_annule(true);
	            	   } else {
	            		   listDetailIter.remove();
	            	   }
	                   //
	            	   if(httpUtil != null) {
	            		   httpUtil.removeUserAttribute("CURRENT_MENU_NUM");
	            		   httpUtil.removeUserAttribute("CURRENT_MENU_COMPOSITION");
	            	   }
	               }
	           }
	       } 
	       // Si suppresion groupe ou article dans menu ------------
	       else if(menuIdx != null){
	    		boolean isManager = ContextAppli.getUserBean().isInProfile("MANAGER");
	    		boolean isAdmin = ContextAppli.getUserBean().isInProfile("ADMIN");
	    		boolean isDelDetMnu = (isAdmin || isManager || StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("DEL_MNU_DET")));
	    		
	           if(typeLigne.equals(TYPE_LIGNE_COMMANDE.GROUPE_FAMILLE.toString()) || typeLigne.equals(TYPE_LIGNE_COMMANDE.GROUPE_MENU.toString())){
	        	   List<CaisseMouvementArticlePersistant> listToDelete = new ArrayList<>();
	        	   
	        	   // Ctrl remove avec menu
	        	   if(!isDelDetMnu) {
	        		   List<CaisseMouvementArticlePersistant> listToDeleteAll = new ArrayList<>();   
	        		   getAllDeleteRecursiveGroupe(listToDeleteAll, listDetail, clientIdx, refTable, typeLigne, menuIdx, elementId, parentCode);
		        	   for (CaisseMouvementArticlePersistant cap : listToDeleteAll) {
		        		   if(!BigDecimalUtil.isZero(cap.getMtt_total())) {
		        			   MessageService.addGrowlMessage("", "<h3>Une ligne avec montant ne peut pas être supprimée.</h3>");
		        			   return;
		        	   		}
		        	   }
	        	   }
	        	   
	        	   deleteRecursiveGroupe(listToDelete, listDetail, isSaved, clientIdx, refTable, typeLigne, menuIdx, elementId, code);
	        	   //
	        	   for (CaisseMouvementArticlePersistant cap : listToDelete) {
	        		   listDetail.remove(cap);
	        	   }
	           } else if(typeLigne.equals(TYPE_LIGNE_COMMANDE.ART_MENU.toString())){
	        	   while (listDetailIter.hasNext()) {
	        		   CaisseMouvementArticlePersistant cmd = listDetailIter.next();
	                   if(cmd.getIdx_client().equals(clientIdx) && cmd.getMenu_idx() != null
	                		   && (""+cmd.getRef_table()).equals(""+refTable)
	                		   && cmd.getType_ligne().equals(TYPE_LIGNE_COMMANDE.ART_MENU.toString()) 
	                           && cmd.getParent_code().equals(parentCode) 
	                           && cmd.getElementId().equals(elementId)
	                           && cmd.getCode().equals(code) 
	                           && !BooleanUtil.isTrue(cmd.getIs_annule())){
	                	   
	                	   if(!isDelDetMnu && !BigDecimalUtil.isZero(cmd.getMtt_total())) {
	                		   MessageService.addGrowlMessage("", "<h3>Cette ligne avec montant ne peut pas être supprimée.</h3>");
	                		   return;
	                	   }
	                	   
	                	   if(isSaved) {
	                		   cmd.setIs_annule(true);
	                		   cmd.setType_opr(Integer.valueOf(3));
	                		   cmd.setLast_statut(ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.VALIDE.toString());
	                	   } else {
	                		   listDetailIter.remove();
	                	   }
	                       break;
	                   }
	               }
	           }
	       } 
	       // Si suppresion groupe ou article hors menu ------------
	       else{
	           if(typeLigne.equals(TYPE_LIGNE_COMMANDE.GROUPE_FAMILLE.toString())){
	        	   List<CaisseMouvementArticlePersistant> listToDelete = new ArrayList<>();
	        	   deleteRecursiveGroupe(listToDelete, listDetail, isSaved, clientIdx, refTable, typeLigne, menuIdx, elementId, code);
	        	   //
	        	   for (CaisseMouvementArticlePersistant cap : listToDelete) {
	        		   listDetail.remove(cap);
	        	   }
	           } else if(typeLigne.equals(TYPE_LIGNE_COMMANDE.ART.toString()) || typeLigne.equals(TYPE_LIGNE_COMMANDE.GARANTIE.toString())){
	        	   while (listDetailIter.hasNext()) {
	        		   CaisseMouvementArticlePersistant cmd = listDetailIter.next();
	                   if(cmd.getIdx_client().equals(clientIdx) && cmd.getMenu_idx() == null
	                		   && (""+cmd.getRef_table()).equals(""+refTable)
	                		   && (cmd.getType_ligne().equals(TYPE_LIGNE_COMMANDE.ART.toString()) || cmd.getType_ligne().equals(TYPE_LIGNE_COMMANDE.GARANTIE.toString()))
	                           && cmd.getParent_code().equals(parentCode) 
	                           && cmd.getElementId().equals(elementId) 
	                           && cmd.getCode().equals(code) 
	                           && !BooleanUtil.isTrue(cmd.getIs_annule())){
	                	   if(isSaved) {
	                		   cmd.setIs_annule(true);
	                		   cmd.setType_opr(Integer.valueOf(3));
	                		   cmd.setLast_statut(ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.VALIDE.toString());
	                	   } else {
	                		   listDetailIter.remove();
	                	   }
	                       break;
	                   }
	               }
	           }
	       }
	       
			//
			isHasChild(httpUtil, clientIdx, refTable, menuIdx, parentCode, isForceDelete);
	   }
	 
	 public void addMargeCaissier(CaisseMouvementPersistant CURRENT_COMMANDE){
	        BigDecimal totalComCaissier = null;
	        //
	        for(CaisseMouvementArticlePersistant mvmDet : CURRENT_COMMANDE.getList_article()){
	        	ArticlePersistant opc_article = mvmDet.getOpc_article();
				if(!BooleanUtil.isTrue(mvmDet.getIs_annule())
	        			&& opc_article != null 
	        			&& !BigDecimalUtil.isZero(opc_article.getTaux_marge_caissier())){
					mvmDet.setTaux_marge_cai(opc_article.getTaux_marge_caissier());// Marge caissier dans détail article ou cas ou la marge change dans l'article
					
	        		BigDecimal marge = BigDecimalUtil.divide(
	        				BigDecimalUtil.multiply(mvmDet.getMtt_total(), opc_article.getTaux_marge_caissier()), 
	        				BigDecimalUtil.get(100));
	        		totalComCaissier = BigDecimalUtil.add(totalComCaissier, marge);
	        	}
	        }
	        CURRENT_COMMANDE.setMtt_marge_caissier(totalComCaissier);
	 }
	   
	   /**
		 * @param groupId
		 * @param listCmdBean
		 * @return
		 */
		private void isHasChild(ActionUtil httpUtil, 
				Integer idxCli,
				String refTable,
				String idxMnu,
				String parentCodeDeleted, boolean isForceDelete) {
			CaisseMouvementPersistant currMvm = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");

			CaisseMouvementArticlePersistant cmDetail = null;
			boolean isHasChild = false;
			
			if(StringUtil.isNotEmpty(parentCodeDeleted)) {
				for (CaisseMouvementArticlePersistant cmArtP : currMvm.getList_article()) {
					if(cmArtP.getCode().equals(parentCodeDeleted)
							&& (""+cmArtP.getRef_table()).equals(refTable)
							&& (
									(cmArtP.getMenu_idx() != null && cmArtP.getMenu_idx().equals(idxMnu)) 
									|| (cmArtP.getMenu_idx() == null && idxMnu == null))
							&& (
									(cmArtP.getIdx_client() != null && cmArtP.getIdx_client().equals(idxCli))
									|| (cmArtP.getIdx_client() == null && idxCli == null))
							) {
						cmDetail = cmArtP;
					}
					if(cmArtP.getParent_code() != null 
							&& cmArtP.getParent_code().equals(parentCodeDeleted)
							&& (""+cmArtP.getRef_table()).equals(refTable)
							&& (
									(cmArtP.getMenu_idx() != null && cmArtP.getMenu_idx().equals(idxMnu)) 
									|| (cmArtP.getMenu_idx() == null && idxMnu == null))
							&& (
									(cmArtP.getIdx_client() != null && cmArtP.getIdx_client().equals(idxCli))
									|| (cmArtP.getIdx_client() == null && idxCli == null))
							) {
						isHasChild = true;
					}
					
					if(isHasChild && cmDetail != null) {
						break;
					}
				}
			}
			
			if( (isHasChild || cmDetail == null) || (BooleanUtil.isTrue(cmDetail.getIs_menu()) && cmDetail.getType_ligne().equals(TYPE_LIGNE_COMMANDE.MENU.toString())) ) {
				return;
			}
			deleteRow(httpUtil, currMvm, 
						cmDetail.getCode(), 
						cmDetail.getType_ligne(), 
						cmDetail.getElementId(), 
						cmDetail.getParent_code(), 
						cmDetail.getMenu_idx(), 
						cmDetail.getIdx_client(),
						cmDetail.getRef_table(),
						isForceDelete);
			
			// Purger les clients inutilisé
			if(currMvm.getMax_idx_client() != null && currMvm.getMax_idx_client() > 1) {
				for(int i=1; i<=currMvm.getMax_idx_client(); i++) {
					boolean isFounded = false;
					for (CaisseMouvementArticlePersistant cmArtP : currMvm.getList_article()) {
						if(cmArtP.getIdx_client() != null && cmArtP.getIdx_client() == i) {
							isFounded = true;
							break;
						}
					}
					if(!isFounded && currMvm.getMax_idx_client() > 1) {
						currMvm.setMax_idx_client(currMvm.getMax_idx_client() - 1);
						httpUtil.setUserAttribute("CURRENT_IDX_CLIENT", currMvm.getMax_idx_client());
					}
				}
			}
		}
	
		/**
		 * 
		 * @param httpUtil
		 */
		public void manageModeLivraison(ActionUtil httpUtil, boolean isReset) {
			CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
			// Selection auto mode livraison
			if(CURRENT_COMMANDE.getOpc_societe_livr() != null || CURRENT_COMMANDE.getOpc_livreurU() != null) {
				CURRENT_COMMANDE.setType_commande(ContextAppli.TYPE_COMMANDE.L.toString());
			}

			if(CURRENT_COMMANDE.getList_article() != null) {
    			List<CaisseMouvementArticlePersistant> listMvmDetail = CURRENT_COMMANDE.getList_article();
		        for (Iterator<CaisseMouvementArticlePersistant> iterator = listMvmDetail.iterator(); iterator.hasNext();) {
		            CaisseMouvementArticlePersistant mvmDet = iterator.next();
		            //
		            if(mvmDet.getElementId() == -1 || "LIV".equals(mvmDet.getCode())){
		                // Suppression
		                iterator.remove();
		                break;
		            }
		        }
    		}
			
			if(isReset) {
				CURRENT_COMMANDE.setMtt_livraison_livr(null);
	        	CURRENT_COMMANDE.setMtt_livraison_ttl(null);
	        	majTotalMontantCommande(httpUtil, CURRENT_COMMANDE);
	        	
				return;
			}
			
			SocieteLivrPersistant opc_societe_livr = CURRENT_COMMANDE.getOpc_societe_livr();
			if (opc_societe_livr != null) {// Société
				//OffrePersistant offrePersistant = caisseWebService.getOffreDisponible("S");
		        BigDecimal mttLivraison = null;
        		if(!BigDecimalUtil.isZero(opc_societe_livr.getTaux_marge())){// Taux
        			mttLivraison = BigDecimalUtil.divide(
        						BigDecimalUtil.multiply(CURRENT_COMMANDE.getMtt_commande_net(), opc_societe_livr.getTaux_marge()), BigDecimalUtil.get(100));
        		} else if(!BigDecimalUtil.isZero(opc_societe_livr.getMtt_marge())){// Montant
        			mttLivraison = opc_societe_livr.getMtt_marge();
        		}
        		CURRENT_COMMANDE.setMtt_livraison_ttl(mttLivraison);
        		
        		
			} else if(CURRENT_COMMANDE.getOpc_livreurU() != null){//--------------- Livreur
			        BigDecimal fraisLivraisonTotal = BigDecimalUtil.get(ContextGloabalAppli.getGlobalConfig(ContextAppli.PARAM_APPLI_ENUM.FRAIS_LIVRAISON.toString()));
			        if(!BigDecimalUtil.isZero(fraisLivraisonTotal)){
			        	BigDecimal mttLivraison = fraisLivraisonTotal;
			        	BigDecimal fraisLivraisonPartSociete = BigDecimalUtil.get(ContextGloabalAppli.getGlobalConfig("FRAIS_LIVRAISON_PART"));
			        	BigDecimal fraisLivraisonPartLivreur = BigDecimalUtil.substract(fraisLivraisonTotal, fraisLivraisonPartSociete);
			        	
			        	CURRENT_COMMANDE.setMtt_livraison_livr(fraisLivraisonPartLivreur.abs());
			        	CURRENT_COMMANDE.setMtt_livraison_ttl(mttLivraison);
			        }
			}
			 
			// Créer une ligne pour ce cas
		    if(!BigDecimalUtil.isZero(CURRENT_COMMANDE.getMtt_livraison_ttl())){
	        	CaisseMouvementArticlePersistant cmdP = new CaisseMouvementArticlePersistant();
	            cmdP.setCode("LIV");
	            cmdP.setLibelle("Frais de livraison");
	            cmdP.setType_ligne(TYPE_LIGNE_COMMANDE.LIVRAISON.toString());
	            cmdP.setElementId(Long.valueOf(-1));
	            cmdP.setOpc_mouvement_caisse(CURRENT_COMMANDE);
	            cmdP.setMtt_total(CURRENT_COMMANDE.getMtt_livraison_ttl());
	            cmdP.setIdx_client((Integer)httpUtil.getUserAttribute("CURRENT_IDX_CLIENT"));
	            //
	            CURRENT_COMMANDE.getList_article().add(cmdP);
		    }
		    majTotalMontantCommande(httpUtil, CURRENT_COMMANDE);
		}
		
	/**
	 * 
	 */
		public void manageOffreTarif(ActionUtil httpUtil) {
			CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
			String currDestination = null;
			if (CURRENT_COMMANDE.getOpc_client() != null) {
				currDestination = "C";
			} else if (CURRENT_COMMANDE.getOpc_employe() != null) {
				currDestination = "E";
			}/* else if (CURRENT_COMMANDE.getOpc_societe_livr() != null) {
				currDestination = "S";
			}*/

			// Offres automatiques pour client ou employé ------------------
			if (currDestination != null) {
				CaisseMouvementOffrePersistant currentOffreEmplCli = null;

				for (CaisseMouvementOffrePersistant offreP : CURRENT_COMMANDE.getList_offre()) {
					String dest = offreP.getOpc_offre().getDestination();
					if (offreP.getIs_annule() == null || !offreP.getIs_annule() && (dest.equals(currDestination))) {
						currentOffreEmplCli = offreP;
					}
				}
				// Si pas d'offre
				if (currentOffreEmplCli == null) {
					OffrePersistant offrePersistant = null;
					// Vérifier s'il est illigible à une offre
					if (CURRENT_COMMANDE.getOpc_client() != null) {
						offrePersistant = caisseWebService.getOffreDisponible("C");
					} else if (CURRENT_COMMANDE.getOpc_employe() != null) {
						offrePersistant = caisseWebService.getOffreDisponible("E");
					}/* else if (CURRENT_COMMANDE.getOpc_societe_livr() != null) {
						offrePersistant = caisseWebService.getOffreDisponible("S");
					}*/
					BigDecimal mttReduction = /*(currDestination.equals("S") ? CURRENT_COMMANDE.getOpc_societe_livr().getTaux_marge() 
																								: */getMontantOffre(httpUtil, offrePersistant);
					
					/*if (CURRENT_COMMANDE.getOpc_societe_livr() != null) {
						if(mttReduction != null){
							mttReduction = BigDecimalUtil.negate(mttReduction);
						}
						if(offrePersistant != null){
							offrePersistant.setTaux_reduction(mttReduction);
							offrePersistant.setIs_ventil(CURRENT_COMMANDE.getOpc_societe_livr().getIs_ventille());
						}
					}*/
					if (!BigDecimalUtil.isZero(mttReduction)) {
						CaisseMouvementOffrePersistant caisseMvmOffre = new CaisseMouvementOffrePersistant();
						caisseMvmOffre.setOpc_offre(offrePersistant);
						caisseMvmOffre.setMtt_reduction(mttReduction);

						CURRENT_COMMANDE.getList_offre().add(caisseMvmOffre);
					}
				} else {
					BigDecimal mttReduction = getMontantOffre(httpUtil, currentOffreEmplCli.getOpc_offre());
					currentOffreEmplCli.setMtt_reduction(mttReduction);
				}
			}
			// -------------------------------- Fin automatique --------------------------------

			// Offres manuelles
			// ---------------------------------------------------------------
			if(CURRENT_COMMANDE.getList_offre() != null) {
				for (CaisseMouvementOffrePersistant offreP : CURRENT_COMMANDE.getList_offre()) {
					String dest = offreP.getOpc_offre().getDestination();
					if (offreP.getIs_annule() == null || !offreP.getIs_annule() && (dest.equals("A"))) {
						BigDecimal mttReduction = getMontantOffre(httpUtil, offreP.getOpc_offre());
						offreP.setMtt_reduction(mttReduction);
					}
				}
			}

			// Maj total commande
			this.majTotalMontantCommande(httpUtil, CURRENT_COMMANDE);
		}
   
   /**
    * @param type d'offre
    */
    public void supprimerOffre(ActionUtil httpUtil) {
    	
    	String type = httpUtil.getParameter("type");
    	CaisseMouvementPersistant currMvm = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
        Long offreId = null;
        List<CaisseMouvementOffrePersistant> listOffre = currMvm.getList_offre();
        for(CaisseMouvementOffrePersistant offreP : listOffre){
            if(offreP.getIs_annule() == null || !offreP.getIs_annule()){
                if(type.equals(offreP.getOpc_offre().getDestination())){
                    offreP.setIs_annule(true);
                    offreId = offreP.getOpc_offre().getId();
                    break;
                }
            }
        }

        // Supprimer la ligne du tableau
        if(offreId != null){
            this.majTotalMontantCommande(httpUtil, currMvm);
        }
        
        httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
    }
    
    /**
     * @param commentaire 
     */
    public void initCommentaireCommande(ActionUtil httpUtil){
    	
    	 String params = "cd="+httpUtil.getParameter("cd")
    	 	+ "&tp="+httpUtil.getParameter("tp")
    	 	+"&elm="+httpUtil.getLongParameter("elm")
    	 	+"&par="+httpUtil.getParameter("par")
    	 	+"&mnu="+httpUtil.getParameter("mnu")
    	 	+"&cli="+httpUtil.getParameter("cli");
    	httpUtil.setRequestAttribute("params", params); 

    	httpUtil.setRequestAttribute("listComments", valTypeEnum.getListValeursByType(ModelConstante.ENUM_COMMENTAIRE_CAISSE));
    	
    	CaisseMouvementArticlePersistant cmvP = getSelectedCommandeLigne(httpUtil);
        if(cmvP != null){
            httpUtil.setRequestAttribute("commentaire", cmvP.getCommentaire());
        }
    	
    	httpUtil.setDynamicUrl(httpUtil.getUserAttribute("PATH_JSP_CAISSE")+"/add-commentaire.jsp");
    }
    
   /**
    * @param commentaire 
    */
   public void addCommentaireCommande(ActionUtil httpUtil){
	   String commentaire = httpUtil.getParameter("commentaire");
       
       CaisseMouvementArticlePersistant cmvP = getSelectedCommandeLigne(httpUtil);
       if(cmvP != null){
           cmvP.setCommentaire(commentaire);
       }
       
       httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
   }
   
   /** ------------------------------------------------ Autres ----------------------------------------*/
   
   /**
	 * @param httpUtil
	 */
	public void loadCalc(ActionUtil httpUtil){
		httpUtil.setDynamicUrl("/domaine/caisse/calculatrice.jsp"); 
	}
   
	/** ------------------------------------------------ Offre ----------------------------------------*/ 
	public void initOffre(ActionUtil httpUtil) {
		//
		String req = "from OffrePersistant offre where "
				+ "offre.date_debut<='[currDate1]' and (offre.date_fin is null or offre.date_fin>='[currDate2]') "
				+ "and (offre.is_disable is null or offre.is_disable=0) "
				+ "order by offre.destination, offre.ordre asc, offre.date_debut desc, offre.id desc";
		
       RequestTableBean cplxTable = getTableBean(httpUtil, "list_offre");
       Map<String, Object> formCriterion = cplxTable.getFormBean().getFormCriterion();
       Date currDate = new Date();
       formCriterion.put("currDate1", currDate);
       formCriterion.put("currDate2", currDate);
       
		List<OffrePersistant> list_offre = (List<OffrePersistant>) familleService.findByCriteria(cplxTable, req);
		httpUtil.setRequestAttribute("list_offre", list_offre);
		
		httpUtil.setDynamicUrl("/domaine/caisse/"+httpUtil.getUserAttribute("PATH_JSP_CM")+"/offre-list.jsp");
	}
	public void selectOffre(ActionUtil httpUtil) {
		
		 Long currId = httpUtil.getWorkIdLong();
        
		 CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
        // une seule offre à la foix
        List<CaisseMouvementOffrePersistant> listOffres = CURRENT_COMMANDE.getList_offre();                    
        boolean isOffreExist = false;
        for (CaisseMouvementOffrePersistant offre : listOffres) {
            if((offre.getIs_annule() == null || !offre.getIs_annule()) && offre.getOpc_offre().getId().equals(currId)){
                isOffreExist = true;
            }
        }
        if(isOffreExist){
            MessageService.addGrowlMessage("Offre existante", "Une offre est déjà positionnée sur cette commande. Veuillez la supprimer avant d'jouter une autre.");
            return;
        }
        
        OffrePersistant offrePersistant = (OffrePersistant) familleService.findById(OffrePersistant.class, currId);
        
        if(CURRENT_COMMANDE.getOpc_user_confirm() == null) {
        	UserBean userConfirm = (UserBean)httpUtil.getRequestAttribute("user_annul");
        	if(userConfirm != null) {
        		CURRENT_COMMANDE.setOpc_user_confirm(userConfirm);
        	}
		}
        
        CaisseMouvementOffrePersistant caisseMvmOffre = new CaisseMouvementOffrePersistant();
        caisseMvmOffre.setOpc_offre(offrePersistant);
        CURRENT_COMMANDE.getList_offre().add(caisseMvmOffre);
        // Maj offres
        manageOffreTarif(httpUtil);
        manageModeLivraison(httpUtil, false);
        
        httpUtil.addJavaScript("$('#close_modal').trigger('click');");
        httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
	}
   
	/**
	 * @param offrePersistant
	 * @return
	 */
	public BigDecimal getMontantOffre(ActionUtil httpUtil, OffrePersistant offrePersistant) {
		if (offrePersistant == null) {
			return null;
		}
		// On arrondi
		boolean isArroundiAchat = StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig(ContextAppli.PARAM_APPLI_ENUM.AROUNDI_PRIX_FOURN.toString()));

		CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
		BigDecimal mttReduction = null;

		if (offrePersistant.getType_offre().equals("P")) {// Prix fournisseur
			BigDecimal mttAchat = BigDecimalUtil.ZERO;
			for (CaisseMouvementArticlePersistant caisseArticleP : CURRENT_COMMANDE.getList_article()) {
				if (caisseArticleP.getIs_annule() == null || !caisseArticleP.getIs_annule()) {
					ArticlePersistant opc_article = caisseArticleP.getOpc_article();
					if (opc_article != null) {
						opc_article = (ArticlePersistant) familleService.findById(ArticlePersistant.class, opc_article.getId());
						//
						List<ArticleDetailPersistant> listComposantP = opc_article.getList_article();
						if (listComposantP != null) {
							for (ArticleDetailPersistant detailP : listComposantP) {
								BigDecimal prixAchat = BigDecimalUtil.multiply(detailP.getQuantite(),
										detailP.getOpc_article_composant().getPrix_achat_ht());
								mttAchat = BigDecimalUtil.add(mttAchat, prixAchat);
							}
						}
					}
				}
			}
			// prix achat ttc
			BigDecimal tauxTva = BigDecimalUtil
					.get(ContextGloabalAppli.getGlobalConfig(ContextAppli.PARAM_APPLI_ENUM.TVA_VENTE.toString()));
			if (tauxTva != null) {
				mttAchat = BigDecimalUtil.add(mttAchat,	BigDecimalUtil.divide(BigDecimalUtil.multiply(mttAchat, tauxTva), BigDecimalUtil.get(100)));
			}
			// On calcul la différence entre le prix d'achat et celui de la commande
			mttReduction = BigDecimalUtil.substract(CURRENT_COMMANDE.getMtt_commande(), mttAchat);
		} else {
			BigDecimal mttCommande = CURRENT_COMMANDE.getMtt_commande();
			if (offrePersistant.getMtt_seuil() == null
					|| (mttCommande != null && mttCommande.compareTo(offrePersistant.getMtt_seuil()) >= 0)) {
				BigDecimal taux = offrePersistant.getTaux_reduction();
				if (taux != null && taux.compareTo(BigDecimalUtil.ZERO) != 0) {
					boolean isOffreArticle = (CURRENT_COMMANDE.getOpc_client()!=null && BooleanUtil.isTrue(CURRENT_COMMANDE.getOpc_client().getIs_reduc_art()));
					
					if(isOffreArticle){
						for(CaisseMouvementArticlePersistant mvmArt : CURRENT_COMMANDE.getList_article()){
							if(ContextAppli.TYPE_LIGNE_COMMANDE.ART.toString().equals(mvmArt.getType_ligne())|| ContextAppli.TYPE_LIGNE_COMMANDE.MENU.toString().equals(mvmArt.getType_ligne())){
								BigDecimal mttR = BigDecimalUtil.divide(BigDecimalUtil.multiply(mvmArt.getMtt_total(),
										offrePersistant.getTaux_reduction()), BigDecimalUtil.get(100));
								
								if (isArroundiAchat && !BigDecimalUtil.isZero(mttR)) {
									mttR = mttR.setScale(0, BigDecimal.ROUND_UP);
								}
								mttReduction = BigDecimalUtil.add(mttReduction, mttR);
							}
						}
						
					} else{
						BigDecimal mttR = BigDecimalUtil.divide(BigDecimalUtil.multiply(CURRENT_COMMANDE.getMtt_commande(),
								offrePersistant.getTaux_reduction()), BigDecimalUtil.get(100));
						// Si dépacement plafond on prend le plafond
						if (offrePersistant.getMtt_plafond() != null && mttR != null
								&& mttR.compareTo(offrePersistant.getMtt_plafond()) > 0) {
							mttR = offrePersistant.getMtt_plafond();
						}
						mttReduction = mttR;
					}
				}
			}
		}

		if (isArroundiAchat && !BigDecimalUtil.isZero(mttReduction)) {
			mttReduction = mttReduction.setScale(0, BigDecimal.ROUND_UP);
		}
		
		if(mttReduction != null 
				&& CURRENT_COMMANDE.getMtt_commande_net() != null
				&& mttReduction.compareTo(CURRENT_COMMANDE.getMtt_commande_net()) > 0) {
			mttReduction = CURRENT_COMMANDE.getMtt_commande_net();
		}

		return mttReduction;
	}
	
	/**
	 * 
	 */
	public void sortAndAddCommandeLigne(ActionUtil httpUtil, CaisseMouvementPersistant CURRENT_COMMANDE) {
		List<CaisseMouvementArticlePersistant> list_article = CURRENT_COMMANDE.getList_article();
		
		//
		List<CmdBean> listCmdBean = new ArrayList<>();
		List<MenuCmdBean> listMenuCmdBean = new ArrayList<>();

		// Isoler les groupes de familles hors menus
		for (CaisseMouvementArticlePersistant cmArtP : list_article) {
			if(cmArtP == null){
				continue;
			}
			if (cmArtP.getMenu_idx() == null && !cmArtP.getType_ligne().equals(TYPE_LIGNE_COMMANDE.MENU.toString())) {
				// Not menu ---------------------------------------------------------------------------------------------------
				if (cmArtP.getType_ligne().equals(TYPE_LIGNE_COMMANDE.GROUPE_FAMILLE.toString())) {
					CmdBean cmdBean = getCmdBean(cmArtP.getIdx_client(), cmArtP.getCode(), listCmdBean);
					CmdBean cmdParentBean = getCmdBean(cmArtP.getIdx_client(), cmArtP.getParent_code(), listCmdBean);
					
					if (cmdBean == null) {
						cmdBean = new CmdBean();
						cmdBean.setClientIdx(cmArtP.getIdx_client());
						cmdBean.setGroupId(cmArtP.getCode());
						cmdBean.setGroupElement(cmArtP);
						//
						if(cmdParentBean == null) {
							listCmdBean.add(cmdBean);
						} else {
							if(cmdParentBean.getListGroupe() == null) {
								cmdParentBean.setListGroupe(new ArrayList<>());
							}
							cmdParentBean.getListGroupe().add(cmdBean);
						}
					}
				} else if (cmArtP.getType_ligne().equals(TYPE_LIGNE_COMMANDE.ART.toString()) 
						|| cmArtP.getType_ligne().equals(TYPE_LIGNE_COMMANDE.GARANTIE.toString())) {
					CmdBean cmdBean = getCmdBean(cmArtP.getIdx_client(), cmArtP.getParent_code(), listCmdBean);
					if(cmdBean != null){
						if(cmdBean.getListArticle() == null) {
							cmdBean.setListArticle(new ArrayList<>());
						}
						cmdBean.getListArticle().add(cmArtP);  
					}
				}
			} else {// Menu -----------------------------------------------------------------------------------------------------
				if (cmArtP.getType_ligne().equals(TYPE_LIGNE_COMMANDE.MENU.toString()) || cmArtP.getType_ligne().equals(TYPE_LIGNE_COMMANDE.GROUPE_MENU.toString())) {
					MenuCmdBean menuCmdBean = getMenuCmdBean(cmArtP.getIdx_client(), cmArtP.getMenu_idx(), cmArtP.getCode(), listMenuCmdBean);
					MenuCmdBean menuCmdParentBean = getMenuCmdBean(cmArtP.getIdx_client(), cmArtP.getMenu_idx(), cmArtP.getParent_code(), listMenuCmdBean);
					if (menuCmdBean == null) {
						menuCmdBean = new MenuCmdBean();
						menuCmdBean.setClientIdx(cmArtP.getIdx_client());
						menuCmdBean.setMenuIdx(cmArtP.getMenu_idx());
						menuCmdBean.setGroupId(cmArtP.getCode());
						menuCmdBean.setGroupElement(cmArtP);
						//
						//
						if(menuCmdParentBean == null) {
							listMenuCmdBean.add(menuCmdBean);
						} else {
							if(menuCmdParentBean.getListGroupe() == null) {
								menuCmdParentBean.setListGroupe(new ArrayList<>());
							}
							menuCmdParentBean.getListGroupe().add(menuCmdBean);
						}
					}
				} else if (cmArtP.getType_ligne().equals(TYPE_LIGNE_COMMANDE.ART_MENU.toString())) {
					MenuCmdBean menuCmdBean = getMenuCmdBean(cmArtP.getIdx_client(), cmArtP.getMenu_idx(), cmArtP.getParent_code(), listMenuCmdBean);
					if(menuCmdBean != null && menuCmdBean.getListArticle() == null) {
						menuCmdBean.setListArticle(new ArrayList<>());
					}
					if(menuCmdBean != null){
						menuCmdBean.getListArticle().add(cmArtP);
					}
				}
			}
		}

		// Effacer la liste
		list_article.clear();

		// Ajouter les menus -----------------------------------------------------
		populateSortedMenuArticle(list_article, listMenuCmdBean);
		// Ajouter les familles et leurs articles---------------------------------
		populateSortedArticle(list_article, listCmdBean);
		
		majTotalMontantCommande(httpUtil, CURRENT_COMMANDE);
		
		// Alimenter l'odre des éléments (utile lors de la reprise car l'ordre dans la base ne change pas et on va se baser sur cet ordre
		int idx = 0;
		for(CaisseMouvementArticlePersistant detail : list_article){
			detail.setIdx_element(idx);
			idx++;
		}
	}

	public void populateSortedArticle(List<CaisseMouvementArticlePersistant> list_article, List<CmdBean> listCmdBean) {
		for (CmdBean menuBean : listCmdBean) {
			list_article.add(menuBean.getGroupElement());
			//
			if(menuBean.getListArticle() != null && !menuBean.getListArticle().isEmpty()) {
				list_article.addAll(menuBean.getListArticle());	
			}
			if(menuBean.getListGroupe() != null) {
				populateSortedArticle(list_article, menuBean.getListGroupe());	
			}
		}
	}
	public void populateSortedMenuArticle(List<CaisseMouvementArticlePersistant> list_article, List<MenuCmdBean> listCmdBean) {
		for (MenuCmdBean menuBean : listCmdBean) {
			list_article.add(menuBean.getGroupElement());
			//
			if(menuBean.getListArticle() != null && !menuBean.getListArticle().isEmpty()) {
				list_article.addAll(menuBean.getListArticle());	
			}
			if(menuBean.getListGroupe() != null) {
				populateSortedMenuArticle(list_article, menuBean.getListGroupe());	
			}
		}
	}

	/**
	 * @param groupId
	 * @param listCmdBean
	 * @return
	 */
	public CmdBean getCmdBean(Integer clientIdx, String groupId, List<CmdBean> listCmdBean) {
		for (CmdBean cmd : listCmdBean) {
			if (cmd.getGroupId().equals(groupId) && cmd.getClientIdx().equals(clientIdx)) {
				return cmd;
			}
			if(cmd.getListGroupe() != null) {
				CmdBean cm = getCmdBean(clientIdx, groupId, cmd.getListGroupe());
				if(cm != null) {
					return cm;
				}
			}
		}
		return null;
	}

	
	/**
	 * @param httpUtil
	 */
	public void ajouterGroupe(ActionUtil httpUtil) {
		boolean isAddPersonne = (httpUtil.getParameter("addCli") != null);
		CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
		Integer max_idx_client = null;
		
		List<Integer> listIdxClient = new ArrayList<>();
		for(CaisseMouvementArticlePersistant caisseMvmP : CURRENT_COMMANDE.getList_article()){
			if(BooleanUtil.isTrue(caisseMvmP.getIs_annule())){
				continue;
			}
			if(!listIdxClient.contains(caisseMvmP.getIdx_client())){
				listIdxClient.add(caisseMvmP.getIdx_client());
			}
		 }
		
		if(CURRENT_COMMANDE.getMax_idx_client()==null || listIdxClient.size() == 0) {
			max_idx_client = 1;
		} else if(isAddPersonne && listIdxClient.contains(CURRENT_COMMANDE.getMax_idx_client())) {
			max_idx_client = CURRENT_COMMANDE.getMax_idx_client() + 1;	
		} else {
			max_idx_client = CURRENT_COMMANDE.getMax_idx_client();
		}
		
		if(listIdxClient.size() == 0){
			httpUtil.setRequestAttribute("is_cliIdxAdded", true);
		}
		httpUtil.setUserAttribute("CURRENT_IDX_CLIENT", max_idx_client);
		CURRENT_COMMANDE.setMax_idx_client(max_idx_client);
		
		// Affecter les article sans client à ce client
		if(CURRENT_COMMANDE != null && CURRENT_COMMANDE.getList_article() != null){
			for(CaisseMouvementArticlePersistant caisseMvmP : CURRENT_COMMANDE.getList_article()){
				if(caisseMvmP.getIdx_client() == null){
					caisseMvmP.setIdx_client(max_idx_client);
				}
			}
		}
		
		// Selectionner la table de ce client
		if(CURRENT_COMMANDE != null && CURRENT_COMMANDE.getList_article() != null){
			List<String> listTables = new ArrayList<>();
			for(CaisseMouvementArticlePersistant caisseMvmP : CURRENT_COMMANDE.getList_article()){
				if(BooleanUtil.isTrue(caisseMvmP.getIs_annule())){
					continue;
				}
				if(StringUtil.isNotEmpty(caisseMvmP.getRef_table()) && !listTables.contains(caisseMvmP.getRef_table())){
					listTables.add(caisseMvmP.getRef_table());
				}
			}
			if(listTables.size() > 0 && StringUtil.isEmpty(httpUtil.getUserAttribute("CURRENT_TABLE_REF"))){
				Collections.sort(listTables);
				httpUtil.setUserAttribute("CURRENT_TABLE_REF", listTables.get(listTables.size()-1));
			}
		}
		
		httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
	}
	/**
	 * @param httpUtil
	 */
	public void selectGroupe(ActionUtil httpUtil) {
		CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
		if(CURRENT_COMMANDE.getId() != null && CURRENT_COMMANDE.getLast_statut().equals(STATUT_CAISSE_MOUVEMENT_ENUM.LIVRE.toString())) {
			MessageService.addGrowlMessage("Mise à jour impossible", "Cette commande est marquée comme livrée. <br/>Elle ne peut plus être modifiée.");
			return;
		}
		
		Integer currIdx = Integer.valueOf(httpUtil.getParameter("idx_cli"));
		httpUtil.setUserAttribute("CURRENT_IDX_CLIENT", currIdx);
		
		// Selectionner la table de ce client
		if(CURRENT_COMMANDE != null && CURRENT_COMMANDE.getList_article() != null){
			for(CaisseMouvementArticlePersistant caisseMvmP : CURRENT_COMMANDE.getList_article()){
				if(caisseMvmP == null){
					continue;
				}
				if(currIdx.equals(caisseMvmP.getIdx_client()) && StringUtil.isNotEmpty(caisseMvmP.getRef_table())){
					httpUtil.setUserAttribute("CURRENT_TABLE_REF", caisseMvmP.getRef_table());
					break;
				}
			}
		}
		httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
	}
  
	/**
	 * @param httpUtil
	 */
	public void finaliserMenuCmdStep(ActionUtil httpUtil) {
		
		// Purge infos steps
		httpUtil.removeUserAttribute("STEP_MNU");
		httpUtil.removeUserAttribute("LIST_SOUS_MENU");
		
		// Données de la session
		manageDataSession(httpUtil);
		//
		httpUtil.removeUserAttribute("CURRENT_MENU_COMPOSITION");
		httpUtil.removeUserAttribute("CURRENT_MENU_NUM");

		httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
	}
	
	/**
	 * @param httpUtil
	 */
	public void selectTable(ActionUtil httpUtil) { 
		CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
		if(CURRENT_COMMANDE.getId() != null && CURRENT_COMMANDE.getLast_statut().equals(STATUT_CAISSE_MOUVEMENT_ENUM.LIVRE.toString())) {
			MessageService.addGrowlMessage("Mise à jour impossible", "Cette commande est marquée comme livrée. <br/>Elle ne peut plus être modifiée.");
			return;
		}
		
		String refTable = httpUtil.getParameter("ref_tab");
		httpUtil.setUserAttribute("CURRENT_TABLE_REF", refTable);
		
		httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
	}
	
	 /**
	    * @param listDetailIter
	    * @param isSaved
	    * @param clientIdx
	    * @param typeLigne
	    * @param menuIdx
	    * @param elementId
	    * @param code
	    */
		private void getAllDeleteRecursiveGroupe(List<CaisseMouvementArticlePersistant> listToDelete, 
								List<CaisseMouvementArticlePersistant> listDetail, 
								Integer clientIdx, 
								String refTable,
								String typeLigne, 
								String menuIdx, 
								Long elementId, 
								String code) {
		   for (CaisseMouvementArticlePersistant cmd : listDetail) {
			   if(cmd.getIdx_client().equals(clientIdx) && !BooleanUtil.isTrue(cmd.getIs_annule())){
				   if(cmd.getType_ligne().equals(TYPE_LIGNE_COMMANDE.GROUPE_FAMILLE.toString()) || cmd.getType_ligne().equals(TYPE_LIGNE_COMMANDE.GROUPE_MENU.toString())) {
					   if(menuIdx == null){
						   if(cmd.getParent_code() != null && cmd.getParent_code().equals(code)){
							   getAllDeleteRecursiveGroupe(listToDelete, listDetail, clientIdx, refTable, cmd.getType_ligne(), cmd.getMenu_idx(), cmd.getElementId(), cmd.getCode());
						   }
					   } else if(menuIdx.equals(cmd.getMenu_idx()) && cmd.getParent_code().equals(code)){
						   getAllDeleteRecursiveGroupe(listToDelete, listDetail, clientIdx, refTable, cmd.getType_ligne(), cmd.getMenu_idx(), cmd.getElementId(), cmd.getCode());
					   }
				   } else if(cmd.getType_ligne().equals(TYPE_LIGNE_COMMANDE.ART_MENU.toString()) || cmd.getType_ligne().equals(TYPE_LIGNE_COMMANDE.ART.toString())) {
					   if(menuIdx == null){
						   if(cmd.getParent_code() != null && cmd.getParent_code().equals(code)) {
							   listToDelete.add(cmd);
						   }
					   } else if(menuIdx.equals(cmd.getMenu_idx()) && cmd.getParent_code().equals(code)) {
	                		listToDelete.add(cmd);
					   }
				   }
				   //
	               if(cmd.getType_ligne().equals(typeLigne) && cmd.getElementId().equals(elementId) && cmd.getCode().equals(code)){
	            		listToDelete.add(cmd);
	               }
			   }
	       }
	   }
	
		private void deleteRecursiveGroupe(List<CaisseMouvementArticlePersistant> listToDelete, 
							List<CaisseMouvementArticlePersistant> listDetail, 
							boolean isSaved, 
							Integer clientIdx, 
							String refTable,
							String typeLigne, 
							String menuIdx, 
							Long elementId, 
							String code) {
		   for (CaisseMouvementArticlePersistant cmd : listDetail) {
			   if(cmd.getIdx_client().equals(clientIdx) && !BooleanUtil.isTrue(cmd.getIs_annule())){
				   if(cmd.getType_ligne().equals(TYPE_LIGNE_COMMANDE.GROUPE_FAMILLE.toString()) || cmd.getType_ligne().equals(TYPE_LIGNE_COMMANDE.GROUPE_MENU.toString())) {
					   if(menuIdx == null){
						   if(cmd.getParent_code() != null && cmd.getParent_code().equals(code)){
							   deleteRecursiveGroupe(listToDelete, listDetail, isSaved, clientIdx, refTable, cmd.getType_ligne(), cmd.getMenu_idx(), cmd.getElementId(), cmd.getCode());
						   }
					   } else if(menuIdx.equals(cmd.getMenu_idx()) && cmd.getParent_code().equals(code)){
						   deleteRecursiveGroupe(listToDelete, listDetail, isSaved, clientIdx, refTable, cmd.getType_ligne(), cmd.getMenu_idx(), cmd.getElementId(), cmd.getCode());
					   }
				   } else if(cmd.getType_ligne().equals(TYPE_LIGNE_COMMANDE.ART_MENU.toString()) || cmd.getType_ligne().equals(TYPE_LIGNE_COMMANDE.ART.toString())) {
					   if(menuIdx == null){
						   if(cmd.getParent_code() != null && cmd.getParent_code().equals(code)) {
							   if(isSaved) {
		                		   cmd.setIs_annule(true);
		                	   } else {
		                		   listToDelete.add(cmd);
		                	   }
						   }
					   } else if(menuIdx.equals(cmd.getMenu_idx()) && cmd.getParent_code().equals(code)) {
						   if(isSaved) {
	                		   cmd.setIs_annule(true);
	                	   } else {
	                		   listToDelete.add(cmd);
	                	   }
					   }
				   }
				   //
	               if(cmd.getType_ligne().equals(typeLigne) && cmd.getElementId().equals(elementId) && cmd.getCode().equals(code)){
	            	   if(isSaved) {
	            		   cmd.setIs_annule(true);
	            	   } else {
	            		   listToDelete.add(cmd);
	            	   }
	               }
			   }
	       }
	   }
	   
	   /** ------------------------------------------------ historique ----------------------------------------*/ 
//	   @WorkForward(useBean=true, useFormValidator=true, bean=CaisseJourneeBean.class)
//		public void selectShift(ActionUtil httpUtil) {
//			String tp = httpUtil.getParameter("tp");
//			CaisseJourneeBean caisseJBean = (CaisseJourneeBean)httpUtil.getViewBean();
//			CaissePersistant currentCaisse = ContextAppliCaisse.getCaisseBean();
//			CaisseJourneePersistant currentJourneeP = caisseService.getJourneCaisseOuverte(currentCaisse.getId());
//			//
//			if(tp.equals("clo")) {
//		         if(currentJourneeP != null){
//		        	 MessageService.addBannerMessage("Le shift est déjà ouvert.");
//		             return;
//		         }
//		         caisseService.ouvrirCaisse(currentCaisse.getId(), caisseJBean.getMtt_cloture_caissier_espece());
//			} else {
//				CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
//				if(CURRENT_COMMANDE != null && CURRENT_COMMANDE.getList_article().size() > 0) {
//					MessageService.addBannerMessage("Veuillez valider ou annuler les commandes en cours avant de continuer.");
//			        return;
//				}
//				if(!currentJourneeP.getStatut_caisse().equals(ContextAppli.STATUT_JOURNEE.OUVERTE.getStatut())) {
//					MessageService.addBannerMessage("Le shift est déjà clos.");
//					return;
//				}
//				
//				BigDecimal total =  BigDecimalUtil.add(caisseJBean.getMtt_cloture_caissier_espece(), 
//	                    caisseJBean.getMtt_cloture_caissier_cb(), 
//	                    caisseJBean.getMtt_cloture_caissier_cheque(),
//	                    caisseJBean.getMtt_cloture_caissier_dej());
//				if(total.compareTo(caisseJBean.getMtt_cloture_caissier()) != 0){
//					MessageService.addBannerMessage("Le montant total ne correspond pas au total des montants saisis.");
//				    return;
//				}
//				// Commande en attente
//				List<CaisseMouvementPersistant> listCmdEnAttente = caisseWebService.getListMouvementTemp(currentJourneeP.getOpc_journee().getId(), currentCaisse.getId(), null);
//				if (listCmdEnAttente != null && listCmdEnAttente.size() > 0) {
//					MessageService.addBannerMessage("Vous devez valider ou supprimer les commandes en cours ou en attente avant de clore le shift.");
//				    return;
//				}
//				//
//				caisseService.cloturerDefinitive(currentJourneeP.getOpc_caisse().getId(), caisseJBean.getMtt_cloture_caissier_espece(), caisseJBean.getMtt_cloture_caissier_cb(), caisseJBean.getMtt_cloture_caissier_cheque(), caisseJBean.getMtt_cloture_caissier_dej(), false);
//			}
//			// Mettre la nouvelle journée caisse dans la session
//			CaisseJourneePersistant caisseJourneeP = (CaisseJourneePersistant) caisseService.findAll(CaisseJourneePersistant.class, Order.desc("id")).get(0);
//			MessageService.getGlobalMap().put("CURRENT_JOURNEE_CAISSE", caisseJourneeP);
//			
//			httpUtil.writeResponse("REDIRECT");
//		}
		/** ------------------------------------------------Paiement ----------------------------------------*/
		
		
		/**
		 * @param carteClientP 
		 * @param portefeuilleClientP 
		 * 
		 */
//		private void setDataCommandeImpression(CaisseMouvementPersistant CURRENT_COMMANDE, ClientPortefeuillePersistant portefeuilleClientP, CarteFideliteClientPersistant carteClientP){
//			String msgPub = ContextAppli.getResraurantBean().getMsg_publicite();
//	        String titrePub = ContextAppli.getResraurantBean().getTitre_publicite();
//	        // Ajout infos paramètrage
//	    	CURRENT_COMMANDE.setText_entete_ticket_1(ContextGloabalAppli.getGlobalConfig(PARAM_APPLI_ENUM.TEXT_ENTETE_TICKET_1.toString()));
//	    	CURRENT_COMMANDE.setText_entete_ticket_2(ContextGloabalAppli.getGlobalConfig(PARAM_APPLI_ENUM.TEXT_ENTETE_TICKET_2.toString()));
//	    	CURRENT_COMMANDE.setAdresse_etablissement(ContextGloabalAppli.getGlobalConfig(PARAM_APPLI_ENUM.ADRESSE_ETABLISSEMENT.toString()));
//	    	CURRENT_COMMANDE.setInformation_contact(ContextGloabalAppli.getGlobalConfig(PARAM_APPLI_ENUM.INFORMATION_CONTACT.toString()));
//	    	CURRENT_COMMANDE.setText_pied_ticket(ContextGloabalAppli.getGlobalConfig(PARAM_APPLI_ENUM.TEXT_PIED_TICKET.toString()));
//	    	CURRENT_COMMANDE.setTva_vente(BigDecimalUtil.get(ContextGloabalAppli.getGlobalConfig(PARAM_APPLI_ENUM.TVA_VENTE.toString())));
//	    	CURRENT_COMMANDE.setInformation_contact_mail(ContextGloabalAppli.getGlobalConfig(PARAM_APPLI_ENUM.INFORMATION_CONTACT_MAIL.toString()));
//	    	CURRENT_COMMANDE.setInformation_contact_phone(ContextGloabalAppli.getGlobalConfig(PARAM_APPLI_ENUM.INFORMATION_CONTACT_PHONE.toString()));
//	    	CURRENT_COMMANDE.setIce(ContextGloabalAppli.getGlobalConfig(PARAM_APPLI_ENUM.ICE.toString()));
//	    	CURRENT_COMMANDE.setNbr_niveau_ticket(Integer.valueOf(ContextGloabalAppli.getGlobalConfig(PARAM_APPLI_ENUM.NBR_NIVEAU_TICKET.toString())));
//	    	CURRENT_COMMANDE.setTitre_publicite(titrePub);
//	    	CURRENT_COMMANDE.setMsg_publicite(msgPub);
//	    	// Portefeuille et fidélité
//	    	if(portefeuilleClientP != null){
//	    		CURRENT_COMMANDE.setSolde_portefeuille(portefeuilleClientP.getSolde());
//	    	}
//	    	if(carteClientP != null){
//		    	CURRENT_COMMANDE.setTotal_point(carteClientP.getTotal_point());
//		    	CURRENT_COMMANDE.setMontant_point(carteClientP.getMtt_total());
//	    	}
//		}
		
		/** ------------------------------------------------ Autres ----------------------------------------*/
		
		
		/**
		 * @param httpUtil
		 */
		public void initNewCommande(ActionUtil httpUtil){
			httpUtil.removeUserAttribute("IS_RETOUR");// Annuler variable du retour
			// Reset infos
	        resetInfosSession(httpUtil);
	        
	        CaisseMouvementPersistant newCmd = new CaisseMouvementPersistant();
	        newCmd.setRef_commande(""+System.currentTimeMillis());
	        newCmd.setDate_vente(new Date());
	        newCmd.setOpc_caisse_journee(ContextAppliCaisse.getJourneeCaisseBean());
	        //
	        newCmd.setList_article(new ArrayList<>());
	        newCmd.setList_offre(new ArrayList<>());
			newCmd.setMax_idx_client(1);
			httpUtil.setUserAttribute("CURRENT_IDX_CLIENT", 1);// Index client
			httpUtil.removeUserAttribute("CURRENT_TABLE_REF");// Ref table
			httpUtil.removeMenuAttribute("COUVERTS_TABLE"); // Nombre de couverts
			httpUtil.setUserAttribute("CURRENT_COMMANDE", newCmd);

			boolean isCaisseFrom = ContextAppli.APPLI_ENV.cais.toString().equals(httpUtil.getUserAttribute("CURRENT_ENV"));
			if(isCaisseFrom){
				httpUtil.addJavaScript("resetDetailCmd();");
			}
	    }
		
		/**
		 * @param httpUtil
		 */
		public void resetElement(ActionUtil httpUtil) {
			String tp = httpUtil.getRequest().getParameter("tp");
			CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
			if(CURRENT_COMMANDE.getId() != null && CURRENT_COMMANDE.getLast_statut().equals(STATUT_CAISSE_MOUVEMENT_ENUM.LIVRE.toString())) {
				MessageService.addGrowlMessage("Mise à jour impossible", "Cette commande est marquée comme livrée. <br/>Elle ne peut plus être modifiée.");
				return;
			}
			
			//
			if(tp.equals("cli")) {
				CURRENT_COMMANDE.setOpc_client(null);
			} else if(tp.equals("empl")) {
				CURRENT_COMMANDE.setOpc_employe(null);
			} else if(tp.equals("livr")) {
				CURRENT_COMMANDE.setOpc_livreurU(null);
				manageModeLivraison(httpUtil, true);
				
			} else if(tp.equals("socLivr")) {
				CURRENT_COMMANDE.setOpc_societe_livr(null);
				manageModeLivraison(httpUtil, true);
				
			} else if(tp.equals("serv")) {
				CURRENT_COMMANDE.setOpc_serveur(null);
			} else if(tp.equals("TAB")) {
				String refTab = httpUtil.getParameter("ref_tab"); 
				List<String> listTables = new ArrayList<>();
				List<CaisseMouvementArticlePersistant> listSortedArticle = CURRENT_COMMANDE.getList_article();
				//
				for(CaisseMouvementArticlePersistant caisseMvmP : listSortedArticle){
					if(BooleanUtil.isTrue(caisseMvmP.getIs_annule())){
						continue;
					}
					// Ajout tables
					 if(!refTab.equals(caisseMvmP.getRef_table()) && caisseMvmP.getRef_table() != null && !listTables.contains(caisseMvmP.getRef_table())){
						 listTables.add(caisseMvmP.getRef_table());
					 }
				}
				// Trier les tables
				Collections.sort(listTables);
				String lastRefTable = listTables.size()>0?listTables.get(listTables.size()-1) : null;
				
				
				for(CaisseMouvementArticlePersistant caisseMvmP : CURRENT_COMMANDE.getList_article()){
					if(refTab.equals(caisseMvmP.getRef_table())){
						caisseMvmP.setRef_table(lastRefTable);
					}
				 }
				if(httpUtil.getUserAttribute("CURRENT_TABLE_REF") != null && httpUtil.getUserAttribute("CURRENT_TABLE_REF").equals(refTab)){
					httpUtil.setUserAttribute("CURRENT_TABLE_REF", lastRefTable);
				}
				MessageService.addGrowlMessage(MSG_TYPE.SUCCES, "", "La table est annulée.");
			}
			
			httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
		}
		
		/**
		 * @param httpUtil
		 */
		public void resetInfosSession(ActionUtil httpUtil) {
			httpUtil.removeUserAttribute("CURRENT_MENU_NUM");
	        httpUtil.removeUserAttribute("HISTORIQUE_NAV");
	        httpUtil.removeUserAttribute("CURRENT_MENU_COMPOSITION");
	        httpUtil.removeUserAttribute("CURRENT_ITEM_ADDED");
		}
		
		/**
		 * @param caisseId
		 * @param value
		 * @param tp : AF=Afficheur, PR=Impression
		 */
		public void sendDataToScreen(ActionUtil httpUtil, String value){
			CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
			
			if(CURRENT_COMMANDE == null){
				return;
			}
			
			List<CaissePersistant> listAfficheurs = (List<CaissePersistant>) httpUtil.getUserAttribute("LIST_AFFICHEUR");
	        String cmd = "", detail = "", detCurrArt = "";
	        
	        boolean isAfficheur = (listAfficheurs != null && listAfficheurs.size() > 0);	        
	        boolean isAfficheurCom =  StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("AFFICHEUR_PORT_COM"));
	        String currentAdded = (String) httpUtil.getUserAttribute("CURRENT_ART_TRACK");
	        String currentItem = (String) httpUtil.getUserAttribute("CURRENT_ITEM_ADDED");

	        if(!isAfficheurCom && !isAfficheur){
	        	return;
	        }
	        
	        ///
	        if(isAfficheur){
				// Si validation de la commande
				if(value.startsWith("V")){
					String[] vals = StringUtil.getArrayFromStringDelim(value, ":");
					value = vals[0];
					
					cmd = "\"total\":\""+BigDecimalUtil.formatNumber(CURRENT_COMMANDE.getMtt_commande_net())+"\","
						  + "\"ref_cmd\":\""+(CURRENT_COMMANDE.getRef_commande().length() > 12 ? CURRENT_COMMANDE.getRef_commande().substring(12) : CURRENT_COMMANDE.getRef_commande())+"\","
						  + "\"a_rendre\":\""+vals[1]+"\"";
				} else{
					cmd = "\"total\":\""+BigDecimalUtil.formatNumber(CURRENT_COMMANDE.getMtt_commande_net())+"\"";
					//
					for(CaisseMouvementArticlePersistant det : CURRENT_COMMANDE.getList_article()){
						if(BooleanUtil.isTrue(det.getIs_annule())){
							continue;
						}
						String currId = "";
						if(det.getOpc_article() != null) {
							if(det.getMenu_idx() == null) {
								currId = det.getIdx_client()+"-"+det.getOpc_article().getId() + "-" + TYPE_LIGNE_COMMANDE.ART.toString() + "-" + det.getParent_code().substring(det.getParent_code().indexOf("_")+1);
							} else {
								currId = det.getIdx_client()+"-"+det.getOpc_article().getId() + "-" + TYPE_LIGNE_COMMANDE.ART_MENU.toString() + "-" + det.getParent_code()+"-"+det.getMenu_idx();
							}
						}
						
						detail = detail + 
								"{"
								+ "\"art\":\""+det.getLibelle()+"\","
								+ "\"qte\":\""+det.getQuantite()+"\","
								+ "\"curr_path\":\""+currId+"\","
								+ "\"level\":\""+det.getLevel()+"\","
								+ "\"type\":\""+det.getType_ligne()+"\","
								+ "\"is_offert\":\""+(BooleanUtil.isTrue(det.getIs_offert())?true:false)+"\","
								+ "\"prix\":\""+BigDecimalUtil.formatNumber(det.getMtt_total())+"\""
							    + "},";
					}
					if(StringUtil.isNotEmpty(detail)){
						detail = detail.substring(0, detail.length()-1);
					}
					
					String[] vals = StringUtil.getArrayFromStringDelim(currentAdded, "|");
					if(vals != null && vals.length > 1){
						detCurrArt = "\"qte\":\""+vals[1]+"\","
							  + "\"art\":\""+vals[0]+"\","
							  + "\"curr_path\":\""+currentItem+"\","
							  + ( vals.length>2 ? "\"prix\":\""+vals[2]+"\"" : "\"prix\":\"\"");
					}
				}
				cmd = "{"+cmd+", "+detCurrArt+", \"detail\":["+detail+"], \"val\":\""+value+"\"}";
				//
	        	for (CaissePersistant caissePersistant : listAfficheurs) {
	        		ClientTrackCmdSocketController.sendDataToScreen(caissePersistant.getId(), cmd, "");
	        	}
	        }
	        if(isAfficheurCom){		
	        	// Si validation de la commande
				if("X".equals(value)){
					sendComData(httpUtil, "Bienvenue ...");
					return;
				} else if(value.startsWith("V")){
					String[] vals = StringUtil.getArrayFromStringDelim(value, ":");
//					value = vals[0];
					
					cmd = "[TOTAL : "+BigDecimalUtil.formatNumber(CURRENT_COMMANDE.getMtt_commande_net())+"] ... "
						  + " RENDU : "+vals[2];
				} else{
					String[] vals = StringUtil.getArrayFromStringDelim(currentAdded, "|");
					
					if(vals != null && vals.length > 1){
						if(vals.length == 2){
							cmd = vals[1]+" "+vals[0]+"..."
									  + "["+BigDecimalUtil.formatNumber(CURRENT_COMMANDE.getMtt_commande_net())+"]";
						} else if(vals.length > 2){
							cmd = vals[1]+" "+vals[0]+"("+vals[2]+")..."
									  + "["+BigDecimalUtil.formatNumber(CURRENT_COMMANDE.getMtt_commande_net())+"]";
						}
					}
				}
	        	
				sendComData(httpUtil, cmd);// Cas caisse liée à un serveur externe
	        }
	        
	        if(CURRENT_COMMANDE.getOpc_livreurU() != null) {
	        	ClientTrackCmdSocketController.sendDataToScreen(CURRENT_COMMANDE.getOpc_livreurU().getId(), cmd, "CMD");
	        }
		}
		
		/**
		 * @param httpUtil
		 */
		@SuppressWarnings("unchecked")
		public void manageDataSession(ActionUtil httpUtil) {
			List<String> HISTORIQUE_NAV = (List<String>) httpUtil.getUserAttribute("HISTORIQUE_NAV");
			if (HISTORIQUE_NAV == null) {
				HISTORIQUE_NAV = new ArrayList<>();
				httpUtil.setUserAttribute("HISTORIQUE_NAV", HISTORIQUE_NAV);
			}
			HISTORIQUE_NAV.clear();
		}
		
		 /**
		    * @param httpUtil
		    * @return
		    */
		   public CaisseMouvementArticlePersistant getSelectedCommandeLigne(ActionUtil httpUtil){ 
//			   String code = httpUtil.getParameter("cd");
		       String typeLigne = httpUtil.getParameter("tp");
		       Long elementId = httpUtil.getLongParameter("elm");
		       String parentCode = httpUtil.getParameter("par");
		       String menuIdx = httpUtil.getParameter("mnu");
		       String clientIdx = httpUtil.getParameter("cli");
		       
		       if(StringUtil.isEmpty(typeLigne)) {
		    	   MessageService.addGrowlMessage("", "Veuillez sélectionner une ligne.");
		    	   return null;
		       }
		       
		       CaisseMouvementPersistant currMvm = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
		       List<CaisseMouvementArticlePersistant> listDetail = currMvm.getList_article();

		       // Si suppresion menu ------------
		       if(typeLigne.equals(TYPE_LIGNE_COMMANDE.MENU.toString())){
		           for (CaisseMouvementArticlePersistant cmd : listDetail) {
		               if(cmd.getIdx_client() != null 
		            		   && cmd.getIdx_client().toString().equals(clientIdx) 
		            		   && cmd.getMenu_idx() != null 
		            		   && cmd.getMenu_idx().equals(menuIdx)){
		                   return cmd;
		               }
		           }
		       } 
		       // Si groupe ou article dans menu ------------
		       else if(menuIdx != null){
		           if(typeLigne.equals(TYPE_LIGNE_COMMANDE.GROUPE_FAMILLE.toString()) || typeLigne.equals(TYPE_LIGNE_COMMANDE.GROUPE_MENU.toString())){
		               for (CaisseMouvementArticlePersistant cmd : listDetail) {
		                   if(cmd.getIdx_client().toString().equals(clientIdx) && cmd.getMenu_idx() != null){
		                       if(cmd.getType_ligne().equals(typeLigne) 
		                               && cmd.getElementId().equals(elementId)){
		                           return cmd;
		                       }
		                   }
		               }
		           } else if(typeLigne.equals(TYPE_LIGNE_COMMANDE.ART_MENU.toString())){
		               for (CaisseMouvementArticlePersistant cmd : listDetail) {
		                   if(cmd.getIdx_client().toString().equals(clientIdx) && cmd.getMenu_idx() != null && cmd.getType_ligne().equals(TYPE_LIGNE_COMMANDE.ART_MENU.toString()) 
		                           && cmd.getParent_code().equals(parentCode) 
		                           && cmd.getElementId().equals(elementId)){
		                       return cmd;
		                   }
		               }
		           }
		       } 
		       // Si groupe ou article hors menu ------------
		       else{
		           if(typeLigne.equals(TYPE_LIGNE_COMMANDE.GROUPE_FAMILLE.toString())){
		               for (CaisseMouvementArticlePersistant cmd : listDetail) {
		                   if(cmd.getIdx_client().toString().equals(clientIdx) && cmd.getMenu_idx() == null){
		                       if(cmd.getType_ligne().equals(typeLigne) && cmd.getElementId().equals(elementId)){
		                           return cmd;
		                       }
		                   }
		               }
		           } else if(typeLigne.equals(TYPE_LIGNE_COMMANDE.ART.toString()) || typeLigne.equals(TYPE_LIGNE_COMMANDE.GARANTIE.toString())){
		               for (CaisseMouvementArticlePersistant cmd : listDetail) {
		                   if(cmd.getIdx_client().toString().equals(clientIdx) && cmd.getMenu_idx() == null 
		                		   && (cmd.getType_ligne().equals(TYPE_LIGNE_COMMANDE.ART.toString()) || cmd.getType_ligne().equals(TYPE_LIGNE_COMMANDE.GARANTIE.toString())) 
		                           && cmd.getParent_code().equals(parentCode) 
		                           && cmd.getElementId().equals(elementId)){
		                      return cmd;
		                   }
		               }
		           }
		       }
		       return null;
		   }
		
		/**
		 * @param httpUtil
		 */
		public void annulerEncaissement(ActionUtil httpUtil){
		   CaisseMouvementArticlePersistant cmvP = getSelectedCommandeLigne(httpUtil);
	       if(cmvP != null){
	    	   CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
	    	   
	    	   cmvP.setIs_encaisse(false);
	           if(CURRENT_COMMANDE.getId() != null){
	        	   caisseWebService.majMouvementPaiement(CURRENT_COMMANDE);
	           }
	           
	           MessageService.addGrowlMessage(MSG_TYPE.SUCCES, "", "L'encaissement est annulé.");
	           
	           httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
	       }
		}
		
		public void chargerPortefeuille(ActionUtil httpUtil){
			Long clientId = httpUtil.getWorkIdLong();
			BigDecimal montant = BigDecimalUtil.get(httpUtil.getParameter("portefeuille.mtt_recharge"));
			String modePaiement = httpUtil.getParameter("portefeuille.mode_paie");
			ClientPersistant clientP = clientService.findById(clientId);
			//
			if(BooleanUtil.isTrue(clientP.getIs_portefeuille())){
				PrintPosBean pu = portefeuilleService2.ajouterRecharge(clientId, montant, modePaiement, true, "CLI");
				portefeuilleService2.majSoldePortefeuilleMvm(clientId, "CLI");
				
				if(pu != null){
					printData(httpUtil, pu, true);
				}
				
				MessageService.addGrowlMessage(MSG_TYPE.SUCCES, "", "Portefeuille rechargé.");
			} else{
				MessageService.addGrowlMessage("", "Ce client n'a pas l'option portefeuille activée.");
			}
			
			httpUtil.setDynamicUrl("caisse-web.caisseWeb.initPersonne");
		}
		
		public void initBarreCodeBalance(ActionUtil httpUtil) {
			httpUtil.setDynamicUrl("/domaine/caisse/balance/barrecode_reader.jsp");
		}
		public void initBarreCodeLecteur(ActionUtil httpUtil) {
			httpUtil.setDynamicUrl("/domaine/caisse/lecteur-prix/barrecode_reader.jsp");
		}
		
		public void loadArtCodeBarre(ActionUtil httpUtil) {
			httpUtil.setMenuAttribute("IS_SEARCH", true);
			boolean isPharma = ContextAppli.SOFT_ENVS.pharma.toString().equals(StrimUtil.getGlobalConfigPropertie("context.soft"));
			String codeBarre = httpUtil.getParameter("art.code_barre");
			List<ArticlePersistant> listArticleBarre = null;
			//
			if(isPharma) {
				String marque = httpUtil.getParameter("art.marque");
				if(StringUtil.isEmpty(codeBarre) && StringUtil.isEmpty(marque)) {
					MessageService.addGrowlMessage("Erreur saisie", "Veuillez saisir un critère de recherche.");
					return;
				}
				listArticleBarre = articleService2.getArticlesByCodeBarreAndMarque(codeBarre, marque);
			} else {
				String codeLibelle = httpUtil.getParameter("art.code");
				String codePese = httpUtil.getParameter("art.code_pese");
				if(StringUtil.isEmpty(codeBarre) && StringUtil.isEmpty(codeLibelle) && StringUtil.isEmpty(codePese)) {
					MessageService.addGrowlMessage("Erreur saisie", "Veuillez saisir un critère de recherche.");
					return;
				}
				if(StringUtil.isNotEmpty(codePese) && StringUtil.isEmpty(codeBarre) && StringUtil.isEmpty(codeLibelle)){
					httpUtil.setRequestAttribute("codePese", codePese);
				}
				listArticleBarre = articleService2.getArticlesByCodeBarre(codeBarre, codeLibelle, codePese);
			}
			//
			
			httpUtil.setRequestAttribute("listArticle", listArticleBarre);
			httpUtil.setRequestAttribute("nbrTotal", listArticleBarre.size());
			
			// AJouter controle stock si il est paramétré
			addCtrlStock(httpUtil, listArticleBarre);
			
			httpUtil.setDynamicUrl(getDetail_choix_path(httpUtil));
		}
		
		
		/**
		 * @param httpUtil
		 */
		public void verrouillerCaisse(ActionUtil httpUtil) {
			httpUtil.setMenuAttribute("IS_CAISSE_VERROUILLE", true);
			httpUtil.writeResponse("REDIRECT");
		}
		
		/**
		 * @param httpUtil
		 */
		public void initLivraison(ActionUtil httpUtil) {
			//httpUtil.setRequestAttribute("listLivreur", employeService.getListEmployeActifs("LIVREUR"));
			httpUtil.setRequestAttribute("listLivreur", userService.getListUserActifsByProfile("LIVREUR"));
			httpUtil.setRequestAttribute("mvm", httpUtil.getWorkId());
			httpUtil.setRequestAttribute("caisse", EncryptionUtil.encrypt(httpUtil.getParameter("caisse.id")));
			
			httpUtil.setDynamicUrl("/domaine/caisse/normal/livraison_edit.jsp");
		}
		
		
		public void valider_livraison(ActionUtil httpUtil) {
			Long livreurId = httpUtil.getLongParameter("livreur.id");
			Long mvmId = httpUtil.getWorkIdLong();
			caisseService.validerLivraison(mvmId, livreurId);
	    	
			
			if((ReplicationGenerationEventListener._IS_CLOUD_SYNCHRO_INSTANCE || ContextAppli.IS_FULL_CLOUD()) 
					&& httpUtil.getUserAttribute("ENV_MOBILE") != null) {
				CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
	 
				String typeNotif = TYPE_NOTIFICATION.ETS_CLIENT_CMD_VALIDE.toString();
				String message = "La commande est marquée comme livrée";
		    	//notifier
				Map<String, String> mapData = new HashMap<>();
				mapData.put("title", "Statut commande");
				mapData.put("message", message);
				mapData.put("type", typeNotif);
				mapData.put("livreur", CURRENT_COMMANDE.getOpc_employe()!=null ? CURRENT_COMMANDE.getOpc_employe().getId().toString() : "");
				UserPersistant user = (UserPersistant) MessageService.getGlobalMap().get(ProjectConstante.SESSION_GLOBAL_USER);
				NotificationQueuService notifService = ServiceUtil.getBusinessBean(NotificationQueuService.class);
				notifService.addNotification(mapData, CURRENT_COMMANDE.getOpc_livreurU(), CURRENT_COMMANDE, null);
			}
			
			//
			
			MessageService.addGrowlMessage(MSG_TYPE.SUCCES, "Gestion livraison", (livreurId==null?"La commande est marquée comme non livrée":"La commande est marquée comme livrée"));
			//
			initHistorique(httpUtil);
		}
		/**
		 * @param httpUtil
		 */
		public void confirmAnnuleCmd(ActionUtil httpUtil) {
			String badge = (String)ControllerUtil.getParam(httpUtil.getRequest(), "tkn");
			
			String typeAct = httpUtil.getParameter("tp");
			if(httpUtil.getParameter("tpact") != null){
				typeAct = httpUtil.getParameter("tpact");
			}
			if(typeAct == null) {
				typeAct = "delrow";;
			}
			
			boolean isBadge = StringUtil.isNotEmpty(badge);
			UserBean userBean = null;
			//
			if(isBadge){
				userBean = userService.getUserByBadge(badge.trim());
				//
				if(userBean == null){
					MessageService.addGrowlMessage("Erreur authentification", "<b>Ce badge n'a pas été encore enregistré.</b>");
					return;
				}
			} else {
				if(StringUtil.isEmpty(httpUtil.getParameter("cmd.password"))) {
					MessageService.addFieldMessage("cmd.password", "Le mot depasse est obligatoire.");
					return;
				}
				if(StringUtil.isEmpty(httpUtil.getParameter("cmd.user.id"))) {
					MessageService.addFieldMessage("cmd.user.id", "Le login est obligatoire.");
					return;
				}
				
				Long userId = Long.valueOf(httpUtil.getParameter("cmd.user.id"));
				String pw = new EncryptionEtsUtil(EncryptionEtsUtil.getDecrypKey()).encrypt(httpUtil.getParameter("cmd.password"));
				userBean = userService.findById(userId);
				
				if(!pw.equals(userBean.getPassword())) {
					MessageService.addGrowlMessage("Erreur authentification", "<b>Le mot de passe est erroné.</b>");
					return;
				}
			}
				
			// Si le compte est désactivé
			if(BooleanUtil.isTrue(userBean.getIs_desactive())){
				MessageService.addGrowlMessage("Erreur authentification", "<b>Ce compte utilisateur est désactivé.</b>");
				return;
			}
				
			boolean isCaissier = userBean.isInProfile("CAISSIER");
			boolean isManagerOrAdmin = (userBean.isInProfile("MANAGER") || userBean.isInProfile("ADMIN"));
			//
			if(typeAct.equals("delock")) {// Si Delock caisse
				if(!isManagerOrAdmin && !ContextAppli.getUserBean().getLogin().equals(userBean.getLogin())) {
					MessageService.addGrowlMessage("Privélèges insuffisants", "<b>La caisse doit être dé-verrouillée par "+ContextAppli.getUserBean().getLogin()+" ou par un manager.</b>");
					return;
				}
			} else{// Si annulation
				if(isCaissier || !isManagerOrAdmin) {
					MessageService.addGrowlMessage("Privélèges insuffisants", "<b>L'annulation doit être confirmée par un administrateur ou un manager.</b>");
					return;
				}
			}
			
			httpUtil.setRequestAttribute("user_annul", userBean);
			
			if(typeAct.equals("delock")) {
				httpUtil.removeMenuAttribute("IS_CAISSE_VERROUILLE");
				httpUtil.writeResponse("REDIRECT");
			} else if(typeAct.equals("offrir")) {
				offrirLigneCommande(httpUtil);
			} else if(typeAct.equals("reduce")) {
				selectOffre(httpUtil);
			} else if(typeAct.equals("delrow")) {
				deleteRow(httpUtil);
			} else {
				annulerCommande(httpUtil);
			}
			 httpUtil.addJavaScript("$('#close_modal').trigger('click');");
		}

		public boolean checkJournee(){
			IJourneeService journeeService = ServiceUtil.getBusinessBean(IJourneeService.class);
			ICaisseService caisseService = ServiceUtil.getBusinessBean(ICaisseService.class);
			
			JourneePersistant lastJr = journeeService.getLastJournee();
			if(lastJr == null){
				MessageService.addGrowlMessage("", "<h2>Aucune journée n'a été trouvée.</h2>");
				return false;
			}
			CaissePersistant caisseBean = ContextAppliCaisse.getCaisseBean();
			
			if(!lastJr.getId().equals(ContextAppliCaisse.getJourneeBean().getId())){
				MessageService.addGrowlMessage("", "<h2>La journée ouverte ne correspont pas à la dernière journée de travail.</h2>");
				return false;
			} else if(ContextAppli.STATUT_JOURNEE.CLOTURE.getStatut().equals(lastJr.getStatut_journee())){
				MessageService.addGrowlMessage("", "<h2>La journée encours est clôturée.</h2>");
				return false;
			} else if(caisseBean != null && ContextAppli.STATUT_JOURNEE.CLOTURE.getStatut().equals(caisseService.findById(caisseBean.getId()).getStatutCaisse())){
				MessageService.addGrowlMessage("", "<h2>Cette caisse est clôturée.</h2>");
				return false;
			}
			return true;
		}
		
		/**
		 * @param httpUtil
		 */
		public void annulerCommande(ActionUtil httpUtil) {
			httpUtil.removeUserAttribute("IS_RETOUR");// Annuler variable du retour

			if(!checkJournee()){
				return;
			}
					
			httpUtil.setUserAttribute("CURRENT_IDX_CLIENT", 1);// Index client			
			String typeAct = httpUtil.getParameter("tp");
			Long mvmId = httpUtil.getWorkIdLong();
			CaisseMouvementPersistant currCmd = null;
			//
			if(mvmId == null) {
				currCmd = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
				httpUtil.removeUserAttribute("CURRENT_TABLE_REF");// Ref table
				httpUtil.removeMenuAttribute("COUVERTS_TABLE");
				
				currCmd.setMax_idx_client(1);
				
				if(currCmd.getList_article().size() == 0) {
					if(currCmd.getId() != null) {
			            caisseWebService.annulerMouvementCaisse(currCmd.getId(), ContextAppli.getUserBean());
			            
			            // Libérer les commandes
						userService.unlockCommandes(currCmd.getId(), ContextAppli.getUserBean().getId());
						if(currCmd.getOpc_user_lock() != null && currCmd.getOpc_user_lock().getId() == ContextAppli.getUserBean().getId()) {
							currCmd.setOpc_user_lock(null);
						}
						
			            if(currCmd.getOpc_client() != null){
			            	portefeuilleService2.majSoldePortefeuilleMvm(currCmd.getOpc_client().getId(), "CLI");
			            }
					}

					MessageService.addGrowlMessage(MSG_TYPE.SUCCES, "Commande annulée", "La commande en cours est annulée.");
					httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
					return;
				}
			} else {
				currCmd = familleService.findById(CaisseMouvementPersistant.class, mvmId);
			}
			
			
			boolean isAnnulCmdEncaisse = StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("ANNUL_CMD_ENCAISSEE"));
			
			if(currCmd != null && currCmd.getId() != null){
				CaisseMouvementPersistant caiseMvmDb = familleService.findById(CaisseMouvementPersistant.class, currCmd.getId());
				if(ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.ANNUL.toString().equals(caiseMvmDb.getLast_statut())
						|| (!isAnnulCmdEncaisse && caiseMvmDb.getMode_paiement() != null)) {
					initNewCommande(httpUtil);
					 //
			        if(typeAct.equals("histo")) {// Depuis l'historique
			        	initHistorique(httpUtil);
			        } else {// Depuis le détail de la commande
			        	httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
			        }
			        // Close popup
			        httpUtil.addJavaScript("$('#close_modal').trigger('click');");
			        MessageService.addGrowlMessage(MSG_TYPE.SUCCES, "Commande annulée", "La commande est annulée avec succès.");
			        return;
				}
			}
			
			// Sauvegarde de la commande dans le cas de sa suppression -----------------
	        if(currCmd != null && currCmd.getId() != null){
	        	UserBean userAnnulBean = (UserBean) httpUtil.getRequestAttribute("user_annul");
	            // Création de la comm
	            caisseWebService.annulerMouvementCaisse(currCmd.getId(), userAnnulBean);
	            
	            // Libérer les commandes
				userService.unlockCommandes(currCmd.getId(), ContextAppli.getUserBean().getId());
				
	            if(currCmd.getOpc_client() != null){
	            	portefeuilleService2.majSoldePortefeuilleMvm(currCmd.getOpc_client().getId(), "CLI");
	            }
	            
	            // Alerter cuisine
	            if(StringUtil.isNotEmpty(currCmd.getCaisse_cuisine() != null)) {
	            	String[] caisseDestArray = StringUtil.getArrayFromStringDelim(currCmd.getCaisse_cuisine(), ";");
	    	 		 if(caisseDestArray != null){
	    		  		boolean isCoudMaster = ContextAppli.IS_CLOUD_MASTER();
	    		  		boolean isFullCloud = ContextAppli.IS_FULL_CLOUD();
	    		  		EtablissementPersistant ets = ContextAppli.getEtablissementBean();
	    		  		
	    		  		final CaisseMouvementPersistant fcurrCmd = familleService.findById(CaisseMouvementPersistant.class, currCmd.getId());
	    		  		// Prevent lasy Exc
	    		  		for(CaisseMouvementArticlePersistant det : fcurrCmd.getList_article()) {
	    		  			det.getCode();
	    		  		}
	    		  		
	    		  		List<Long> caIds = new ArrayList<>();
	    	 			for(String caisseElement : caisseDestArray){
	    	 				String[] caisseElementArray = StringUtil.getArrayFromStringDelim(caisseElement, ":");
	    	 				Long caisseId = Long.valueOf(caisseElementArray[0]);
	    	 				
	    	 				if(caIds.contains(caisseId)) {
	    	 					continue;
	    	 				}
	    	 				caIds.add(caisseId);
	    	 				// 
							new Thread(() -> {
								if(isCoudMaster || isFullCloud) {
									 MessageService.getGlobalMap().put("GLOBAL_ETABLISSEMENT", ets);
								 }
								
			        			try {
			    	 				new PrintCuisineUtil(
			    	 						familleService.findById(CaissePersistant.class, caisseId), 
			    	 						fcurrCmd).print();
			        			} catch (Exception ex) {
			        				throw new RuntimeException(ex);
			        			}
			        	    }).start();
	    	 			}
	    	 		 }
	            }
	        }
	        //
	        if(mvmId == null) {
	        	initNewCommande(httpUtil);
	        }
	        
	        // Chercher les afficheurs
	        sendDataToScreen(httpUtil, "X");
	        
	        //
	        if(typeAct.equals("histo")) {// Depuis l'historique
	        	initHistorique(httpUtil);
	        } else {// Depuis le détail de la commande
	        	httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
	        }
	        // Close popup
	        httpUtil.addJavaScript("$('#close_modal').trigger('click');");
	        
	        MessageService.addGrowlMessage(MSG_TYPE.SUCCES, "Commande annulée", "La commande est annulée avec succès.");
		}
		
		/**
		 * @param httpUtil
		 */
		public void loadConfirmAnnule(ActionUtil httpUtil) {
			httpUtil.setFormReadOnly(false);
			String typeAct = httpUtil.getParameter("tp");
			
			if(httpUtil.getParameter("tpact") != null){
				typeAct = httpUtil.getParameter("tpact");//"delrow";
			}
			
			httpUtil.setRequestAttribute("tp", typeAct);

			List<UserPersistant> finalListUsers = new ArrayList<UserPersistant> ();
			List<UserPersistant> listUsers = userService.findAllUser(true);

			//
			if(typeAct.equals("histo")) {// Si on annule depuis l'historique
				httpUtil.setRequestAttribute("is_hist", 1);
				httpUtil.setRequestAttribute("mvm", EncryptionUtil.encrypt(httpUtil.getWorkId()));
				httpUtil.setRequestAttribute("caisse", EncryptionUtil.encrypt(httpUtil.getParameter("caisse.id")));
			}
			else if(typeAct.equals("delock")) {// Managers, admin et utilisateur ayant locké
				for (UserPersistant userPersistant : listUsers) {
					if(userPersistant.getLogin().equals(ContextAppli.getUserBean().getLogin()) 
							|| userPersistant.isInProfile("MANAGER")
							|| userPersistant.isInProfile("ADMIN")) {
					finalListUsers.add(userPersistant);
					}
				}
				listUsers = finalListUsers;
			} else if(typeAct.equals("delrow")){
				for (UserPersistant userPersistant : listUsers) {
					if(userPersistant.isInProfile("MANAGER")
							|| userPersistant.isInProfile("ADMIN")) {
					finalListUsers.add(userPersistant);
					}
				}
				listUsers = finalListUsers;
				httpUtil.setRequestAttribute("trParams", httpUtil.getParameter("trParam"));
				
			} else if(typeAct.equals("reduce") || typeAct.equals("offrir")){
				for (UserPersistant userPersistant : listUsers) {
					if(userPersistant.isInProfile("MANAGER")
							|| userPersistant.isInProfile("ADMIN")) {
					finalListUsers.add(userPersistant);
					}
				}
				listUsers = finalListUsers;
				if(typeAct.equals("offrir")) {
					httpUtil.setRequestAttribute("trParams", httpUtil.getParameter("trParam"));
				}
			} else {//Cas annulation
				for (UserPersistant userPersistant : listUsers) {
					if(userPersistant.isInProfile("MANAGER")
							|| userPersistant.isInProfile("ADMIN")) {
					finalListUsers.add(userPersistant);
					}
				}
				listUsers = finalListUsers;
			}
			
			httpUtil.setRequestAttribute("typeAct", typeAct);
			httpUtil.setRequestAttribute("listUser", listUsers);
			httpUtil.setRequestAttribute("cmdWid", httpUtil.getWorkIdLong());
			
			httpUtil.setDynamicUrl("/domaine/caisse/"+httpUtil.getUserAttribute("PATH_JSP_CM")+"/authentification-cmd-popup.jsp");
		}
		public static String GET_STYLE_CONF(String code, String fontColor) {
			String style = "";
			if(StringUtil.isNotEmpty(ContextGloabalAppli.getGlobalConfig(code))) {
				style = "background-color:"+ContextGloabalAppli.getGlobalConfig(code)+" !important;";
			}
			
			if(StringUtil.isNotEmpty(ContextGloabalAppli.getGlobalConfig(fontColor))) {
				style = style + "color:"+ContextGloabalAppli.getGlobalConfig(fontColor)+" !important;";
			}
			
			return style;
		}
		
		/**
		 * @param httpUtil
		 */
		public void deleteRow(ActionUtil httpUtil) {
			   String typeLigne = httpUtil.getParameter("tp");
			   
			   if("cli".equalsIgnoreCase(typeLigne)){
				   supprimerClient(httpUtil);
				   return;
			   } else if("TAB".equalsIgnoreCase(typeLigne)){
				   supprimerTable(httpUtil);
				   return;
			   }
			   
			   //
		       if(StringUtil.isEmpty(typeLigne) 
		    		   || (StringUtil.isEmpty(httpUtil.getParameter("cli")) && !typeLigne.equals(TYPE_LIGNE_COMMANDE.OFFRE.toString()))) {
		    	   httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
		    	   return;
		       }
		       
			   CaisseMouvementPersistant currMvm = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
			   if(currMvm == null) {
				   return;
			   }

			   Integer clientIdx = StringUtil.isNotEmpty(httpUtil.getParameter("cli")) ? Integer.valueOf(httpUtil.getParameter("cli")) : null;
			   String refTable= StringUtil.isNotEmpty(httpUtil.getParameter("tbl")) ? httpUtil.getParameter("tbl") : null;
			   List<CaisseMouvementArticlePersistant> listDetail = currMvm.getList_article();
		       List<CaisseMouvementOffrePersistant> listOffres = currMvm.getList_offre();
		       Iterator<CaisseMouvementOffrePersistant> listOffresIter = listOffres.iterator();
		       Iterator<CaisseMouvementArticlePersistant> listDetailIter = listDetail.iterator();

		       // Marquer comme annulé dans le mouvement ----------------------------
		       String code = httpUtil.getParameter("cd");
		       Long elementId = httpUtil.getLongParameter("elm");
		       String parentCode = httpUtil.getParameter("par");
		       String menuIdx = httpUtil.getParameter("mnu");
		       String idxArt = httpUtil.getParameter("idx");
		       UserBean userAnnulBean = (UserBean) httpUtil.getRequestAttribute("user_annul");
		       boolean isSavedCmd = currMvm.getId() == null ? false : true;
		       boolean isSavedLigne = StringUtil.isTrue(httpUtil.getParameter("isDb"));
		       
		       // Si suppresion offre ------------
		       if(typeLigne.equals(TYPE_LIGNE_COMMANDE.OFFRE.toString())){
		    	   while (listOffresIter.hasNext()) {
		    		   CaisseMouvementOffrePersistant offre = listOffresIter.next();
		               if(offre.getOpc_offre().getId().equals(elementId) && !BooleanUtil.isTrue(offre.getIs_annule())){
		                   if(isSavedLigne) {
		                	   offre.setIs_annule(true);
		                   } else {
		                	   listOffresIter.remove();
		                   }
		                   break;
		               }
		           }
		       } 
		       // Si suppresion menu ------------
		       else if(typeLigne.equals(TYPE_LIGNE_COMMANDE.MENU.toString())){
		    	   while (listDetailIter.hasNext()) {
		    		   CaisseMouvementArticlePersistant cmd = listDetailIter.next();
		    		   //
		               if(cmd.getIdx_client().equals(clientIdx) 
		            		   && (""+cmd.getRef_table()).equals(""+refTable)
		            		   && cmd.getMenu_idx() != null && cmd.getMenu_idx().equals(menuIdx) 
		            		   && !BooleanUtil.isTrue(cmd.getIs_annule())){
		            	   if(isSavedLigne) {
		            		   cmd.setIs_annule(true);
		            		   cmd.setOpc_user_annul(userAnnulBean!=null ? userAnnulBean : ContextAppli.getUserBean());
		            		   cmd.setDate_annul(new Date());
		            		   cmd.setType_opr(Integer.valueOf(3));
		            		   cmd.setLast_statut(ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.VALIDE.toString());
		            	   } else {
		            		   listDetailIter.remove();
		            	   }
		                   //
		                   httpUtil.removeUserAttribute("CURRENT_MENU_NUM");
		                   httpUtil.removeUserAttribute("CURRENT_MENU_COMPOSITION");
		               }
		           }
		       } 
		       // Si suppression groupe ou article dans menu ------------
		       else if(StringUtil.isNotEmpty(menuIdx)){ 
		    	   boolean isManager = ContextAppli.getUserBean().isInProfile("MANAGER");
		    	   boolean isAdmin = ContextAppli.getUserBean().isInProfile("ADMIN"); 
		    	   boolean isDelDetMnu = (isAdmin || isManager || StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("DEL_MNU_DET")));
		    	   
		           if(typeLigne.equals(TYPE_LIGNE_COMMANDE.GROUPE_FAMILLE.toString()) || typeLigne.equals(TYPE_LIGNE_COMMANDE.GROUPE_MENU.toString())){
		        	   // Ctrl remove avec menu
		        	   if(!isDelDetMnu) {
		        		   List<CaisseMouvementArticlePersistant> listToDeleteAll = new ArrayList<>();   
		        		   getAllDeleteRecursiveGroupe(listToDeleteAll, listDetail, clientIdx, refTable, typeLigne, menuIdx, elementId, parentCode);
			        	   for (CaisseMouvementArticlePersistant cap : listToDeleteAll) {
			        		   if(!BigDecimalUtil.isZero(cap.getMtt_total())) {
			        			   MessageService.addGrowlMessage("", "<h3>Une ligne avec montant ne peut pas être supprimée.</h3>");
			        			   return;
			        	   		}
			        	   }
		        	   }
		        	   
		        	   List<CaisseMouvementArticlePersistant> listToDelete = new ArrayList<>();
		        	   deleteRecursiveGroupe(listToDelete, listDetail, isSavedLigne, clientIdx, refTable, typeLigne, menuIdx, elementId, code);
		        	   //
		        	   for (CaisseMouvementArticlePersistant cap : listToDelete) {
		        		   listDetail.remove(cap);
		        	   }
		           } else if(typeLigne.equals(TYPE_LIGNE_COMMANDE.ART_MENU.toString())){
		        	   while (listDetailIter.hasNext()) {
		        		   CaisseMouvementArticlePersistant cmd = listDetailIter.next();
		                   if(cmd.getIdx_client().equals(clientIdx) && cmd.getMenu_idx() != null
		                		   && (""+cmd.getRef_table()).equals(""+refTable)
		                		   && cmd.getType_ligne().equals(TYPE_LIGNE_COMMANDE.ART_MENU.toString()) 
		                           && cmd.getParent_code().equals(parentCode) 
		                           && cmd.getElementId().equals(elementId)
		                           && cmd.getMenu_idx().equals(menuIdx)
		                           && cmd.getCode().equals(code) 
		                           && !BooleanUtil.isTrue(cmd.getIs_annule())){
		                	   
		                	   if(!isDelDetMnu && !BigDecimalUtil.isZero(cmd.getMtt_total())) {
		                		   MessageService.addGrowlMessage("", "<h3>Cette ligne avec montant ne peut pas être supprimée.</h3>");
		                		   return;
		                	   }
		                	   
		                	   if(isSavedLigne) {
		                		   cmd.setIs_annule(true);
			            		   cmd.setOpc_user_annul(userAnnulBean!=null ? userAnnulBean : ContextAppli.getUserBean());
			            		   cmd.setDate_annul(new Date());
			            		   cmd.setType_opr(Integer.valueOf(3));
			            		   cmd.setLast_statut(ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.VALIDE.toString());
		                	   } else {
		                		   listDetailIter.remove();
		                	   }
		                       break;
		                   }
		               }
		           }
		       } 
		       // Si suppresion groupe ou article hors menu ------------
		       else{
		           if(typeLigne.equals(TYPE_LIGNE_COMMANDE.GROUPE_FAMILLE.toString())){
		        	   List<CaisseMouvementArticlePersistant> listToDelete = new ArrayList<>();
		        	   deleteRecursiveGroupe(listToDelete, listDetail, isSavedLigne, clientIdx, refTable, typeLigne, menuIdx, elementId, code);
		        	   //
		        	   for (CaisseMouvementArticlePersistant cap : listToDelete) {
		        		   listDetail.remove(cap);
		        	   }
		           } else if(typeLigne.equals(TYPE_LIGNE_COMMANDE.ART.toString()) || typeLigne.equals(TYPE_LIGNE_COMMANDE.GARANTIE.toString())){
		        	   while (listDetailIter.hasNext()) {
		        		   CaisseMouvementArticlePersistant cmd = listDetailIter.next();
		                   if(cmd.getIdx_client().equals(clientIdx) 
		                		   && cmd.getMenu_idx() == null
		                		   && (""+cmd.getRef_table()).equals(""+refTable)
		                		   && (cmd.getType_ligne().equals(TYPE_LIGNE_COMMANDE.ART.toString()) || cmd.getType_ligne().equals(TYPE_LIGNE_COMMANDE.GARANTIE.toString()))
		                           && cmd.getParent_code().equals(parentCode) 
		                           && cmd.getElementId().equals(elementId) 
		                           && (StringUtil.isEmpty(idxArt) || idxArt.equals(""+cmd.getIdx_element()))// Index utile dans le cas des article balance lors de plusieurs articles
		                           && cmd.getCode().equals(code) 
		                           && !BooleanUtil.isTrue(cmd.getIs_annule())){
		                	   if(isSavedLigne) {
		                		   cmd.setIs_annule(true);
			            		   cmd.setOpc_user_annul(userAnnulBean!=null ? userAnnulBean : ContextAppli.getUserBean());
			            		   cmd.setDate_annul(new Date());
			            		   cmd.setType_opr(Integer.valueOf(3));
			            		   cmd.setLast_statut(ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.VALIDE.toString());
		                	   } else {
		                		   listDetailIter.remove();
		                	   }
		                       break;
		                   }
		               }
		           }
		       }
		       // Garantie
			   ajouterFraisGarantie(httpUtil);
			   //
		       isHasChild(httpUtil, clientIdx, refTable, menuIdx, parentCode, false);
		       // Maj    
		       sortAndAddCommandeLigne(httpUtil, currMvm);
		       
		       // Corriger le problème du montant négatif dans le cas de offerte
		       if(currMvm.getList_offre() != null 
		    		   && currMvm.getList_offre().size() > 0
		    		   && BigDecimalUtil.ZERO.compareTo(currMvm.getMtt_commande_net()) > 0) {
		    	   currMvm.setMtt_commande_net(BigDecimalUtil.ZERO);
		    	   
		    	   for(CaisseMouvementOffrePersistant offreP : currMvm.getList_offre()) {
		    		   if(offreP.getOpc_offre() != null && BigDecimalUtil.get(100).compareTo(offreP.getOpc_offre().getTaux_reduction()) == 0) {
		    			   offreP.setMtt_reduction(currMvm.getMtt_commande());
		    			   currMvm.setMtt_reduction(currMvm.getMtt_commande());
		    		   }
		    	   }
		       }
		      
		       httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
		   }
		
		/**
		 * @param httpUtil
		 */
		public void supprimerTable(ActionUtil httpUtil) {
			String refTab = httpUtil.getParameter("ref_tab");
			
			// Selectionner la table de ce client
			CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
			if(CURRENT_COMMANDE.getId() != null && CURRENT_COMMANDE.getLast_statut().equals(STATUT_CAISSE_MOUVEMENT_ENUM.LIVRE.toString())) {
				MessageService.addGrowlMessage("Mise à jour impossible", "Cette commande est marquée comme livrée. <br/>Elle ne peut plus être modifiée.");
				return;
			}
			
			if(CURRENT_COMMANDE != null && CURRENT_COMMANDE.getList_article() != null){
				 boolean isSaved = CURRENT_COMMANDE.getId() == null ? false : true;
				 Iterator<CaisseMouvementArticlePersistant> listDetailIter = CURRENT_COMMANDE.getList_article().iterator();
				 while (listDetailIter.hasNext()) {
				   CaisseMouvementArticlePersistant caisseMvmP = listDetailIter.next();
				   if(refTab.equals(caisseMvmP.getRef_table())){
					   if(isSaved) {
						   caisseMvmP.setIs_annule(true);
						   caisseMvmP.setType_opr(Integer.valueOf(3));
						   caisseMvmP.setLast_statut(ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.VALIDE.toString());
					   } else {
						   listDetailIter.remove();
					   }
				   }
				}
			}
			List<String> listRefTable = new ArrayList<>();
			for(CaisseMouvementArticlePersistant caisseMvmP : CURRENT_COMMANDE.getList_article()){
				if(BooleanUtil.isTrue(caisseMvmP.getIs_annule())){
					continue;
				}
				if(!listRefTable.contains(caisseMvmP.getRef_table())){
					listRefTable.add(caisseMvmP.getRef_table());
				}
			 }
			if(httpUtil.getUserAttribute("CURRENT_TABLE_REF")!=null && httpUtil.getUserAttribute("CURRENT_TABLE_REF").equals(refTab)){
				if(listRefTable.size()>0){
					httpUtil.setUserAttribute("CURRENT_TABLE_REF", listRefTable.get(listRefTable.size()-1));
				} else{
					httpUtil.removeUserAttribute("CURRENT_TABLE_REF");
				}
			}
			
			// Supprimer les clients orphelins
			// Selectionner la table de ce client
			List<Integer> listIdxClient = new ArrayList();
			if(CURRENT_COMMANDE != null && CURRENT_COMMANDE.getList_article() != null){
				 for(CaisseMouvementArticlePersistant caisseMvmP : CURRENT_COMMANDE.getList_article()){
					if(BooleanUtil.isTrue(caisseMvmP.getIs_annule())){
						continue;
					}
					if(!listIdxClient.contains(caisseMvmP.getIdx_client())){
						listIdxClient.add(caisseMvmP.getIdx_client());
					}
				 }
			}
			// Remettre à 1 le client 
			if(listIdxClient.size() == 0){
				httpUtil.setUserAttribute("CURRENT_IDX_CLIENT", 1);
			}
			
			majTotalMontantCommande(httpUtil, CURRENT_COMMANDE);
			
			httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
		}	
		
		/** ------------------------------------------------ Plan ----------------------------------------*/ 
		public void initTransfertArtMnu(ActionUtil httpUtil) {
			CaisseMouvementArticlePersistant mvmDet = getSelectedCommandeLigne(httpUtil);
			String clientIdx = httpUtil.getParameter("cli");
			String currRefTabeTraget = (String) httpUtil.getUserAttribute("CURRENT_TABLE_REF");
			
			if(mvmDet != null){
				httpUtil.setUserAttribute("CURR_ART_TRANS", mvmDet);
				httpUtil.setRequestAttribute("currArt", mvmDet.getLibelle());
			}
			CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
			List<Integer> listIdxClient = new ArrayList<>();
			// Selectionner la table de ce client
			if(CURRENT_COMMANDE != null && CURRENT_COMMANDE.getList_article() != null){
				for(CaisseMouvementArticlePersistant caisseMvmP : CURRENT_COMMANDE.getList_article()){
					if(BooleanUtil.isTrue(caisseMvmP.getIs_annule()) 
							|| (StringUtil.isNotEmpty(caisseMvmP.getRef_table()) 
									&& !caisseMvmP.getRef_table().equals(currRefTabeTraget))){
						continue;
					}
					if(!listIdxClient.contains(caisseMvmP.getIdx_client()) && !(""+caisseMvmP.getIdx_client()).equals(clientIdx)){
						listIdxClient.add(caisseMvmP.getIdx_client());
					}
				}
			}
			
			if(listIdxClient.size() > 1 || CURRENT_COMMANDE.getMax_idx_client() > 1){
				// Trier les tables
				Collections.sort(listIdxClient);
				
				if(listIdxClient.size() == 0 || CURRENT_COMMANDE.getMax_idx_client() != listIdxClient.get(listIdxClient.size()-1)){
					listIdxClient.add(CURRENT_COMMANDE.getMax_idx_client());
				}
				List<String[]> clientIdxList = new ArrayList<>();
				for (Integer idxCli : listIdxClient) {
					if(!clientIdx.equals(""+idxCli)){
						String[] data = {""+idxCli, "Client "+idxCli};
						clientIdxList.add(data);
					}
				}
				int i = 0;
				String[][] clientIdxArray = new String[clientIdxList.size()][2];
				for (String[] val : clientIdxList) {
					clientIdxArray[i] = val;
					i++;
				}
				
				// Liste des tables
				httpUtil.setRequestAttribute("clientIdxArray", clientIdxArray);
			}

			List<AgencementPersistant> listAgencement = familleService.findAll(AgencementPersistant.class);
			httpUtil.setRequestAttribute("listAgencement", listAgencement);
			
			boolean isConfirmTransfert = StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("SECURE_TRANSFERT_ART"));
			if(isConfirmTransfert) {
				   List<UserPersistant> finalListUsers = new ArrayList<>();
				   List<UserPersistant> listUsers = userService.findAllUser(true);
				   for (UserPersistant userPersistant : listUsers) {
						if(userPersistant.isInProfile("MANAGER")
								|| userPersistant.isInProfile("ADMIN")) {
						finalListUsers.add(userPersistant);
						}
					}
				   httpUtil.setRequestAttribute("listUser", finalListUsers);
			 }
			
			httpUtil.setDynamicUrl("/domaine/caisse/"+httpUtil.getUserAttribute("PATH_JSP_CM")+"/client-transfert.jsp");
		}
		
		/**
		 * @param httpUtil
		 */
		public void supprimerClient(ActionUtil httpUtil) {
			Integer idxCli = Integer.valueOf(httpUtil.getParameter("idx_cli"));
			
			// Selectionner la table de ce client
			CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
			if(CURRENT_COMMANDE.getId() != null && CURRENT_COMMANDE.getLast_statut().equals(STATUT_CAISSE_MOUVEMENT_ENUM.LIVRE.toString())) {
				MessageService.addGrowlMessage("Mise à jour impossible", "Cette commande est marquée comme livrée. <br/>Elle ne peut plus être modifiée.");
				return;
			}
			
			List<Integer> listIdxClient = new ArrayList<>();
			if(CURRENT_COMMANDE != null && CURRENT_COMMANDE.getList_article() != null){
				 boolean isSaved = CURRENT_COMMANDE.getId() == null ? false : true;
				 Iterator<CaisseMouvementArticlePersistant> listDetailIter = CURRENT_COMMANDE.getList_article().iterator();
				 while (listDetailIter.hasNext()) {
		    		   CaisseMouvementArticlePersistant caisseMvmP = listDetailIter.next();
		    		   if(idxCli.equals(caisseMvmP.getIdx_client())){
		    			   if(isSaved) {
		    				   caisseMvmP.setIs_annule(true);
		    				   caisseMvmP.setType_opr(Integer.valueOf(3));
		    				   caisseMvmP.setLast_statut(ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.VALIDE.toString());
		            	   } else {
		            		   listDetailIter.remove();
		            	   }
		    		   }
				}
				
				 for(CaisseMouvementArticlePersistant caisseMvmP : CURRENT_COMMANDE.getList_article()){
					if(BooleanUtil.isTrue(caisseMvmP.getIs_annule()) || caisseMvmP.getIdx_client() == null){
						continue;
					}
					if(!listIdxClient.contains(caisseMvmP.getIdx_client())){
						listIdxClient.add(caisseMvmP.getIdx_client());
					}
				 }
			}
			Collections.sort(listIdxClient);
			if(httpUtil.getUserAttribute("CURRENT_IDX_CLIENT")!=null && httpUtil.getUserAttribute("CURRENT_IDX_CLIENT").equals(idxCli)){
				int idx = (listIdxClient.size()>0 ? listIdxClient.get(listIdxClient.size()-1) : 1);
				httpUtil.setUserAttribute("CURRENT_IDX_CLIENT", idx);
			}
			
			majTotalMontantCommande(httpUtil, CURRENT_COMMANDE);
			
			httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
		}
		
		public void initOffrirLigneCommande(ActionUtil httpUtil){
			   String params = "cd="+httpUtil.getParameter("cd")
		   	 	+ "&tp="+httpUtil.getParameter("tp")
		   	 	+"&elm="+httpUtil.getLongParameter("elm")
		   	 	+"&par="+httpUtil.getParameter("par")
		   	 	+"&mnu="+httpUtil.getParameter("mnu")
		   	 	+"&cli="+httpUtil.getParameter("cli");
		    httpUtil.setRequestAttribute("params", params); 

		    boolean isConfirmReduce = StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("SECURE_REDUCTION_CMD"));
		    if(isConfirmReduce) {
				   List<UserPersistant> finalListUsers = new ArrayList<>();
				   List<UserPersistant> listUsers = userService.findAllUser(true);
				   for (UserPersistant userPersistant : listUsers) {
						if(userPersistant.isInProfile("MANAGER")
								|| userPersistant.isInProfile("ADMIN")) {
						finalListUsers.add(userPersistant);
						}
					}
				   httpUtil.setRequestAttribute("listUser", finalListUsers);
			   }
		    
			httpUtil.setDynamicUrl("/domaine/caisse/init_offre.jsp");
		}
		private void applyOffreReduc(CaisseMouvementArticlePersistant cmd, boolean isAnnuleOffre, 
							BigDecimal mttReduce, BigDecimal tauxReduce) {
			   if(isAnnuleOffre){
      		   		cmd.setMtt_total(BigDecimalUtil.add(cmd.getMtt_total(), cmd.getMtt_reduction()));
      		   		cmd.setMtt_reduction(null);
	      	   } else {
	          	   BigDecimal mttRedution = mttReduce;
	          	   if(!BigDecimalUtil.isZero(tauxReduce)) {		
	          		   mttRedution = BigDecimalUtil.divide(BigDecimalUtil.multiply(tauxReduce, cmd.getMtt_total()), BigDecimalUtil.get(100));
	          	   }
	          	   cmd.setMtt_reduction(mttRedution);
	          	   cmd.setMtt_total(BigDecimalUtil.substract(cmd.getMtt_total(), mttRedution));
	            }
		}
		public void offrirLigneCommandeRed(ActionUtil httpUtil){
			// --------------------------- AUTH -------------------------------------
			boolean isConfirmReduce = StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("SECURE_REDUCTION_CMD"));
			/*if(isConfirmReduce) {
				   String badge = (String)ControllerUtil.getParam(httpUtil.getRequest(), "qte.tkn");
				   
				   boolean isBadge = StringUtil.isNotEmpty(badge);
					UserBean userBean = null;
					//
					if(isBadge){
						userBean = userService.getUserByBadge(badge.trim());
						//
						if(userBean == null){
							MessageService.addBannerMessage("Ce badge n'a pas été encore enregistré.");
							return;
						}
					} else {
						if(StringUtil.isEmpty(httpUtil.getParameter("unlockQte.password"))) {
							MessageService.addFieldMessage("unlockQte.password", "Le mot depasse est obligatoire.");
							return;
						}
						if(StringUtil.isEmpty(httpUtil.getParameter("unlockQte.login"))) {
							MessageService.addFieldMessage("unlockQte.login", "Le login est obligatoire.");
							return;
						}
						
						Long userId = Long.valueOf(httpUtil.getParameter("unlockQte.login"));
						String pw = new EncryptionEtsUtil(EncryptionEtsUtil.getDecrypKey()).encrypt(httpUtil.getParameter("unlockQte.password"));
						userBean = userService.findById(userId);
						
						if(!pw.equals(userBean.getPassword())) {
							MessageService.addBannerMessage("Le mot de passe est erroné.");
							return;
						}
					}
						
					// Si le compte est désactivé
					if(BooleanUtil.isTrue(userBean.getIs_desactive())){
						MessageService.addBannerMessage("Ce compte utilisateur est désactivé.");
						return;
					}
			   }*/
			   // ----------------------------------------------------------------------
			   
			CaisseMouvementPersistant currMvm = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
		    List<CaisseMouvementArticlePersistant> listDetail = currMvm.getList_article();
		    boolean isAnnuleOffre = StringUtil.isTrue(httpUtil.getParameter("isafR"));
		    
		    // Marquer comme annulé dans le mouvement ----------------------------
	       String code = httpUtil.getParameter("cd");
	       String typeLigne = httpUtil.getParameter("tp");
	       Long elementId = httpUtil.getLongParameter("elm");
	       String parentCode = httpUtil.getParameter("par");
	       String menuIdx = httpUtil.getParameter("mnu");
	       String clientIdx = httpUtil.getParameter("cli");
	       
	       BigDecimal mttReduce = BigDecimalUtil.get(httpUtil.getParameter("mtt_reduce"));
	       BigDecimal tauxReduce = BigDecimalUtil.get(httpUtil.getParameter("taux_reduce"));
	       
	       // Si suppresion offre ------------
	       if(typeLigne.equals(TYPE_LIGNE_COMMANDE.OFFRE.toString())){
	          MessageService.addGrowlMessage("Ligne contenant offre", "On ne peut pas réduire une ligne contenant une autre réduction.");
	          return;
	       } 
	       // Si suppresion menu ------------
	       else if(typeLigne.equals(TYPE_LIGNE_COMMANDE.MENU.toString())){
	           for (CaisseMouvementArticlePersistant cmd : listDetail) {
	               if(cmd.getIdx_client().toString().equals(clientIdx) 
	            		   && cmd.getMenu_idx() != null 
	            		   && cmd.getMenu_idx().equals(menuIdx) 
	            		   && !BooleanUtil.isTrue(cmd.getIs_annule())){
	                   
	            	   applyOffreReduc(cmd, isAnnuleOffre, mttReduce, tauxReduce);
	                   break;
	               }
	           }
	       } 
	       // Si suppresion groupe ou article dans menu ------------
	       else if(menuIdx != null){
	           if(typeLigne.equals(TYPE_LIGNE_COMMANDE.GROUPE_FAMILLE.toString()) || typeLigne.equals(TYPE_LIGNE_COMMANDE.GROUPE_MENU.toString())){
	               for (CaisseMouvementArticlePersistant cmd : listDetail) {
	                   if(cmd.getIdx_client().toString().equals(clientIdx) && cmd.getMenu_idx() != null && !BooleanUtil.isTrue(cmd.getIs_annule())){
	                       if(BigDecimalUtil.isZero(cmd.getMtt_total())){
	                          continue; 
	                       }
	                       if(cmd.getType_ligne().equals(TYPE_LIGNE_COMMANDE.ART_MENU.toString()) && cmd.getMenu_idx().equals(menuIdx)
	                           && cmd.getParent_code().equals(code)){
	                    	   applyOffreReduc(cmd, isAnnuleOffre, mttReduce, tauxReduce);
	                       }
	                       if(cmd.getType_ligne().equals(typeLigne) && cmd.getElementId().equals(elementId)){
	                    	   applyOffreReduc(cmd, isAnnuleOffre, mttReduce, tauxReduce);
	                       }
	                   }
	               }
	           } else if(typeLigne.equals(TYPE_LIGNE_COMMANDE.ART_MENU.toString())){
	               for (CaisseMouvementArticlePersistant cmd : listDetail) {
	                   if(cmd.getIdx_client().toString().equals(clientIdx) && cmd.getMenu_idx() != null && cmd.getType_ligne().equals(TYPE_LIGNE_COMMANDE.ART_MENU.toString()) 
	                           && cmd.getParent_code().equals(parentCode) 
	                           && cmd.getElementId().equals(elementId) 
	                           && !BooleanUtil.isTrue(cmd.getIs_annule())){
	                       if(!BigDecimalUtil.isZero(cmd.getMtt_total())){
	                    	   applyOffreReduc(cmd, isAnnuleOffre, mttReduce, tauxReduce);
	                       }
	                       break;
	                   }
	               }
	           }
	       } 
	       // Si suppresion groupe ou article hors menu ------------
	       else{
	           if(typeLigne.equals(TYPE_LIGNE_COMMANDE.GROUPE_FAMILLE.toString())){
	               for (CaisseMouvementArticlePersistant cmd : listDetail) {
	                   if(cmd.getIdx_client().toString().equals(clientIdx) && cmd.getMenu_idx() == null && !BooleanUtil.isTrue(cmd.getIs_annule())){
	                       if(BigDecimalUtil.isZero(cmd.getMtt_total())){
	                           continue;
	                       }
	                       if(cmd.getType_ligne().equals(TYPE_LIGNE_COMMANDE.ART.toString()) 
	                               && cmd.getParent_code().equals(code)){
	                    	   applyOffreReduc(cmd, isAnnuleOffre, mttReduce, tauxReduce);
	                       }
	                       if(cmd.getType_ligne().equals(typeLigne) && cmd.getElementId().equals(elementId)){
	                    	   applyOffreReduc(cmd, isAnnuleOffre, mttReduce, tauxReduce);
	                       }
	                   }
	               }
	           } else if(typeLigne.equals(TYPE_LIGNE_COMMANDE.ART.toString())){
	               for (CaisseMouvementArticlePersistant cmd : listDetail) {
	                   if(cmd.getIdx_client().toString().equals(clientIdx) && cmd.getMenu_idx() == null && cmd.getType_ligne().equals(TYPE_LIGNE_COMMANDE.ART.toString()) 
	                           && cmd.getParent_code().equals(parentCode) 
	                           && cmd.getElementId().equals(elementId) 
	                           && !BooleanUtil.isTrue(cmd.getIs_annule())){
	                       if(!BigDecimalUtil.isZero(cmd.getMtt_total())){
	                    	   applyOffreReduc(cmd, isAnnuleOffre, mttReduce, tauxReduce);
	                       }
	                       break;
	                   }
	               }
	           }
	       }
	       
	       // Maj
	       sortAndAddCommandeLigne(httpUtil, currMvm);
	       //
	       sendDataToScreen(httpUtil, "O");
	       
	       httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
		}
			/**
		    * 
		    */
		   public void offrirLigneCommande(ActionUtil httpUtil){
			   
			   CaisseMouvementPersistant currMvm = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
		       List<CaisseMouvementArticlePersistant> listDetail = currMvm.getList_article();
		       boolean isAnnuleOffre = StringUtil.isTrue(httpUtil.getParameter("isaf"));
		       
		       // Marquer comme annulé dans le mouvement ----------------------------
		       String code = httpUtil.getParameter("cd");
		       String typeLigne = httpUtil.getParameter("tp");
		       Long elementId = httpUtil.getLongParameter("elm");
		       String parentCode = httpUtil.getParameter("par");
		       String menuIdx = httpUtil.getParameter("mnu");
		       String clientIdx = httpUtil.getParameter("cli");
		       
		       // Si suppresion offre ------------
		       if(typeLigne.equals(TYPE_LIGNE_COMMANDE.OFFRE.toString())){
		          MessageService.addGrowlMessage("Ligne contenant offre", "On ne peut pas réduire une ligne contenant une autre réduction.");
		          return;
		       } 
		       // Si suppresion menu ------------
		       else if(typeLigne.equals(TYPE_LIGNE_COMMANDE.MENU.toString())){
		           for (CaisseMouvementArticlePersistant cmd : listDetail) {
		               if(cmd.getIdx_client().toString().equals(clientIdx) && cmd.getMenu_idx() != null && cmd.getMenu_idx().equals(menuIdx) && !BooleanUtil.isTrue(cmd.getIs_annule())){
		                   if(!BigDecimalUtil.isZero(cmd.getMtt_total())){
		                       cmd.setIs_offert(isAnnuleOffre?false:true);
		                   }
		               }
		           }
		       } 
		       // Si suppresion groupe ou article dans menu ------------
		       else if(menuIdx != null){
		           if(typeLigne.equals(TYPE_LIGNE_COMMANDE.GROUPE_FAMILLE.toString()) || typeLigne.equals(TYPE_LIGNE_COMMANDE.GROUPE_MENU.toString())){
		               for (CaisseMouvementArticlePersistant cmd : listDetail) {
		                   if(cmd.getIdx_client().toString().equals(clientIdx) && cmd.getMenu_idx() != null && !BooleanUtil.isTrue(cmd.getIs_annule())){
		                       if(BigDecimalUtil.isZero(cmd.getMtt_total())){
		                          continue; 
		                       }
		                       if(cmd.getType_ligne().equals(TYPE_LIGNE_COMMANDE.ART_MENU.toString()) && cmd.getMenu_idx().equals(menuIdx)
		                           && cmd.getParent_code().equals(code)){
		                           cmd.setIs_offert(isAnnuleOffre?false:true);
		                       }
		                       if(cmd.getType_ligne().equals(typeLigne) && cmd.getElementId().equals(elementId)){
		                           cmd.setIs_offert(true);
		                       }
		                   }
		               }
		           } else if(typeLigne.equals(TYPE_LIGNE_COMMANDE.ART_MENU.toString())){
		               for (CaisseMouvementArticlePersistant cmd : listDetail) {
		                   if(cmd.getIdx_client().toString().equals(clientIdx) && cmd.getMenu_idx() != null && cmd.getType_ligne().equals(TYPE_LIGNE_COMMANDE.ART_MENU.toString()) 
		                           && cmd.getParent_code().equals(parentCode) 
		                           && cmd.getElementId().equals(elementId) 
		                           && !BooleanUtil.isTrue(cmd.getIs_annule())){
		                       if(!BigDecimalUtil.isZero(cmd.getMtt_total())){
		                           cmd.setIs_offert(isAnnuleOffre?false:true);
		                       }
		                       break;
		                   }
		               }
		           }
		       } 
		       // Si suppresion groupe ou article hors menu ------------
		       else{
		           if(typeLigne.equals(TYPE_LIGNE_COMMANDE.GROUPE_FAMILLE.toString())){
		               for (CaisseMouvementArticlePersistant cmd : listDetail) {
		                   if(cmd.getIdx_client().toString().equals(clientIdx) && cmd.getMenu_idx() == null && !BooleanUtil.isTrue(cmd.getIs_annule())){
		                       if(BigDecimalUtil.isZero(cmd.getMtt_total())){
		                           continue;
		                       }
		                       if(cmd.getType_ligne().equals(TYPE_LIGNE_COMMANDE.ART.toString()) 
		                               && cmd.getParent_code().equals(code)){
		                           cmd.setIs_offert(isAnnuleOffre?false:true);
		                       }
		                       if(cmd.getType_ligne().equals(typeLigne) && cmd.getElementId().equals(elementId)){
		                           cmd.setIs_offert(isAnnuleOffre?false:true);
		                       }
		                   }
		               }
		           } else if(typeLigne.equals(TYPE_LIGNE_COMMANDE.ART.toString())){
		               for (CaisseMouvementArticlePersistant cmd : listDetail) {
		                   if(cmd.getIdx_client().toString().equals(clientIdx) && cmd.getMenu_idx() == null && cmd.getType_ligne().equals(TYPE_LIGNE_COMMANDE.ART.toString()) 
		                           && cmd.getParent_code().equals(parentCode) 
		                           && cmd.getElementId().equals(elementId) 
		                           && !BooleanUtil.isTrue(cmd.getIs_annule())){
		                       if(!BigDecimalUtil.isZero(cmd.getMtt_total())){
		                           cmd.setIs_offert(isAnnuleOffre?false:true);
		                       }
		                       break;
		                   }
		               }
		           }
		       }
		       
		       // Maj
		       sortAndAddCommandeLigne(httpUtil, currMvm);
		       
		       //
		       sendDataToScreen(httpUtil, "O");
		       
		       httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
		   }
		
		/**
		 * @param menuIdx
		 * @param code
		 * @param listCmdBean
		 * @return
		 */
		public MenuCmdBean getMenuCmdBean(Integer clientIdx, String menuIdx, String code, List<MenuCmdBean> listCmdBean) {
			for (MenuCmdBean cmd : listCmdBean) {
				if (cmd.getClientIdx().equals(clientIdx) 
						&& (cmd.getMenuIdx() == null || cmd.getMenuIdx().equals(menuIdx)) 
						&& cmd.getGroupId().equals(code)) {
					return cmd;
				}
				if(cmd.getListGroupe() != null) {
					MenuCmdBean cm = getMenuCmdBean(clientIdx, menuIdx, code, cmd.getListGroupe());
					if(cm != null) {
						return cm;
					}
				}
			}
			return null;
		}
		
		public void init_conf_imprimantes(ActionUtil httpUtil) {		
			CaisseBean caisseB = (CaisseBean) ServiceUtil.persistantToBean(CaisseBean.class, ContextAppliCaisse.getCaisseBean());
			if(StringUtil.isNotEmpty(caisseB.getImprimantes())) {
	        	String[] imprArray = StringUtil.getArrayFromStringDelim(caisseB.getImprimantes(), "|");
	        	caisseB.setImprimante_array(imprArray);
	        	httpUtil.setRequestAttribute("imprArray", imprArray);
	    	}
			httpUtil.setViewBean(caisseB);

			// Liste des imprimantes
	        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
	        if(printServices != null) {
		        String[][] listImprimantes = new String[printServices.length][2];
		    	//
		        int idx = 0;
	            for (PrintService printer : printServices){
	            	listImprimantes[idx][0] = printer.getName();
	            	listImprimantes[idx][1] = printer.getName();
	            	idx++;
	        	}
	            httpUtil.setRequestAttribute("list_imprimante", listImprimantes);
	        }
	        
			httpUtil.setDynamicUrl("/domaine/caisse/print/print_conf.jsp");
		}
		public void merge_conf_imprimantes(ActionUtil httpUtil) {
			CaisseBean caisseB = caisseService.findById(ContextAppliCaisse.getCaisseBean().getId());
			
			String[] imprimantes = httpUtil.getRequest().getParameterValues("caisse.imprimante_array");
			
			String imprimante = StringUtil.getStringDelimFromStringArray(imprimantes, "|"); 
			
			caisseB.setImprimantes(imprimante);
			caisseService.merge(caisseB);
			
			 MessageService.getGlobalMap().put("CURRENT_CAISSE", caisseB);
			 httpUtil.writeResponse("MSG_CUSTOM:Imprimantes mise à jour.");
		}
		
		 /**
	    * @param httpUtil
	    */
	   public void changeQuantite(ActionUtil httpUtil){
		   String quantite = httpUtil.getParameter("quantite_custom");
		   BigDecimal ihmQuantite = BigDecimalUtil.get(quantite);
		   CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
		   boolean isConfirmReduceQte = StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("SECURE_DECREASE_QTE"));
		   
		   if(CURRENT_COMMANDE == null){
			   MessageService.addGrowlMessage("Aucune commande", "Aucune commande trouvée.");
			   return;
		   }
		   
		   if(CURRENT_COMMANDE.getId() != null && CURRENT_COMMANDE.getLast_statut().equals(STATUT_CAISSE_MOUVEMENT_ENUM.LIVRE.toString())) {
			    MessageService.addGrowlMessage("Mise à jour impossible", "Cette commande est marquée comme livrée. <br/>Elle ne peut plus être modifiée.");
				return;
		   }
		   
		   CaisseMouvementArticlePersistant currentLigne = getSelectedCommandeLigne(httpUtil);
		   //
		   if(httpUtil.getParameter("quantite_custom") == null){
			   BigDecimal oriQte = (currentLigne != null ? currentLigne.getQuantite() : BigDecimalUtil.get(1));
			   if(currentLigne != null && currentLigne.getId() != null) {
				   CaisseMouvementArticlePersistant currentLigneDb = caisseMvmtService.findById(CaisseMouvementArticlePersistant.class, currentLigne.getId());
				   oriQte = currentLigneDb.getQuantite();
			   } else {
				   oriQte = BigDecimalUtil.get(1);
			   }
			   httpUtil.setRequestAttribute("quantite_custom", (currentLigne != null ? currentLigne.getQuantite() : BigDecimalUtil.get(1)));
			   httpUtil.setRequestAttribute("quantite_ori", oriQte);
			   
			   String params = "cd="+httpUtil.getParameter("cd")
			   	 	+ "&tp="+httpUtil.getParameter("tp")
			   	 	+"&elm="+httpUtil.getLongParameter("elm")
			   	 	+"&par="+httpUtil.getParameter("par")
			   	 	+"&mnu="+httpUtil.getParameter("mnu")
			   	 	+"&cli="+httpUtil.getParameter("cli");
			   httpUtil.setRequestAttribute("params", params); 
			   
			   if(isConfirmReduceQte) {
				   List<UserPersistant> finalListUsers = new ArrayList<>();
				   List<UserPersistant> listUsers = userService.findAllUser(true);
				   for (UserPersistant userPersistant : listUsers) {
						if(userPersistant.isInProfile("MANAGER")
								|| userPersistant.isInProfile("ADMIN")) {
						finalListUsers.add(userPersistant);
						}
					}
				   httpUtil.setRequestAttribute("listUser", finalListUsers);
			   }
			   httpUtil.setDynamicUrl("/domaine/caisse/"+httpUtil.getUserAttribute("PATH_JSP_CM")+"/change-quantite.jsp");   
		   } else{
			   
			   if(BigDecimalUtil.isZero(ihmQuantite)) {
				    MessageService.addBannerMessage("La quantité zéro n'est pas autorisée.");
					return;
			   }
			   
			   if(StringUtil.isNotEmpty(httpUtil.getParameter("mnu"))){
				   MessageService.addBannerMessage("Uniquement la quantité des articles peut être ajustée.");
				   return;
			   }
			   
			   // --------------------------- AUTH -------------------------------------
			   BigDecimal quantiteDb = BigDecimalUtil.get(httpUtil.getParameter("orqt"));
			   if(isConfirmReduceQte && quantiteDb.compareTo(ihmQuantite) > 0 && CURRENT_COMMANDE.getId() != null) {
				   String badge = (String)ControllerUtil.getParam(httpUtil.getRequest(), "qte.tkn");
				   
				   boolean isBadge = StringUtil.isNotEmpty(badge);
					UserBean userBean = null;
					//
					if(isBadge){
						userBean = userService.getUserByBadge(badge.trim());
						//
						if(userBean == null){
							MessageService.addBannerMessage("Ce badge n'a pas été encore enregistré.");
							return;
						}
					} else {
						if(StringUtil.isEmpty(httpUtil.getParameter("unlockQte.password"))) {
							MessageService.addFieldMessage("unlockQte.password", "Le mot depasse est obligatoire.");
							return;
						}
						if(StringUtil.isEmpty(httpUtil.getParameter("unlockQte.login"))) {
							MessageService.addFieldMessage("unlockQte.login", "Le login est obligatoire.");
							return;
						}
						
						Long userId = Long.valueOf(httpUtil.getParameter("unlockQte.login"));
						String pw = new EncryptionEtsUtil(EncryptionEtsUtil.getDecrypKey()).encrypt(httpUtil.getParameter("unlockQte.password"));
						userBean = userService.findById(userId);
						
						if(!pw.equals(userBean.getPassword())) {
							MessageService.addBannerMessage("Le mot de passe est erroné.");
							return;
						}
					}
						
					// Si le compte est désactivé
					if(BooleanUtil.isTrue(userBean.getIs_desactive())){
						MessageService.addBannerMessage("Ce compte utilisateur est désactivé.");
						return;
					}
			   }
			   // ----------------------------------------------------------------------
			   
			   ArticlePersistant opc_article = currentLigne.getOpc_article();
			    // Prendre en compte les prix spécifique à un client
				ArticleClientPrixPersistant ccP = null;
				if(CURRENT_COMMANDE.getOpc_client() != null && currentLigne.getOpc_article() != null){
					ccP = caisseWebService.getArticleClientPrix(CURRENT_COMMANDE.getOpc_client().getId(), currentLigne.getOpc_article().getId());
				}
				if(ccP != null){
					currentLigne.setIs_client_pr(true);
				}
				
				if(TYPE_LIGNE_COMMANDE.GARANTIE.toString().equals(currentLigne.getType_ligne())) {
					String prixUnite = currentLigne.getLibelle().substring(currentLigne.getLibelle().indexOf("[")+1, currentLigne.getLibelle().indexOf("]"));
					currentLigne.setMtt_total(BigDecimalUtil.multiply(BigDecimalUtil.get(prixUnite), BigDecimalUtil.get(quantite)));
					currentLigne.setQuantite(BigDecimalUtil.get(quantite));
				} else {
					BigDecimal prixVenteC = (ccP != null ? ccP.getMtt_prix() : opc_article.getPrix_vente());
					
					// ---------------------------------------------
					if(opc_article != null && StringUtil.isNotEmpty(opc_article.getCode_barre())){
						   String codeBarre = opc_article.getCode_barre();
						   Map<String, String> codesBarStart = (Map<String, String>) MessageService.getGlobalMap().get("LIST_CODE_BALANCE");
						   boolean isArtBalance = (codesBarStart != null && codeBarre != null && codesBarStart.get(codeBarre.substring(0, 2)) != null);
						   
						   if(isArtBalance){
								BigDecimal poids = BigDecimalUtil.get(quantite);
								ValTypeEnumPersistant uniteVenteP = opc_article.getOpc_unite_vente_enum();
								String uniteVente = "KG";// Par defaut
								
								if(uniteVenteP != null){
									uniteVente = opc_article.getOpc_unite_vente_enum().getCode();
								}
								BigDecimal prixVente = BigDecimalUtil.ZERO;
								//
								if(uniteVente.equalsIgnoreCase("KG") || uniteVente.equalsIgnoreCase("L")){
									BigDecimal poidsKg = BigDecimalUtil.divide(poids, BigDecimalUtil.get(1000));
									prixVente = BigDecimalUtil.multiply(prixVenteC, poidsKg);
								} else if(uniteVente.equalsIgnoreCase("G") || uniteVente.equalsIgnoreCase("ML")){
									prixVente = BigDecimalUtil.multiply(prixVenteC, poids);
								} else{// Piece, boite, ....
									prixVente = BigDecimalUtil.multiply(prixVenteC, poids);
								}
									
								opc_article.setPrix_vente_tmp(prixVente);
								
								BigDecimal ration = poids;
								if(uniteVente.equalsIgnoreCase("KG") || uniteVente.equalsIgnoreCase("L")){ 
									ration = BigDecimalUtil.divide(ration, BigDecimalUtil.get(1000));
								}
								opc_article.setLibelle_compl(" ["+BigDecimalUtil.formatNumber(ration)+ "" + uniteVente.toUpperCase()+"]");
								
								currentLigne.setLibelle(opc_article.getLibelle()+ opc_article.getLibelle_compl());
								currentLigne.setMtt_total(prixVente);
								currentLigne.setQuantite(BigDecimalUtil.get(1));
						   }
					   }// else {
						   currentLigne.setMtt_total(BigDecimalUtil.multiply(prixVenteC, ihmQuantite));
						   currentLigne.setQuantite(ihmQuantite);
					  // }
					
					boolean isRestau = SOFT_ENVS.restau.toString().equals(StrimUtil.getGlobalConfigPropertie("context.soft"));
					if(isRestau){
						// Maj statut pour les écrans cuisine
						if(currentLigne.getQuantite().compareTo(ihmQuantite) != 0){
							boolean isAutoCmdPrep = StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("ECRAN_CMD_AUTO"));// Si passage à prête directement
							String last_statut = (isAutoCmdPrep ? ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.PREP.toString() : ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.VALIDE.toString());
							currentLigne.setLast_statut(last_statut);
						}
					}
					
					currentLigne.setType_opr(Integer.valueOf(2));
				}
				// Maj total de la commande
				majTotalMontantCommande(httpUtil, CURRENT_COMMANDE);
		
				// Vérifier si on doit activer ou désactiver une offre ----------------------
				manageOffreTarif(httpUtil);
				manageModeLivraison(httpUtil, false);
					
			   httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
		   }
	   }
		public ArticlePersistant getArticleGeneric(ActionUtil httpUtil) {
			String typeCharge = httpUtil.getParameter("chargeDivers.type");
			boolean isRestau = SOFT_ENVS.restau.toString().equals(StrimUtil.getGlobalConfigPropertie("context.soft"));
			
			Map params = (Map)httpUtil.getRequest().getAttribute(ProjectConstante.WORK_PARAMS);
			params.put("chargeDivers.montant", BigDecimalUtil.get(httpUtil.getRequest().getParameter("chargeDivers.montant")));
			
			ChargeDiversBean chargeB = ControllerBeanUtil.mapToBean(ChargeDiversBean.class, params);
			
			FamillePersistant genFam = articleService.getGenericFamille();
			if(genFam == null){
				articleService.addGenericFamille();
				genFam = articleService.getGenericFamille();
			}
			ArticlePersistant articleP = articleService.getGenericArticle(genFam);
			
			BigDecimal prix = chargeB.getMontant();
			if("A".equals(typeCharge)){
				prix = BigDecimalUtil.negate(prix);
			}
			// Libellé prédéfini
			if(StringUtil.isNotEmpty(httpUtil.getParameter("chargeDivers.libelle2"))){
				chargeB.setLibelle(httpUtil.getParameter("chargeDivers.libelle2"));
			}
			
			articleP = (ArticlePersistant) ReflectUtil.cloneBean(articleP);
			articleP.setCode(articleP.getCode()+"_"+new Random(100).nextInt());
			articleP.setLibelle(chargeB.getLibelle());
			articleP.setPrix_vente(prix);
			articleP.setPrix_vente_ht(prix);
			articleP.setPrix_vente_tmp(prix);
			
			List<String> listNav = (List<String>) httpUtil.getUserAttribute("HISTORIQUE_NAV");
			if(listNav == null && httpUtil.getUserAttribute("CURRENT_COMMANDE") == null){
				initNewCommande(httpUtil);
			} else{
				listNav = new ArrayList<>();
				httpUtil.setUserAttribute("HISTORIQUE_NAV", listNav);
			}
			
			listNav.clear();
			listNav.add("FAM_" + (isRestau ? articleP.getOpc_famille_cuisine().getId() : articleP.getOpc_famille_stock().getId()));
			
			return articleP;
		}
		
		/**
		 * @param httpUtil
		 */
		public void manageDevise(ActionUtil httpUtil){
			boolean isAllCmd = (httpUtil.getParameter("isTop") != null);
			CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
			
			if(isAllCmd) {
				if(BooleanUtil.isTrue(CURRENT_COMMANDE.getIs_devise())) {
					CURRENT_COMMANDE.setIs_devise(null);
					for(CaisseMouvementArticlePersistant mvmDet : CURRENT_COMMANDE.getList_article()) {
						mvmDet.setIs_devise(null);
					}
				} else {
					CURRENT_COMMANDE.setIs_devise(true);
				}
			} else {
				CaisseMouvementArticlePersistant ligne = getSelectedCommandeLigne(httpUtil);
				if(BooleanUtil.isTrue(ligne.getIs_devise())) {
					ligne.setIs_devise(false);					
				} else {
					ligne.setIs_devise(true);
				}
			}
			httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
		}
		
		public void init_situation(ActionUtil httpUtil){
			boolean isSubmit = httpUtil.getParameter("isSub") != null;
			if(httpUtil.getParameter("curMnu") == null && !isSubmit){
				httpUtil.setDynamicUrl("/domaine/caisse/normal/situation/situation_init.jsp");
				return;
			}
			
			Long elementId = httpUtil.getLongParameter("elmentId");
			if(httpUtil.getMenuAttribute("clientId") != null){
				elementId = (Long)httpUtil.getMenuAttribute("clientId");
			}
			
			if(isSubmit && elementId == null){
				MessageService.addBannerMessage("Veuillez sélectionner un élément.");
				return;
			}
			
			//			
			Date dateDebut = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateDebut"));
			Date dateFin = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateFin"));
			
			if(dateDebut == null) {
				JourneePersistant lastJrn = journeeService.getLastJournee();
				dateDebut = (lastJrn == null ? new Date() : lastJrn.getDate_journee());
				
				Calendar cal = DateUtil.getCalendar(dateDebut);
				String dateString = (cal.get(Calendar.MONTH)+1)+"/"+cal.get(Calendar.YEAR);
				dateDebut = DateUtil.stringToDate("01/"+dateString);
				dateFin = DateUtil.stringToDate(DateUtil.getMaxMonthDate(dateDebut)+"/"+dateString);
			}
			// Postionner l'heure
			dateDebut = DateUtil.getStartOfDay(dateDebut);
			dateFin = DateUtil.getEndOfDay(dateFin);
			
			httpUtil.setRequestAttribute("dateDebut", dateDebut);
			httpUtil.setRequestAttribute("dateFin", dateFin);
			
			httpUtil.setFormReadOnly(false);
			
			httpUtil.setMenuAttribute("elementId", elementId);
			httpUtil.setMenuAttribute("dateDebut", dateDebut);
			httpUtil.setMenuAttribute("dateFin", dateFin);
			
			String mnu = httpUtil.getParameter("curMnu");
			if(mnu != null){
				httpUtil.setMenuAttribute("curMnu", mnu);
			}
			//httpUtil.setRequestAttribute("listLivreur",  employeService.getListEmployeActifs("LIVREUR"));
			httpUtil.setRequestAttribute("listLivreur",  userService.getListUserActifsByProfile("LIVREUR"));
			httpUtil.setRequestAttribute("listSociete", societeLivrsService.getSocieteLivrsActifs());
			httpUtil.setRequestAttribute("listClient", clientService.getClientsActifs());
			
			if(elementId == null || !isSubmit){
				httpUtil.setDynamicUrl("/domaine/caisse/normal/situation/situation.jsp");
				return;
			}
			mnu = (String) httpUtil.getMenuAttribute("curMnu");
			Map mapData = null;
			//
			if(mnu == null || mnu.equals("cli")){
				mapData = ticketCaisseService.getSituationClient(null, elementId, dateDebut, dateFin);	
			} else if(mnu.equals("livr")){
				mapData = ticketCaisseService.getSituationLivreur(null, elementId, dateDebut, dateFin);	
			} else if(mnu.equals("socLivr")){
				mapData = ticketCaisseService.getSituationSocieteLivr(null, elementId, dateDebut, dateFin);	
			}
			httpUtil.setRequestAttribute("mapData", mapData);
			init_situation_mvm(httpUtil);
			
			httpUtil.setDynamicUrl("/domaine/caisse/normal/situation/situation_detail.jsp"); 
		}
		
		public void init_situation_mvm(ActionUtil httpUtil){
			RequestTableBean cplxTable = getTableBean(httpUtil, "list_situation");
			String mnu = (String) httpUtil.getMenuAttribute("curMnu");
			Map mapData = null;
			Long clientId = (Long) httpUtil.getMenuAttribute("elementId");
			Date dateDebut = (Date) httpUtil.getMenuAttribute("dateDebut");
			Date dateFin = (Date) httpUtil.getMenuAttribute("dateFin");
			//
			ITicketCaisseService ticketCaisseSrv = ServiceUtil.getBusinessBean(ITicketCaisseService.class);
			
			if(mnu == null || mnu.equals("cli")){
				mapData = ticketCaisseSrv.getSituationClient(cplxTable, clientId, dateDebut, dateFin);	
			} else if(mnu.equals("livr")){
				mapData = ticketCaisseSrv.getSituationLivreur(cplxTable, clientId, dateDebut, dateFin);	
			} else if(mnu.equals("socLivr")){
				mapData = ticketCaisseSrv.getSituationSocieteLivr(cplxTable, clientId, dateDebut, dateFin);	
			}
			
			if(clientId != null  && (mnu == null || mnu.equals("cli"))) {
				clientService = (clientService == null ? ServiceUtil.getBusinessBean(IClientService.class) : clientService);
				httpUtil.setRequestAttribute("cliSituation", clientService.findById(clientId));
			}
			
			httpUtil.setRequestAttribute("MapRazCli", mapData);// Pour impression raz solde
			httpUtil.setRequestAttribute("listMouvement", mapData.get("data"));
			httpUtil.setDynamicUrl("/domaine/caisse/normal/situation/situation_mvm.jsp");
		}
		
		/**
		 * @param httpUtil
		 */
		public void initPaiement(ActionUtil httpUtil) {
			httpUtil.removeUserAttribute("listEncaissePartiel");
			
			MessageService.getGlobalMap().put("NO_ETS", true);
			httpUtil.setRequestAttribute("listVille", caisseService.getListData(VillePersistant.class, "opc_region.libelle, libelle"));
			MessageService.getGlobalMap().remove("NO_ETS");
			
			// Securiser  en mettant à jour le total -----------------------------
			CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
			majTotalMontantCommande(httpUtil, CURRENT_COMMANDE);
			// --------------------------------------------------------------------
					
			boolean isPartielTab = StringUtil.isTrue(httpUtil.getParameter("istab"));
			boolean isPartielCli = StringUtil.isTrue(httpUtil.getParameter("iscli"));
			boolean isPartielArt = StringUtil.isTrue(httpUtil.getParameter("isart"));
			
			List<CaisseMouvementArticlePersistant> listEncaisse = new ArrayList<>();
			// Gestion du paiement partiel
			String[] dataPaieCliPartiel = httpUtil.getRequest().getParameterValues("checkcli_");
			String[] dataPaieTabPartiel = httpUtil.getRequest().getParameterValues("checktab_");
			List<CaisseMouvementArticlePersistant> listArt = CURRENT_COMMANDE.getList_article();
			BigDecimal mttTotal = null;
			//
			if(isPartielArt){
				CaisseMouvementArticlePersistant cmvP =  getSelectedCommandeLigne(httpUtil);
				if(cmvP != null){ 
					mttTotal = BigDecimalUtil.add(mttTotal, cmvP.getMtt_total());
					listEncaisse.add(cmvP);
				}
			} else if(isPartielCli){
				String idxCli = httpUtil.getParameter("idx_cli");
				for (CaisseMouvementArticlePersistant cmd : listArt) {
	               if(cmd.getIdx_client() != null && cmd.getIdx_client().toString().equals(idxCli) 
	            		   && !BooleanUtil.isTrue(cmd.getIs_annule())
	            		   && !BooleanUtil.isTrue(cmd.getIs_offert())
	            		   && !BooleanUtil.isTrue(cmd.getIs_encaisse())){
	            	   mttTotal = BigDecimalUtil.add(mttTotal, cmd.getMtt_total());
	            	   listEncaisse.add(cmd);
	               }
	            }
				mttTotal = (mttTotal == null) ? BigDecimalUtil.ZERO : mttTotal;
			} else {
				if(isPartielTab){
					String refTable = httpUtil.getParameter("ref_tab");
					for (CaisseMouvementArticlePersistant cmd : listArt) {
		               if(cmd.getRef_table() != null && cmd.getRef_table().equals(refTable) 
		            		   && !BooleanUtil.isTrue(cmd.getIs_annule())
		            		   && !BooleanUtil.isTrue(cmd.getIs_offert())
		            		   && !BooleanUtil.isTrue(cmd.getIs_encaisse())){
		            	   mttTotal = BigDecimalUtil.add(mttTotal, cmd.getMtt_total());
		            	   listEncaisse.add(cmd);
		               }
		            }
					mttTotal = (mttTotal == null) ? BigDecimalUtil.ZERO : mttTotal;
				} else {
					boolean isCli = dataPaieCliPartiel != null && dataPaieCliPartiel.length > 0;
					boolean isTab = dataPaieTabPartiel != null && dataPaieTabPartiel.length > 0;
							
					//
					if(isCli || isTab){
						if(isCli){
							for(String clientIdx : dataPaieCliPartiel){
					    		for (CaisseMouvementArticlePersistant cmd : listArt) {
					               if(cmd.getIdx_client() != null && cmd.getIdx_client().toString().equals(clientIdx) 
					            		   && !BooleanUtil.isTrue(cmd.getIs_annule())
					            		   && !BooleanUtil.isTrue(cmd.getIs_offert())
					            		   && !BooleanUtil.isTrue(cmd.getIs_encaisse())){
					            	   mttTotal = BigDecimalUtil.add(mttTotal, cmd.getMtt_total());
					            	   listEncaisse.add(cmd);
					               }
					            }
							}
							mttTotal = (mttTotal == null) ? BigDecimalUtil.ZERO : mttTotal;
						} else{
							for(String tableIdx : dataPaieTabPartiel){
					    		for (CaisseMouvementArticlePersistant cmd : listArt) {
					               if(cmd.getRef_table() != null && cmd.getRef_table().equals(tableIdx) 
					            		   && !BooleanUtil.isTrue(cmd.getIs_annule())
					            		   && !BooleanUtil.isTrue(cmd.getIs_offert())
					            		   && !BooleanUtil.isTrue(cmd.getIs_encaisse())){
					            	   mttTotal = BigDecimalUtil.add(mttTotal, cmd.getMtt_total());
					            	   listEncaisse.add(cmd);
					               }
					            }
							}
							mttTotal = (mttTotal == null) ? BigDecimalUtil.ZERO : mttTotal;
						}
					}
				}
			}
			
			if(mttTotal != null){
				httpUtil.setRequestAttribute("mtt_total", mttTotal);
			} else{
				// Retirer ce qui est déjà payé
				BigDecimal mttEncaisse = null;
				for (CaisseMouvementArticlePersistant det : CURRENT_COMMANDE.getList_article()) {
					if(BooleanUtil.isTrue(det.getIs_encaisse())){
						mttEncaisse = BigDecimalUtil.add(mttEncaisse, det.getMtt_total());
					}
				}
				if(mttEncaisse != null){
					httpUtil.setRequestAttribute("mtt_deja_encaisse", BigDecimalUtil.formatNumber(mttEncaisse));
				}
				
				httpUtil.setRequestAttribute("mtt_total", BigDecimalUtil.substract(CURRENT_COMMANDE.getMtt_commande_net(), mttEncaisse));
			}
			
			CaisseJourneePersistant journeeCaisseOuverte = ContextAppliCaisse.getJourneeCaisseBean();
		        
			if(journeeCaisseOuverte == null) {
				MessageService.addGrowlMessage("Journée caisse", "Aucune journée caisse ouverte n'a été trouvée.");
				return;
			}
			
			if(listEncaisse.size() == 0){
				String token = this.caisseWebService.loadNextCustomCall(journeeCaisseOuverte.getOpc_journee().getId());
		        httpUtil.setRequestAttribute("num_token", token);
			} else{
				httpUtil.setUserAttribute("listEncaissePartiel", listEncaisse);
			}
			
	        // On affiche les points si le seuil d'utilisatino est atteint et que le solde est positif
	        if(CURRENT_COMMANDE.getOpc_client() != null){
				boolean isPortefeuille =  StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("PORTEFEUILLE"));
				boolean isPoints =  StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("POINTS"));
				CarteFideliteClientPersistant carteClientP = carteClientService.getCarteClientActive(CURRENT_COMMANDE.getOpc_client().getId());
	        	// Caret fidélité
	        	if(isPoints){
		        	if(carteClientP != null && !BigDecimalUtil.isZero(carteClientP.getMtt_total())){
		        		BigDecimal mttPointsUtilisable = carteClientP.getMtt_total();
			        	CarteFidelitePersistant opc_carte_fidelite = carteClientP.getOpc_carte_fidelite();
						BigDecimal mtt_seuil_util = opc_carte_fidelite.getMtt_seuil_util();
						mtt_seuil_util = (mtt_seuil_util == null ? BigDecimalUtil.ZERO : mtt_seuil_util);
			        	// Calcul du montant
						if(!BigDecimalUtil.isZero(mtt_seuil_util) || carteClientP.getMtt_total().compareTo(mtt_seuil_util)>=0){
							// Si on depasse le plafond alors on utilise le plafond
							BigDecimal mttPlafond = (opc_carte_fidelite.getMtt_plafond() == null ? BigDecimalUtil.ZERO : opc_carte_fidelite.getMtt_plafond());
							
							if(carteClientP.getMtt_total() != null 
									&& carteClientP.getMtt_total().compareTo(mttPlafond)>0){
								mttPointsUtilisable = mttPlafond;
							}
							// Si montant points dépasse commande alors on prend le montant de la commande
							if(mttPointsUtilisable.compareTo(CURRENT_COMMANDE.getMtt_commande_net())>0){
								mttPointsUtilisable = CURRENT_COMMANDE.getMtt_commande_net();
							}
							// On fractionne par bloc d'utilisation
							if(!BigDecimalUtil.isZero(opc_carte_fidelite.getMtt_bloc_util())){
								int nbrBloc = (mttPointsUtilisable.intValue()%opc_carte_fidelite.getMtt_bloc_util().intValue());
								mttPointsUtilisable = BigDecimalUtil.multiply(opc_carte_fidelite.getMtt_bloc_util(), BigDecimalUtil.get(nbrBloc));
							}
							httpUtil.setRequestAttribute("mttPointsUtilisable", mttPointsUtilisable);
			        	}
		        	}
	        	}
	        	// Portefeuille virtuel
				if(isPortefeuille){
		        	if(BooleanUtil.isTrue(CURRENT_COMMANDE.getOpc_client().getIs_portefeuille())){
		        		BigDecimal mttUtilisable = CURRENT_COMMANDE.getOpc_client().getSolde_portefeuille();
		        		if(!BigDecimalUtil.isZero(mttUtilisable)){
			        		// Si montant points dépasse commande alors on prend le montant de la commande
							if(mttUtilisable.compareTo(CURRENT_COMMANDE.getMtt_commande_net())>0){
								mttUtilisable = CURRENT_COMMANDE.getMtt_commande_net();
							}
		        		} else{
		        			mttUtilisable = BigDecimalUtil.ZERO;
		        		}
		        		
		        		if(BooleanUtil.isTrue(CURRENT_COMMANDE.getOpc_client().getIs_solde_neg())){
		        			httpUtil.setRequestAttribute("isSoldeNegatif", true);	
		        		}
		        		
		        		httpUtil.setRequestAttribute("mttPortefeuilleUtilisable", mttUtilisable);
		        	}
				}
				httpUtil.setRequestAttribute("carteClientP", carteClientP);
	        }
	        if(CURRENT_COMMANDE.getOpc_societe_livr() != null){
	        	if(BooleanUtil.isTrue(CURRENT_COMMANDE.getOpc_societe_livr().getIs_portefeuille())){
	        		BigDecimal mttUtilisable = CURRENT_COMMANDE.getOpc_societe_livr().getSolde_portefeuille();
	        		if(!BigDecimalUtil.isZero(mttUtilisable)){
		        		// Si montant points dépasse commande alors on prend le montant de la commande
						if(mttUtilisable.compareTo(CURRENT_COMMANDE.getMtt_commande_net())>0){
							mttUtilisable = CURRENT_COMMANDE.getMtt_commande_net();
						}
	        		} else{
	        			mttUtilisable = BigDecimalUtil.ZERO;
	        		}
	        		
	        		if(BooleanUtil.isTrue(CURRENT_COMMANDE.getOpc_societe_livr().getIs_solde_neg())){
	        			httpUtil.setRequestAttribute("isSoldeNegatif", true);	
	        		}
	        		
	        		httpUtil.setRequestAttribute("mttPortefeuilleUtilisable", mttUtilisable);
	        	}
	        }
	        
	        BigDecimal mttTotalEncaiss = BigDecimalUtil.get(""+httpUtil.getRequestAttribute("mtt_total"));
			if(CURRENT_COMMANDE.getList_article().size()>0 && BigDecimalUtil.isZero(mttTotalEncaiss)){
	        	if((isPartielTab || isPartielArt || isPartielCli) && BigDecimalUtil.isZero(mttTotalEncaiss)){
	        		httpUtil.setRequestAttribute("is_elment_encaisse", true);
	        	}
	        	
	        	httpUtil.setDynamicUrl("/domaine/caisse/"+httpUtil.getUserAttribute("PATH_JSP_CM")+"/paiement-edit-partiel-full.jsp");
	        } else{
	        	httpUtil.setDynamicUrl(getPaiement_path(httpUtil));
	        }
		}
		
		public boolean checkTable(ActionUtil httpUtil) {
			CaisseMouvementPersistant currCmd = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
			// Interdir d'attribuer la  meme table à plusieurs commandes------------------------------
			boolean isReprise = "REP".equals(httpUtil.getUserAttribute("PLAN_MODE"));
			boolean isTransfert = "TRA".equals(httpUtil.getUserAttribute("PLAN_MODE"));
			//
			if(currCmd.getRef_commande() != null && !isReprise && !isTransfert){
				CaisseMouvementPersistant cmd = caisseWebService.getMouvementByTable(
									ContextAppliCaisse.getJourneeBean().getId(), 
									currCmd.getRefTablesDetail(), 
									STATUT_CAISSE_MOUVEMENT_ENUM.TEMP);
				if(cmd != null && !cmd.getRef_commande().equals(currCmd.getRef_commande())){
	                return false;
				}
			}//-----------
			return true;
		}
		
		/**
		 * @param httpUtil
		 */
		public void validerPaiement(ActionUtil httpUtil) {
			if(!checkJournee()){
				return;
			}
			boolean isRestau = SOFT_ENVS.restau.toString().equals(StrimUtil.getGlobalConfigPropertie("context.soft"));
			String typeCmd = httpUtil.getParameter("typeCmd");
			
			CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
			// Securiser  en mettant à jour le total -----------------------------
			majTotalMontantCommande(httpUtil, CURRENT_COMMANDE);
			// --------------------------------------------------------------------
			
			if(isRestau) {
				boolean isTableRequired =  StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("SELECT_TABLE_REQUIRED"));
				if(isTableRequired 
						&& ContextAppli.TYPE_COMMANDE.P.toString().equals(typeCmd)
						&& StringUtil.isEmpty(CURRENT_COMMANDE.getRefTablesDetail())){
					MessageService.addGrowlMessage("", "<h2>Veuillez sélectionner une table.</h2>");
					return;				
				}
			}
			
			List<CaisseMouvementArticlePersistant> listEncaissePartiel = (List<CaisseMouvementArticlePersistant>)httpUtil.getUserAttribute("listEncaissePartiel");
			
			boolean isFinalisationPaiement = StringUtil.isTrue(httpUtil.getParameter("isfin"));
			
			BigDecimal mtt_especes = BigDecimalUtil.get(httpUtil.getParameter("mtt_especes"));
			BigDecimal mtt_cheque = BigDecimalUtil.get(httpUtil.getParameter("mtt_cheque"));
			BigDecimal mtt_carte = BigDecimalUtil.get(httpUtil.getParameter("mtt_carte"));
			BigDecimal mtt_dej = BigDecimalUtil.get(httpUtil.getParameter("mtt_dej"));
			BigDecimal mtt_points = BigDecimalUtil.get(httpUtil.getParameter("mtt_points"));
			BigDecimal mtt_portefeuille = BigDecimalUtil.get(httpUtil.getParameter("mtt_portefeuille"));
			boolean isPoints =  StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("POINTS"));
			boolean isPortefeuille =  StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("PORTEFEUILLE"));
			
			String custom_call = httpUtil.getParameter("custom_call");
			BigDecimal mtt_rendu = BigDecimalUtil.get(httpUtil.getParameter("mtt_rendu"));
			BigDecimal mttTotalDonnee = BigDecimalUtil.add(mtt_especes, mtt_cheque, mtt_carte, mtt_dej, mtt_points, mtt_portefeuille);
			
			CURRENT_COMMANDE.setListEncaisse(listEncaissePartiel);
			CURRENT_COMMANDE.setCode_barre(caisseMvmtService.generateCodeBarre());
			
			// Commandes payes par petit bouts
			if(isFinalisationPaiement && StringUtil.isEmpty(CURRENT_COMMANDE.getMode_paiement())){
				CURRENT_COMMANDE.setMode_paiement("ESPECES");
			}
			
			boolean isPaiementPartiel = (listEncaissePartiel != null && listEncaissePartiel.size() > 0);
			
			// Ajouter la ville pour éviter l'exception lors de l'impression liée à la ville
			if(CURRENT_COMMANDE.getOpc_client() != null && CURRENT_COMMANDE.getOpc_client().getOpc_ville() != null){
				ClientPersistant opcClient = (ClientPersistant) familleService.findById(ClientPersistant.class, CURRENT_COMMANDE.getOpc_client().getId());
				VillePersistant opcVille = (VillePersistant) familleService.findById(VillePersistant.class, opcClient.getOpc_ville().getId());
				CURRENT_COMMANDE.getOpc_client().setVilleStr(StringUtil.getValueOrEmpty(opcVille.getCode_postal()) + " " + opcVille.getLibelle());
			} 
			
			UserBean userBean = ContextAppli.getUserBean();
			if(!isFinalisationPaiement || !isPaiementPartiel){
				CURRENT_COMMANDE.setMtt_donne_all(mttTotalDonnee);
				CURRENT_COMMANDE.setType_commande(typeCmd);
				
				if(CURRENT_COMMANDE.getOpc_user() == null) {
					CURRENT_COMMANDE.setOpc_user(userBean);
				}
				// Si caisse livraison alors on met le livreur tout de suite
				if(BooleanUtil.isTrue(ContextAppliCaisse.getCaisseBean().getIs_livraison())
						&& userBean != null
						&& CURRENT_COMMANDE.getOpc_livreurU() == null){
					CURRENT_COMMANDE.setOpc_livreurU(userBean);
				}
			}
			
			BigDecimal mttNetCmd = null;
	    	if(isPaiementPartiel){
				for (CaisseMouvementArticlePersistant cmd : listEncaissePartiel) {
					if(!BooleanUtil.isTrue(cmd.getIs_annule()) && !BooleanUtil.isTrue(cmd.getIs_offert())){
						mttNetCmd = BigDecimalUtil.add(mttNetCmd, cmd.getMtt_total());
					}
		    	}
	    	} else if(!isFinalisationPaiement){
	    		// Retirer ce qui est déjà payé
				BigDecimal mttEncaisse = null;
				for (CaisseMouvementArticlePersistant det : CURRENT_COMMANDE.getList_article()) {
					if(BooleanUtil.isTrue(det.getIs_encaisse()) && !BooleanUtil.isTrue(det.getIs_annule()) && !BooleanUtil.isTrue(det.getIs_offert())){
						mttEncaisse = BigDecimalUtil.add(mttEncaisse, det.getMtt_total());
					}
				}
	    		mttNetCmd = BigDecimalUtil.substract(CURRENT_COMMANDE.getMtt_commande_net(), mttEncaisse);
	    	}
	    	
		    //----------------------------------------------------------------------   
	        CarteFideliteClientPersistant carteClientP = null;
	        //
	        if(!isFinalisationPaiement && CURRENT_COMMANDE.getOpc_client() != null){
	        	// Points de fidélité
		        if(isPoints){
		        	carteClientP = carteClientService.getCarteClientActive(CURRENT_COMMANDE.getOpc_client().getId());
		        }
	        }
	        
			// Interdir d'attribuer la  meme table à plusieurs commandes------------------------------
	        if(isRestau) {
				if(!checkTable(httpUtil)) {
					MessageService.addGrowlMessage("", "<h4>Cette table est déjà utilisée pour la commande ** <b>"+CURRENT_COMMANDE.getRef_commande()+"</b> **</h4>");
					return;
				}
	        }
	        
			if (CURRENT_COMMANDE == null || CURRENT_COMMANDE.getList_article().size() == 0) {
				 MessageService.addBannerMessage("Cette commande ne contient pas d'articles.");
		         return;
		    }
			
			if(StringUtil.isEmpty(typeCmd) && StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("CAISS_TYPE_CMD"))){
				typeCmd = ContextAppli.TYPE_COMMANDE.E.toString();
			}
			
			if(StringUtil.isEmpty(typeCmd) && !StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("SHOW_MODE_CMD"))){
				typeCmd = ContextAppli.TYPE_COMMANDE.P.toString();
			}
			
			if(!isFinalisationPaiement && StringUtil.isEmpty(typeCmd)) {
				MessageService.addBannerMessage("Le type de commande est obligatoire");
				return;
			}
			
			if(!isFinalisationPaiement && mttTotalDonnee.compareTo(mttNetCmd)<0) {
				// Si portefeuille et autorisation négatif laisser passer
				boolean isSoldeNegatif = false;
				if(CURRENT_COMMANDE.getOpc_client() != null){
					isSoldeNegatif = isPortefeuille 
							&& BooleanUtil.isTrue(CURRENT_COMMANDE.getOpc_client().getIs_portefeuille())
							&& BooleanUtil.isTrue(CURRENT_COMMANDE.getOpc_client().getIs_solde_neg());					
				}
				if(CURRENT_COMMANDE.getOpc_societe_livr() != null){
					isSoldeNegatif = isPortefeuille 
							&& BooleanUtil.isTrue(CURRENT_COMMANDE.getOpc_societe_livr().getIs_portefeuille())
							&& BooleanUtil.isTrue(CURRENT_COMMANDE.getOpc_societe_livr().getIs_solde_neg());
				}
				if(!isSoldeNegatif){
					MessageService.addBannerMessage("Le montant du paiement est insuffisant. Il manque <b>"+BigDecimalUtil.formatNumber(mttTotalDonnee.subtract(mttNetCmd).negate())+"</b> Dhs");
					return;
				}
			}
			
			CaissePersistant caisseP = ContextAppliCaisse.getCaisseBean();
	        CaisseJourneePersistant journeeCaisseOuverte = caisseWebService.getJourneCaisseOuverte(caisseP.getId());
	        MessageService.getGlobalMap().put("CURRENT_JOURNEE_CAISSE", journeeCaisseOuverte);
		        
		    if (!journeeCaisseOuverte.getStatut_caisse().equals(ContextAppli.STATUT_JOURNEE.OUVERTE.getStatut().toString())) {
		    	MessageService.addBannerMessage("Aucune journée caisse ouverte n'a été trouvé.");
		        return;
		    }
		    // Client et livreur obligatoire si livraison
		    if(ContextAppli.TYPE_COMMANDE.L.toString().equals(typeCmd)){
		    	 boolean isLivreurOblige =  StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("LIVREUR_REQUIRED"));
		    	 boolean isClientOblige =  StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("CLIENT_REQUIRED"));
		    
		    	 if(isLivreurOblige && CURRENT_COMMANDE.getOpc_livreurU() == null){
		    		MessageService.addBannerMessage("Les commandes livraison doivent être liées à un <b>livreur</b>.");
		    		return;
		    	}
		    if(isClientOblige && CURRENT_COMMANDE.getOpc_client() == null){
					MessageService.addBannerMessage("Les commandes livraison doivent être liées à un <b>client</b>.");
					return;
		        }
		    }
		    
		    if((ReplicationGenerationEventListener._IS_CLOUD_SYNCHRO_INSTANCE || ContextAppli.IS_FULL_CLOUD()) 
					&& httpUtil.getUserAttribute("ENV_MOBILE") != null) {
		    //  Notifier le livreur
	    	    String typeNotif = TYPE_NOTIFICATION.ETS_CLIENT_CMD_VALIDE.toString();
				String message = "La commande est validé";
		    	//notifier
				Map<String, String> mapData = new HashMap<>();
				mapData.put("title", "Statut commande");
				mapData.put("message", message);
				mapData.put("type", typeNotif);
				mapData.put("livreur", CURRENT_COMMANDE.getOpc_livreurU().getId().toString());
				
				NotificationQueuService notifService = ServiceUtil.getBusinessBean(NotificationQueuService.class);
				notifService.addNotification(mapData, CURRENT_COMMANDE.getOpc_livreurU(), CURRENT_COMMANDE, null);
		    }
		    
		    boolean isCtrlStock =  StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("CTRL_STOCK_MVM_CAISSE"));
		    boolean isCtrlStockCaisse = StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("INVENTAIRE_CAISSE"));
		    
		    if(isCtrlStockCaisse || (isCtrlStock && BooleanUtil.isTrue(ContextGloabalAppli.getAbonementBean().isOptStock()))){
		    	if(!caisseWebService.isEtatStockArticlesValide(CURRENT_COMMANDE)){
		    		return;
		    	}
		    }
		    
		    if(!isPaiementPartiel && !isFinalisationPaiement){
			    // Contrôler si le Coaster call n'est pas pris
//		        if(StringUtil.isNotEmpty(custom_call)){
//		            if(this.caisseWebService.isCustomCallUtilise(journeeCaisseOuverte.getOpc_journee().getId(), custom_call)){
//		            	MessageService.addBannerMessage("Le Coaster call saisi est déjà utilisé.");
//		                return;
//		            }
//		        }
		        // Token
		        CURRENT_COMMANDE.setNum_token_cmd(custom_call);
		    }

		    if(!isFinalisationPaiement || !isPaiementPartiel){
		        CURRENT_COMMANDE.setMtt_donne_cheque(BigDecimalUtil.add(CURRENT_COMMANDE.getMtt_donne_cheque(), mtt_cheque));
		        CURRENT_COMMANDE.setMtt_donne_cb(BigDecimalUtil.add(CURRENT_COMMANDE.getMtt_donne_cb(), mtt_carte));
		        CURRENT_COMMANDE.setMtt_donne_dej(BigDecimalUtil.add(CURRENT_COMMANDE.getMtt_donne_dej(), mtt_dej));
		        CURRENT_COMMANDE.setMtt_donne_point(BigDecimalUtil.add(CURRENT_COMMANDE.getMtt_donne_point(), mtt_points));
		        CURRENT_COMMANDE.setMtt_portefeuille(BigDecimalUtil.add(CURRENT_COMMANDE.getMtt_portefeuille(), mtt_portefeuille));
		    
			    if(!isPaiementPartiel){
				    String mode_paiement = "";
				    
			    	// Ne mettre l'espéce que si il est réelement utilisé
			    	if(BigDecimalUtil.add(mtt_cheque, mtt_carte, mtt_dej, mtt_points, mtt_portefeuille)
			    			.compareTo(CURRENT_COMMANDE.getMtt_commande_net()) < 0) {
			    		 if(!BigDecimalUtil.isZero(mtt_especes)) {
					        	mode_paiement = mode_paiement+"ESPECES, ";
					     }
			    	} else {
			    		CURRENT_COMMANDE.setMtt_donne(null);
			    	}
			    	
			        if(!BigDecimalUtil.isZero(mtt_carte)) {
			        	mode_paiement = "CARTE, ";
			        }
			        if(!BigDecimalUtil.isZero(mtt_cheque)) {
			        	mode_paiement = mode_paiement+"CHEQUE, ";
			        }
			        if(!BigDecimalUtil.isZero(mtt_dej)) {
			        	mode_paiement = mode_paiement+"DEJ, ";
			        }
			        if(!BigDecimalUtil.isZero(mtt_points)) {
			        	mode_paiement = mode_paiement+"POINTS, ";
			        }
			        if(!BigDecimalUtil.isZero(mtt_portefeuille)) {
			        	mode_paiement = mode_paiement+"RESERVE, ";
			        }
					// Si offert alors mode espece par defaut
					if(StringUtil.isEmpty(mode_paiement)){
						if(BigDecimalUtil.ZERO.compareTo(CURRENT_COMMANDE.getMtt_commande_net()) > 0) {
							mode_paiement = "ESPECES, ";
						} else {
							boolean isCmdOfferte = (BigDecimalUtil.isZero(CURRENT_COMMANDE.getMtt_commande_net()) 
									&& !BigDecimalUtil.isZero(CURRENT_COMMANDE.getMtt_commande())
									&& (CURRENT_COMMANDE.getList_offre()!=null && CURRENT_COMMANDE.getList_offre().size()>0));
							if(isCmdOfferte){
								mode_paiement = mode_paiement+"ESPECES, ";
								CURRENT_COMMANDE.setType_commande(ContextAppli.TYPE_COMMANDE.P.toString());
							}
						}
					}
					
			        if(StringUtil.isNotEmpty(mode_paiement)) {
			        	mode_paiement = mode_paiement.substring(0, (mode_paiement.length()-2));
			        	CURRENT_COMMANDE.setMode_paiement(mode_paiement);
			        }
			    }
			    
			    // Calcul marge caisse
			    addMargeCaissier(CURRENT_COMMANDE); 
			    
		        // Calculer les points
		        if(CURRENT_COMMANDE.getOpc_client() != null && !BigDecimalUtil.isZero(mtt_points)){
					if(isPoints){
			        	if(carteClientP != null){
				        	CarteFidelitePersistant opc_carte_fidelite = carteClientP.getOpc_carte_fidelite();
							Integer nbrPointGagne = BigDecimalUtil.divide(BigDecimalUtil.multiply(mtt_points, opc_carte_fidelite.getMtt_pf_palier()), opc_carte_fidelite.getMtt_palier()).intValue();
				        	
							CURRENT_COMMANDE.setNbr_donne_point((CURRENT_COMMANDE.getNbr_donne_point()!=null?CURRENT_COMMANDE.getNbr_donne_point():0)+nbrPointGagne);
			        	}
					}
		        }
		        
//		        BigDecimal mttEspece = BigDecimalUtil.substract(mttNetCmd, BigDecimalUtil.add(mtt_cheque, mtt_carte, mtt_dej, mtt_points, mtt_portefeuille));	
		        
//		        CURRENT_COMMANDE.setMtt_donne(BigDecimalUtil.add(CURRENT_COMMANDE.getMtt_donne(), mttEspece));
		        
		        BigDecimal mttDonneeAll = BigDecimalUtil.add(
		        		//mtt_especes,
		        		mtt_cheque,
		        		mtt_carte,
		        		mtt_dej,
		        		mtt_points,
		        		mtt_portefeuille
		        	);
		        CURRENT_COMMANDE.setMtt_donne(BigDecimalUtil.substract(mttNetCmd, mttDonneeAll));
		        
		        CURRENT_COMMANDE.setOpc_caisse_journee(journeeCaisseOuverte);
		        
				if(isRestau 
						&& userBean.isInProfile("SERVEUR")
						&& CURRENT_COMMANDE.getOpc_serveur() == null){
					CURRENT_COMMANDE.setOpc_serveur(userBean);
				}
				
		        // Afficher le montant à rendre dans la caisse
		        CURRENT_COMMANDE.setMtt_a_rendre(mtt_rendu);
		        httpUtil.setRequestAttribute("mtt_arendre", mtt_rendu);
			 }
	        // Si temporaire alors nouvelle référence au norme
	        if((CURRENT_COMMANDE.getRef_commande()==null || CURRENT_COMMANDE.getRef_commande().indexOf("CM-")==-1) 
	        		&& (CURRENT_COMMANDE.getId() == null || CURRENT_COMMANDE.getLast_statut().equals(ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.TEMP.toString()))){
	            String ref_commande = caisseWebService.getNextRefCommande();
	            CURRENT_COMMANDE.setRef_commande(ref_commande);
	        } 
	        
	        // Annulation ligne
    		BigDecimal mttlAnnul = null;
    		if(CURRENT_COMMANDE.getList_article() != null) {
    			for(CaisseMouvementArticlePersistant det : CURRENT_COMMANDE.getList_article()) {
    				if(BooleanUtil.isTrue(det.getIs_annule())) {
    					mttlAnnul = BigDecimalUtil.add(mttlAnnul, det.getMtt_total());
    				}
    			}
    			CURRENT_COMMANDE.setMtt_annul_ligne(mttlAnnul);
    		}
	        
    		CURRENT_COMMANDE.setDate_encais(new Date());
    		
	        // Imprimer le ticket --------------------------------------------------
        	boolean isRetour = httpUtil.getUserAttribute("IS_RETOUR") != null;
    		// Si retour mettre les montant en négatif
    		if(isRetour){
    			CURRENT_COMMANDE.setIs_retour(true);
    		}
    		
	        // Si impression locale
    		try{
    			boolean isA4Printer = "A4".equals(ContextGloabalAppli.getGlobalConfig("FORMAT_TICKET"));
    			if(isA4Printer){
	        		File pdfFile = new FactureVentePDF(CURRENT_COMMANDE, true).exportPdf();
	        		PrintPosBean pb = new PrintPosBean();
	        		pb.setA4File(pdfFile);
	        		
	        		boolean isAsync = printData(httpUtil, pb);
	        	} else{
	        		PrintTicketUtil pu = new PrintTicketUtil(CURRENT_COMMANDE, carteClientP);
	        		if(httpUtil.getParameter("not") == null){
		        		boolean isAsync = printData(httpUtil, pu.getPrintPosBean(), true);
		        	} else {
		        		openDash(httpUtil, ContextAppliCaisse.getCaisseBean().getImprimantes());
		        	}
	        	}
    		} catch(Exception e){
    			e.printStackTrace();
    		}
    		
	        if(!isPaiementPartiel){
	        	boolean isAutoCmdPrep = StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("ECRAN_CMD_AUTO"));// Si passage à prête directement
	        	STATUT_CAISSE_MOUVEMENT_ENUM LAST_STATUT = null;
	        	//
	        	if(StringUtil.isEmpty(CURRENT_COMMANDE.getLast_statut())){// Premier paiement <----------
		        	STATUT_CAISSE_MOUVEMENT_ENUM statut = STATUT_CAISSE_MOUVEMENT_ENUM.VALIDE;
		        	if(isAutoCmdPrep){
		    			statut = STATUT_CAISSE_MOUVEMENT_ENUM.PREP;
		    		}
		            // Mise à jour des statut détail afin de gérre les reprises et modification de commandes
		    		for (CaisseMouvementArticlePersistant caisseMvmArtP :  CURRENT_COMMANDE.getList_article()) {
		    			boolean isMenu = BooleanUtil.isTrue(caisseMvmArtP.getIs_menu());
		    			boolean isArticle = caisseMvmArtP.getType_ligne().equals(ContextAppli.TYPE_LIGNE_COMMANDE.ART.toString()) 
		    							&& caisseMvmArtP.getMenu_idx() == null 
		    							&& "C".equals(caisseMvmArtP.getOpc_article().getDestination());
		    			if(!isMenu && !isArticle){
		    				continue;
		    			}
		    			caisseMvmArtP.setLast_statut(statut.toString());
		    		}
		    		LAST_STATUT = statut;
	        	} else{
	        		if(STATUT_CAISSE_MOUVEMENT_ENUM.TEMP.toString().equals(CURRENT_COMMANDE.getLast_statut())){// Sortie de mise en attente <----------
	        			LAST_STATUT = isAutoCmdPrep ? STATUT_CAISSE_MOUVEMENT_ENUM.PREP : STATUT_CAISSE_MOUVEMENT_ENUM.VALIDE;
	        			// Mise à jour des statut détail afin de gérre les reprises et modification de commandes
	    	    		for (CaisseMouvementArticlePersistant caisseMvmArtP :  CURRENT_COMMANDE.getList_article()) {
	    	    			boolean isMenu = BooleanUtil.isTrue(caisseMvmArtP.getIs_menu());
	    	    			boolean isArticle = caisseMvmArtP.getType_ligne().equals(ContextAppli.TYPE_LIGNE_COMMANDE.ART.toString()) 
	    	    							&& caisseMvmArtP.getMenu_idx() == null 
	    	    							&& "C".equals(caisseMvmArtP.getOpc_article().getDestination());
	    	    			if(!isMenu && !isArticle){
	    	    				continue;
	    	    			}
	    	    			caisseMvmArtP.setLast_statut(LAST_STATUT.toString());
	    	    		}
	        		} else{
	        			LAST_STATUT = STATUT_CAISSE_MOUVEMENT_ENUM.valueOf(CURRENT_COMMANDE.getLast_statut());
	        		}
	        	}
	        	
	        	AbonnementBean abonnementBean = ContextAppli.getAbonementBean();
	        	final STATUT_CAISSE_MOUVEMENT_ENUM statutMvm = LAST_STATUT;
	        	EtablissementPersistant etsB = ContextAppli.getEtablissementBean();
	        	UserBean userB = userBean;
	        	
            	ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor)ServiceUtil.getBusinessBean("taskExecutor");
            	taskExecutor.submit(new Callable<Object>() {
    	            public Object call() throws Exception {
    	            	
    	            	try{
    	            		// Annulation stock en cas de modification de la commande
	    	            	if(abonnementBean.isOptStock()) {
		    	            	if(CURRENT_COMMANDE.getId() != null){ 
		    	            		List<ArticleStockInfoBean> listArtInfos = caisseWebService.clearMvmCaisseStock(CURRENT_COMMANDE);
		    	            		mouvementService.majQteArticleInfo(listArtInfos);
		    	            	}
	    	            	}
    	            	} catch(Exception e){
    	            		e.printStackTrace();  
    	            	};
    	            	try{
    		                //  Création mouvement
    	            		CaisseMouvementPersistant currCmd2 = null;
    	            		try{
    	            			currCmd2 = caisseWebService.createMouvementCaisse(CURRENT_COMMANDE, statutMvm, isPoints);
    	            			
    	                    	// Libérer les commandes
    	                		if(CURRENT_COMMANDE.getId() != null) {
    	                			userService.unlockCommandes(CURRENT_COMMANDE.getId(), userBean.getId());
    	                			if(CURRENT_COMMANDE.getOpc_user_lock() != null && CURRENT_COMMANDE.getOpc_user_lock().getId() == userB.getId()) {
    	                				CURRENT_COMMANDE.setOpc_user_lock(null);
    	                				currCmd2.setOpc_user_lock(null);
    	                			}
    	                		}
    	                		
    	            		} catch(Exception e){
    	            			currCmd2 = CURRENT_COMMANDE;
    	            			e.printStackTrace();
    	            		}
    	            		try{
    	            			caisseMvmtService.caisseMvmTraceur(currCmd2, etsB, userB);
    	            		} catch(Exception e){
    	            			e.printStackTrace();
    	            		}
    	            		
    		            	if(CURRENT_COMMANDE.getOpc_client() != null){
    		            		try{
    		            			portefeuilleService2.majSoldePortefeuilleMvm(CURRENT_COMMANDE.getOpc_client().getId(), "CLI");
    		            		} catch(Exception e){
        	            			e.printStackTrace();
        	            		}	
    			            }
    		            	CaisseMouvementPersistant currCmd3 = currCmd2;
    		            	// Destockage
    		            	new Thread(() -> {
    		        			try {
    		        				// Impression
    		        				if(isRestau){
    		        					caisseService.gestionEcranImprimante(currCmd3);
    		        				}
    		        			} catch (Exception ex) {
    		        				ex.printStackTrace();
    		        			}
    		        			try {
									if(abonnementBean.isOptStock()) {
										List<ArticleStockInfoBean> listArtInfos = null;
	    		    					// Destoquer
										if(isRestau){
											listArtInfos = caisseWebService.destockerArticleMvmRestau(currCmd3);
											CURRENT_COMMANDE.setMvm_stock_ids(currCmd3.getMvm_stock_ids());
										} else{
											listArtInfos = caisseWebService.destockerArticleMvmNonRestau(currCmd3);
										}
	    		    					mouvementService.majQteArticleInfo(listArtInfos);
	    				            }
									
									if(isCtrlStockCaisse) {
										mouvementService.majQteArticleCaisseInfo(currCmd3, false);
									}
									
	    		            	} catch (Exception ex) {
			        				ex.printStackTrace();
			        			}
    		        	    }).start();
    					} catch(Exception e){
    						e.printStackTrace();
    					}
    					return null;
    		        }
    		    });
	        } else{
	        	// Maj flag encaissement
	        	for(CaisseMouvementArticlePersistant det : listEncaissePartiel){
	        		det.setIs_encaisse(true);
	        	}
	        	// Sauvegarde
	        	if(CURRENT_COMMANDE.getId() != null){
	    		   	 ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor)ServiceUtil.getBusinessBean("taskExecutor");
	    		        taskExecutor.submit(new Callable() {
	    	            public Object call() throws Exception {
	    	            	try{
	    	            		caisseWebService.majMouvementPaiement(CURRENT_COMMANDE);
	    	            	} catch(Exception e){
	    						e.printStackTrace();
	    					}
	    	            	return null;
	    	            }
	    		   });
	        	}
	        }
	        
	        // Envoi temps réel
	        String type = "Sur place";
	        if(CURRENT_COMMANDE.getType_commande().equals(ContextAppli.TYPE_COMMANDE.E.toString())) {
	        	type = "A emporter";
	        } else if(CURRENT_COMMANDE.getType_commande().equals(ContextAppli.TYPE_COMMANDE.L.toString())) {
	        	type = "Livraison"; 
	        }
	        
	        // Chercher les afficheurs
	        sendDataToScreen(httpUtil, "V:"+type+":"+BigDecimalUtil.formatNumber(mtt_rendu));
	        
	        if(isPaiementPartiel){// Afficher boite de dialogue avec reste si paiement partiel
	        	httpUtil.setRequestAttribute("mtt_rendu_partiel", mtt_rendu);
	        } else{
		        // Nouvelle commande
		        initNewCommande(httpUtil);
		        
		        MessageService.addGrowlMessage(MSG_TYPE.SUCCES, "Commande validée", "La commande est validée avec succès.");
		        
		        httpUtil.setRequestAttribute("mtt_rendu", mtt_rendu);
	        }
	        
	        httpUtil.addJavaScript("$('#close_modal').trigger('click');");
	        httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
	        
	        
	        if(BooleanUtil.isTrue(ContextAppliCaisse.getCaisseBean().getIs_livraison())){
	        	if(StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("AUTH_REQUIRED_OUT"))){
	        		httpUtil.setDynamicUrl("commun.login.disconnect");
	        		return;
	        	} else if(StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("AUTH_REQUIRED"))){
	        		httpUtil.setUserAttribute("LOCK_MODE", true);
	        		httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
	        	}
	        } else if(StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("CAISS_AUTH_REQUIRED_OUT"))){
//	        	if(ContextAppli.getUserBean().isInProfile(ProfileService.PROFIL_CODE_ENUM.CAISSIER.toString())){
	        		httpUtil.setDynamicUrl("commun.login.disconnect");
	        		return;
//	        	}
	        } else if(StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("CAISS_AUTH_REQUIRED"))){ 
	        	httpUtil.setUserAttribute("LOCK_MODE", true);
        		httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
	        }
		}
		
		public void initQuitterLockMode(ActionUtil httpUtil) {
			List<UserPersistant> listUsers = userService.findAllUser(true);
			httpUtil.setRequestAttribute("listUser", listUsers);
			
			httpUtil.setFormReadOnly(false);
			
			httpUtil.setDynamicUrl("/domaine/caisse/normal/authentification-unlock-popup.jsp");
		}
		
		public void quitterLockMode(ActionUtil httpUtil) {
			HttpServletRequest request = httpUtil.getRequest();
			String mail = (String)ControllerUtil.getParam(request, "unlock.login");
			String pw = (String)ControllerUtil.getParam(request, "unlock.password");
			String badge = (String)ControllerUtil.getParam(request, "unlock.tkn");
			
			if(StringUtil.isNotEmpty(mail)){
				UserBean userB = userService.findById(Long.valueOf(mail));
				if(userB != null){
					mail = userB.getLogin();
				} 
			}
			
			UserBean userBean;
			if(StringUtil.isNotEmpty(badge)){
				userBean = userService.getUserByBadge(badge.trim()); 
				if(userBean == null){
					MessageService.addBannerMessage("Ce badge n'a pas été encore enregistré.");
					return;
				}
				// Si le compte est désactivé
				if(BooleanUtil.isTrue(userBean.getIs_desactive())){
					MessageService.addBannerMessage("Cet utilisateur est désactivé");
					return;
				}
			} else {
				mail = (mail != null) ? mail.trim() : null; 
				pw = (pw != null) ? pw.trim() : null;
				//
				userBean = userService.getUserByLoginAndPw(mail, new EncryptionEtsUtil(EncryptionEtsUtil.getDecrypKey()).encrypt(pw)); 
				if(userBean == null){
					MessageService.addBannerMessage("Le login ou le mot de passse est erroné");
					return;
				}
				// Si le compte est désactivé
				if(BooleanUtil.isTrue(userBean.getIs_desactive())){
					MessageService.addBannerMessage("L'utilisateur est désactivé");
					return;
				}
			}
			ProfilePersistant opc_profile = userBean.getOpc_profile();
			
			if(BooleanUtil.isTrue(opc_profile.getIs_desactive())) {
				MessageService.addBannerMessage("Le profile de cet utilisateur est désactivé");
				return;
			}
			//
			MessageService.getGlobalMap().put(ProjectConstante.SESSION_GLOBAL_USER, userBean); 
			
			httpUtil.removeUserAttribute("LOCK_MODE");
			
			httpUtil.setRequestAttribute("isUnlockQuit", true);
			
			httpUtil.setDynamicUrl(getCommande_detail_path(httpUtil));
		}
		
		/**
		 * @param httpUtil
		 * @param listArticleActifs
		 */
		public void addCtrlStock(ActionUtil httpUtil, List<ArticlePersistant> listArticleActifs) {
			boolean isCtrlStock = StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("SHOW_STOCK_MVM_CAISSE"));
			boolean isCtrlStockCaisse = StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("INVENTAIRE_CAISSE"));
			boolean isRestauDev = ContextAppli.IS_RESTAU_ENV();
			
			if(!isCtrlStock) {
				return;
			}
			if(ContextAppliCaisse.getCaisseBean() == null 
					|| ContextAppliCaisse.getCaisseBean().getOpc_stock_cible() == null) {
				return;
			}

			EtablissementPersistant etsP = familleService.findById(EtablissementPersistant.class, ContextAppli.getEtablissementBean().getId());
			String[] famInvParams = StringUtil.getArrayFromStringDelim(etsP.getFam_caisse_inv(), ";");
			List<Long> famInventaire = new ArrayList<>();
			//
			if(famInvParams != null) {
				for (String famId : famInvParams) {
					if(StringUtil.isEmpty(famId)) {
						continue;
					}
					famInventaire.add(Long.valueOf(famId));	
				}
			}
			
			Long emplId = ContextAppliCaisse.getCaisseBean().getOpc_stock_cible().getId();			
			IMouvementService mvmService = ServiceUtil.getBusinessBean(IMouvementService.class);
			CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");	
			Map<Long, String> mapStock = new HashMap<>();
			
			if(isCtrlStockCaisse) {
				Map<Long, BigDecimal> mapQteCmdTmp = caisseWebService.getEtatStockCmdTmp(CURRENT_COMMANDE, ContextAppliCaisse.getJourneeBean().getId());
				//
				for (ArticlePersistant artP : listArticleActifs) {
					Long famArtId = isRestauDev ? artP.getOpc_famille_cuisine().getId() : artP.getOpc_famille_stock().getId();
					if(!famInventaire.contains(famArtId)) {
						continue;
					}
					List<ArticleStockCaisseInfoPersistant> listArt = familleService.getQuery("from ArticleStockCaisseInfoPersistant "
							+ "where opc_emplacement.id=:emplId "
							+ "and opc_article.id=:artId")
							.setParameter("emplId", emplId)
							.setParameter("artId", artP.getId())
							.getResultList();
					ArticleStockCaisseInfoPersistant infoP = (listArt.size() > 0 ? listArt.get(0) : null);
					 if(infoP == null) {
						 continue;
					 }
					 BigDecimal qteStock = infoP.getQte_reel();					 
					 // On ajoute la qte cmd en attente ou cuisine non encore destoquée
					 BigDecimal qteTmp = mapQteCmdTmp.get(artP.getId());
					 BigDecimal qteFinal = BigDecimalUtil.substract(qteStock, qteTmp);
						 
					String qteFmt = BigDecimalUtil.formatNumberZero(qteFinal);
					mapStock.put(artP.getId(), (BigDecimalUtil.isZero(qteFinal) || qteFinal.compareTo(BigDecimalUtil.ZERO)<0) ? "*-*"+qteFmt : qteFmt);
				}	
			} else {
				Map<Long, BigDecimal> mapQteCmdTmp = caisseWebService.getEtatStockCmdTmp(CURRENT_COMMANDE, ContextAppliCaisse.getJourneeBean().getId());

				for (ArticlePersistant artP : listArticleActifs) {
					if(artP.getList_article() == null) {
						continue;
					}
					Long famArtId = isRestauDev ? artP.getOpc_famille_cuisine().getId() : artP.getOpc_famille_stock().getId();
					if(!famInventaire.contains(famArtId)) {
						continue;
					}
					
					String data = "";
					
					for(ArticleDetailPersistant artComp : artP.getList_article()) {
						 Long compId = artComp.getOpc_article_composant().getId();
					   //BigDecimal seuil = artComp.getOpc_article_composant().getSeuilEmpl(emplId);
						 ArticleStockInfoPersistant infoP = mvmService.getArticleEtatStock(compId, emplId);
						
						 if(infoP == null) {
							 continue;
						 }
						 BigDecimal qteStock = infoP.getQte_reel();					 
						 
						 // On ajoute la qte cmd en attente ou cuisine non encore destoquée
						 BigDecimal qteTmp = mapQteCmdTmp.get(compId);
						 BigDecimal qteFinal = BigDecimalUtil.substract(qteStock, qteTmp);
						 
						 if(BigDecimalUtil.ZERO.compareTo(qteFinal) >= 0) {
							 data += "*-*";
						 }
						 data += BigDecimalUtil.formatNumberZero(qteFinal)+" ("+artComp.getOpc_article_composant().getLibelle()+")<br>";
					}
					 
					if(StringUtil.isNotEmpty(data)) {
						mapStock.put(artP.getId(), data);
					}
				}	
			}
							 
			httpUtil.setRequestAttribute("mapStock", mapStock);
		}
   }
   