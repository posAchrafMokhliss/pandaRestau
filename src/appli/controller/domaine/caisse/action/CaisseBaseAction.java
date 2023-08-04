package appli.controller.domaine.caisse.action;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.hibernate.criterion.Order;

import appli.controller.domaine.administration.ParametrageRightsConstantes;
import appli.controller.domaine.caisse.bean.CaisseBean;
import appli.controller.domaine.caisse.bean.CaisseMouvementBean;
import appli.controller.domaine.util_erp.ContextAppli;
import appli.controller.domaine.util_erp.ContextAppli.APPLI_ENV;
import appli.controller.domaine.util_erp.ContextAppli.SOFT_ENVS;
import appli.controller.domaine.util_erp.ContextAppli.TYPE_CAISSE_ENUM;
import appli.model.domaine.administration.persistant.UserPersistant;
import appli.model.domaine.administration.service.IUserService;
import appli.model.domaine.caisse.service.ICaisseMouvementService;
import appli.model.domaine.caisse.service.ICaisseService;
import appli.model.domaine.caisse.service.IJourneeService;
import appli.model.domaine.caisse.service.impl.HistoriqueEcartPDF;
import appli.model.domaine.caisse.service.impl.HistoriqueLivraisonPDF;
import appli.model.domaine.caisse.service.impl.HistoriqueReductionPDF;
import appli.model.domaine.personnel.persistant.ClientPersistant;
import appli.model.domaine.personnel.persistant.EmployePersistant;
import appli.model.domaine.personnel.persistant.OffrePersistant;
import appli.model.domaine.personnel.service.IEmployeService;
import appli.model.domaine.stock.persistant.ArticlePersistant;
import appli.model.domaine.stock.persistant.FamillePersistant;
import appli.model.domaine.stock.service.IArticleService;
import appli.model.domaine.stock.service.IEmplacementService;
import appli.model.domaine.stock.service.IFamilleService;
import appli.model.domaine.vente.persistant.CaisseJourneePersistant;
import appli.model.domaine.vente.persistant.CaisseMouvementPersistant;
import appli.model.domaine.vente.persistant.CaissePersistant;
import appli.model.domaine.vente.persistant.JourneePersistant;
import framework.component.complex.table.RequestTableBean;
import framework.controller.ActionBase;
import framework.controller.ActionUtil;
import framework.controller.Context;
import framework.controller.ContextGloabalAppli;
import framework.controller.annotation.WorkForward;
import framework.model.beanContext.AbonnementBean;
import framework.model.common.constante.ProjectConstante.MSG_TYPE;
import framework.model.common.service.MessageService;
import framework.model.common.util.BigDecimalUtil;
import framework.model.common.util.BooleanUtil;
import framework.model.common.util.DateUtil;
import framework.model.common.util.DateUtil.TIME_ENUM;
import framework.model.common.util.StrimUtil;
import framework.model.common.util.StringUtil;

public class CaisseBaseAction extends ActionBase {
	@Inject
	private ICaisseService caisseService;
	@Inject
	private IJourneeService journeeService;
	@Inject
	private IEmplacementService emplacementService;
	@Inject
	private ICaisseMouvementService caisseMouvementService;
	@Inject
	private IArticleService articleService;
	@Inject
	private IEmployeService employeService;
	@Inject
	private IFamilleService familleService;
	@Inject
	private IUserService userService;
	
	public void work_init(ActionUtil httpUtil){
		httpUtil.setRequestAttribute("listeEmplacement", emplacementService.getListEmplacementActifs());

		Long caisseId = httpUtil.getWorkIdLong();
		if(caisseId != null){
			httpUtil.setMenuAttribute("caisseId", caisseId);
			CaisseBean caisseP = caisseService.findById(caisseId);
			if(caisseP != null){
				httpUtil.setMenuAttribute("typeCaisse", caisseP.getType_ecran());
			}
		}
		
		if(!httpUtil.isCreateAction()){
			if(caisseId == null){
				caisseId = (Long)httpUtil.getMenuAttribute("caisseId");
			}
		}
		// Gérer le retour sur cet onglet
		if(caisseId != null && !httpUtil.isCrudOperation()){
			loadBean(httpUtil, caisseId);
		}
	
		if(httpUtil.isEditionPage()){
			if(httpUtil.isCreateAction()){
				httpUtil.removeMenuAttribute("caisseId");
			}
			
			// Liste des imprimantes
			httpUtil.setRequestAttribute("list_imprimante", ParametrageRightsConstantes.getListPrinters());
		}
		
		if(httpUtil.getViewBean() != null ) {
        	CaisseBean caisseBean = (CaisseBean)httpUtil.getViewBean();
        	
        	if(httpUtil.isCrudOperation()) {
        		String imprimante = StringUtil.getStringDelimFromStringArray(caisseBean.getImprimante_array(), "|");
        		caisseBean.setImprimantes(imprimante);
        		
        		String familles = StringUtil.getStringDelimFromStringArray(caisseBean.getFamilles_balance_array(), "|");
        		caisseBean.setFamille_balance("|"+familles+"|");
        	} else {
	        	if(StringUtil.isNotEmpty(caisseBean.getImprimantes())) {
		        	String[] imprArray = StringUtil.getArrayFromStringDelim(caisseBean.getImprimantes(), "|");
		        	caisseBean.setImprimante_array(imprArray);
		        	httpUtil.setRequestAttribute("imprArray", imprArray);
	        	}
	        	if(StringUtil.isNotEmpty(caisseBean.getFamille_balance())) {
		        	String[] familles = StringUtil.getArrayFromStringDelim(caisseBean.getFamille_balance(), "|");
		        	caisseBean.setFamilles_balance_array(familles);
		        	httpUtil.setRequestAttribute("famBalArray", familles);
	        	}
        	}
        }
		
		String context = StrimUtil.getGlobalConfigPropertie("context.soft");
		boolean isRestau = SOFT_ENVS.restau.toString().equals(context);
		boolean isMarket = SOFT_ENVS.market.toString().equals(context);		
		boolean isPharma = SOFT_ENVS.pharma.toString().equals(context);
		
		AbonnementBean abonnementBean = ContextGloabalAppli.getAbonementBean();
		List<String[]> terminaux = new ArrayList<>();
		
		if(isMarket || isPharma || isRestau){
			terminaux.add(new String[]{TYPE_CAISSE_ENUM.CAISSE.toString(), TYPE_CAISSE_ENUM.CAISSE.getLibelle()});
			if(isRestau && BooleanUtil.isTrue(abonnementBean.isSatCaisseAuto())) {
				terminaux.add(new String[]{TYPE_CAISSE_ENUM.CAISSE_CLIENT.toString(), TYPE_CAISSE_ENUM.CAISSE_CLIENT.getLibelle()});
			}
			if(BooleanUtil.isTrue(abonnementBean.isSatAffCaisse())){
				terminaux.add(new String[]{TYPE_CAISSE_ENUM.AFFICHEUR.toString(), TYPE_CAISSE_ENUM.AFFICHEUR.getLibelle()});
			}
			if(BooleanUtil.isTrue(abonnementBean.isSatAffClient())) {
				terminaux.add(new String[]{TYPE_CAISSE_ENUM.AFFICLIENT.toString(), TYPE_CAISSE_ENUM.AFFICLIENT.getLibelle()});
			}
			if(isMarket || isPharma){
				if(BooleanUtil.isTrue(abonnementBean.isSatLecteurBarre())){
					terminaux.add(new String[]{TYPE_CAISSE_ENUM.LECTEUR.toString(), TYPE_CAISSE_ENUM.LECTEUR.getLibelle()});
				}
			} else if(isRestau) {
				if(BooleanUtil.isTrue(abonnementBean.isSatCuisine())){
					terminaux.add(new String[]{TYPE_CAISSE_ENUM.PILOTAGE.toString(), TYPE_CAISSE_ENUM.PILOTAGE.getLibelle()});
					terminaux.add(new String[]{TYPE_CAISSE_ENUM.CUISINE.toString(), TYPE_CAISSE_ENUM.CUISINE.getLibelle()});
					terminaux.add(new String[]{TYPE_CAISSE_ENUM.PRESENTOIRE.toString(), TYPE_CAISSE_ENUM.PRESENTOIRE.getLibelle()});
				}
			}
			if(BooleanUtil.isTrue(abonnementBean.isSatBalance())){
				terminaux.add(new String[]{TYPE_CAISSE_ENUM.BALANCE.toString(), TYPE_CAISSE_ENUM.BALANCE.getLibelle()});
			}
			
			String[][] data = new String[terminaux.size()][2];
			int idx = 0;
			for (String[] val : terminaux) {
				data[idx] = val; idx++;
			}
			
			httpUtil.setRequestAttribute("listType", data);
		}
		// Caisse
		List<CaissePersistant> listCaisse = caisseService.getListCaisseActive(ContextAppli.TYPE_CAISSE_ENUM.CAISSE.toString(), true);
		httpUtil.setRequestAttribute("listCaisseNoAfficheur", listCaisse);
		
		if(isRestau){
			List<FamillePersistant> listFamilleBalance = familleService.getListeFamille("CU", true, true);
			httpUtil.setRequestAttribute("listFamilleBalance", listFamilleBalance);
		} else{
			List<FamillePersistant> listFamilleBalance = familleService.getListeFamille("ST", true, true);
			httpUtil.setRequestAttribute("listFamilleBalance", listFamilleBalance);
		}
		
		JourneePersistant lastJournee = journeeService.getLastJournee();
		httpUtil.setRequestAttribute("lastJournee", lastJournee);
		// Variable pour savoir si on est dans le menu caisse
		httpUtil.setMenuAttribute("isCaisse", true);
	}
	
	@Override
	public void work_find(ActionUtil httpUtil) {
		RequestTableBean cplxTable = getTableBean(httpUtil, "list_caisse");
		
		String req = "from CaissePersistant caisse where 1=1 "
		+ getFilterStateRequest(httpUtil, "is_desactive");
		req += "order by caisse.type_ecran, caisse.reference desc";
		
		List<CaissePersistant>	listData = (List<CaissePersistant>) caisseService.findByCriteria(cplxTable, req);
		
	   	httpUtil.setRequestAttribute("list_caisse", listData);
		httpUtil.setDynamicUrl("/domaine/caisse//back-office/caisse_list.jsp");
	}
	
	/**
	 * @param httpUtil
	 */
	public void init_ouverture_caisse(ActionUtil httpUtil){
		Long caisseId = httpUtil.getWorkIdLong();
		httpUtil.setRequestAttribute("caisseId", caisseId);
		
		httpUtil.setDynamicUrl("/domaine/caisse/normal/caisse_ouverture_popup.jsp");
	}

	@Override 
	public void work_merge(ActionUtil httpUtil) {
		CaisseBean caisseBean = (CaisseBean)httpUtil.getViewBean();
		Long workId = httpUtil.getWorkIdLong();    	
    	String env = (String) httpUtil.getUserAttribute("CURRENT_ENV");
    	if(env.equals(APPLI_ENV.cais.toString())) {
    		String[] imprimantes = caisseBean.getImprimante_array();
    		
    		caisseBean = caisseService.findById(workId);
    		
    		String imprimante = StringUtil.getStringDelimFromStringArray(imprimantes, "|");
    		caisseBean.setImprimantes(imprimante);
    		caisseService.update(caisseBean);
    		
    		MessageService.getGlobalMap().put("CURRENT_CAISSE", caisseBean);
			httpUtil.writeResponse("REDIRECT:");
			return;
		}
		
		caisseBean.setId(workId);
		//
		if(caisseBean.getId() != null){
			CaissePersistant caisseDbP = caisseService.findById(caisseBean.getId());
			
			caisseBean.setNbr_max_cmd(caisseDbP.getNbr_max_cmd());
			caisseBean.setArticles_cmd(caisseDbP.getArticles_cmd());
			caisseBean.setFamilles_cmd(caisseDbP.getFamilles_cmd());
			caisseBean.setMenus_cmd(caisseDbP.getMenus_cmd());
			caisseBean.setIs_auto_cmd(caisseDbP.getIs_auto_cmd());
			caisseBean.setIs_desactive(caisseDbP.getIs_desactive());
		}
		caisseBean.setOpc_abonne(ContextAppli.getAbonneBean());
		caisseBean.setOpc_societe(ContextAppli.getSocieteBean());
		caisseBean.setOpc_etablissement(ContextAppli.getEtablissementBean());
		
		super.work_merge(httpUtil);
	}
	
	/**
	 * @param httpUtil
	 */
	public void find_reduction(ActionUtil httpUtil){
		Date dateRef = null;
		JourneePersistant lastJrn = journeeService.getLastJournee();
		if(lastJrn != null){
			dateRef = lastJrn.getDate_journee();
		} else{
			dateRef = new Date();
		}
		
		// Initialiser les listes pour les filtres
		initDataListFilter(httpUtil);
		
		httpUtil.setRequestAttribute("listOffre", caisseService.findAll(OffrePersistant.class, Order.asc("libelle")));
		
		RequestTableBean cplxTable = getTableBean(httpUtil, "list_mouvement_reduction");
		Map<String, Object> formCriterion = cplxTable.getFormBean().getFormCriterion();
		boolean isFilterAct = StringUtil.isTrue(httpUtil.getRequest().getParameter("is_filter_act"));
		
		//----------------------------- Date -------------------------
		Date dateDebut = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateDebut"));
		Date dateFin = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateFin"));
		//
		if(httpUtil.getRequest().getParameter("is_fltr") == null) {
			dateDebut = DateUtil.stringToDate(httpUtil.getParameter("dateDebut"));
			dateFin = DateUtil.stringToDate(httpUtil.getParameter("dateFin"));
		}
		
		if(dateDebut == null) {
			dateDebut = (httpUtil.getMenuAttribute("dateDebut")==null ? dateRef : (Date)httpUtil.getMenuAttribute("dateDebut"));
			dateFin = (httpUtil.getMenuAttribute("dateFin")==null ? dateRef : (Date)httpUtil.getMenuAttribute("dateFin"));
			httpUtil.getDate("dateDebut").setValue(dateDebut);
			httpUtil.getDate("dateFin").setValue(dateDebut);
		}
		
		if(httpUtil.getParameter("prev") != null) {
			dateDebut = DateUtil.addSubstractDate(dateDebut, TIME_ENUM.DAY, -1);
			dateFin = DateUtil.addSubstractDate(dateFin, TIME_ENUM.DAY, -1);
		} else if(httpUtil.getParameter("next") != null) {
			dateDebut = DateUtil.addSubstractDate(dateDebut, TIME_ENUM.DAY, 1);
			dateFin = DateUtil.addSubstractDate(dateFin, TIME_ENUM.DAY, 1);
		}
		
		dateDebut = DateUtil.getStartOfDay(dateDebut);
		dateFin = DateUtil.getEndOfDay(dateFin);
		
		httpUtil.setRequestAttribute("dateDebut", dateDebut);
		httpUtil.setRequestAttribute("dateFin", dateFin);
		httpUtil.setMenuAttribute("dateDebut", dateDebut);
		httpUtil.setMenuAttribute("dateFin", dateFin);
		
		JourneePersistant journeeDebut = journeeService.getJourneeOrNextByDate(dateDebut);
    	JourneePersistant journeeFin = journeeService.getJourneeOrPreviousByDate(dateFin);
		
		if(!isFilterAct){
			formCriterion.put("dateDebut", (journeeDebut!=null?journeeDebut.getId():null));
			formCriterion.put("dateFin", (journeeFin!=null?journeeFin.getId():null));
		} else{
			formCriterion.remove("dateDebut");
			formCriterion.remove("dateFin");
		}
		//-----------------------------------------------------------
		List<CaisseMouvementPersistant> listCaisseMouvement = (List<CaisseMouvementPersistant>) caisseMouvementService.findByCriteriaByQueryId(cplxTable, "mouvementReduction_find");
		httpUtil.setRequestAttribute("list_caisseMouvement", listCaisseMouvement);
	
		CaisseMouvementPersistant jp = new CaisseMouvementPersistant();
		List<CaisseMouvementPersistant> listCaisseMouvementAll = (List<CaisseMouvementPersistant>) caisseMouvementService.findByCriteriaByQueryId(cplxTable, "mouvementReduction_find", false);
		httpUtil.setRequestAttribute("list_livraisonMouvementNoPage", listCaisseMouvementAll);
		//
	   	for (CaisseMouvementPersistant jvp : listCaisseMouvementAll) {
	   		jp.setMtt_commande(BigDecimalUtil.add(jp.getMtt_commande(), jvp.getMtt_commande()));
	   		jp.setMtt_reduction(BigDecimalUtil.add(jp.getMtt_reduction(), jvp.getMtt_reduction()));
	   		jp.setMtt_art_reduction(BigDecimalUtil.add(jp.getMtt_art_reduction(), jvp.getMtt_art_reduction()));
	   		jp.setMtt_art_offert(BigDecimalUtil.add(jp.getMtt_art_offert(), jvp.getMtt_art_offert()));
	   		jp.setMtt_commande_net(BigDecimalUtil.add(jp.getMtt_commande_net(), jvp.getMtt_commande_net()));
		}
	   	httpUtil.setRequestAttribute("mvm_total", jp);
	   	
		httpUtil.setDynamicUrl("/domaine/caisse//back-office/mouvements_reduit_list.jsp");
	}
	
	/*@WorkForward(useBean=true, useFormValidator=false, bean=CaisseBean.class)
	public void find_statut_commande(ActionUtil httpUtil) {
		CaisseBean caisseBean = (CaisseBean) httpUtil.getViewBean();
		
		if(caisseBean == null){
			caisseBean = new CaisseBean();
		}
		
		Date dateRef = null;
		JourneePersistant lastJrn = journeeService.getLastJournee();
		if(lastJrn != null){
			dateRef = lastJrn.getDate_journee();
		} else{
			dateRef = new Date();
		}
		
		//----------------------------- Date -------------------------
		Date dateDebut = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateDebut"));
		Date dateFin = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateFin"));
		//
		boolean isFilter = httpUtil.getRequest().getParameter("is_fltr") != null;
		if(!isFilter) {
			dateDebut = DateUtil.stringToDate(httpUtil.getParameter("dateDebut"));
			dateFin = DateUtil.stringToDate(httpUtil.getParameter("dateFin"));
		}
		
		if(dateDebut == null) {
			dateDebut = dateRef;
			dateFin = dateRef;
			httpUtil.getDate("dateDebut").setValue(dateDebut);
			httpUtil.getDate("dateFin").setValue(dateDebut);
		}
		
		if(httpUtil.getParameter("prev") != null) {
			dateDebut = DateUtil.addSubstractDate(dateDebut, TIME_ENUM.DAY, -1);
			dateFin = DateUtil.addSubstractDate(dateFin, TIME_ENUM.DAY, -1);
		} else if(httpUtil.getParameter("next") != null) {
			dateDebut = DateUtil.addSubstractDate(dateDebut, TIME_ENUM.DAY, 1);
			dateFin = DateUtil.addSubstractDate(dateFin, TIME_ENUM.DAY, 1);
		}
		
		dateDebut = DateUtil.getStartOfDay(dateDebut);
		dateFin = DateUtil.getEndOfDay(dateFin);
		
		httpUtil.setRequestAttribute("dateDebut", dateDebut);
		httpUtil.setRequestAttribute("dateFin", dateFin);
		
		Map<String, Integer[]> mapGlobalData = new HashMap<>();
		if(isFilter) {
			mapGlobalData = caisseService.getEvolutionTempsCmd(dateDebut, dateFin);
			
			List<Integer> listNbr = new ArrayList<>();
			List<Integer> listDuree = new ArrayList<>();
			List<String> listEmpl= new ArrayList<>();
			
			for(String empl : mapGlobalData.keySet()) {
				Integer[] det = mapGlobalData.get(empl);
				listDuree.add(det[0]);
				listNbr.add(det[1]);
				listEmpl.add(empl);
			}
			httpUtil.setRequestAttribute("listNbr", listNbr);
			httpUtil.setRequestAttribute("listDuree", listDuree);
			httpUtil.setRequestAttribute("listEmpl", listEmpl);
		}
		
		httpUtil.setMenuAttribute("tpSuivi", "stat");
		
		//
		httpUtil.setDynamicUrl("/domaine/caisse//back-office/mouvements_statutCmd_list.jsp");
	}*/
	
	public void getStatusCmd(ActionUtil httpUtil) { // TODO L Isolution du pack stock/caisse
		httpUtil.setDynamicUrl("/domaine/stock/article_edit.jsp");
	}
	
	/***************editPdfReduction**************************/
	
	public void editPdfReduction(ActionUtil httpUtil){
		find_reduction(httpUtil);
		
		Date dateDebut = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateDebut"));
		Date dateFin = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateFin"));
		List<CaisseMouvementPersistant> listCaisseMouvement = (List<CaisseMouvementPersistant>)httpUtil.getRequestAttribute("list_livraisonMouvementNoPage");
		CaisseMouvementPersistant jp = (CaisseMouvementPersistant) httpUtil.getRequestAttribute("mvm_total");
		
		File pdfFile = new HistoriqueReductionPDF().exportPdf(listCaisseMouvement,jp, dateDebut, dateFin);
		
		httpUtil.doDownload(pdfFile, true);
	}
	
	/**
	 * @param httpUtil
	 */
	public void find_ecarts(ActionUtil httpUtil){
		Date dateRef = null;
		JourneePersistant lastJrn = journeeService.getLastJournee();
		if(lastJrn != null){
			dateRef = lastJrn.getDate_journee();
		} else{
			dateRef = new Date();
		}
		
		// Initialiser les listes pour les filtres
		initDataListFilter(httpUtil);
				
		RequestTableBean cplxTable = getTableBean(httpUtil, "list_vente_ecart");
		Map<String, Object> formCriterion = cplxTable.getFormBean().getFormCriterion();
		boolean isFilterAct = StringUtil.isTrue(httpUtil.getRequest().getParameter("is_filter_act"));
		
		//----------------------------- Date -------------------------
		Date dateDebut = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateDebut"));
		Date dateFin = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateFin"));
		//
		if(httpUtil.getRequest().getParameter("is_fltr") == null) {
			dateDebut = DateUtil.stringToDate(httpUtil.getParameter("dateDebut"));
			dateFin = DateUtil.stringToDate(httpUtil.getParameter("dateFin"));
		}
		
		if(dateDebut == null) {
			dateDebut = (httpUtil.getMenuAttribute("dateDebut")==null ? dateRef : (Date)httpUtil.getMenuAttribute("dateDebut"));
			dateFin = (httpUtil.getMenuAttribute("dateFin")==null ? dateRef : (Date)httpUtil.getMenuAttribute("dateFin"));
			httpUtil.getDate("dateDebut").setValue(dateDebut);
			httpUtil.getDate("dateFin").setValue(dateDebut);
		}
		
		if(httpUtil.getParameter("prev") != null) {
			dateDebut = DateUtil.addSubstractDate(dateDebut, TIME_ENUM.DAY, -1);
			dateFin = DateUtil.addSubstractDate(dateFin, TIME_ENUM.DAY, -1);
		} else if(httpUtil.getParameter("next") != null) {
			dateDebut = DateUtil.addSubstractDate(dateDebut, TIME_ENUM.DAY, 1);
			dateFin = DateUtil.addSubstractDate(dateFin, TIME_ENUM.DAY, 1);
		}
		
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
		
		String requete = "from CaisseJourneePersistant caisseV where (caisseV.mtt_cloture_caissier-caisseV.mtt_total_net-caisseV.mtt_ouverture)!=0 "
		 + "and caisseV.opc_journee.date_journee>='[dateDebut]' and caisseV.opc_journee.date_journee<='[dateFin]' "
		 + " order by caisseV.opc_journee.date_journee desc, caisseV.date_ouverture";
		
		List<CaisseJourneePersistant> listCaisseMouvement = (List<CaisseJourneePersistant>) caisseMouvementService.findByCriteria(cplxTable, requete);
		httpUtil.setRequestAttribute("list_caisseMouvement", listCaisseMouvement);
		
		CaisseJourneePersistant jp = new CaisseJourneePersistant();
		List<CaisseJourneePersistant> listCaisseMouvementAll = (List<CaisseJourneePersistant>) caisseMouvementService.findByCriteria(cplxTable, requete, false);
		httpUtil.setRequestAttribute("list_EcartMouvementNoPage", listCaisseMouvementAll);
		
		//
	   	for (CaisseJourneePersistant jvp : listCaisseMouvementAll) {
	   		BigDecimal mttCloture = BigDecimalUtil.add(jp.getMtt_cloture_caissier(), BigDecimalUtil.substract(jvp.getMtt_cloture_caissier(), jvp.getMtt_ouverture()));
	   		//
	   		jp.setNbr_vente(getInt(jp.getNbr_vente())+getInt(jvp.getNbr_vente()));
			jp.setMtt_cloture_caissier(mttCloture);
			jp.setMtt_reduction(BigDecimalUtil.add(jp.getMtt_reduction(), jvp.getMtt_reduction()));
			jp.setMtt_art_offert(BigDecimalUtil.add(jp.getMtt_art_offert(), jvp.getMtt_art_offert()));
			jp.setMtt_art_reduction(BigDecimalUtil.add(jp.getMtt_art_reduction(), jvp.getMtt_art_reduction()));
			jp.setMtt_annule(BigDecimalUtil.add(jp.getMtt_annule(), jvp.getMtt_annule()));
			jp.setMtt_annule_ligne(BigDecimalUtil.add(jp.getMtt_annule_ligne(), jvp.getMtt_annule_ligne()));
			jp.setMtt_total(BigDecimalUtil.add(jp.getMtt_total(), jvp.getMtt_total()));
			jp.setMtt_total_net(BigDecimalUtil.add(jp.getMtt_total_net(), jvp.getMtt_total_net()));
			
			// Pour stoquer les ecrats negatifs
			BigDecimal ecart = BigDecimalUtil.substract(jvp.getMtt_cloture_caissier(), jvp.getMtt_total_net(), jvp.getMtt_ouverture());
			if(ecart.compareTo(BigDecimalUtil.ZERO)>=0){
				jp.setMtt_cloture_old_dej(BigDecimalUtil.add(jp.getMtt_cloture_old_dej(), ecart));
			} else{
				jp.setMtt_cloture_old_espece(BigDecimalUtil.add(jp.getMtt_cloture_old_espece(), ecart));
			}
		}
	   	httpUtil.setRequestAttribute("mvm_total", jp);
	   	
		httpUtil.setDynamicUrl("/domaine/caisse//back-office/ventes_ecarts_list.jsp");
	}
	
	private Integer getInt(Integer val){
		return (val==null ? 0 : val);
	}

	/***************editPdfEcart**************************/
	
	public void editPdfEcart(ActionUtil httpUtil){
		find_ecarts(httpUtil);
		
		Date dateDebut = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateDebut"));
		Date dateFin = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateFin"));
		List<CaisseJourneePersistant> listCaisseMouvement = (List<CaisseJourneePersistant>)httpUtil.getRequestAttribute("list_EcartMouvementNoPage");
		CaisseJourneePersistant jp = (CaisseJourneePersistant) httpUtil.getRequestAttribute("mvm_total");
		
		File pdfFile = new HistoriqueEcartPDF().exportPdf(listCaisseMouvement,jp, dateDebut, dateFin);
		
		httpUtil.doDownload(pdfFile, true);
	}
	
	/**
	 * @param httpUtil
	 */
	private void initDataListFilter(ActionUtil httpUtil){
		String[][] modePaie = {
				  {ContextAppli.MODE_PAIEMENT.ESPECES.toString(), "Espèces"}, 
				  {ContextAppli.MODE_PAIEMENT.CHEQUE.toString(), "Chèque"}, 
				  {ContextAppli.MODE_PAIEMENT.DEJ.toString(), "Chèque déj."},
				  {ContextAppli.MODE_PAIEMENT.CARTE.toString(), "Carte"}
			};
		httpUtil.setRequestAttribute("modePaie", modePaie);
		
		String[][] typeCmd = {
				{ContextAppli.TYPE_COMMANDE.E.toString(), "A emporter"}, 
				{ContextAppli.TYPE_COMMANDE.P.toString(), "Sur place"}, 
				{ContextAppli.TYPE_COMMANDE.L.toString(), "Livraison"}
			};
		httpUtil.setRequestAttribute("typeCmd", typeCmd);
	
		String[][] statutArray = {
				  {ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.ANNUL.toString(), "Annulée"}, 
				  {ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.VALIDE.toString(), "Validée"}, 
				  {ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.PREP.toString(), "En préparation"},
				  {ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.PRETE.toString(), "Prête"},
				  {ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.LIVRE.toString(), "livrée"},
				  {ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.TEMP.toString(), "En attente"}
			};
		httpUtil.setRequestAttribute("statutArray", statutArray);
	
		httpUtil.setRequestAttribute("listUser", caisseService.findAll(UserPersistant.class, Order.asc("login")));
		httpUtil.setRequestAttribute("listEmploye", caisseService.findAll(EmployePersistant.class, Order.asc("nom")));
		//httpUtil.setRequestAttribute("listLivreur", employeService.getListEmployeActifs("LIVREUR"));
		httpUtil.setRequestAttribute("listLivreur", userService.getListUserActifsByProfile("LIVREUR"));
		httpUtil.setRequestAttribute("listClient", caisseService.findAll(ClientPersistant.class, Order.asc("nom")));
	}
	
	/**
	 * @param httpUtil
	 */
	public void find_livraison(ActionUtil httpUtil){
		Date dateRef = null;
		JourneePersistant lastJrn = journeeService.getLastJournee();
		if(lastJrn != null){
			dateRef = lastJrn.getDate_journee();
		} else{
			dateRef = new Date();
		}
		
		// Initialiser les listes pour les filtres
		initDataListFilter(httpUtil);
				
		RequestTableBean cplxTable = getTableBean(httpUtil, "list_mouvement_livraison");
		
		//----------------------------- Date -------------------------
		Date dateDebut = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateDebut"));
		Date dateFin = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateFin"));
		//
		if(httpUtil.getRequest().getParameter("is_fltr") == null) {
			dateDebut = DateUtil.stringToDate(httpUtil.getParameter("dateDebut"));
			dateFin = DateUtil.stringToDate(httpUtil.getParameter("dateFin"));
		}
		
		if(dateDebut == null) {
			dateDebut = dateRef;
			dateFin = dateRef;
			httpUtil.getDate("dateDebut").setValue(dateDebut);
			httpUtil.getDate("dateFin").setValue(dateDebut);
		}
		
		if(httpUtil.getParameter("prev") != null) {
			dateDebut = DateUtil.addSubstractDate(dateDebut, TIME_ENUM.DAY, -1);
			dateFin = DateUtil.addSubstractDate(dateFin, TIME_ENUM.DAY, -1);
		} else if(httpUtil.getParameter("next") != null) {
			dateDebut = DateUtil.addSubstractDate(dateDebut, TIME_ENUM.DAY, 1);
			dateFin = DateUtil.addSubstractDate(dateFin, TIME_ENUM.DAY, 1);
		}
		
		dateDebut = DateUtil.getStartOfDay(dateDebut);
		dateFin = DateUtil.getEndOfDay(dateFin);
		
		httpUtil.setRequestAttribute("dateDebut", dateDebut);
		httpUtil.setRequestAttribute("dateFin", dateFin);
		
		JourneePersistant journeeDebut = journeeService.getJourneeOrNextByDate(dateDebut);
    	JourneePersistant journeeFin = journeeService.getJourneeOrPreviousByDate(dateFin);
		
		Map<String, Object> formCriterion = cplxTable.getFormBean().getFormCriterion();
		formCriterion.put("dateDebut", (journeeDebut!=null?journeeDebut.getId():null));
		formCriterion.put("dateFin", (journeeFin!=null?journeeFin.getId():null));
		
		//-----------------------------------------------------------
		formCriterion.put("typeLivraison", ContextAppli.TYPE_COMMANDE.L.toString());
		
		String etsCond = 
				(ContextAppli.IS_FULL_CLOUD() || ContextAppli.IS_CLOUD_MASTER() ? (" caisseMouvement.opc_etablissement.id="+ContextAppli.getEtablissementBean().getId())+" and " : " ");
		
		String req = "from CaisseMouvementPersistant caisseMouvement "
				+ "where "+etsCond+" caisseMouvement.opc_caisse_journee.opc_journee.id>='[dateDebut]' "
				+ "and caisseMouvement.opc_caisse_journee.opc_journee.id<='[dateFin]' "
				+ "and caisseMouvement.type_commande = '[typeLivraison]' "
				+ "order by caisseMouvement.opc_caisse_journee.opc_journee.id desc, "
				+ "caisseMouvement.opc_caisse_journee.opc_caisse.id, "
				+ "caisseMouvement.opc_caisse_journee.id, "
				+ "caisseMouvement.id desc";
		
		List<CaisseMouvementPersistant> listCaisseMouvement = (List<CaisseMouvementPersistant>) caisseMouvementService.findByCriteria(cplxTable, req);
		httpUtil.setRequestAttribute("list_livraisonMouvement", listCaisseMouvement);

		// Total
		List<CaisseMouvementPersistant> listCaisseMouvementAll = (List<CaisseMouvementPersistant>) caisseMouvementService.findByCriteria(cplxTable, req, false);
		httpUtil.setRequestAttribute("list_livraisonMouvementNoPage", listCaisseMouvementAll);
		
		CaisseMouvementPersistant mvmTotal = new CaisseMouvementPersistant();
		for (CaisseMouvementPersistant mvmDet : listCaisseMouvementAll) {
			mvmTotal.setMtt_art_offert(BigDecimalUtil.add(mvmTotal.getMtt_art_offert(), mvmDet.getMtt_art_offert()));
			mvmTotal.setMtt_art_reduction(BigDecimalUtil.add(mvmTotal.getMtt_art_reduction(), mvmDet.getMtt_art_reduction()));
			mvmTotal.setMtt_commande(BigDecimalUtil.add(mvmTotal.getMtt_commande(), mvmDet.getMtt_commande()));
			if(!BooleanUtil.isTrue(mvmDet.getIs_annule())) {
				mvmTotal.setMtt_commande_net(BigDecimalUtil.add(mvmTotal.getMtt_commande_net(), mvmDet.getMtt_commande_net()));
			}
			mvmTotal.setMtt_reduction(BigDecimalUtil.add(mvmTotal.getMtt_reduction(), mvmDet.getMtt_reduction()));
			mvmTotal.setMtt_annul_ligne(BigDecimalUtil.add(mvmTotal.getMtt_annul_ligne(), mvmDet.getMtt_annul_ligne()));
		}
		httpUtil.setRequestAttribute("mvmDetTotal", mvmTotal);
		
		httpUtil.setDynamicUrl("/domaine/caisse/back-office/mouvements_livraison_list.jsp");
	}
	
	/**
	 * @param httpUtil
	 */
	public void find_annulation(ActionUtil httpUtil){
		Date dateRef = null;
		JourneePersistant lastJrn = journeeService.getLastJournee();
		if(lastJrn != null){
			dateRef = lastJrn.getDate_journee();
		} else{
			dateRef = new Date();
		}
		
		// Initialiser les listes pour les filtres
		initDataListFilter(httpUtil);		
		
		RequestTableBean cplxTable = getTableBean(httpUtil, "list_mouvement_annulation");
		RequestTableBean cplxTableDet = getTableBean(httpUtil, "list_mouvementDet_annulation");
		
		//----------------------------- Date -------------------------
		Date dateDebut = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateDebut"));
		Date dateFin = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateFin"));
		//
		if(httpUtil.getRequest().getParameter("is_fltr") == null) {
			dateDebut = DateUtil.stringToDate(httpUtil.getParameter("dateDebut"));
			dateFin = DateUtil.stringToDate(httpUtil.getParameter("dateFin"));
		}
		
		if(dateDebut == null) {
			dateDebut = dateRef;
			dateFin = dateRef;
			httpUtil.getDate("dateDebut").setValue(dateDebut);
			httpUtil.getDate("dateFin").setValue(dateDebut);
		}
		
		if(httpUtil.getParameter("prev") != null) {
			dateDebut = DateUtil.addSubstractDate(dateDebut, TIME_ENUM.DAY, -1);
			dateFin = DateUtil.addSubstractDate(dateFin, TIME_ENUM.DAY, -1);
		} else if(httpUtil.getParameter("next") != null) {
			dateDebut = DateUtil.addSubstractDate(dateDebut, TIME_ENUM.DAY, 1);
			dateFin = DateUtil.addSubstractDate(dateFin, TIME_ENUM.DAY, 1);
		}
		
		dateDebut = DateUtil.getStartOfDay(dateDebut);
		dateFin = DateUtil.getEndOfDay(dateFin);
		
		httpUtil.setRequestAttribute("dateDebut", dateDebut);
		httpUtil.setRequestAttribute("dateFin", dateFin);
		
		JourneePersistant journeeDebut = journeeService.getJourneeOrNextByDate(dateDebut);
    	JourneePersistant journeeFin = journeeService.getJourneeOrPreviousByDate(dateFin);
		
		Map<String, Object> formCriterion = cplxTableDet.getFormBean().getFormCriterion();
		formCriterion.put("dateDebut", (journeeDebut!=null?journeeDebut.getId():null));
		formCriterion.put("dateFin", (journeeFin!=null?journeeFin.getId():null));
		
		
		Map<String, Object> formCr = cplxTable.getFormBean().getFormCriterion();
		formCr.put("dateDebut", (journeeDebut!=null?journeeDebut.getId():null));
		formCr.put("dateFin", (journeeFin!=null?journeeFin.getId():null));
		
		//-----------------------------------------------------------
		List<CaisseMouvementPersistant> listCaisseMouvement = (List<CaisseMouvementPersistant>) caisseMouvementService.findByCriteriaByQueryId(cplxTable, "annulationMouvement_find");
		httpUtil.setRequestAttribute("list_annulationMouvement", listCaisseMouvement);
		
		List<CaisseMouvementPersistant> listCaisseMouvementDet = (List<CaisseMouvementPersistant>) caisseMouvementService.findByCriteriaByQueryId(cplxTableDet, "annulationMouvementDet_find");
		httpUtil.setRequestAttribute("list_annulationMouvementDet", listCaisseMouvementDet);

		// Total
		List<CaisseMouvementPersistant> listCaisseMouvementAll = (List<CaisseMouvementPersistant>) caisseMouvementService.findByCriteriaByQueryId(cplxTable, "annulationMouvement_find", false);
		List<CaisseMouvementPersistant> listCaisseMouvementAllDet = (List<CaisseMouvementPersistant>) caisseMouvementService.findByCriteriaByQueryId(cplxTableDet, "annulationMouvementDet_find", false);
		
		CaisseMouvementPersistant mvmTotal = new CaisseMouvementPersistant();
		for (CaisseMouvementPersistant mvmDet : listCaisseMouvementAll) {
			mvmTotal.setMtt_commande(BigDecimalUtil.add(mvmTotal.getMtt_commande(), mvmDet.getMtt_commande()));
			mvmTotal.setMtt_commande_net(BigDecimalUtil.add(mvmTotal.getMtt_commande_net(), mvmDet.getMtt_commande_net()));
		}
		httpUtil.setRequestAttribute("mvmDetTotal", mvmTotal);
		
		CaisseMouvementPersistant mvmTotalDet = new CaisseMouvementPersistant();
		for (CaisseMouvementPersistant mvmDet : listCaisseMouvementAllDet) {
			mvmTotalDet.setMtt_commande(BigDecimalUtil.add(mvmTotalDet.getMtt_commande(), mvmDet.getMtt_commande()));
			mvmTotalDet.setMtt_commande_net(BigDecimalUtil.add(mvmTotalDet.getMtt_commande_net(), mvmDet.getMtt_commande_net()));
			mvmTotalDet.setMtt_annul_ligne(BigDecimalUtil.add(mvmTotalDet.getMtt_annul_ligne(), mvmDet.getMtt_annul_ligne()));
		}
		httpUtil.setRequestAttribute("mvmDetTotalDet", mvmTotalDet);
		
		httpUtil.setRequestAttribute("totalAnnulationAll", BigDecimalUtil.formatNumber(BigDecimalUtil.add(mvmTotal.getMtt_commande_net(), mvmTotalDet.getMtt_annul_ligne())));
		
		httpUtil.setDynamicUrl("/domaine/caisse//back-office/mouvements_annulation_list.jsp");
	}
	
	/**
	 * @param httpUtil
	 */
	public void editPdfLivraison(ActionUtil httpUtil){
		find_livraison(httpUtil);
		
		Date dateDebut = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateDebut"));
		Date dateFin = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateFin"));
		List<CaisseMouvementPersistant> listCaisseMouvement = (List<CaisseMouvementPersistant>)httpUtil.getRequestAttribute("list_livraisonMouvementNoPage");
		CaisseMouvementPersistant mvmTotal = (CaisseMouvementPersistant) httpUtil.getRequestAttribute("mvmDetTotal");
		
		File pdfFile = new HistoriqueLivraisonPDF().exportPdf(listCaisseMouvement,mvmTotal, dateDebut, dateFin);
		
		httpUtil.doDownload(pdfFile, true);
	}
	/**
	 * @param httpUtil
	 */
	public void find_mouvement(ActionUtil httpUtil){
		// Initialiser les listes pour les filtres
		initDataListFilter(httpUtil);
		
		RequestTableBean cplxTable = getTableBean(httpUtil, "list_caisseMouvement");
		Map<String, Object> formCriterion = cplxTable.getFormBean().getFormCriterion();
		boolean isFilterAct = StringUtil.isTrue(httpUtil.getRequest().getParameter("is_filter_act"));
		
		if(StringUtil.isNotEmpty(httpUtil.getParameter("curr_journee")) || StringUtil.isNotEmpty(httpUtil.getParameter("jr"))){
			String journeeId = StringUtil.isNotEmpty(httpUtil.getParameter("curr_journee")) ? 
					httpUtil.getParameter("curr_journee") : httpUtil.getParameter("jr");
			formCriterion.put("journeeId", Long.valueOf(journeeId));
			
			httpUtil.removeMenuAttribute("dateDebut");
			httpUtil.removeMenuAttribute("dateFin");
			formCriterion.remove("dateDebut");
			formCriterion.remove("dateFin");
			httpUtil.setRequestAttribute("curr_journee", journeeId);
		} else{
			//----------------------------- Date -------------------------
			Date dateDebut = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateDebut"));
			Date dateFin = DateUtil.stringToDate(httpUtil.getRequest().getParameter("dateFin"));
			//
			if(httpUtil.getRequest().getParameter("is_fltr") == null) {
				dateDebut = DateUtil.stringToDate(httpUtil.getParameter("dateDebut"));
				dateFin = DateUtil.stringToDate(httpUtil.getParameter("dateFin"));
			}
			
			if(dateDebut == null) {
				dateDebut = (httpUtil.getMenuAttribute("dateDebut")==null ? new Date() : (Date)httpUtil.getMenuAttribute("dateDebut"));
				dateFin = (httpUtil.getMenuAttribute("dateFin")==null ? new Date() : (Date)httpUtil.getMenuAttribute("dateFin"));
				httpUtil.getDate("dateDebut").setValue(dateDebut);
				httpUtil.getDate("dateFin").setValue(dateDebut);
			}
			
			if(httpUtil.getParameter("prev") != null) {
				dateDebut = DateUtil.addSubstractDate(dateDebut, TIME_ENUM.DAY, -1);
				dateFin = DateUtil.addSubstractDate(dateFin, TIME_ENUM.DAY, -1);
			} else if(httpUtil.getParameter("next") != null) {
				dateDebut = DateUtil.addSubstractDate(dateDebut, TIME_ENUM.DAY, 1);
				dateFin = DateUtil.addSubstractDate(dateFin, TIME_ENUM.DAY, 1);
			}
			
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
			httpUtil.removeMenuAttribute("journeeId");
			formCriterion.remove("journeeId");
		}
		//-----------------------------------------------------------
		Long caisseId = (Long)httpUtil.getMenuAttribute("caisseId");
		formCriterion.put("caisseId", caisseId);
		
		List<CaisseMouvementPersistant> listCaisseMouvement = (List<CaisseMouvementPersistant>) caisseMouvementService.findByCriteriaByQueryId(cplxTable, "caisseMouvement_find");
		httpUtil.setRequestAttribute("list_caisseMouvement", listCaisseMouvement);

		List<JourneePersistant> listJournee = (List<JourneePersistant>) caisseMouvementService.findAll(JourneePersistant.class, Order.desc("date_journee"));
		httpUtil.setRequestAttribute("listJournee", listJournee);
		
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
		
		Long journeeId = (Long)httpUtil.getMenuAttribute("journeeId");
		if(journeeId == null){
			JourneePersistant lastJournee = journeeService.getLastJournee();
			journeeId = (lastJournee!=null ? lastJournee.getId() : null);
		}
		JourneePersistant journeeV = journeeService.getJourneeView(journeeId);
		httpUtil.setRequestAttribute("journeeView", (journeeV==null ? new JourneePersistant() : journeeV));
		
		httpUtil.setDynamicUrl("/domaine/caisse//back-office/mouvements_caisses_list.jsp");
	}
	
	/**
	 * @param httpUtil
	 */
	public void edit_mouvement(ActionUtil httpUtil){
		httpUtil.setFormReadOnly();
		CaisseMouvementBean viewBean = caisseMouvementService.findById(httpUtil.getWorkIdLong());
		httpUtil.setRequestAttribute("caisseMouvement", viewBean);
		httpUtil.setDynamicUrl("/domaine/caisse//back-office/mouvement_caisse_edit.jsp");
	}
	
	/**
	 * @param httpUtil
	 */
	public void ouvrir_caisse(ActionUtil httpUtil){
		Long caisseId = httpUtil.getWorkIdLong();
		String mtt = httpUtil.getParameter("caisseJournee.mtt_ouverture_caissier");
		caisseService.ouvrirCaisse(caisseId, BigDecimalUtil.get(mtt));
		
		work_find(httpUtil);
	}

	/**
	 * @param httpUtil
	 */
	public void init_cloturer_definitive(ActionUtil httpUtil){
		boolean isRectMode = StringUtil.isTrue(httpUtil.getParameter("rect"));// Mode correction par le comptable
		Long caisseId = httpUtil.getWorkIdLong();
		
		if(isRectMode) {
			caisseId = httpUtil.getLongParameter("ca");
		}
		
		boolean isAutoPassation = StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("SHIFT_PASSASION"));
		if(isAutoPassation && httpUtil.getMenuAttribute("CURR_JRN_DBL_CLOT") == null){
			CaisseJourneePersistant caisseJ = caisseService.getJourneCaisseOuverte(caisseId);
			if(caisseJ != null && (caisseService.getListCmdTemp(caisseJ.getId()).size() > 0 
					|| caisseService.getListCmdNonPaye(caisseJ.getId()).size() > 0)){
				httpUtil.setRequestAttribute("isPass", true);
				httpUtil.setRequestAttribute("listUser", userService.getListUserActifsByProfile("SERVEUR"));
			}
		}
		
		httpUtil.setRequestAttribute("caisseId", caisseId);
		httpUtil.setRequestAttribute("isRectMode", isRectMode);
		
		httpUtil.setDynamicUrl("/domaine/caisse/normal/caisse_cloture_popup.jsp");
	}
	
	@WorkForward
	public void cloturer_definitive(ActionUtil httpUtil){
		boolean isRectMode = StringUtil.isTrue(httpUtil.getParameter("rect"));// Mode correction par le comptable
		Long caisseId = httpUtil.getWorkIdLong();
		BigDecimal mtt_clotureEspeces = BigDecimalUtil.get(httpUtil.getParameter("caisseJournee.mtt_cloture_caissier_espece"));
		BigDecimal mtt_clotureCb = BigDecimalUtil.get(httpUtil.getParameter("caisseJournee.mtt_cloture_caissier_cb"));
		BigDecimal mtt_clotureChq = BigDecimalUtil.get(httpUtil.getParameter("caisseJournee.mtt_cloture_caissier_cheque"));
		BigDecimal mtt_clotureDej = BigDecimalUtil.get(httpUtil.getParameter("caisseJournee.mtt_cloture_caissier_dej"));
		boolean isPassasionSub = StringUtil.isTrue(httpUtil.getParameter("isPass"));
		
		if(isPassasionSub){
			if(StringUtil.isEmpty(httpUtil.getLongParameter("userPass.id"))){
				MessageService.addBannerMessage("Merci de sélectionner le prochain caissier.");
				return;
			}
		}
		
		CaisseJourneePersistant caisseJourneeP = null;
		if(httpUtil.getMenuAttribute("CURR_JRN_DBL_CLOT") != null) {
			//Long journeeId = (Long) httpUtil.getMenuAttribute("CURR_JRN_DBL_CLOT");
			caisseJourneeP = (CaisseJourneePersistant) caisseService.findById(CaisseJourneePersistant.class, caisseId);
		} else {
			if(isRectMode) {
				caisseJourneeP = (CaisseJourneePersistant) caisseService.findById(CaisseJourneePersistant.class, caisseId);
			} else {
				CaisseBean caisseB = caisseService.findById(caisseId);
				caisseJourneeP = caisseB.getList_caisse_journee().get(0);
			}
		}
		
		// Si Mode passasion alors on passe les mouvements vers le nouveau shift
		if(!isRectMode && isPassasionSub){
			caisseService.gererPassasionShift(httpUtil.getLongParameter("userPass.id"), 
					caisseJourneeP, 
					BigDecimalUtil.get(httpUtil.getParameter("mttOuvertureCaissier")), isRectMode);
		}
		
		// --> Si rectification on passe le Caisse_journee_id et pas caisse_id
		caisseService.cloturerDefinitive(caisseJourneeP, false,
				mtt_clotureEspeces, 
				mtt_clotureCb, 
				mtt_clotureChq, 
				mtt_clotureDej, 
				isRectMode, isPassasionSub);
		
		if(isRectMode) {
			MessageService.addGrowlMessage(MSG_TYPE.SUCCES, "Clôture effectuée", "Les montants de clôture sont pris en compte.");
			init_cloturer_shifts(httpUtil); 
		} else {
			work_find(httpUtil);
		}
	}
	
	/**
	 * @param httpUtil
	 */
	public void activer_caisse(ActionUtil httpUtil){
		Long caisseId = httpUtil.getWorkIdLong();
		caisseService.activerDesactiverCaisse(caisseId);
		
		work_find(httpUtil);
	}
	
	/**
	 * @param httpUtil
	 */
	public void init_cloturer_shifts(ActionUtil httpUtil) {
		Long currJourneeId = httpUtil.getLongParameter("jrn");
		if(currJourneeId == null) {
			currJourneeId = (Long)httpUtil.getMenuAttribute("CURR_JRN_ID");
		} else {
			httpUtil.setMenuAttribute("CURR_JRN_DBL_CLOT", currJourneeId);
		}
		
		if(currJourneeId == null) {
			currJourneeId = (Long) httpUtil.getMenuAttribute("CURR_JRN_DBL_CLOT");
		}
		
		List listDataShift = journeeService.getJourneeCaisseView(currJourneeId);
		boolean isDoubleCloture = StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("DOUBLE_CLOTURE")) 
				&& Context.isOperationAvailable("DBLCLO");
		//
		if(listDataShift == null || listDataShift.size()==0) {
			MessageService.addGrowlMessage("Shift non trouvé", "Aucun shift clos n'a été trouvé.");
			return;
		} else if(!isDoubleCloture) {
			MessageService.addGrowlMessage("Droits insuffisants", "Vous ne disposez pas de suffisaments de droits pour effectuer cette opération.");
			return;
		}
		httpUtil.setRequestAttribute("listDataShift", listDataShift);
		
		httpUtil.setDynamicUrl("/domaine/caisse/normal/caisse_cloture_double_list.jsp");
	}
	
	
	/**
	 * @param httpUtil
	 */
	public void find_mvm_employe(ActionUtil httpUtil){
		RequestTableBean cplxTable = getTableBean(httpUtil, "list_caisseMouvement");
		
		Long employeId = (Long)httpUtil.getMenuAttribute("employeId");
		
		cplxTable.getFormBean().getFormCriterion().put("employeId", employeId);
		
		List<CaisseMouvementPersistant> listCaisseMouvement = (List<CaisseMouvementPersistant>) employeService.findByCriteriaByQueryId(cplxTable, "caisseMouvement_tiers_find");
		httpUtil.setRequestAttribute("list_caisseMouvement", listCaisseMouvement);

		httpUtil.setDynamicUrl("/domaine/personnel/employe_mouvements.jsp");
	}
	
	public void find_marge_vente(ActionUtil httpUtil) {
		boolean isRestauEnv = SOFT_ENVS.restau.toString().equals(StrimUtil.getGlobalConfigPropertie("context.soft"));
		String familleSt = isRestauEnv ? "CU" : "ST";
		RequestTableBean cplxTable = getTableBean(httpUtil, "list_article");
		
		String req = "from ArticlePersistant article "
				+ "where "+(isRestauEnv ? "(article.is_stock is null or article.is_stock=false) ":"1=1"); 
			req = req + " and (article.is_disable is null or article.is_disable=0) ";
		
		if(isRestauEnv){
			req = req + "order by article.opc_famille_cuisine.code, article.opc_famille_cuisine.libelle, article.code, article.libelle ";
		} else{
			req = req + "order by article.opc_famille_stock.code, article.opc_famille_stock.libelle, article.code, article.libelle ";
		}
			
		List<ArticlePersistant> listArticle = (List<ArticlePersistant>) familleService.findByCriteria(cplxTable, req);
		
		for (ArticlePersistant articlePersistant : listArticle) {
			Long famId = (isRestauEnv ? articlePersistant.getOpc_famille_cuisine().getId() : articlePersistant.getOpc_famille_stock().getId());
			List<FamillePersistant> familleStr = familleService.getFamilleParent(familleSt, famId);
			articlePersistant.setFamilleStr(familleStr);
		}
		
		httpUtil.setRequestAttribute("list_article", listArticle);
		
		Map<ArticlePersistant, BigDecimal[]> mapData = articleService.calculMargeArticles(listArticle);
		httpUtil.setRequestAttribute("mapData", mapData);
		List listFamille = familleService.getListeFamille(familleSt, true, false);
		httpUtil.setRequestAttribute("listeFaimlle", listFamille);
		
		httpUtil.setDynamicUrl("/domaine/caisse//back-office/marge_vente_list.jsp");
	}
	
	// Methode temporaire pou recalculer les annulations des aticles
	public void recalculHistoriqueAnnulation(ActionUtil httpUtil){
		caisseService.recalculHistoriqueAnnulation();
		MessageService.addGrowlMessage(MSG_TYPE.SUCCES, "", "Synchronisation effectuée.");
		
		httpUtil.setDynamicUrl("admin.job.work_find");
	}
	public void recalculMouvementsStock(ActionUtil httpUtil){
		caisseService.recalculMouvementsAchat();
		MessageService.addGrowlMessage(MSG_TYPE.SUCCES, "", "Synchronisation effectuée.");
		
		httpUtil.setDynamicUrl("admin.job.work_find");
	}
	
	public void work_post(ActionUtil httpUtil){
		manageDataForm(httpUtil, "CAISSE");
	}

}
