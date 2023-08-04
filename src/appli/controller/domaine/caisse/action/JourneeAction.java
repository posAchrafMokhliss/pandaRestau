package appli.controller.domaine.caisse.action;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Order;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import appli.controller.domaine.administration.bean.JourneeBean;
import appli.controller.domaine.caisse.bean.CaisseMouvementBean;
import appli.controller.domaine.stock.bean.MouvementBean;
import appli.controller.domaine.util_erp.ContextAppli;
import appli.controller.domaine.util_erp.ContextAppli.APPLI_ENV;
import appli.controller.domaine.util_erp.ContextAppli.SOFT_ENVS;
import appli.model.domaine.administration.service.IUserService;
import appli.model.domaine.caisse.service.ICaisseMouvementService;
import appli.model.domaine.caisse.service.ICaisseService;
import appli.model.domaine.caisse.service.ICaisseWebService;
import appli.model.domaine.caisse.service.IJourneeService;
import appli.model.domaine.compta.persistant.PaiementPersistant;
import appli.model.domaine.fidelite.dao.IPortefeuille2Service;
import appli.model.domaine.personnel.persistant.ClientPersistant;
import appli.model.domaine.personnel.persistant.EmployePersistant;
import appli.model.domaine.personnel.service.IEmployeService;
import appli.model.domaine.stock.persistant.EmplacementPersistant;
import appli.model.domaine.stock.persistant.FamilleCuisinePersistant;
import appli.model.domaine.stock.persistant.FamillePersistant;
import appli.model.domaine.stock.persistant.MouvementArticlePersistant;
import appli.model.domaine.stock.persistant.MouvementPersistant;
import appli.model.domaine.stock.service.IFamilleService;
import appli.model.domaine.stock.service.IMouvementService;
import appli.model.domaine.stock.service.impl.RepartitionBean;
import appli.model.domaine.vente.persistant.CaisseJourneePersistant;
import appli.model.domaine.vente.persistant.CaisseMouvementArticlePersistant;
import appli.model.domaine.vente.persistant.CaisseMouvementPersistant;
import appli.model.domaine.vente.persistant.CaisseMouvementTracePersistant;
import appli.model.domaine.vente.persistant.CaissePersistant;
import appli.model.domaine.vente.persistant.JourneePersistant;
import framework.component.complex.table.RequestTableBean;
import framework.controller.ActionBase;
import framework.controller.ActionUtil;
import framework.controller.Context;
import framework.controller.ContextGloabalAppli;
import framework.controller.annotation.WorkController;
import framework.controller.annotation.WorkForward;
import framework.model.common.constante.ProjectConstante.MSG_TYPE;
import framework.model.common.service.MessageService;
import framework.model.common.util.BigDecimalUtil;
import framework.model.common.util.BooleanUtil;
import framework.model.common.util.DateUtil;
import framework.model.common.util.DateUtil.TIME_ENUM;
import framework.model.common.util.ServiceUtil;
import framework.model.common.util.StrimUtil;
import framework.model.common.util.StringUtil;

@WorkController(nameSpace="caisse", bean=JourneeBean.class, jspRootPath="/domaine/caisse/")
public class JourneeAction extends ActionBase {
	@Inject
	private IJourneeService journeeService;
	@Inject
	private ICaisseService caisseService;
	@Inject
	private ICaisseMouvementService caisseMouvementService;
	@Inject
	private IFamilleService familleService;
	@Inject
	private ICaisseWebService caisseWebService;
	@Inject
	private IEmployeService employeService;
	@Inject
	private IUserService userService;
	@Inject
	private IMouvementService mouvementService;
	@Inject
	IPortefeuille2Service portefeuilelService2;
	
	public void work_init(ActionUtil httpUtil){
		List<CaissePersistant> listCaisses = caisseService.getListCaisseActive(ContextAppli.TYPE_CAISSE_ENUM.CAISSE.toString(), true);
		httpUtil.setRequestAttribute("listeCaisses", listCaisses);
		
		if(httpUtil.isEditionPage()){
			Long journeeId = httpUtil.getWorkIdLong();
			if(journeeId != null){
				httpUtil.setMenuAttribute("journeeId", journeeId);
			} else{
				journeeId = (Long)httpUtil.getMenuAttribute("journeeId");
			}
			if(!httpUtil.isCreateAction()){
				// Gérer le retour sur cet onglet
				journeeId = (Long)httpUtil.getMenuAttribute("journeeId");
				if(journeeId != null && !httpUtil.isCrudOperation()){
					loadBean(httpUtil, journeeId);
				}
			} else{
				httpUtil.removeMenuAttribute("journeeId");
			}
		}
		
		if(httpUtil.getParameter("tp") != null){
			httpUtil.setMenuAttribute("tpMnu", httpUtil.getParameter("tp"));
		}
		
		String[][] statutArray = {
				{ContextAppli.STATUT_JOURNEE.OUVERTE.getStatut(), ContextAppli.STATUT_JOURNEE.OUVERTE.toString()}, 
				{ContextAppli.STATUT_JOURNEE.CLOTURE.getStatut(), ContextAppli.STATUT_JOURNEE.CLOTURE.toString()} 
			};
		httpUtil.setRequestAttribute("statutArray", statutArray);
	}
	
	@Override
	public void work_find(ActionUtil httpUtil) {
		httpUtil.removeMenuAttribute("CURR_JRN_DBL_CLOT");
		httpUtil.removeMenuAttribute("isCaisse");
		
		RequestTableBean cplxTable = getTableBean(httpUtil, "list_journee");
		Map<String, Object> formCriterion = cplxTable.getFormBean().getFormCriterion();
		boolean isFilterAct = StringUtil.isTrue(httpUtil.getRequest().getParameter("is_filter_act"));
		if(httpUtil.getParameter("tp") != null){
			httpUtil.setMenuAttribute("tpj", httpUtil.getParameter("tp"));
		}
		String tp = (String)httpUtil.getMenuAttribute("tpj");
		tp = StringUtil.isEmpty(tp) ? "std" : tp;
		//
		//----------------------------- Date -------------------------
		Date dateDebut = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateDebut"));
		Date dateFin = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateFin"));
		boolean isDbFin = true;
		if(dateDebut == null) {
			JourneePersistant lastJrn = journeeService.getLastJournee();
			Date dateLast = (lastJrn == null ? new Date() : lastJrn.getDate_journee());
			
			dateDebut = (httpUtil.getMenuAttribute("dateDebut")==null ? dateLast : (Date)httpUtil.getMenuAttribute("dateDebut"));
			dateFin = (httpUtil.getMenuAttribute("dateFin")==null ? dateLast : (Date)httpUtil.getMenuAttribute("dateFin"));
		} else if(httpUtil.getRequest().getParameter("prev") != null) {
			dateDebut = DateUtil.addSubstractDate(dateDebut, TIME_ENUM.MONTH, -1);
			dateFin = DateUtil.addSubstractDate(dateFin, TIME_ENUM.MONTH, -1);
		} else if(httpUtil.getRequest().getParameter("next") != null) {
			dateDebut = DateUtil.addSubstractDate(dateDebut, TIME_ENUM.MONTH, 1);
			dateFin = DateUtil.addSubstractDate(dateFin, TIME_ENUM.MONTH, 1);
		} else{
			isDbFin = false;
		}
		if(isDbFin){
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
		httpUtil.setMenuAttribute("dateDebut", dateDebut);
		httpUtil.setMenuAttribute("dateFin", dateFin);
		
		if(!isFilterAct){
			formCriterion.put("dateDebut", dateDebut);
			formCriterion.put("dateFin", dateFin);
		} else{
			formCriterion.remove("dateDebut");
			formCriterion.remove("dateFin");
		}
		//-----------------------------------------------------------		
		boolean isMttJrnEnNet =  StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("CHIFFRE_JRN_NET"));
		List<JourneePersistant> listData = (List<JourneePersistant>) journeeService.findByCriteriaByQueryId(cplxTable, "journee_find");
	   	
		if(listData.size() >0) {
			journeeService.setDataJourneeFromView(listData.get(0));
		}
		httpUtil.setRequestAttribute("list_journee", listData);
	   	
	   	JourneePersistant jp = new JourneePersistant();
	   	List<JourneePersistant> listDataAll = (List<JourneePersistant>) journeeService.findByCriteriaByQueryId(cplxTable, "journee_find", false);
	   	for (JourneePersistant jvp : listDataAll) {
	   		if("O".equals(jvp.getStatut_journee())){
	   			journeeService.setDataJourneeFromView(jvp);
	   		}
	   		jp.setMtt_reduction(BigDecimalUtil.add(jp.getMtt_reduction(), jvp.getMtt_reduction()));
	   		jp.setMtt_art_offert(BigDecimalUtil.add(jp.getMtt_art_offert(), jvp.getMtt_art_offert()));
	   		jp.setMtt_art_reduction(BigDecimalUtil.add(jp.getMtt_art_reduction(), jvp.getMtt_art_reduction()));
	   		jp.setMtt_livraison(BigDecimalUtil.add(jp.getMtt_livraison(), jvp.getMttLivraisonGlobal()));
	   		//
	   		if("std".equals(tp)){
		   		jp.setNbr_vente((jp.getNbr_vente()!=null?jp.getNbr_vente():0)+ (jvp.getNbr_vente()!=null?jvp.getNbr_vente():0));
		   		jp.setMtt_ouverture(BigDecimalUtil.add(jp.getMtt_ouverture(), jvp.getMtt_ouverture()));
		   		
		   		// CALCULE
		   		if(isMttJrnEnNet) {
		   			jp.setMtt_total_net(BigDecimalUtil.add(jp.getMtt_total_net(), BigDecimalUtil.substract(jvp.getMtt_total_net(), jvp.getMtt_portefeuille(), jvp.getMtt_donne_point())));
		   		} else {
		   			jp.setMtt_total_net(BigDecimalUtil.add(jp.getMtt_total_net(), jvp.getMtt_total_net()));
		   		}
		   		
		   		// CLOTURE
		   		if("C".equals(jvp.getStatut_journee())){
		   			BigDecimal netCloture = BigDecimalUtil.substract(jvp.getMtt_cloture_caissier(), jvp.getMtt_ouverture());
		   			jp.setMtt_cloture_caissier(BigDecimalUtil.add(jp.getMtt_cloture_caissier(), netCloture));
		   			// ECART
			   		jp.setMtt_total(BigDecimalUtil.add(jp.getMtt_total(), jvp.getEcartNet()));
		   		}
		   		// MARGE
		   		BigDecimal mttMarge = jp.getMtt_total_achat()!=null ? BigDecimalUtil.substract(jp.getMtt_total_net(),jp.getMttLivraisonPartLivreur(), jp.getMtt_total_achat())
							: BigDecimalUtil.substract(jvp.getMtt_total_net(), jvp.getMttLivraisonPartLivreur(), jvp.getMtt_total_achat());
		   		jp.setMtt_cloture_caissier_cb(BigDecimalUtil.add(jp.getMtt_cloture_caissier_cb(), mttMarge));
		   		
		   		jp.setMtt_donne_point(BigDecimalUtil.add(jp.getMtt_donne_point(), jvp.getMtt_donne_point()));
	   			jp.setMtt_portefeuille(BigDecimalUtil.add(jp.getMtt_portefeuille(), jvp.getMtt_portefeuille()));
	   			
	   			jp.setMtt_annule(BigDecimalUtil.add(jp.getMtt_annule(), jvp.getMtt_annule()));
	   			jp.setMtt_annule_ligne(BigDecimalUtil.add(jp.getMtt_annule_ligne(), jvp.getMtt_annule_ligne()));
	   		} else{
	   			jp.setMtt_cheque(BigDecimalUtil.add(jp.getMtt_cheque(), jvp.getMtt_cheque()));
	   			jp.setMtt_espece(BigDecimalUtil.add(jp.getMtt_espece(), jvp.getMtt_espece()));
	   			jp.setMtt_cb(BigDecimalUtil.add(jp.getMtt_cb(), jvp.getMtt_cb()));
	   			jp.setMtt_dej(BigDecimalUtil.add(jp.getMtt_dej(), jvp.getMtt_dej()));
	   			jp.setMtt_donne_point(BigDecimalUtil.add(jp.getMtt_donne_point(), jvp.getMtt_donne_point()));
	   			jp.setMtt_portefeuille(BigDecimalUtil.add(jp.getMtt_portefeuille(), jvp.getMtt_portefeuille()));
	   			
	   			jp.setMtt_annule(BigDecimalUtil.add(jp.getMtt_annule(), jvp.getMtt_annule()));
	   			jp.setMtt_annule_ligne(BigDecimalUtil.add(jp.getMtt_annule_ligne(), jvp.getMtt_annule_ligne()));
	   		}
		} 
	   	httpUtil.setRequestAttribute("journee_total", jp);
	   	
		boolean isDoubleCloture = BooleanUtil.isTrue(ContextAppli.getUserBean().getIs_admin()) 
				|| (StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("DOUBLE_CLOTURE")) 
						&& Context.isOperationAvailable("DBLCLO"));
		httpUtil.setRequestAttribute("isDoubleCloture", isDoubleCloture);

	   	if("std".equals(tp)){
	   		httpUtil.setDynamicUrl("/domaine/caisse/back-office/journee_list.jsp");
	   	} else{
	   		httpUtil.setDynamicUrl("/domaine/caisse/back-office/journee_montant_list.jsp");
	   	}
	}
	
	
	/**
	 * @param httpUtil
	 */
	@WorkForward(useBean=true)
	public void ouvrir_journee(ActionUtil httpUtil) {
		JourneeBean viewBean = (JourneeBean) httpUtil.getViewBean();
		journeeService.ouvrirJournee(viewBean);
		
		String env = (String) httpUtil.getUserAttribute("CURRENT_ENV");
		if(!env.equals(APPLI_ENV.cais.toString()) 
				&& !env.equals(APPLI_ENV.cais_mob.toString())
				&& httpUtil.getMenuAttribute("isCaisse") == null){
			work_find(httpUtil);
		} else if(env.equals(APPLI_ENV.cais.toString()) || env.equals(APPLI_ENV.cais_mob.toString())){
			JourneePersistant journeeOuverte = caisseWebService.getLastJourne();
            MessageService.getGlobalMap().put("CURRENT_JOURNEE", journeeOuverte);

            if(journeeOuverte == null){
            	MessageService.addGrowlMessage("Journée ouverte absente", "Aucune journée ouverte n'est disponible.");
                return;
            }
			httpUtil.writeResponse("REDIRECT:");
		} else {
			httpUtil.setDynamicUrl("caisse.caisse.work_find");
		}
		
		 // --------------------------------------------------------------------------------------------------------------
        
		// Réajuster le stock ------------------------- 
		JourneePersistant lastJournee = journeeService.getLastJournee();
		Date lastDay = null;
		if(lastJournee != null){
			lastDay = DateUtil.addSubstractDate(lastJournee.getDate_journee(), TIME_ENUM.DAY, -1);
			lastDay = DateUtil.getStartOfDay(lastDay);
		} else{
			lastDay = new Date();
		}
		final Date dayRef = lastDay;
	   	 ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor)ServiceUtil.getBusinessBean("taskExecutor");
	        taskExecutor.submit(new Callable() {
            public Object call() throws Exception {
            	try{
	            	JourneePersistant lastJrn = journeeService.getLastJournee();
//	            	if(lastJrn != null){
//	            		caisseWebService.destockerCmdNonTraitee(lastJrn.getId());
//	            	}
	                //  Création mouvement
	                mouvementService.majQteStockArticle(dayRef);
            	} catch(Exception e){
					e.printStackTrace();
				}
                return null;
            }
        });
	    // --------------------------------------------------------------------------------------------------------------
	}
	
	public void reOuvrirJournee(ActionUtil httpUtil) {
		JourneeBean viewBean = journeeService.findById(httpUtil.getWorkIdLong());
		journeeService.reOuvrirJournee(viewBean);
		
		String env = (String) httpUtil.getUserAttribute("CURRENT_ENV");
		if(httpUtil.getMenuAttribute("isCaisse") == null || env.equals(ContextAppli.APPLI_ENV.back.toString())){
			work_find(httpUtil);
		} else{
			httpUtil.setDynamicUrl("caisse.caisse.work_find");
		}
	}
	
	@Override
	public void work_edit(ActionUtil httpUtil){
		boolean isDetailJourneeDroit = Context.isOperationAvailable("DETJRN");
		if(isDetailJourneeDroit) {
			Long journeeId = httpUtil.getWorkIdLong();
			JourneePersistant journeeDB = journeeService.getJourneeView(journeeId);
			boolean isDoubleCloture = BooleanUtil.isTrue(ContextAppli.getUserBean().getIs_admin()) 
											|| (StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("DOUBLE_CLOTURE")) 
													&& Context.isOperationAvailable("DBLCLO"));
			httpUtil.setRequestAttribute("isDoubleCloture", isDoubleCloture);
			httpUtil.setRequestAttribute("mttRecharge", journeeService.getMontantRechargePortefeuille(journeeId));
			
			httpUtil.setRequestAttribute("journeeView", journeeDB);
			httpUtil.setMenuAttribute("CURR_JRN_ID", journeeId);
			
			httpUtil.setDynamicUrl("/domaine/caisse/back-office/journee_edit.jsp");
		}else {
			find_mouvement(httpUtil);
		}
	}
	
	/**
	 * @param httpUtil
	 */
	public void init_update_journee(ActionUtil httpUtil){
		String[][] typeCmd = {{ContextAppli.TYPE_COMMANDE.E.toString(), "A emporter"}, 
				  {ContextAppli.TYPE_COMMANDE.P.toString(), "Sur place"}, 
				  {ContextAppli.TYPE_COMMANDE.L.toString(), "Livraison"}
				  };
		String[][] modePaie = {
		  {ContextAppli.MODE_PAIEMENT.ESPECES.toString(), "Espèces"}, 
		  {ContextAppli.MODE_PAIEMENT.CARTE.toString(), "Carte"},
		  {ContextAppli.MODE_PAIEMENT.CHEQUE.toString(), "Chèque"},
		  
		  {ContextAppli.MODE_PAIEMENT.DEJ.toString(), "Chèque déj."},
		  {"POINTS", "Points"},
		  {"RESERVE", "Réserve"}
		};
		
		httpUtil.setRequestAttribute("typeCmds", typeCmd);
		httpUtil.setRequestAttribute("modePaiement", modePaie);
		
		if(httpUtil.getParameter("isUp") != null) {
			String type_cmd = httpUtil.getParameter("caisseMouvement.type_commande");
			String mode_paiement = httpUtil.getParameter("caisseMouvement.mode_paiement");
			
			Long clientId = httpUtil.getLongParameter("caisseMouvement.opc_client.id");
			Long livreurId = httpUtil.getLongParameter("caisseMouvement.opc_livreurU.id");
			
			Long mvmCaisseId = httpUtil.getLongParameter("cmvm");
			journeeService.updateBasicInfos(mvmCaisseId, type_cmd, mode_paiement, clientId, livreurId);
			
			// Recalcul du shift
			CaisseMouvementPersistant cmP = caisseMouvementService.findById(mvmCaisseId);
			caisseService.refreshDataShift(cmP.getOpc_caisse_journee().getId());

			MessageService.addGrowlMessage(MSG_TYPE.SUCCES, "Mise à jour", "La commande est mise à jour avec succès.");
			
			if(ContextAppli.APPLI_ENV.pil.toString().equals(httpUtil.getUserAttribute("CURRENT_ENV"))) {
				httpUtil.setDynamicUrl("caisse-web.cuisinePilotage.loadCommande");
				return;
			} else if(ContextAppli.APPLI_ENV.cuis.toString().equals(httpUtil.getUserAttribute("CURRENT_ENV"))) {
				httpUtil.setDynamicUrl("caisse-web.cuisine.loadCommande");
				return;
			} else if(ContextAppli.APPLI_ENV.pres.toString().equals(httpUtil.getUserAttribute("CURRENT_ENV"))) {
				httpUtil.setDynamicUrl("caisse-web.presentoire.loadCommande");
				return;
			} else if(ContextAppli.APPLI_ENV.cais.toString().equals(httpUtil.getUserAttribute("CURRENT_ENV"))
					|| ContextAppli.APPLI_ENV.cais_mob.toString().equals(httpUtil.getUserAttribute("CURRENT_ENV"))) {
				httpUtil.setDynamicUrl("caisse-web.caisseWeb.initHistorique");
				return;
			} else if(httpUtil.getMenuAttribute("caisseId") != null){
				httpUtil.setDynamicUrl("caisse.caisse.find_mouvement");
				return;
			}
			find_mouvement(httpUtil);
		} else {
			httpUtil.setRequestAttribute("listClient", employeService.findAll(ClientPersistant.class, Order.asc("nom")));
			//httpUtil.setRequestAttribute("listLivreur", employeService.getListEmployeActifs("LIVREUR"));
			httpUtil.setRequestAttribute("listLivreur", userService.getListUserActifsByProfile("LIVREUR"));
			
			httpUtil.setViewBean(caisseMouvementService.findById(httpUtil.getWorkIdLong()));
			httpUtil.setDynamicUrl("/domaine/caisse/back-office/journee_edit_update.jsp");
		}
	}
	
	public void init_cloture(ActionUtil httpUtil) {
		Long jrnId = httpUtil.getWorkIdLong();
		if(jrnId == null) {
			httpUtil.writeResponse("REDIRECT:");
			return;
		}
		
		JourneeBean viewBean = journeeService.findById(jrnId);
		httpUtil.setRequestAttribute("oldSoldeCoffre", viewBean.getSolde_coffre());
		httpUtil.setRequestAttribute("currJournee", httpUtil.getWorkId());
		//
		httpUtil.setDynamicUrl("/domaine/caisse/back-office/journee_cloture_edit.jsp");
	}
	
	@WorkForward(useBean=true)
	public void cloturer_journee(ActionUtil httpUtil){
		String env = (String) httpUtil.getUserAttribute("CURRENT_ENV");
		boolean isRecloture = (httpUtil.getParameter("isReclos") != null);
		BigDecimal soldeCoffre = BigDecimalUtil.get(httpUtil.getParameter("journee.solde_coffre"));
		
		boolean isFromCaisse = (env.equals(APPLI_ENV.cais.toString()) || env.equals(APPLI_ENV.cais_mob.toString()));
		
		Long journeeId = httpUtil.getWorkIdLong();
		JourneePersistant journeeP = journeeService.findById(JourneePersistant.class, journeeId);
		
		//-------------- Securtité journée -----------------------//
		journeeService.recalculChiffresMvmJournee(journeeId);
		for(CaisseJourneePersistant jc : journeeP.getList_caisse_journee()) {
			caisseService.refreshDataShift(jc.getId());
		}
		//-----------------------------------------------------------
		journeeService.cloturerJournee(journeeId, isRecloture, soldeCoffre, isFromCaisse);
		
		List<CaisseJourneePersistant> list_caisse_journee = new ArrayList(journeeP.getList_caisse_journee());
		for(CaisseJourneePersistant cjP : list_caisse_journee) {
			caisseService.cloturerDefinitive(cjP, true, null, null, null, null, false, false);
		}
		
		if(!env.equals(APPLI_ENV.cais.toString()) 
				&& !env.equals(APPLI_ENV.cais_mob.toString())
				&& httpUtil.getMenuAttribute("isCaisse") == null){
			work_find(httpUtil);
		} else if(env.equals(APPLI_ENV.cais.toString()) || env.equals(APPLI_ENV.cais_mob.toString())){
			JourneePersistant journeeOuverte = caisseWebService.getLastJourne();
            MessageService.getGlobalMap().put("CURRENT_JOURNEE", journeeOuverte);
			
            if(journeeOuverte == null){
            	MessageService.addGrowlMessage("Journée ouverte absente", "Aucune journée ouverte n'est disponible.");
                return;
            }
			httpUtil.writeResponse("REDIRECT:");
		} else{
			httpUtil.setDynamicUrl("caisse.caisse.work_find");
		}
	}
	
	@Override
	public void work_init_create(ActionUtil httpUtil) {
		JourneePersistant lastJournee = journeeService.getLastJournee();
		
		if(lastJournee != null && lastJournee.getStatut_journee().equals(ContextAppli.STATUT_JOURNEE.OUVERTE.getStatut())){
			MessageService.addBannerMessage("Une journée ouverte ("+DateUtil.dateToString(lastJournee.getDate_journee())+") existe déjà");
			return;
		}

		Date nextDate = new Date(); 
		if(lastJournee != null){
			nextDate = DateUtil.addSubstractDate(lastJournee.getDate_journee(), TIME_ENUM.DAY, 1);
		}
		httpUtil.setRequestAttribute("currentDate", nextDate);
		
		if(lastJournee != null){
			httpUtil.setRequestAttribute("currentCustomcall", lastJournee.getCustomcall_out());
		}
		httpUtil.setDynamicUrl("/domaine/caisse/back-office/journee_ouverture_edit.jsp");
	}
	
	/**---------------------------------------------------*/
	public void depenses_journee(ActionUtil httpUtil){
		Long journeeId = (Long)httpUtil.getMenuAttribute("journeeId");
		JourneePersistant journeeP = journeeService.findById(journeeId);
		httpUtil.setRequestAttribute("journee", journeeP);

		RequestTableBean cplxTableDep = getTableBean(httpUtil, "list_depense");
		cplxTableDep.getFormBean().getFormCriterion().put("journeeId", journeeId);
		List<CaisseMouvementArticlePersistant> listCaisseDepenseMvm = (List<CaisseMouvementArticlePersistant>) caisseMouvementService.findByCriteriaByQueryId(cplxTableDep, "depense_journee_find");
		httpUtil.setRequestAttribute("list_depense", listCaisseDepenseMvm);

		RequestTableBean cplxTableRec = getTableBean(httpUtil, "list_recette");
		cplxTableRec.getFormBean().getFormCriterion().put("journeeId", journeeId);
		List<CaisseMouvementArticlePersistant> listCaisseRecetteMvm = (List<CaisseMouvementArticlePersistant>) caisseMouvementService.findByCriteriaByQueryId(cplxTableRec, "recette_journee_find");
		httpUtil.setRequestAttribute("list_recette", listCaisseRecetteMvm);
		
		RequestTableBean cplxTableGar = getTableBean(httpUtil, "list_garantie");
		cplxTableGar.getFormBean().getFormCriterion().put("journeeId", journeeId);
		List<CaisseMouvementArticlePersistant> listCaisseGarantieMvm = (List<CaisseMouvementArticlePersistant>) caisseMouvementService.findByCriteriaByQueryId(cplxTableGar, "garantie_journee_find");
		httpUtil.setRequestAttribute("list_garantie", listCaisseGarantieMvm);
		
		// Calcul du total montants sans pagination
	   	BigDecimal totalTtcAll = null;
	   	List<CaisseMouvementArticlePersistant> listDataAll = (List<CaisseMouvementArticlePersistant>) caisseMouvementService.findByCriteriaByQueryId(cplxTableDep, "depense_journee_find", false);
	   	for (CaisseMouvementArticlePersistant mvmStockViewP : listDataAll) {
		   	totalTtcAll = BigDecimalUtil.add(totalTtcAll, mvmStockViewP.getMtt_total());
	   	}
	   	httpUtil.setRequestAttribute("totalDepTtc", totalTtcAll);
	   	
	   	//
	   	totalTtcAll = null;
	   	listDataAll = (List<CaisseMouvementArticlePersistant>) caisseMouvementService.findByCriteriaByQueryId(cplxTableRec, "recette_journee_find", false);
	   	for (CaisseMouvementArticlePersistant mvmStockViewP : listDataAll) {
		   	totalTtcAll = BigDecimalUtil.add(totalTtcAll, mvmStockViewP.getMtt_total());
	   	}
	   	httpUtil.setRequestAttribute("totalRecTtc", totalTtcAll);
	   	
	   	totalTtcAll = null;
	   	listDataAll = (List<CaisseMouvementArticlePersistant>) caisseMouvementService.findByCriteriaByQueryId(cplxTableGar, "garantie_journee_find", false);
	   	for (CaisseMouvementArticlePersistant mvmStockViewP : listDataAll) {
		   	totalTtcAll = BigDecimalUtil.add(totalTtcAll, mvmStockViewP.getMtt_total());
	   	}
	   	httpUtil.setRequestAttribute("totalGarTtc", totalTtcAll);
	   	
		httpUtil.setDynamicUrl("/domaine/caisse/back-office/depense_journee_list.jsp");
	}
	
	public void find_mvm_annomalie(ActionUtil httpUtil){
		RequestTableBean cplxTable = getTableBean(httpUtil, "listMvmAnnomalie");
		Long journeeId = (Long)httpUtil.getMenuAttribute("journeeId");
		List<CaisseMouvementTracePersistant> listMvmAnnomalie = journeeService.getMouvementAnnomalie(journeeId);
		
		// Total
		CaisseMouvementTracePersistant mvmTotal = new CaisseMouvementTracePersistant();
		for (CaisseMouvementTracePersistant mvmDet : listMvmAnnomalie) {
			mvmTotal.setMtt_art_offert(BigDecimalUtil.add(mvmTotal.getMtt_art_offert(), mvmDet.getMtt_art_offert()));
			mvmTotal.setMtt_art_reduction(BigDecimalUtil.add(mvmTotal.getMtt_art_reduction(), mvmDet.getMtt_art_reduction()));
			mvmTotal.setMtt_commande(BigDecimalUtil.add(mvmTotal.getMtt_commande(), mvmDet.getMtt_commande()));
			mvmTotal.setMtt_commande_net(BigDecimalUtil.add(mvmTotal.getMtt_commande_net(), mvmDet.getMtt_commande_net()));
			mvmTotal.setMtt_reduction(BigDecimalUtil.add(mvmTotal.getMtt_reduction(), mvmDet.getMtt_reduction()));
		}
		httpUtil.setRequestAttribute("mvmDetTotal", mvmTotal);
		
		cplxTable.setDataSize(listMvmAnnomalie.size());
		httpUtil.setRequestAttribute("listMvmAnnomalie", listMvmAnnomalie);
		httpUtil.setRequestAttribute("mvmDetTotal", mvmTotal);
		
		httpUtil.setDynamicUrl("/domaine/caisse/back-office/mouvements_annomalie_list.jsp");
	}
	
	public void find_mouvement(ActionUtil httpUtil){
		httpUtil.setRequestAttribute("listCaisse", caisseService.getListCaisseActive(ContextAppli.TYPE_CAISSE_ENUM.CAISSE.toString(), false));
		Long currCaisse = httpUtil.getLongParameter("cs");
		Long currCaissier = httpUtil.getLongParameter("cc");
		httpUtil.setRequestAttribute("currCaisse", currCaisse);
		httpUtil.setRequestAttribute("currCaissier", currCaissier);
		
		httpUtil.setRequestAttribute("listUser", userService.findAllUser(false));
		httpUtil.setRequestAttribute("listEmploye", employeService.findAll(EmployePersistant.class, Order.asc("nom")));
		httpUtil.setRequestAttribute("listClient", employeService.findAll(ClientPersistant.class, Order.asc("nom")));
		String[][] modePaie = {
			  {ContextAppli.MODE_PAIEMENT.ESPECES.toString(), "Espèces"},
			  {ContextAppli.MODE_PAIEMENT.CHEQUE.toString(), "Chèque"}, 
			  {ContextAppli.MODE_PAIEMENT.DEJ.toString(), "Chèque déj."},
			  {ContextAppli.MODE_PAIEMENT.CARTE.toString(), "Carte"}
			};
			
		httpUtil.setRequestAttribute("modePaiement", modePaie);
			
		//
		String[][] typeCmd = {{ContextAppli.TYPE_COMMANDE.E.toString(), "A emporter"}, 
							  {ContextAppli.TYPE_COMMANDE.P.toString(), "Sur place"}, 
							  {ContextAppli.TYPE_COMMANDE.L.toString(), "Livraison"}
							  };
		httpUtil.setRequestAttribute("typeCmd", typeCmd);
		
		RequestTableBean cplxTable = getTableBean(httpUtil, "list_caisseMouvement");
		
		Long journeeId = (Long)httpUtil.getMenuAttribute("journeeId");
		Map<String, Object> formCriterion = cplxTable.getFormBean().getFormCriterion();
		formCriterion.put("journeeId", journeeId);
		formCriterion.put("caisseId", currCaisse);
		formCriterion.put("caissierId", currCaissier);
		// Force date
		formCriterion.put("dateDebut", checkInstanceDate(httpUtil, formCriterion.get("dateDebut")));
		formCriterion.put("dateFin", checkInstanceDate(httpUtil, formCriterion.get("dateFin")));
		
		List<CaisseMouvementPersistant> listCaisseMouvement = (List<CaisseMouvementPersistant>) caisseMouvementService.findByCriteriaByQueryId(cplxTable, "caisseMouvement_find");
		httpUtil.setRequestAttribute("list_caisseMouvement", listCaisseMouvement);
		
		boolean isDoubleCloture = BooleanUtil.isTrue(ContextAppli.getUserBean().getIs_admin()) 
							    || (StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("DOUBLE_CLOTURE")) && Context.isOperationAvailable("DBLCLO"));
		JourneePersistant journeeV = journeeService.getJourneeView(journeeId);
		httpUtil.setRequestAttribute("journeeView", journeeV);
		httpUtil.setRequestAttribute("isDoubleCloture", isDoubleCloture);
		httpUtil.setMenuAttribute("CURR_JRN_ID", journeeId);
		
		// Total
		List<CaisseMouvementPersistant> listCaisseMouvementAll = (List<CaisseMouvementPersistant>) caisseMouvementService.findByCriteriaByQueryId(cplxTable, "caisseMouvement_find", false);
		CaisseMouvementPersistant mvmTotal = new CaisseMouvementPersistant();
		for (CaisseMouvementPersistant mvmDet : listCaisseMouvementAll) {
			mvmTotal.setMtt_art_offert(BigDecimalUtil.add(mvmTotal.getMtt_art_offert(), mvmDet.getMtt_art_offert()));
			mvmTotal.setMtt_art_reduction(BigDecimalUtil.add(mvmTotal.getMtt_art_reduction(), mvmDet.getMtt_art_reduction()));
			mvmTotal.setMtt_commande(BigDecimalUtil.add(mvmTotal.getMtt_commande(), mvmDet.getMtt_commande()));
			mvmTotal.setMtt_commande_net(BigDecimalUtil.add(mvmTotal.getMtt_commande_net(), mvmDet.getMtt_commande_net()));
			mvmTotal.setMtt_reduction(BigDecimalUtil.add(mvmTotal.getMtt_reduction(), mvmDet.getMtt_reduction()));
			mvmTotal.setMtt_annul_ligne(BigDecimalUtil.add(mvmTotal.getMtt_annul_ligne(), mvmDet.getMtt_annul_ligne()));
		}
		httpUtil.setRequestAttribute("mvmDetTotal", mvmTotal);
		httpUtil.removeMenuAttribute("IS_MODE_MOUVE");
		
		httpUtil.setDynamicUrl("/domaine/caisse/back-office/mouvements_caisses_list.jsp");
	}
	
	/**
	 * @param httpUtil
	 */
	public void chiffre_caisse(ActionUtil httpUtil){
		Long journeeId = (Long)httpUtil.getMenuAttribute("journeeId");
		JourneePersistant journeeV = journeeService.getJourneeView(journeeId);
		
		List<CaisseJourneePersistant> listDataShift = journeeService.getJourneeCaisseView(journeeId);
		httpUtil.setRequestAttribute("listDataShift", listDataShift);
		
		httpUtil.setRequestAttribute("journeeView", journeeV);
		
		httpUtil.setDynamicUrl("/domaine/caisse/back-office/chiffres_caisse_journee.jsp");
	}
	
	public void recalculShifts(ActionUtil httpUtil){
		Long journeeId = (Long)httpUtil.getMenuAttribute("journeeId");
		Long caisseJourneeId = httpUtil.getLongParameter("cjId");
		CaisseJourneePersistant caisseJP = caisseService.findById(CaisseJourneePersistant.class, caisseJourneeId);
		
		caisseService.cloturerDefinitive(caisseJP, 
				true, 
				null, 
				null, 
				null, 
				null, 
				false, false); 
		
		JourneePersistant opc_journee = journeeService.findById(JourneePersistant.class, journeeId);
		journeeService.setDataJourneeFromView(opc_journee);
		journeeService.mergeEntity(opc_journee);
		journeeService.ajouterEcrituresJournee(opc_journee);
		
		httpUtil.setDynamicUrl("caisse.journee.chiffre_caisse");
	}
	
	/**
	 * @param httpUtil
	 */
	public void chiffre_employe(ActionUtil httpUtil){
		boolean isMois = (httpUtil.getParameter("isM") != null);
		Long journeeId = isMois ? null : (Long)httpUtil.getMenuAttribute("journeeId");
		
		if(isMois) {
			Date dateDebut = DateUtil.stringToDate(httpUtil.getParameter("dateDebut"));
			Date dateFin = DateUtil.stringToDate(httpUtil.getParameter("dateFin"));
			dateDebut = DateUtil.getStartOfDay(dateDebut);
			dateFin = DateUtil.getEndOfDay(dateFin);

			httpUtil.setRequestAttribute("isMois", isMois);
			httpUtil.setMenuAttribute("dtDebCh", dateDebut);
			httpUtil.setMenuAttribute("dtFinCh", dateFin);
			
			Map<String, Map<String, RepartitionBean>> listDataEmploye = journeeService.getChiffresServeurLivreurCaissier(dateDebut, dateFin, null);
			httpUtil.setRequestAttribute("mapDataEmploye", listDataEmploye);
		} else {
			Map<String, Map<String, RepartitionBean>> listDataEmploye = journeeService.getChiffresServeurLivreurCaissier(null, null, journeeId);
			httpUtil.setRequestAttribute("mapDataEmploye", listDataEmploye);
		}
		
		httpUtil.setDynamicUrl("/domaine/caisse/back-office/chiffres_employe_journee.jsp");
	}
	
	/**
	 * @param httpUtil
	 */
	public void find_repartition(ActionUtil httpUtil){
		if(StrimUtil.getGlobalConfigPropertie("context.soft").equals(SOFT_ENVS.restau.toString())){
			find_repartition_restau(httpUtil);
		} else{
			find_repartition2(httpUtil);
		}
	}
	
	public void find_rep_stock(ActionUtil httpUtil){
		if(httpUtil.getParameter("tp") != null) {
			httpUtil.setMenuAttribute("IS_MNU", true);
		}
		Map mapData = null;
		//----------------------------- Date -------------------------
		if(httpUtil.getMenuAttribute("IS_MNU") != null) {
			Date dateDebut = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateDebut"));
			Date dateFin = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateFin"));
			//
			if(httpUtil.getRequest().getParameter("is_fltr") == null) {
				dateDebut = DateUtil.stringToDate(httpUtil.getParameter("dateDebut"));
				dateFin = DateUtil.stringToDate(httpUtil.getParameter("dateFin"));
			}
			
			if(dateDebut == null) {
				dateDebut = new Date();
				dateFin = new Date();
				httpUtil.getDate("dateDebut").setValue(dateDebut);
				httpUtil.getDate("dateFin").setValue(dateDebut);
			}
			
			if(httpUtil.getRequest().getParameter("prev") != null) {
				dateDebut = DateUtil.addSubstractDate(dateDebut, TIME_ENUM.DAY, -1);
				dateFin = DateUtil.addSubstractDate(dateFin, TIME_ENUM.DAY, -1);
			} else if(httpUtil.getRequest().getParameter("next") != null) {
				dateDebut = DateUtil.addSubstractDate(dateDebut, TIME_ENUM.DAY, 1);
				dateFin = DateUtil.addSubstractDate(dateFin, TIME_ENUM.DAY, 1);
			}
			
			httpUtil.setRequestAttribute("dateDebut", dateDebut);
			httpUtil.setRequestAttribute("dateFin", dateFin);
			//
			mapData  = journeeService.getRepartitionVenteStock(null, dateDebut, dateFin);
		} else {
			Long journeeId = (Long)httpUtil.getMenuAttribute("journeeId");
			mapData = journeeService.getRepartitionVenteStock(journeeId, null, null);
		}
		if(mapData == null){
			mapData = new HashMap<>();
		}
		//-----------------------------------------------------------
		httpUtil.setRequestAttribute("dataRepartion", mapData);
		httpUtil.setDynamicUrl("/domaine/caisse/dashboard/repartition_article_vente_stock.jsp");
	}
	
	/**
	 * @param httpUtil
	 */
	private void find_repartition2(ActionUtil httpUtil){
		httpUtil.setDynamicUrl("/domaine/caisse/dashboard/repartition_article_vente_mnu.jsp");
		
		if(httpUtil.getParameter("tp") != null) {
			httpUtil.setMenuAttribute("IS_MNU", true);
		}
		Map<String, CaisseMouvementArticlePersistant> mapData = null;
		
		List<FamillePersistant> listFamille = (List<FamillePersistant>) familleService.getListeFamille("ST", true, false);
		httpUtil.setRequestAttribute("list_famille", listFamille);
		
		Long familleId = null;
		if(StringUtil.isEmpty(httpUtil.getParameter("curr_famille"))) {
//			if(!listFamille.isEmpty()) {
//				familleId = listFamille.get(0).getId();
//			}
		} else {
			familleId = httpUtil.getLongParameter("curr_famille");
		}
		httpUtil.setRequestAttribute("curr_famille", familleId);
		httpUtil.setRequestAttribute("familleId", familleId);
		
		//----------------------------- Date -------------------------
		if(httpUtil.getMenuAttribute("IS_MNU") != null) {
			Date dateDebut = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateDebut"));
			Date dateFin = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateFin"));
			//
			if(httpUtil.getRequest().getParameter("is_fltr") == null) {
				dateDebut = DateUtil.stringToDate(httpUtil.getParameter("dateDebut"));
				dateFin = DateUtil.stringToDate(httpUtil.getParameter("dateFin"));
			}
			
			if(dateDebut == null) {
				dateDebut = new Date();
				dateFin = new Date();
				httpUtil.getDate("dateDebut").setValue(dateDebut);
				httpUtil.getDate("dateFin").setValue(dateDebut);
			}
			
			if(httpUtil.getRequest().getParameter("prev") != null) {
				dateDebut = DateUtil.addSubstractDate(dateDebut, TIME_ENUM.DAY, -1);
				dateFin = DateUtil.addSubstractDate(dateFin, TIME_ENUM.DAY, -1);
			} else if(httpUtil.getRequest().getParameter("next") != null) {
				dateDebut = DateUtil.addSubstractDate(dateDebut, TIME_ENUM.DAY, 1);
				dateFin = DateUtil.addSubstractDate(dateFin, TIME_ENUM.DAY, 1);
			}
			
			httpUtil.setRequestAttribute("dateDebut", dateDebut);
			httpUtil.setRequestAttribute("dateFin", dateFin);
			
			JourneePersistant journeeDebut = journeeService.getJourneeOrNextByDate(dateDebut);
	    	JourneePersistant journeeFin = journeeService.getJourneeOrPreviousByDate(dateFin);
	    	
	    	if(journeeDebut == null) {
//	    		MessageService.addGrowlMessage("", "Aucune journée ne correspond à ces dates.");
	    		return;
	    	}
			//
			mapData = journeeService.getRepartitionVenteArticle(journeeDebut, journeeFin, familleId, false);
		} else {
			Long journeeId = (Long)httpUtil.getMenuAttribute("journeeId");
			JourneePersistant journeeDebut = journeeService.findById(JourneePersistant.class, journeeId);
	    	JourneePersistant journeeFin = journeeService.findById(JourneePersistant.class, journeeId);
	    	
			mapData = journeeService.getRepartitionVenteArticle(journeeDebut, journeeFin, familleId, false);
		}
		if(mapData == null){
			mapData = new HashMap<>();
		}
		//-----------------------------------------------------------
		httpUtil.setRequestAttribute("dataRepartion", mapData);
	}
	
	/**
	 * @param httpUtil
	 */
	private void find_repartition_restau(ActionUtil httpUtil){
		boolean isParPoste = httpUtil.getParameter("is_poste") != null;
		
		if(httpUtil.getParameter("tp") != null) {
			httpUtil.setMenuAttribute("IS_MNU", true);
		}
		Map<String, CaisseMouvementArticlePersistant> mapData = null;
		
		List<FamillePersistant> listFamille = (List<FamillePersistant>) familleService.getListeFamille("CU", true, false);
		// Ajout menu
		FamillePersistant fpf = new FamilleCuisinePersistant();
		fpf.setId(Long.valueOf(-999));
		fpf.setB_left(1);
		fpf.setB_right(2);
		fpf.setCode("MNU");
		fpf.setLibelle("Tous les Menus");
		fpf.setLevel(2);
		listFamille.add(0, fpf);
		
		// Ajout famille
		fpf = new FamilleCuisinePersistant();
		fpf.setId(Long.valueOf(-888));
		fpf.setB_left(1);
		fpf.setB_right(2);
		fpf.setCode("FAM");
		fpf.setLibelle("Toutes les Familles");
		fpf.setLevel(2);
		listFamille.add(1, fpf);
				
		//
		httpUtil.setRequestAttribute("list_famille", listFamille);
		
		Long familleId = null;
		if(StringUtil.isNotEmpty(httpUtil.getParameter("curr_famille"))) {
			familleId = httpUtil.getLongParameter("curr_famille");
		}
		httpUtil.setRequestAttribute("curr_famille", familleId);
		httpUtil.setRequestAttribute("familleId", familleId);
		
		//----------------------------- Date -------------------------
		if(httpUtil.getMenuAttribute("IS_MNU") != null) {
			Date dateDebut = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateDebut"));
			Date dateFin = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateFin"));
			//
			if(httpUtil.getRequest().getParameter("is_fltr") == null) {
				dateDebut = DateUtil.stringToDate(httpUtil.getParameter("dateDebut"));
				dateFin = DateUtil.stringToDate(httpUtil.getParameter("dateFin"));
			}
			
			if(dateDebut == null) {
				dateDebut = new Date();
				dateFin = new Date();
				httpUtil.getDate("dateDebut").setValue(dateDebut);
				httpUtil.getDate("dateFin").setValue(dateDebut);
			}
			
			if(httpUtil.getRequest().getParameter("prev") != null) {
				dateDebut = DateUtil.addSubstractDate(dateDebut, TIME_ENUM.DAY, -1);
				dateFin = DateUtil.addSubstractDate(dateFin, TIME_ENUM.DAY, -1);
			} else if(httpUtil.getRequest().getParameter("next") != null) {
				dateDebut = DateUtil.addSubstractDate(dateDebut, TIME_ENUM.DAY, 1);
				dateFin = DateUtil.addSubstractDate(dateFin, TIME_ENUM.DAY, 1);
			}
			
			httpUtil.setRequestAttribute("dateDebut", dateDebut);
			httpUtil.setRequestAttribute("dateFin", dateFin);
			
			JourneePersistant journeeDebut = journeeService.getJourneeOrNextByDate(dateDebut);
	    	JourneePersistant journeeFin = journeeService.getJourneeOrPreviousByDate(dateFin);
	    	
	    	if(journeeDebut == null) {
	    		journeeDebut = journeeFin;
	    	}
			
//	    	if(journeeDebut == null) {
//	    		MessageService.addGrowlMessage("", "Aucune journée ne correspond à ces dates.");
//	    		return;
//	    	}
	    	
			//
			if(isParPoste){
				Map<String, Map> mapDataResult = journeeService.getRepartitionVenteArticleParPosteCuisine(journeeDebut, journeeFin, familleId);
				httpUtil.setRequestAttribute("dataRepartion", mapDataResult);
			} else{
				mapData = journeeService.getRepartitionVenteArticle(journeeDebut, journeeFin, familleId, false);
				httpUtil.setRequestAttribute("dataRepartion", mapData);
			}
		} else {
			Long journeeId = (Long)httpUtil.getMenuAttribute("journeeId");
			JourneePersistant journeeDebut = journeeService.findById(JourneePersistant.class, journeeId);
	    	JourneePersistant journeeFin = journeeService.findById(JourneePersistant.class, journeeId);
			
			if(isParPoste){
				Map<String, Map> mapDataResult = journeeService.getRepartitionVenteArticleParPosteCuisine(journeeDebut, journeeFin, familleId);
				httpUtil.setRequestAttribute("dataRepartion", mapDataResult);
			} else{
				mapData = journeeService.getRepartitionVenteArticle(journeeDebut, journeeFin, familleId, false);
				httpUtil.setRequestAttribute("dataRepartion", mapData);
			}
		}
		//-----------------------------------------------------------
		
		
		if(isParPoste){
			httpUtil.setDynamicUrl("/domaine/caisse_restau/back/repartition_article_vente_poste.jsp");
		} else{
			httpUtil.setDynamicUrl("/domaine/caisse_restau/back/repartition_article_vente_mnu.jsp");
		}
	}
	
	/**
	 * @param httpUtil
	 */
	public void annulerCommande(ActionUtil httpUtil) {
		Long mvmId = httpUtil.getLongParameter("mvm");
		CaisseMouvementPersistant currCmd = (CaisseMouvementPersistant) familleService.findById(CaisseMouvementPersistant.class, mvmId);
		
        caisseWebService.annulerMouvementCaisse(currCmd.getId(), ContextAppli.getUserBean());
        if(currCmd.getOpc_client() != null){
        	portefeuilelService2.majSoldePortefeuilleMvm(currCmd.getOpc_client().getId(), "CLI");
        }
        MessageService.addGrowlMessage(MSG_TYPE.SUCCES, "Commande annulée", "La commande est annulée avec succès.");
        
        if(httpUtil.getMenuAttribute("IS_MENU_CMLIENT") == null){
        	find_mouvement(httpUtil);	
        } else{
        	httpUtil.setDynamicUrl("pers.client.find_mvm_client");
        }
	}
	
	/**
	 * @param httpUtil
	 */
	public void edit_mouvement(ActionUtil httpUtil){
		httpUtil.setFormReadOnly();
		Long caisseMvmId = httpUtil.getWorkIdLong();
		if(caisseMvmId != null) {
			CaisseMouvementBean viewBean = caisseMouvementService.findById(caisseMvmId);
			httpUtil.setRequestAttribute("caisseMouvement", viewBean);
		} else {
			MessageService.addGrowlMessage("", "Aucun mouvement sélectionné");
			return;
		}
		
		httpUtil.setDynamicUrl("/domaine/caisse/back-office/mouvement_caisse_edit.jsp");
	}
	
	/**
	 * @param httpUtil
	 */
	public void edit_mvm_marge(ActionUtil httpUtil){
		Long userId = httpUtil.getLongParameter("user");
		Long journeeId = httpUtil.getLongParameter("jour");
		boolean isMois = (httpUtil.getParameter("isMois") != null);
		List<CaisseMouvementArticlePersistant> listMvm = null;
		//
		if(isMois) {
			Date dateDebut = (Date) httpUtil.getMenuAttribute("dtDebCh");
			Date dateFin = (Date) httpUtil.getMenuAttribute("dtFinCh");
			listMvm = caisseMouvementService.getMvmMargeEmploye(null, dateDebut, dateFin, userId);
		} else {
			listMvm = caisseMouvementService.getMvmMargeEmploye(journeeId, null, null, userId);
		}
		
		httpUtil.setRequestAttribute("listMvm", listMvm);
		
		httpUtil.setDynamicUrl("/domaine/caisse/back-office/caisse_marge_articles_journee.jsp");
	}
	
	/**
	 * @param httpUtil
	 */
	public void edit_date_journee(ActionUtil httpUtil) {
		Long workId = httpUtil.getWorkIdLong();
		
		if(StringUtil.isEmpty(httpUtil.getParameter("init"))) {
			Date date_journee = DateUtil.stringToDate(httpUtil.getParameter("journee.date_journee"));
			journeeService.majDateJournee(workId, date_journee);
			
			work_find(httpUtil);
			MessageService.addGrowlMessage(MSG_TYPE.SUCCES, "", "Mise à jour effectuée avec succès.");
		} else {
			JourneeBean journeeBean = journeeService.findById(workId);
			httpUtil.setViewBean(journeeBean);
			httpUtil.setRequestAttribute("isUpdateDate", true);
			
			httpUtil.setDynamicUrl("/domaine/caisse/back-office/journee_ouverture_edit.jsp");
		}
	}
	
	/**
	 * @param httpUtil
	 */
	public void work_post(ActionUtil httpUtil){
		if(httpUtil.getRequestAttribute("isCaisse") != null){
			httpUtil.setDynamicUrl("caisse.caisse.work_find");
		}
		JourneePersistant lastJournee = journeeService.getLastJournee();
		httpUtil.setRequestAttribute("lastJournee", lastJournee);
	}
	
	public void deplace_cmds(ActionUtil httpUtil){
		if(httpUtil.getParameter("isDep") != null){
			Long shiftTargetId = httpUtil.getLongParameter("shift_target");
			Long[] checkedTr = httpUtil.getCheckedElementsLong("list_caisseMouvement");
			
			if(checkedTr == null || checkedTr.length == 0){
				MessageService.addGrowlMessage("", "Veuillez sélectionner au moins un mouvement.");
				return;
			}
			
			Set<Long>[] listSetJrnShiftIds = caisseService.deplacerMouvement(shiftTargetId, checkedTr);
			caisseService.updateChiffresShiftJour(listSetJrnShiftIds[0], listSetJrnShiftIds[1]);
			MessageService.addGrowlMessage(MSG_TYPE.SUCCES, "", "Les mouvements ont été déplacés vers le shift cible.");
			
			find_mouvement(httpUtil);
		} else{
			httpUtil.setRequestAttribute("listShift", caisseService.findAll(CaisseJourneePersistant.class, Order.desc("date_ouverture")));
			httpUtil.setRequestAttribute("listJournee", caisseService.findAll(JourneePersistant.class, Order.desc("date_journee")));
			
			find_mouvement(httpUtil);
			httpUtil.setMenuAttribute("IS_MODE_MOUVE", true);
		}
	}
	
	public void deplacerShifts(ActionUtil httpUtil){
		Long caisseJourneeId = httpUtil.getLongParameter("cjId");
		Long journeeId = httpUtil.getLongParameter("journee_target");
		
		if(caisseJourneeId == null || journeeId == null) {
			find_mouvement(httpUtil);
			return;
		}
		Long currJournee = (Long)httpUtil.getMenuAttribute("journeeId");
		CaisseJourneePersistant caisseJP = caisseService.findById(CaisseJourneePersistant.class, caisseJourneeId);
		caisseJP.setOpc_journee(caisseService.findById(JourneePersistant.class, journeeId));
		caisseService.mergeEntity(caisseJP);
		
		caisseService.updateChiffresShiftJour(journeeId, caisseJourneeId);
		caisseService.updateChiffresShiftJour(currJournee, caisseJourneeId);
		MessageService.addGrowlMessage(MSG_TYPE.SUCCES, "", "Le shift est déplacé vers la journée cible.");
		
		find_mouvement(httpUtil);
	}
	
	//********* STOCK ******************/
	public void editTrMvm(ActionUtil httpUtil){
		Long mvmId = httpUtil.getLongParameter("art");
		
		IMouvementService mvmS = (IMouvementService) ServiceUtil.getBusinessBean(IMouvementService.class);
		MouvementBean mvmBean = mvmS.findById(mvmId);
		if(mvmBean == null) { //si on vient depuis pointage chèques
			PaiementPersistant paiementP = (PaiementPersistant) mvmS.findById(PaiementPersistant.class, mvmId);
			mvmBean = mvmS.findById(paiementP.getElementId());
		}
		
		List<MouvementArticlePersistant> listDetail = new ArrayList<>();
		listDetail = mvmBean.getList_article();
		
		httpUtil.setRequestAttribute("listArtDetail", listDetail);
		httpUtil.setRequestAttribute("mouvementBean", mvmBean);
		
		httpUtil.setDynamicUrl("/domaine/caisse/back-office/journee_mvm_stock_tr.jsp");
	}
	
	public void recalculChiffresMvmJournee(ActionUtil httpUtil){
		Long journeeId = (Long)httpUtil.getMenuAttribute("journeeId");
		String rapport = journeeService.recalculChiffresMvmJournee(journeeId);
		
		Logger LOGGER = Logger.getLogger(JourneeAction.class);
		LOGGER.info(rapport);
		
		JourneePersistant journeeP = caisseService.findById(JourneePersistant.class, journeeId);
		// Shifts
		List<CaisseJourneePersistant> list_caisse_journee = new ArrayList<>(journeeP.getList_caisse_journee());
		for(CaisseJourneePersistant jc : list_caisse_journee) {
			caisseService.updateChiffresShiftJour(journeeId, jc.getId());
		}
		
		MessageService.addBannerMessage(MSG_TYPE.SUCCES, rapport.replaceAll("\n", "<br>"));
		
		find_mouvement(httpUtil);
	}
	
	public void find_mvmStock(ActionUtil httpUtil){
		JourneePersistant journeeP = journeeService.findById(JourneePersistant.class, (Long)httpUtil.getMenuAttribute("journeeId"));
		httpUtil.setRequestAttribute("listEmplacement", mouvementService.findAll(EmplacementPersistant.class, Order.asc("titre")));
		// Ajouter le paramétre dans la requête
		RequestTableBean cplxTable = getTableBean(httpUtil, "list_mouvement");
		Map<String, Object> formCriterion = cplxTable.getFormBean().getFormCriterion();
		formCriterion.put("type", ContextAppli.TYPE_MOUVEMENT_ENUM.vc.toString());
		//----------------------------- Date -------------------------
		Date dateDebut = DateUtil.getStartOfDay(journeeP.getDate_journee());
		Date dateFin = DateUtil.getEndOfDay(journeeP.getDate_journee());
		
		formCriterion.put("dateDebut", dateDebut);
		formCriterion.put("dateFin", dateFin);
		
		List<MouvementPersistant> listData = (List<MouvementPersistant>) mouvementService.findByCriteriaByQueryId(cplxTable, "mouvement_find");
		mouvementService.refreshEntities(listData);
		//
		// Calcul du total montants sans pagination
	   	BigDecimal totalHtAll = null, totalTtcAll = null, totalRemiseAll = null, totalTva = null;
	   	List<MouvementPersistant> listDataAll = (List<MouvementPersistant>) mouvementService.findByCriteriaByQueryId(cplxTable, "mouvement_find", false);
		//
	   	
	   	if(listDataAll.size() > 0) {
		   	for (MouvementPersistant mvmStockViewP : listDataAll) {
	   			totalHtAll = BigDecimalUtil.add(totalHtAll, mvmStockViewP.getMontant_ht());
		   		totalTtcAll = BigDecimalUtil.add(totalTtcAll, mvmStockViewP.getMontant_ttc());
		   		totalTva = BigDecimalUtil.add(totalTva, mvmStockViewP.getMontant_tva());
		   	}
		}
	   	httpUtil.setRequestAttribute("totalHt", totalHtAll);
	   	httpUtil.setRequestAttribute("totalTtc", totalTtcAll);
	   	httpUtil.setRequestAttribute("totalTva", totalTva);
	   	
	   	// Associer aux mouvements stock
	   	Map<Long, CaisseMouvementPersistant> mapMvmStockCaisse = journeeService.getMapMvmStockCaisse(journeeP.getId());
	   	
	   	httpUtil.setRequestAttribute("mapMvmStockCaisse", mapMvmStockCaisse);
		httpUtil.setRequestAttribute("list_mouvement", listData);
		
		httpUtil.setDynamicUrl("/domaine/caisse/back-office/journee_mvm_stock.jsp");
	}
}

class SortByQuantite implements Comparator<CaisseMouvementArticlePersistant>{
	public int compare(CaisseMouvementArticlePersistant o1, CaisseMouvementArticlePersistant o2) {
		return o2.getQuantite().compareTo(o1.getQuantite()) ;
	}
}
