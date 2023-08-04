/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appli.model.domaine.caisse.service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.springframework.transaction.annotation.Transactional;

import appli.controller.domaine.caisse.ContextAppliCaisse;
import appli.controller.domaine.caisse.bean.CaisseBean;
import appli.controller.domaine.util_erp.ContextAppli;
import appli.controller.domaine.util_erp.ContextAppli.SOFT_ENVS;
import appli.controller.domaine.util_erp.ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM;
import appli.controller.domaine.util_erp.ContextAppli.TYPE_CAISSE_ENUM;
import appli.model.domaine.administration.persistant.CaisseVenteView;
import appli.model.domaine.administration.persistant.MailQueuePersistant;
import appli.model.domaine.administration.persistant.UserPersistant;
import appli.model.domaine.administration.persistant.ValTypeEnumPersistant;
import appli.model.domaine.administration.service.IMailUtilService;
import appli.model.domaine.administration.service.IValTypeEnumService;
import appli.model.domaine.administration.service.impl.MailUtilService;
import appli.model.domaine.caisse.dao.ICaisseDao;
import appli.model.domaine.caisse.dao.ICaisseJourneeDao;
import appli.model.domaine.caisse.persistant.ArticleBalancePersistant;
import appli.model.domaine.caisse.persistant.ArticleStockCaisseInfoPersistant;
import appli.model.domaine.caisse.persistant.JourneeVenteView;
import appli.model.domaine.caisse.service.ICaisseMouvementStatutService;
import appli.model.domaine.caisse.service.ICaisseService;
import appli.model.domaine.caisse.service.ICaisseWebService;
import appli.model.domaine.caisse.service.IJourneeService;
import appli.model.domaine.caisse.service.PrintCuisineUtil;
import appli.model.domaine.caisse.service.PrintPilotageUtil;
import appli.model.domaine.caisse.validator.CaisseValidator;
import appli.model.domaine.personnel.persistant.EmployePersistant;
import appli.model.domaine.stock.persistant.ArticlePersistant;
import appli.model.domaine.stock.persistant.EmplacementPersistant;
import appli.model.domaine.stock.persistant.MouvementArticlePersistant;
import appli.model.domaine.stock.persistant.MouvementPersistant;
import appli.model.domaine.vente.persistant.CaisseJourneePersistant;
import appli.model.domaine.vente.persistant.CaisseMouvementArticlePersistant;
import appli.model.domaine.vente.persistant.CaisseMouvementPersistant;
import appli.model.domaine.vente.persistant.CaisseMouvementStatutPersistant;
import appli.model.domaine.vente.persistant.CaissePersistant;
import appli.model.domaine.vente.persistant.JourneePersistant;
import framework.controller.ContextGloabalAppli;
import framework.controller.bean.PagerBean;
import framework.model.beanContext.EtablissementPersistant;
import framework.model.common.annotation.validator.WorkModelClassValidator;
import framework.model.common.annotation.validator.WorkModelMethodValidator;
import framework.model.common.service.MessageService;
import framework.model.common.util.BigDecimalUtil;
import framework.model.common.util.BooleanUtil;
import framework.model.common.util.DateUtil;
import framework.model.common.util.NumericUtil;
import framework.model.common.util.StrimUtil;
import framework.model.common.util.StringUtil;
import framework.model.service.GenericJpaService;

@Named
@WorkModelClassValidator(validator=CaisseValidator.class)
public class CaisseService extends GenericJpaService<CaisseBean, Long> implements ICaisseService{
	@Inject
	private ICaisseDao caisseDao; 
	@Inject
	private IJourneeService journeeService;
	@Inject
	private ICaisseJourneeDao caisseJourneeDao;
	@Inject
	private ICaisseMouvementStatutService caisseMvmStatutService;
	@Inject
	private IMailUtilService mailService;
	@Inject
	private IValTypeEnumService valEnumService;
	@Inject
	private ICaisseWebService caisseWebService;
	
	@Override
	@Transactional
	@WorkModelMethodValidator
	public void activerDesactiverCaisse(Long caisseId) {
		CaissePersistant caissePersistant = caisseDao.findById(caisseId);
		caissePersistant.setIs_desactive(BooleanUtil.isTrue(caissePersistant.getIs_desactive()) ? false : true);
			
		caisseDao.update(caissePersistant);
	}
	
	@Override
	public List<CaissePersistant> getListCaisse(boolean activeOnly) {
		String request = "from CaissePersistant where 1=1 ";
		if(activeOnly){
			request = request + "and (is_desactive is null or is_desactive=false)";
		}
		request = request + " order by type_ecran, reference";
		
		return getQuery(request).getResultList();
	}
	
	@Override
	public List<CaissePersistant> getListCaisseActive(String typeCaisse, boolean activeOnly) {
		String request = "from CaissePersistant where type_ecran=:type ";
		if(activeOnly){
			request = request + "and (is_desactive is null or is_desactive=false)";
		}
		request = request + " order by reference";
		
		return getQuery(request).setParameter("type", typeCaisse).getResultList();
	}
	
	@Override
	public List<CaisseBean> getListCaisseCuisineActive() {
		String request = "from CaissePersistant where (is_desactive is null or is_desactive=false) "
			+ " and type_ecran='"+TYPE_CAISSE_ENUM.CUISINE.toString()+"' "
			+ " order by reference";
		
		return getQuery(request).getResultList();
	}
	
	@Override
	public List<Object> tempsGlobalParEmploye(Date date_debut, Date date_fin,String[] articleIds,String[] menuIds) {
		Set<Long> articleIncludeIds = new HashSet<Long>();
		Set<Long> menuIncludeIds = new HashSet<Long>();
		if(articleIds != null) { 
			for (String article : articleIds) {
				if(StringUtil.isEmpty(article)) {
					continue;
				}
				articleIncludeIds.add(Long.valueOf(article));
			}
		}
		if(menuIds != null) { 
			for (String menu : menuIds) {
				if(StringUtil.isEmpty(menu)) {
					continue;
				}
				menuIncludeIds.add(Long.valueOf(menu));
			}
		}
		
		String request = "select date_debut_stat,date_fin_stat,coalesce(opc_user_stat.nom,'INCONNU') "
				+ " from CaisseMouvementArticlePersistant "
				+ "where date_fin_stat IS NOT NULL "
				+ "and date_debut_stat IS NOT NULL  " 
				+ "and opc_user_stat.id IS NOT null "
				+ "and opc_mouvement_caisse.date_vente>=:dateDebut "
				+ "and opc_mouvement_caisse.date_vente<=:dateFin ";
		
		if(articleIncludeIds.size() > 0){
        	request += "and opc_article.id in (:listArts) ";
		}
		if(menuIncludeIds.size() > 0){
        	request += "and opc_menu.id in (:listMenus) ";
		}
		
		request +="group by opc_user_stat.id ";
		
		Query query = getQuery(request);
		if(articleIncludeIds.size() > 0){
			query.setParameter("listArts", articleIncludeIds);
		}
		if(menuIncludeIds.size() > 0){
			query.setParameter("listMenus", menuIncludeIds);
		}
		
		query.setParameter("dateDebut", date_debut)
			.setParameter("dateFin", date_fin);
		
		List<Object> listDataTemp = new ArrayList<>();
		List<Object[]> listData = query.getResultList();
		for (Object[] objects : listData) {
			listDataTemp.add(new Object[]{DateUtil.getDiffMinuts((Date)objects[0], (Date)objects[1]), objects[2]});
		}
		return listDataTemp;
	}
	
	
	
	@Override
	public List<CaisseBean> getListCaissePilotageActive() {
		String request = "from CaissePersistant where (is_desactive is null or is_desactive=false) "
			+ " and type_ecran='"+TYPE_CAISSE_ENUM.PILOTAGE.toString()+"' "
			+ " order by reference";
		
		return getQuery(request).getResultList();
	}

	@Override
	@Transactional
	@WorkModelMethodValidator
	public void ouvrirCaisse(Long caisseId, BigDecimal mtt_ouverture) {
		JourneePersistant journeePersistant = journeeService.getLastJournee();
		CaissePersistant caissePersistant = caisseDao.findById(caisseId);
		
		List<CaisseJourneePersistant> listCaisseJournee = caisseDao.getQuery("from CaisseJourneePersistant where opc_caisse.id=:caisseId and opc_journee.id=:journeeId "
				+ "and statut_caisse='O'")
			.setParameter("caisseId", caisseId)
			.setParameter("journeeId", journeePersistant.getId())
			.getResultList();
		
		if(listCaisseJournee.size()> 0){
			return;
		}
		
		CaisseJourneePersistant caisseJourneeP = new CaisseJourneePersistant( );
		caisseJourneeP.setStatut_caisse(ContextAppli.STATUT_JOURNEE.OUVERTE.getStatut());
		caisseJourneeP.setOpc_user(ContextAppli.getUserBean());
		caisseJourneeP.setOpc_caisse(caissePersistant);
		caisseJourneeP.setOpc_journee(journeePersistant);
		caisseJourneeP.setMtt_ouverture(mtt_ouverture);
		caisseJourneeP.setDate_ouverture(new Date());
		
		// 
	  	caisseJourneeDao.create(caisseJourneeP);
	 }

	@Override
	@Transactional
	@WorkModelMethodValidator
	public void cloturerDefinitive(CaisseJourneePersistant caisseJourneeP,
			boolean isRecalcul,// Recalcul si bug calcul
			BigDecimal mtt_clotureEspeces, 
			BigDecimal mtt_clotureCb, 
			BigDecimal mtt_clotureChq, 
			BigDecimal mtt_clotureDej, 
			boolean isRectif, boolean isPassassion) {
		 
		if(!isRectif && !isRecalcul) {
			caisseJourneeP.setDate_cloture(new Date());
		}

		if(!isRecalcul) {
			caisseJourneeP.setStatut_caisse(ContextAppli.STATUT_JOURNEE.CLOTURE.getStatut());
			
			// Rectification
			if(isRectif && caisseJourneeP.getMtt_cloture_old_espece() == null) {
				caisseJourneeP.setMtt_cloture_old_espece(caisseJourneeP.getMtt_cloture_caissier_espece());
				caisseJourneeP.setMtt_cloture_old_cb(caisseJourneeP.getMtt_cloture_caissier_cb());
				caisseJourneeP.setMtt_cloture_old_cheque(caisseJourneeP.getMtt_cloture_caissier_cheque());
				caisseJourneeP.setMtt_cloture_old_dej(caisseJourneeP.getMtt_cloture_caissier_dej());
			}
			//
			caisseJourneeP.setMtt_cloture_caissier_espece(mtt_clotureEspeces);
			caisseJourneeP.setMtt_cloture_caissier_cb(mtt_clotureCb);
			caisseJourneeP.setMtt_cloture_caissier_cheque(mtt_clotureChq);
			caisseJourneeP.setMtt_cloture_caissier_dej(mtt_clotureDej);
			
			caisseJourneeP.setMtt_cloture_caissier(BigDecimalUtil.add(mtt_clotureEspeces, mtt_clotureCb, mtt_clotureChq, mtt_clotureDej));
		}

		// Mettre à jour les cumuls depuis les vues
		setDataJourneeCaisseFromView(caisseJourneeP);
		
		//
		if(!isRecalcul) {
			caisseJourneeP.setOpc_user_cloture(ContextAppli.getUserBean());
		}
		//
		caisseJourneeDao.update(caisseJourneeP); 
		
		JourneePersistant opc_journee = caisseJourneeP.getOpc_journee();
		if(ContextAppli.STATUT_JOURNEE.CLOTURE.getStatut().equals(opc_journee.getStatut_journee())){
			EntityManager entityManager = getEntityManager();
			entityManager.flush();
			journeeService.setDataJourneeFromView(opc_journee);
			entityManager.merge(opc_journee);
			entityManager.flush();
			
			journeeService.ajouterEcrituresJournee(opc_journee);
		}
		
		// Envoi mail info --------------------------------------------------
		String mailsShifts = ContextGloabalAppli.getGlobalConfig("MAIL_ALERT_SHIFT");
		if(!isRecalcul && StringUtil.isNotEmpty(mailsShifts)){
			Map<String, String> mapParams = new HashMap<>();
			mapParams.put("1", DateUtil.dateToString(opc_journee.getDate_journee())+" "+DateUtil.dateToString(caisseJourneeP.getDate_ouverture(), "HH:mm:ss"));
			
			BigDecimal netCaisse = BigDecimalUtil.substract(
					caisseJourneeP.getMtt_total_net(), 
					caisseJourneeP.getMtt_portefeuille(), 
					caisseJourneeP.getMtt_donne_point());
			
			BigDecimal mttEcart = BigDecimalUtil.substract(
   					caisseJourneeP.getMtt_cloture_caissier(),
   					caisseJourneeP.getMtt_ouverture(),
	   				netCaisse);
			
			StringBuilder sb = new StringBuilder();
			sb.append("<table>");
			sb.append("<tr style='background-color:white;'>"
						+ "<td>FOND DE ROULEMENT : </td>"
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(caisseJourneeP.getMtt_ouverture())+"</td>"
					+ "</tr>");
			
			sb.append("<tr style='background-color:white;'>"
						+ "<td>TOTAL BRUT : </td>"
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(caisseJourneeP.getMtt_total())+"</td>"
					+ "</tr>");
			
			sb.append("<tr style='background-color:white;'>"
						+ "<td style='font-weight:bold;'>TOTAL NET : </td>"
						+ "<td style='text-align:right;font-weight:bold;'>"+BigDecimalUtil.formatNumber(caisseJourneeP.getMtt_total_net())+"</td>"
					+ "</tr>");
			sb.append("<tr style='background-color:white;'>"
					+ "<td>TOTAL CLOTURE : </td>"
					+ "<td style='text-align:right;'><b>"+BigDecimalUtil.formatNumber(caisseJourneeP.getMtt_cloture_caissier())+"</b></td>"
				+ "</tr>");
			sb.append("<tr style='background-color:white;'>"
					+ "<td>ECART : </td>"
					+ "<td style='text-align:right;color:orange;'><b>"+BigDecimalUtil.formatNumber(mttEcart)+"</b></td>"
				+ "</tr>");
			
//			sb.append("<tr><td colspan='3'>----------------------------CALCUL------------------------</td></tr>");
			
			if(!BigDecimalUtil.isZero(caisseJourneeP.getMtt_annule())){
				sb.append("<tr style='background-color:white;'>"
						+ "<td style='color:red;'>ANNULATION CMD : </td>"
						+ "<td style='text-align:right;color:red;'>"+BigDecimalUtil.formatNumber(caisseJourneeP.getMtt_annule())+"</td>"
					+ "</tr>");
			}
			if(!BigDecimalUtil.isZero(caisseJourneeP.getMtt_annule_ligne())){
				sb.append("<tr style='background-color:white;'>"
						+ "<td style='color:red;'>ANNULATION LIGNE : </td>"
						+ "<td style='text-align:right;color:red;'>"+BigDecimalUtil.formatNumber(caisseJourneeP.getMtt_annule_ligne())+"</td>"
					+ "</tr>");
			}
			if(!BigDecimalUtil.isZero(caisseJourneeP.getMtt_art_offert())){
				sb.append("<tr style='background-color:white;'>"
						+ "<td>OFFERT : </td>"
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(caisseJourneeP.getMtt_art_offert())+"</td>"
					+ "</tr>");
			}
			if(!BigDecimalUtil.isZero(caisseJourneeP.getMtt_art_reduction())){
				sb.append("<tr style='background-color:white;'>"
						+ "<td>REDUCTIONS ARTICLES : </td>"
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(caisseJourneeP.getMtt_art_reduction())+"</td>"
					+ "</tr>");
			}
			if(!BigDecimalUtil.isZero(caisseJourneeP.getMtt_reduction())){
				sb.append("<tr style='background-color:white;'>"
						+ "<td>REDUCTIONS : </td>"
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(caisseJourneeP.getMtt_reduction())+"</td>"
					+ "</tr>");
			}
			if(!BigDecimalUtil.isZero(caisseJourneeP.getMtt_donne_point())){
				sb.append("<tr style='background-color:white;'>"
						+ "<td>POINTS : </td>"
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(caisseJourneeP.getMtt_donne_point())+"</td>"
					+ "</tr>");
			}
			if(!BigDecimalUtil.isZero(caisseJourneeP.getMtt_portefeuille())){
				sb.append("<tr style='background-color:white;'>"
						+ "<td>PORTEFEUILLE : </td>"
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(caisseJourneeP.getMtt_portefeuille())+"</td>"
					+ "</tr>");
			}
			sb.append("</table>");
			
			sb.append("<br>"
					+ "<br>"
					+ "<table>");
			
			boolean isDblCloture = !BigDecimalUtil.isZero(caisseJourneeP.getMtt_cloture_old_espece()) ;
   			BigDecimal mttEcartEsp = BigDecimalUtil.substract(caisseJourneeP.getMtt_cloture_caissier_espece(), caisseJourneeP.getMtt_espece(), caisseJourneeP.getMtt_ouverture());
			BigDecimal mttEcartCb = BigDecimalUtil.substract(caisseJourneeP.getMtt_cloture_caissier_cb(), caisseJourneeP.getMtt_cb());
			BigDecimal mttEcartChq = BigDecimalUtil.substract(caisseJourneeP.getMtt_cloture_caissier_cheque(), caisseJourneeP.getMtt_cheque());
			BigDecimal mttEcartDej = BigDecimalUtil.substract(caisseJourneeP.getMtt_cloture_caissier_dej(), caisseJourneeP.getMtt_dej());
   			
			sb.append("<tr style='background-color:orange;'>"
					+ "<td>MODE</td>"
					+ "<td>CALCUL SYSTEM</td>"
					+ "<td>CLOTURE CAISSIER</td>"
					+ (isDblCloture ? 
							"<td>CLÔTURE MANAGER</td>" : "")
					+"<td>ECART</td>"
				+ "</tr>");
			if(!BigDecimalUtil.isZero(caisseJourneeP.getMtt_espece())){
				sb.append("<tr style='background-color:white;'>"
						+ "<td>ESPECES : </td>"
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(caisseJourneeP.getMtt_espece())+"</td>"
						+ (isDblCloture ? "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(caisseJourneeP.getMtt_cloture_old_espece())+"</td>" : "")
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(caisseJourneeP.getMtt_cloture_caissier_espece())+"</td>"
						+ "<td style='text-align:right;color:orange;'>"+BigDecimalUtil.formatNumber(mttEcartEsp)+"</td>"
					+ "</tr>");
			}
			if(!BigDecimalUtil.isZero(caisseJourneeP.getMtt_cb())){
				sb.append("<tr style='background-color:white;'>"
						+ "<td>CARTE : </td>"
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(caisseJourneeP.getMtt_cb())+"</td>"
						+ (isDblCloture ? "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(caisseJourneeP.getMtt_cloture_old_cb())+"</td>" : "")
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(caisseJourneeP.getMtt_cloture_caissier_cb())+"</td>"
						+ "<td style='text-align:right;color:orange;'>"+BigDecimalUtil.formatNumber(mttEcartCb)+"</td>"
					+ "</tr>");
			}
			if(!BigDecimalUtil.isZero(caisseJourneeP.getMtt_cheque())){
				sb.append("<tr style='background-color:white;'>"
						+ "<td>CHEQUE : </td>"
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(caisseJourneeP.getMtt_cheque())+"</td>"
						+ (isDblCloture ? "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(caisseJourneeP.getMtt_cloture_old_cheque())+"</td>" : "")
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(caisseJourneeP.getMtt_cloture_caissier_cheque())+"</td>"
						+ "<td style='text-align:right;color:orange;'>"+BigDecimalUtil.formatNumber(mttEcartChq)+"</td>"
					+ "</tr>");
			}
			if(!BigDecimalUtil.isZero(caisseJourneeP.getMtt_dej())){
				sb.append("<tr style='background-color:white;'>"
						+ "<td>CHEQUE DEJ. : </td>"
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(caisseJourneeP.getMtt_dej())+"</td>"
						+ (isDblCloture ? "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(caisseJourneeP.getMtt_cloture_old_dej())+"</td>" : "")
						+ "<td style='text-align:right;'>"+BigDecimalUtil.formatNumber(caisseJourneeP.getMtt_cloture_caissier_dej())+"</td>"
						+ "<td style='text-align:right;color:orange;'>"+BigDecimalUtil.formatNumber(mttEcartDej)+"</td>"
					+ "</tr>");
			}
			
			sb.append("</table>");
					
			mapParams.put("2", sb.toString());
			
			MailQueuePersistant mail = new MailQueuePersistant();
			mail.setDestinataires(mailsShifts);
			mail.setExpediteur_mail(StrimUtil.getGlobalConfigPropertie("mail.sender"));
			mail.setExpediteur_nom("Caisse Manager");
			mail.setDate_mail(new Date());
			mail.setSource("SHIFT");
			mail.setSujet("Chiffre shift "+DateUtil.dateToString(opc_journee.getDate_journee())+" Shift "+DateUtil.dateToString(caisseJourneeP.getDate_ouverture(), "HH:mm:ss"));
			mail.setMessage(MailUtilService.getMailContent(mapParams, "SHIFT"));
			
			mail.setDate_mail(new Date());
			mail.setMail_signature("ADMIN");		
			mail.setDate_creation(new Date());
			//
			mailService.addMailToQueue(mail);
		}
	}
	
	@Override
	@Transactional
	@WorkModelMethodValidator
	public void gererPassasionShift(Long userOuvertureId, 
		CaisseJourneePersistant caisseJourneeP, BigDecimal mtt_ouverture, boolean isRectMode){
		EntityManager em = getEntityManager();	
		// Création nouveaux shift
		CaisseJourneePersistant caisseJourneePass = new CaisseJourneePersistant( );
		caisseJourneePass.setStatut_caisse(ContextAppli.STATUT_JOURNEE.OUVERTE.getStatut());
		caisseJourneePass.setOpc_user(findById(UserPersistant.class, userOuvertureId));
		caisseJourneePass.setOpc_caisse(caisseJourneeP.getOpc_caisse());
		caisseJourneePass.setOpc_journee(caisseJourneeP.getOpc_journee());
		caisseJourneePass.setMtt_ouverture(mtt_ouverture);
		caisseJourneePass.setDate_ouverture(new Date());
		
		// 
	  	caisseJourneeDao.create(caisseJourneePass);
	  	
	  	// On cloture directement celle ci pour eviter plusieurs résultats
	  	caisseJourneeP.setDate_cloture(new Date());
	  	caisseJourneeP.setStatut_caisse(ContextAppli.STATUT_JOURNEE.CLOTURE.getStatut());
	  	caisseJourneeDao.update(caisseJourneeP);
		
		Long journeeId = caisseJourneeP.getOpc_journee().getId();
		List<CaisseMouvementPersistant> listMvmTemp = getListCmdTemp(caisseJourneeP.getId());
		for(CaisseMouvementPersistant caisseMvm : listMvmTemp){
			caisseMvm.setOpc_caisse_journee(caisseJourneePass);
		}
		List<CaisseMouvementPersistant> listMvmNonPaye = getListCmdNonPaye(caisseJourneeP.getId());
		for(CaisseMouvementPersistant caisseMvm : listMvmNonPaye){
			caisseMvm.setOpc_caisse_journee(caisseJourneePass);
			em.merge(caisseMvm);
		}
	}
	
	@Override
	@Transactional
	public void refreshDataShift(Long caisseJourneeId) {
		CaisseJourneePersistant cjP = (CaisseJourneePersistant) findById(CaisseJourneePersistant.class, caisseJourneeId);
		setDataJourneeCaisseFromView(cjP);
		caisseJourneeDao.update(cjP);
	}
	
	@Override
	public void setDataJourneeCaisseFromView(CaisseJourneePersistant caisseJourneeP) {
		CaisseVenteView caisseJourneeView = (CaisseVenteView) getSingleResult(getQuery("from CaisseVenteView "
				+ "where caisse_journee_id=:caisseJourneeId")
				.setParameter("caisseJourneeId", caisseJourneeP.getId()));
		//
		if(caisseJourneeView == null) {
			caisseJourneeView = new CaisseVenteView();
		}
		caisseJourneeP.setMtt_espece(caisseJourneeView.getMtt_espece());
		caisseJourneeP.setMtt_cheque(caisseJourneeView.getMtt_cheque());
		caisseJourneeP.setMtt_dej(caisseJourneeView.getMtt_dej());
		caisseJourneeP.setMtt_cb(caisseJourneeView.getMtt_cb());
		
		caisseJourneeP.setMtt_portefeuille(caisseJourneeView.getMtt_portefeuille());
		caisseJourneeP.setMtt_donne_point(caisseJourneeView.getMtt_donne_point());
		
		caisseJourneeP.setMtt_total(caisseJourneeView.getMtt_total());
		caisseJourneeP.setMtt_total_net(caisseJourneeView.getMtt_total_net());
		caisseJourneeP.setMtt_annule(caisseJourneeView.getMtt_annule());
		caisseJourneeP.setMtt_annule_ligne(caisseJourneeView.getMtt_annul_ligne());
		caisseJourneeP.setMtt_reduction(caisseJourneeView.getMtt_reduction());
		caisseJourneeP.setMtt_art_offert(caisseJourneeView.getMtt_art_offert());
		caisseJourneeP.setMtt_art_reduction(caisseJourneeView.getMtt_art_reduction());
		caisseJourneeP.setNbr_vente(caisseJourneeView.getNbr_vente());
		
		caisseJourneeP.setNbr_livraison(caisseJourneeView.getNbr_livraison());
		caisseJourneeP.setMtt_marge_caissier(caisseJourneeView.getMtt_marge_caissier());
		
		// Ajout contrôle cohérence chiffre
		List<Object[]> listMvm = getNativeQuery("SELECT cmTtl.id2, cmTtl.ttl FROM caisse_mouvement cm "
			+ "INNER JOIN ("
			+ "		select cm2.id as id2, etablissement_id, "
			+ "		sum(cm2.mtt_donne+cm2.mtt_donne_cb+cm2.mtt_donne_cheque+cm2.mtt_donne_dej+cm2.mtt_donne_point+cm2.mtt_portefeuille) AS ttl "
			+ "FROM caisse_mouvement cm2 where cm2.last_statut!='ANNUL' AND cm2.mode_paiement IS not NULL " 
			+ "GROUP BY cm2.id) cmTtl ON cmTtl.id2=cm.id "
			+ "WHERE cm.mtt_commande_net!=cmTtl.ttl and cm.caisse_journee_id=:cjId")
			.setParameter("cjId", caisseJourneeP.getId())
			.getResultList();

		EntityManager em = getEntityManager();
		for (Object[] caisseMvmId : listMvm) {
			Long mvmId = Long.valueOf(""+caisseMvmId[0]);
			CaisseMouvementPersistant cmvP = findById(CaisseMouvementPersistant.class, mvmId);
			
			// Soustraction du montant de la commande du total donnée hors espece pour equilibrer
			
			BigDecimal mttFinal = (BooleanUtil.isTrue(cmvP.getIs_retour()) ? cmvP.getMtt_commande_net().abs() : cmvP.getMtt_commande_net()); 
			
			cmvP.setMtt_donne(
					BigDecimalUtil.substract(mttFinal,
						BigDecimalUtil.add(
								cmvP.getMtt_donne_cb(),
								cmvP.getMtt_donne_cheque(),
								cmvP.getMtt_donne_dej(),
								cmvP.getMtt_donne_point(),
								cmvP.getMtt_portefeuille()))
					);
			em.merge(cmvP);
		}
	}
	
	    @Override
	    public CaisseJourneePersistant getJourneCaisseOuverte(Long caisseId){
	         CaisseJourneePersistant caisseJourneeP = (CaisseJourneePersistant) getSingleResult(getQuery("from CaisseJourneePersistant where "
	                 + "opc_journee.statut_journee=:statut and opc_caisse.id=:caisseId and statut_caisse=:statut")
	                .setParameter("statut", ContextAppli.STATUT_JOURNEE.OUVERTE.getStatut())
	                .setParameter("caisseId", caisseId)
	         );
	        return caisseJourneeP; 
	    }

		@Override
		@Transactional
		public void validerLivraison(Long mvmId, Long livreurId) {
			if(mvmId == null) {
				return;
			}
			CaisseMouvementPersistant cmvP = (CaisseMouvementPersistant) findById(CaisseMouvementPersistant.class, mvmId);
			
//			if(cmvP.getOpc_livreur() != null && !cmvP.getOpc_livreur().getId().equals(ContextRestaurant.getUserBean().getOpc_employe().getId())) {
//				MessageService.addGrowlMessage("Affectation livreur", "Le livreur de cette commande ne peut être modifié que par le premier livreur l'ayant enregistrée ("+cmvP.getOpc_livreur().getNom()+")");
//				return;
//			}
			if(cmvP.getOpc_livreurU() == null) {
				cmvP.setOpc_livreurU(findById(UserPersistant.class, livreurId));
			} else {
				cmvP.setOpc_livreurU(null);
			}
			
			getEntityManager().merge(cmvP);
		}

		@Override
		@Transactional
		public void mergeCaisseCuisineConfig(CaisseBean caisseBean) {
			EntityManager entityManager = getEntityManager();
			CaissePersistant caisseP = caisseDao.findById(caisseBean.getId());
			
			caisseP.setNbr_max_cmd(caisseBean.getNbr_max_cmd());
			caisseP.setArticles_cmd(caisseBean.getArticles_cmd());
			caisseP.setFamilles_cmd(caisseBean.getFamilles_cmd());
			caisseP.setMenus_cmd(caisseBean.getMenus_cmd());
			caisseP.setIs_auto_cmd(caisseBean.getIs_auto_cmd());
			//
			entityManager.merge(caisseP);
		}

		@Override
		@Transactional
		public void transfererCmdCuisine(STATUT_CAISSE_MOUVEMENT_ENUM currStatut, Long mvmId, Long detailId, Long caisseId, Long caisseIdTarget) {
			CaisseMouvementPersistant caisseMvm = (CaisseMouvementPersistant) findById(CaisseMouvementPersistant.class, mvmId);
			
			if(!caisseMvm.getLast_statut().equals(currStatut.toString())){
				MessageService.addGrowlMessage("Statut non valide", "Cette commande a changé de staut. Veuillez <b>actualiser</b> l'écran.");
				return;
			}
			if(detailId != null){
				CaisseMouvementArticlePersistant caisseMvmArticle = (CaisseMouvementArticlePersistant) findById(CaisseMouvementArticlePersistant.class, detailId);
				if(caisseMvmArticle.getLast_statut() != null && !caisseMvmArticle.getLast_statut().equals(currStatut.toString())){
					MessageService.addGrowlMessage("Statut non valide", "Cette commande a changé de staut. Veuillez <b>actualiser</b> l'écran.");
					return;
				}
			}
			
			if(detailId != null){
				caisseMvm.setCaisse_cuisine(caisseMvm.getCaisse_cuisine().replaceAll(";"+caisseId+":"+detailId+";" , ";"+caisseIdTarget+":"+detailId+";"));
			} else{
				caisseMvm.setCaisse_cuisine(caisseMvm.getCaisse_cuisine().replaceAll(";"+caisseId , ";"+caisseIdTarget));
			}
			
			//
			getEntityManager().merge(caisseMvm);
		}

//		@Override
//		@Transactional
//		public void initMergeCaissePilotage(String cmdVal, String cmdEnPreparation, String methodeDispachNonAuto, String statut/*, String modeTransfert*/) {
//			EntityManager entityManager = getEntityManager();
//			ParametragePersistant paramValide = parametreService.getParameterByCode("ECRAN_CMD_VALIDE");
//			ParametragePersistant paramPreparation = parametreService.getParameterByCode("ECRAN_CMD_ENPREPARATION");
//			ParametragePersistant paramStrategieEcran = parametreService.getParameterByCode("ECRAN_STRATEGIE");
//			ParametragePersistant paramStatutEcran = parametreService.getParameterByCode("ECRAN_STATUT");
//			ParametragePersistant paramCmdAutoEcran = parametreService.getParameterByCode("ECRAN_CMD_AUTO");
//			
//			if(paramValide == null){
//				paramValide = new ParametragePersistant();
//				paramValide.setCode("ECRAN_CMD_VALIDE");
//				paramValide.setLibelle("Changer statut <b>VALIDEE</b> vers <b>EN PREPARATION</b> depuis");
//				paramValide.setGroupe("CUISINE_PIL");
//				paramValide.setHelp("Source du changement du statut de la commande de VALIDE à EN PREPARATION");
//				paramValide.setType("STRING");
//				paramValide.setDate_maj(new Date());
//				paramValide.setValeur(cmdVal);
//				paramValide.setAbonnement("SAT_CUISINE");
//				//
//				entityManager.merge(paramValide);
//			}
//			if(paramPreparation == null){
//				paramPreparation = new ParametragePersistant();
//				paramPreparation.setCode("ECRAN_CMD_ENPREPARATION");
//				paramPreparation.setLibelle("Changer statut <b>EN PREPARATION</b> vers <b>PRETE</b> depuis");
//				paramPreparation.setGroupe("CUISINE_PIL");
//				paramPreparation.setHelp("Source du changement du statut de la commande de EN PREPARATION à PRETE");
//				paramPreparation.setType("STRING");
//				paramPreparation.setDate_maj(new Date());
//				paramPreparation.setValeur(cmdEnPreparation);
//				paramPreparation.setAbonnement("SAT_CUISINE");
//				//
//				entityManager.merge(paramPreparation);
//			}
//			if(paramStrategieEcran == null){
//				paramStrategieEcran = new ParametragePersistant();
//				paramStrategieEcran.setCode("ECRAN_STRATEGIE");
//				paramStrategieEcran.setLibelle("Comment gérer les écrans non automatiques ?");
//				paramStrategieEcran.setGroupe("CUISINE_PIL");
//				paramStrategieEcran.setHelp("Statégie d'affichage des commandes dans les écrans de cuisine non automatiques");
//				paramStrategieEcran.setType("STRING");
//				paramStrategieEcran.setDate_maj(new Date());
//				paramStrategieEcran.setValeur(methodeDispachNonAuto);
//				paramStrategieEcran.setAbonnement("SAT_CUISINE");
//				//
//				entityManager.merge(paramStrategieEcran);
//				
//			}
//			if(paramStatutEcran == null){	
//				paramStatutEcran = new ParametragePersistant();
//				paramStatutEcran.setCode("ECRAN_STATUT");
//				paramStatutEcran.setLibelle("Onglets statuts à afficher dans la cuisine");
//				paramStatutEcran.setGroupe("CUISINE_PIL");
//				paramStatutEcran.setHelp("Onglets statuts commandes à afficher dans les écrans de cuisine");
//				paramStatutEcran.setType("STRING");
//				paramStatutEcran.setDate_maj(new Date());
//				paramStatutEcran.setValeur(statut);
//				paramStatutEcran.setAbonnement("SAT_CUISINE");
//				//
//				entityManager.merge(paramStatutEcran);
//			}
//			if(paramCmdAutoEcran == null){	
//				paramCmdAutoEcran = new ParametragePersistant();
//				paramCmdAutoEcran.setCode("ECRAN_CMD_AUTO");
//				paramCmdAutoEcran.setLibelle("Transférer automatiquement les commandes vers la cuisine en état EN PREPARATION");
//				paramCmdAutoEcran.setGroupe("CUISINE_PIL");
//				paramCmdAutoEcran.setType("BOOLEAN");
//				paramCmdAutoEcran.setDate_maj(new Date());
//				paramCmdAutoEcran.setAbonnement("SAT_CUISINE");
//				//
//				entityManager.merge(paramCmdAutoEcran);
//			}
//		}
		
//		@Override
//		@Transactional
//		public void mergeCaissePilotage(String cmdVal, String cmdEnPreparation, String methodeDispachNonAuto, String statut, Boolean isCmdAuto) {
//			EntityManager entityManager = getEntityManager();
//			ParametragePersistant paramValide = parametreService.getParameterByCode("ECRAN_CMD_VALIDE");
//			ParametragePersistant paramPreparation = parametreService.getParameterByCode("ECRAN_CMD_ENPREPARATION");
//			ParametragePersistant paramStrategieEcran = parametreService.getParameterByCode("ECRAN_STRATEGIE");
//			ParametragePersistant paramStatutEcran = parametreService.getParameterByCode("ECRAN_STATUT");
//			ParametragePersistant paramCmdAutoEcran = parametreService.getParameterByCode("ECRAN_CMD_AUTO");
//			
//			paramValide.setDate_maj(new Date());
//			paramValide.setValeur(cmdVal);
//			
//			paramPreparation.setDate_maj(new Date());
//			paramStrategieEcran.setValeur(methodeDispachNonAuto);
//			
//			paramStrategieEcran.setDate_maj(new Date());
//			paramPreparation.setValeur(cmdEnPreparation);
//			
//			paramStatutEcran.setDate_maj(new Date());
//			paramStatutEcran.setValeur(statut);
//			
//			paramCmdAutoEcran.setDate_maj(new Date());
//			paramCmdAutoEcran.setValeur(isCmdAuto.toString());
//			//
//			entityManager.merge(paramValide);
//			entityManager.merge(paramStrategieEcran);
//			entityManager.merge(paramPreparation);
//			entityManager.merge(paramStatutEcran);
//			entityManager.merge(paramCmdAutoEcran);
//		}
		
//		@Override
//		public void printEtiquettes(CaisseMouvementPersistant cmP) {
//			PrintEtiquetteCuisineUtil pu = new PrintEtiquetteCuisineUtil(cmP);
//			new PrintCommunUtil(pu.getPrintPosBean()).print();
//		}
		
		@Override
		@Transactional
		public void recalculHistoriqueAnnulation() {
			EntityManager em = getEntityManager();
			List<CaisseMouvementPersistant> listMvm = getQuery("from CaisseMouvementPersistant where (is_annule is null or is_annule=0)").getResultList();
			for (CaisseMouvementPersistant caisseMouvementP : listMvm) {
				BigDecimal mttlAnnul = null;
				for(CaisseMouvementArticlePersistant det : caisseMouvementP.getList_article()) {
					if(BooleanUtil.isTrue(det.getIs_annule())) {
						mttlAnnul = BigDecimalUtil.add(mttlAnnul, det.getMtt_total());
					}
				}
				caisseMouvementP.setMtt_annul_ligne(mttlAnnul);
				
				em.merge(caisseMouvementP);
			}
			List<CaisseVenteView> listMvmCaisse = getQuery("from CaisseVenteView").getResultList();
			for (CaisseVenteView caisseVenteView : listMvmCaisse) {
				CaisseJourneePersistant cjP = findById(CaisseJourneePersistant.class, caisseVenteView.getCaisse_journee_id());
				cjP.setMtt_annule_ligne(caisseVenteView.getMtt_annul_ligne());
				em.merge(cjP);
			}
			List<JourneeVenteView> listMvmJournee = getQuery("from JourneeVenteView").getResultList();
			for (JourneeVenteView journeeVenteView : listMvmJournee) {
				JourneePersistant cjP = findById(JourneePersistant.class, journeeVenteView.getJournee_id());
				cjP.setMtt_annule_ligne(journeeVenteView.getMtt_annul_ligne());
				em.merge(cjP);
			}
		}

		@Override
		@Transactional
		public void updateNbrOuvertureCaisse(Long journeeCaisseId) {
			CaisseJourneePersistant caisseJourneeP = (CaisseJourneePersistant) findById(CaisseJourneePersistant.class, journeeCaisseId);
			int nbr = (caisseJourneeP.getNbr_dash_open()!=null?caisseJourneeP.getNbr_dash_open():0);
			caisseJourneeP.setNbr_dash_open(nbr+1);
		}
		
//		@Override
//		public CaisseMouvementStatutPersistant getCaisseMvmStatutByStatutAndCaisseMvm(Long caisse_mvm_id, String statut) {
//			return (CaisseMouvementStatutPersistant) getSingleResult(getQuery("from CaisseMouvementStatutPersistant where statut_cmd=:statut and opc_caisse_mouvement.id=:mvmId")
//					.setParameter("statut", statut)
//					.setParameter("mvmId", caisse_mvm_id));
//		}
	
	@Override
	public Map<String, Integer[]> getEvolutionTempsCmd(Date date_debut, Date date_fin) {
		Map<String, Integer[]> mapData = new HashMap<>();
		
		List<EmployePersistant> listEmploye = caisseMvmStatutService.getCuisiniers();
		
		for (EmployePersistant employeP : listEmploye) {
			List<CaisseMouvementStatutPersistant> listCommandes = getQuery(
					"from CaisseMouvementStatutPersistant cs where "
					+ "cs.opc_employe.id=:emplId "
					+ "and cs.opc_caisse_mouvement.date_vente>=:dateDebut and cs.opc_caisse_mouvement.date_vente<=:dateFin "
					+ "order by cs.opc_caisse_mouvement.id, cs.id")
					.setParameter("dateDebut", date_debut)
					.setParameter("dateFin", date_fin)
					.setParameter("emplId", employeP.getId())
					.getResultList();
			
			int dureeTtl = 0;
			int nbrCmdTtl = 0;
			Long oldCmdId = null;
			
			CaisseMouvementStatutPersistant firstStatutCmdP = null;
			CaisseMouvementStatutPersistant lastStatutCmdP = null;
			
			int cpt = 0;
			for (CaisseMouvementStatutPersistant cmdP : listCommandes) {
				Long cmdId = cmdP.getOpc_caisse_mouvement().getId();
				//
				
				if(firstStatutCmdP == null) {
					firstStatutCmdP = cmdP;
				}
				boolean isPassed = false;
				if(cpt<listCommandes.size()-1 && listCommandes.get(cpt+1).getOpc_caisse_mouvement().getId() != cmdId) {
					lastStatutCmdP = listCommandes.get(cpt+1);
					isPassed= true;
				}
				
				if(oldCmdId == null || oldCmdId != cmdId) {
					nbrCmdTtl++;
				}
				if(lastStatutCmdP != null && firstStatutCmdP != null){
					long duree = (lastStatutCmdP.getDate_statut().getTime()) - (firstStatutCmdP.getDate_statut().getTime());
					Duration d = Duration.ofMillis(duree) ;
					long minutes = d.toMinutes();
					
					// Cmd moins de 30 min si non ...
					if(minutes < 30) {
						dureeTtl += minutes;
					}
				}
				
				if(isPassed) {
					firstStatutCmdP = null;
					lastStatutCmdP = null;
				}
				oldCmdId = cmdId;
				lastStatutCmdP = cmdP;
				cpt++;
			}
			if(nbrCmdTtl > 0) {
				mapData.put(employeP.getNom()+" "+StringUtil.getValueOrEmpty(employeP.getPrenom()), new Integer[]{dureeTtl, nbrCmdTtl});
			}
		}
			
		return mapData;
	}
	
//	@Override
//	public List<Date> getListDateStatutCmd(){
//		return getNativeQuery("select distinct(DATE(date_statut)) from caisse_mouvement_statut")
//				.getResultList();
//	}
	
	@Override
	public List<ArticlePersistant> getFavorisBalance(PagerBean pagerBean) {
		String req = "from ArticlePersistant where is_fav_balance is not null and is_fav_balance=1 "
				+ "and (is_disable is null or is_disable=0) ";
		
		if(pagerBean != null){
        	Long count = (Long) getQuery("select count(0) "+req).getSingleResult(); 
    		pagerBean.setNbrLigne(count.intValue());
		}
		Query query = getQuery(req + "order by opc_famille_stock.b_left, code, libelle");
		if(pagerBean != null){
        	query.setMaxResults(pagerBean.getElementParPage());
        	query.setFirstResult(pagerBean.getStartIdx());
        }
		
		return query.getResultList();
	}
	
	@Override
	public List<ArticlePersistant> getFavorisCaisse(PagerBean pagerBean){
		String req = "from ArticlePersistant where is_fav_caisse is not null "
				+ "and is_fav_caisse=1 and (is_disable is null or is_disable=0) ";
		
		if(pagerBean != null){
        	Long count = (Long) getQuery("select count(0) "+req).getSingleResult(); 
    		pagerBean.setNbrLigne(count.intValue());
		}
		String soft = StrimUtil.getGlobalConfigPropertie("context.soft");
		boolean isRestau = SOFT_ENVS.restau.toString().equals(soft);
		
		Query query = getQuery(req + "order by "+(isRestau?"opc_famille_cuisine":"opc_famille_stock")+".b_left, code, libelle");
		if(pagerBean != null){
        	query.setMaxResults(pagerBean.getElementParPage());
        	query.setFirstResult(pagerBean.getStartIdx());
        }
		
		return query.getResultList();
	}
	
	@Override
	@Transactional
	public String addArticleBalance(Long articleId, BigDecimal poids, boolean isCodeBarre) {
		Query query = getNativeQuery("select max(CAST(code AS UNSIGNED)) from article_balance");
		BigInteger max_num = (BigInteger)query.getSingleResult();
		BigInteger code = null;
		
		if(max_num != null){
			code = (max_num.add(new BigInteger("1")));
		} else{
			code = isCodeBarre ? new BigInteger("0") : new BigInteger("1");
		}
		
		String codeStr = code.toString();
		
		if(isCodeBarre){
			while(codeStr.length()<8){
				codeStr = "0" + codeStr;
			}
		}
		    
		ArticleBalancePersistant abP = new ArticleBalancePersistant();
		abP.setCode(codeStr);
		abP.setOpc_article(findById(ArticlePersistant.class, articleId));
		abP.setPoids(poids);
		abP.setOpc_user(ContextAppli.getUserBean());
		abP.setOpc_caisse(ContextAppliCaisse.getCaisseBean());
		abP.setOpc_journee(ContextAppliCaisse.getJourneeBean());
		
		getEntityManager().merge(abP);
		
		return codeStr;
	}
	
	@Override
	public List<ArticlePersistant> getListArticleCaisseActif() {
		return getQuery("from ArticlePersistant where (is_disable is null or is_disable=0) "
				+ "and (is_noncaisse is null or is_noncaisse = 0) "
				+ "order by libelle")
			.getResultList();
	}
	
	 @Override
	 @Transactional
	 public void gestionEcranImprimante(CaisseMouvementPersistant caisseMvm){
		  
    	String modeTravail =  ContextGloabalAppli.getGlobalConfig("MODE_TRAVAIL_CUISINE");
		modeTravail = (StringUtil.isEmpty(modeTravail) ? "EO" : modeTravail);// EcranOnly
    	final boolean isPrintOnly = "PO".equals(modeTravail) || "PE".equals(modeTravail);// PrintOnly or PrintEcran
    	
		String modeCuisine = ContextGloabalAppli.getGlobalConfig("ECRAN_STRATEGIE");
		
		CaisseMouvementPersistant caisseMvmDB = findById(CaisseMouvementPersistant.class, caisseMvm.getId());
		caisseMvm.setList_article(caisseMvmDB.getList_article());
		caisseMvm.setList_offre(caisseMvmDB.getList_offre());
		caisseMvm.setList_statut(caisseMvmDB.getList_statut());
		caisseMvm.setListEncaisse(caisseMvmDB.getListEncaisse());
		
		List<CaisseBean> listCaisseCuisine = getListCaisseCuisineActive();
		// Chercher s'il y a un écran cuisine avec emplacement
	    boolean isCuisineExist = false;
		for (CaissePersistant caisseCuisineP : listCaisseCuisine) {
	   		isCuisineExist = (caisseCuisineP.getOpc_stock_cible() != null);
	   		break;
		}
		 // Si pas d'écran cuisine alors tout se destock depuis la caisse
	     boolean articleCuisineExist = false;
	     for (CaisseMouvementArticlePersistant caisseMvmArt : caisseMvm.getList_article()) {
		       	if(caisseMvmArt.getOpc_article() != null && "C".equals(caisseMvmArt.getOpc_article().getDestination())) {
		       		articleCuisineExist = true;
		       		break;
		       	}
			}
	      if(isCuisineExist && articleCuisineExist) {
				// Envoi commande vers l'écran cuisine
		        manageEcranCuisine(caisseMvm, listCaisseCuisine, modeCuisine);
		    	// Gérer les impression cuisine
		        if(isPrintOnly){
		        	manageImprimanteCuisine(caisseMvm, listCaisseCuisine);
		   		}
		   	}
	 }
	 
	 @Transactional
    private void manageImprimanteCuisine(CaisseMouvementPersistant caisseMvmOri, List<CaisseBean> listCaisseCuisine){
    	Long journeeId = caisseMvmOri.getOpc_caisse_journee().getOpc_journee().getId();
        // Dans le cas de l'impression seule, on met le statut des éléments imprimés au statut en cours
    	EntityManager entityManager = getEntityManager();
    	
    	String STATUT_EN_PREP = ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.PREP.toString();
    	String STATUT_PRETE = ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.PRETE.toString();
    	String STATUT_VALIDE = ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.VALIDE.toString();
    	boolean isPrinted = false;
    	List<Long> listChangedIds = new ArrayList<>();
    	
    	List<String> listStatut = new ArrayList<>();
    	listStatut.add(STATUT_EN_PREP);
    	listStatut.add(STATUT_VALIDE);
    	
    	String request = "select distinct det.opc_mouvement_caisse FROM CaisseMouvementArticlePersistant det left join det.opc_article art "
         		+ "WHERE ((det.is_annule is null or det.is_annule = 0) or (det.is_annule is not null and det.is_annule = 1 and det.type_opr=3)) "
				+ "and (det.opc_mouvement_caisse.is_annule is null or det.opc_mouvement_caisse.is_annule = 0) "
         		+ "and det.last_statut is not null and det.last_statut in (:statut) "
         		+ "and ( (det.is_menu is not null and det.is_menu=1 ) or ((det.type_ligne='ART' or det.type_ligne='ART_MENU') and det.menu_idx is null and art.destination='C') or det.type_opr is not null ) " //and det.is_menu=1
            	+ "and det.opc_mouvement_caisse.caisse_cuisine like :caisseId "
            	+ "and det.opc_mouvement_caisse.opc_caisse_journee.opc_journee.id=:journeeId "
         		+ "order by det.opc_mouvement_caisse.id desc, det.idx_element";
        
    	Map<CaissePersistant, List<CaisseMouvementPersistant>> mapCaisseMvm = new HashMap<>();
    	for (CaissePersistant caisseP : listCaisseCuisine) {
    		String[] listImprimante = StringUtil.getArrayFromStringDelim(caisseP.getImprimantes(), "|");
        	if(listImprimante == null || listImprimante.length == 0) {
        		continue;
        	}
	        Query query = getQuery(request)
	    		.setParameter("statut", listStatut)
	            .setParameter("journeeId", journeeId)
	          	.setParameter("caisseId", "%;"+caisseP.getId()+":%");
		
           List<CaisseMouvementPersistant> listCaisseMvm = query.getResultList();
           if(listCaisseMvm.size() > 0){
        	   mapCaisseMvm.put(caisseP, listCaisseMvm);
           }
    	}
    	
  	  	//
  	  	if(mapCaisseMvm.size() > 0) {
	        for (CaissePersistant caisseP : mapCaisseMvm.keySet()) {
	        	List<CaisseMouvementPersistant> listCaisseMvm = mapCaisseMvm.get(caisseP);
	           // Impression en cas d'utilisation d'imprimantes sans écrans
	           if(listCaisseMvm.size() > 0){
	        	  for (CaisseMouvementPersistant caisseMvm : listCaisseMvm) {
	        		  // Vérifier si cette caisse fait partie de la conf
	        		  List<Long> listDetailConfCaisse = new ArrayList<>();
	                  String[] caisseDestArray = StringUtil.getArrayFromStringDelim(caisseMvm.getCaisse_cuisine(), ";");
	                  if(caisseDestArray != null){
	           			for(String caisseElement : caisseDestArray){
	           				String[] caisseElementArray = StringUtil.getArrayFromStringDelim(caisseElement, ":");
	           				Long caisseId = Long.valueOf(caisseElementArray[0]);
	           				if(caisseP.getId() == caisseId){
	           					if(StringUtil.isNotEmpty(caisseElementArray[1])){
		           					Long elementId = Long.valueOf(caisseElementArray[1]);
		           					listDetailConfCaisse.add(elementId);
	           					}
	           				}
	           			}
	           		 }
	                  
	                  String mnuIdxCaisseConfig = null;
	    			  boolean isInCaisseConf = false;
	    			  // Détail
	            	  for (CaisseMouvementArticlePersistant detail : caisseMvm.getList_article()) {
	            		  if( (!STATUT_CAISSE_MOUVEMENT_ENUM.PRETE.toString().equals(detail.getLast_statut()) || BooleanUtil.isTrue(detail.getIs_menu()) )
	            				  			&& !STATUT_CAISSE_MOUVEMENT_ENUM.LIVRE.toString().equals(detail.getLast_statut())
	            				  			&& listDetailConfCaisse != null 
	            				  			&& (listDetailConfCaisse.contains(detail.getId()) || (detail.getMenu_idx() != null && detail.getMenu_idx().equals(mnuIdxCaisseConfig)))){
					    		if(detail.getMenu_idx() != null){
					    			mnuIdxCaisseConfig = detail.getMenu_idx();
					    		} else{
					    			mnuIdxCaisseConfig = null;
					    		}
					    		isInCaisseConf = true;
					    		//break;
					    		
					    		listChangedIds.add(detail.getId());
	            		  }
	            	  }
	                  
	                  // Impression
	            	  if(isInCaisseConf){
	            		  // Imprimer cuisine
	            		  new PrintCuisineUtil(caisseP, caisseMvm).print();
	            		  isPrinted = true;
	            		  
	            		  getQuery("update CaisseMouvementArticlePersistant cm set cm.type_opr = null "
		            		  		+ "where cm.opc_mouvement_caisse.id=:mvmId")
		      		 		   .setParameter("mvmId", caisseMvm.getId())
		      		 		   .executeUpdate();
	            		  
	            		  getQuery("update CaisseMouvementArticlePersistant cm set cm.last_statut=:lastStatut "
		            		  		+ "where cm.opc_mouvement_caisse.id=:mvmId "
		            		  		+ "and cm.is_annule is not null "
		            		  		+ "and cm.is_annule = 1 ")
		      		 		   .setParameter("mvmId", caisseMvm.getId())
		      		 		   .setParameter("lastStatut", STATUT_PRETE)
		      		 		   .executeUpdate();
	            		  
	            		  getQuery("update CaisseMouvementArticlePersistant cm set cm.last_statut=:lastStatut "
	            		  		+ "where (cm.is_annule is null or cm.is_annule = 0) "
	            		  		+ "and ( (cm.is_menu is not null and cm.is_menu=1) or (cm.type_ligne='ART' and cm.menu_idx is null) ) "
	            		  		+ "and cm.last_statut is not null "
	            		  		+ "and cm.opc_mouvement_caisse.id=:mvmId")
	      		 		   .setParameter("mvmId", caisseMvm.getId())
	      		 		   .setParameter("lastStatut", STATUT_PRETE)
	      		 		   .executeUpdate();
	            		  
	            		  getQuery("update CaisseMouvementPersistant cm set cm.last_statut=:lastStatut "
	            		  		+ "where cm.id=:currMvm")
			       			.setParameter("currMvm", caisseMvm.getId())
			       			.setParameter("lastStatut", STATUT_PRETE)
			       			.executeUpdate();
		        		  
	            		  entityManager.flush();
	            	  }
	        	  }
	           }
	       }
  	  	}
  	  	
		 // Imprimer pilotage
  	  	 if(isPrinted){
			  List<CaisseBean> listPilotage = getListCaissePilotageActive();
			  for (CaissePersistant caisseBean : listPilotage) {
				  String[] listImprimante = StringUtil.getArrayFromStringDelim(caisseBean.getImprimantes(), "|");
		        	if(StringUtil.isEmpty(listImprimante) || listImprimante.length == 0) {
		        		continue;
		        	}
				  new PrintPilotageUtil(caisseBean, caisseMvmOri, listChangedIds).print();
			  }
  	  	 }
    }
 
	 /**
	     * @param caisseMvm
	     */
	    @Transactional
	    private void manageEcranCuisine(CaisseMouvementPersistant caisseMvm, List<CaisseBean> listCaisseCuisine, String modeCuisine){
	    	// Si pas de caisse actives
	    	if(listCaisseCuisine.size() == 0){
	    		return;
	    	}
	    	// Si pas de caisse automatique
	    	boolean isCaisseAuto = false;
	    	for (CaissePersistant caisseBean : listCaisseCuisine) {//---------------------------------------
				if(BooleanUtil.isTrue(caisseBean.getIs_auto_cmd())){
					isCaisseAuto = true;
					break;
				}
	    	}

	    	boolean isAllEcran = "D".equals(modeCuisine);
	    	List<CaisseMouvementArticlePersistant> listDetail = caisseMvm.getList_article();
	    	for (CaisseMouvementArticlePersistant caisseMvmArtP : listDetail) {//-----------------------------------------------
				String caisseId = null;
				boolean isMenu = BooleanUtil.isTrue(caisseMvmArtP.getIs_menu());
				boolean isArticle = caisseMvmArtP.getType_ligne().equals(ContextAppli.TYPE_LIGNE_COMMANDE.ART.toString()) 
								&& caisseMvmArtP.getMenu_idx() == null 
								&& "C".equals(caisseMvmArtP.getOpc_article().getDestination());
				boolean isArtMenu = caisseMvmArtP.getType_ligne().equals(ContextAppli.TYPE_LIGNE_COMMANDE.ART_MENU.toString())
						&& caisseMvmArtP.getMenu_idx() != null 
						&& "C".equals(caisseMvmArtP.getOpc_article().getDestination());
				isArticle = (isArticle || isArtMenu);
				
				//
				
				if(!isMenu && !isArticle){
					continue;
				}
		    	
				// On dispatch d'abord vers la caisse automatique
		    	if(isCaisseAuto){ 
		    		int nbrCmdCaisse = 0;
		    		int idx = 0;
					for (CaissePersistant caisseBean : listCaisseCuisine) {//---------------------------------------
						if(BooleanUtil.isTrue(caisseBean.getIs_auto_cmd())){
							String[] menus = StringUtil.getArrayFromStringDelim(caisseBean.getMenus_cmd(), ";");
							String[] articles = StringUtil.getArrayFromStringDelim(caisseBean.getArticles_cmd(), ";");
							String[] familles = StringUtil.getArrayFromStringDelim(caisseBean.getFamilles_cmd(), ";");
							
							//
							boolean isPassed = false;
							if(isMenu && menus != null && menus.length > 0 && StringUtil.contains(caisseMvmArtP.getElementId().toString(), menus)){
								isPassed = true;
							} else if(isArticle && articles != null && articles.length > 0 && StringUtil.contains(caisseMvmArtP.getElementId().toString(), articles)){
								isPassed = true;
							} else if(isArticle && familles != null && familles.length > 0 && StringUtil.contains(caisseMvmArtP.getOpc_article().getOpc_famille_cuisine().getId().toString(), familles)){
								isPassed = true;
							} else if(isArtMenu && menus != null && menus.length > 0 && caisseMvmArtP.getOpc_menu() != null && StringUtil.contains(caisseMvmArtP.getOpc_menu().getId().toString(), menus)){
								isPassed = true;
							}
							
							if(isPassed){
								if(isAllEcran){
									caisseId = (caisseId!=null?caisseId+";":"")+caisseBean.getId()+":"+caisseMvmArtP.getId();
								} else{
									// Mouvement valides
									List<CaisseMouvementPersistant> listMvmCaisse = getQuery("from CaisseMouvementPersistant "
											+ "where caisse_cuisine is not null and caisse_cuisine like :caisseId "
											+ "and last_statut in ('"+ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.PREP.toString()+"','"+ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.VALIDE.toString()+"') "
											+ " and opc_caisse_journee.opc_journee.id=:journeeId")
											.setParameter("caisseId", "%;"+caisseBean.getId()+":%")
										.setParameter("journeeId", caisseMvm.getOpc_caisse_journee().getOpc_journee().getId())
										.getResultList();
									// Envoi vers l'écran ayant le moins de commandes
							    	 if(idx == 0 || listMvmCaisse.size() < nbrCmdCaisse){
							    		caisseId =  caisseBean.getId()+":"+caisseMvmArtP.getId();
							    		nbrCmdCaisse = listMvmCaisse.size();
							    		idx++;
							    	}
								}
							}
						}
					}
		    	}
	    	
				// Ecran cuisine à gestion non automatique
		    	int nbrCmdCaisse = 0;
		    	int idx = 0;
				if(caisseId == null && (isMenu || isArticle)){
					for (CaissePersistant caisseBean : listCaisseCuisine) {//---------------------------------------
						if(!BooleanUtil.isTrue(caisseBean.getIs_auto_cmd())){
							//
							if(isAllEcran){
								caisseId = (caisseId!=null?caisseId+";":"")+caisseBean.getId()+":"+caisseMvmArtP.getId();
							} else{
								// Mouvement valides
								List<CaisseMouvementPersistant> listMvmCaisse = getQuery("from CaisseMouvementPersistant "
										+ "where caisse_cuisine is not null and caisse_cuisine like :caisseId "
										+ "and last_statut in ('"+ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.PREP.toString()+"','"+ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.VALIDE.toString()+"') "
												+ " and opc_caisse_journee.opc_journee.id=:journeeId")
									.setParameter("caisseId", "%;"+caisseBean.getId()+":%")
									.setParameter("journeeId", caisseMvm.getOpc_caisse_journee().getOpc_journee().getId())
									.getResultList();
								// Envoi vers l'écran ayant le moins de commandes
						    	 if(idx == 0 || listMvmCaisse.size() < nbrCmdCaisse){
						    		caisseId =  caisseBean.getId()+":"+caisseMvmArtP.getId();
						    		nbrCmdCaisse = listMvmCaisse.size();
						    		idx++;
						    	}
							}
						}
					}
					// Si rien n'est trouvé, alors on attribue à la première caisse ==> Cas article,menu aussi ??
					if(caisseId == null && isArticle){
						for (CaissePersistant caisseBean : listCaisseCuisine) {//---------------------------------------
							caisseId = caisseBean.getId()+":"+caisseMvmArtP.getId();
							break;
						}
					}
				}
				
				//
				if(caisseId != null && (caisseMvm.getCaisse_cuisine() == null || caisseMvm.getCaisse_cuisine().indexOf(";"+caisseId+";") == -1)){
					caisseMvm.setCaisse_cuisine((StringUtil.isEmpty(caisseMvm.getCaisse_cuisine()) ? ";":caisseMvm.getCaisse_cuisine())+caisseId+";");
				}
	    	}
			//
	    	EntityManager em = getEntityManager();
			if(StringUtil.isNotEmpty(caisseMvm.getCaisse_cuisine())){
	    		em.merge(caisseMvm);    		
	    	}
			//
	    	em.flush();
	    }
	    
	    @Override
	    public List<CaisseMouvementPersistant> getListCmdTemp(Long caisseJourneeId){
	 		if(caisseJourneeId == null){
	 			return null;
	 		}
 			List<CaisseMouvementPersistant> listCmdEnAttente = caisseDao.getQuery("from CaisseMouvementPersistant"
 				+ " where last_statut = :statut "
 				+ " and opc_caisse_journee.id=:caisseJId")
 				.setParameter("statut", ContextAppli.STATUT_CAISSE_MOUVEMENT_ENUM.TEMP.toString())
 				.setParameter("caisseJId", caisseJourneeId)
 				.getResultList();
 			
 			return listCmdEnAttente;
	    }
	    @Override
	    public List<CaisseMouvementPersistant> getListCmdNonPaye(Long caisseJourneeId){
	    	if(caisseJourneeId == null){
	 			return null;
	 		}
	    	
	 		List<CaisseMouvementPersistant> listCmdNonPaye = caisseDao.getQuery("from CaisseMouvementPersistant "
	 				+ " where last_statut not in ('"+STATUT_CAISSE_MOUVEMENT_ENUM.TEMP+"', '"+STATUT_CAISSE_MOUVEMENT_ENUM.ANNUL+"')"
	 				+ " and mode_paiement is null "
	 				+ " and opc_caisse_journee.id=:caisseJId")
	 				.setParameter("caisseJId", caisseJourneeId)
	 				.getResultList();
	 		
	 		return listCmdNonPaye;
	 	}

		@Override
		@Transactional
		public Set<Long>[] deplacerMouvement(Long shiftTargetId, Long[] checkedTr) {
			EntityManager em = getEntityManager();
			CaisseJourneePersistant caisseJourneeTarget = findById(CaisseJourneePersistant.class, shiftTargetId);
			JourneePersistant journeeP = caisseJourneeTarget.getOpc_journee();
			//
			Set<Long> listJourneeImpactees = new HashSet<>();
			Set<Long> listShiftImpactees = new HashSet<>();
			
			listJourneeImpactees.add(journeeP.getId());
			listShiftImpactees.add(caisseJourneeTarget.getId());
			
			for (Long caissMvmId : checkedTr) {
				CaisseMouvementPersistant caisseMvmP = findById(CaisseMouvementPersistant.class, caissMvmId);
				
				CaisseJourneePersistant opc_caisse_journee = caisseMvmP.getOpc_caisse_journee();
				listJourneeImpactees.add(opc_caisse_journee.getOpc_journee().getId());
				listShiftImpactees.add(opc_caisse_journee.getId());
				
				caisseMvmP.setOpc_caisse_journee(caisseJourneeTarget);
				em.merge(caisseMvmP);
			}
			
			return new Set[]{listJourneeImpactees, listShiftImpactees};
		}

		@Override
		@Transactional
	    public void updateChiffresShiftJour(Long journeeId, Long shiftId){
			EntityManager em = getEntityManager();
			
			CaisseJourneePersistant caisseJourneeP = findById(CaisseJourneePersistant.class, shiftId);
			setDataJourneeCaisseFromView(caisseJourneeP);
			
			em.merge(caisseJourneeP);
			em.flush();
			
			JourneePersistant journeeImpactP = findById(JourneePersistant.class, journeeId);
			journeeService.setDataJourneeFromView(journeeImpactP);
			em.merge(journeeImpactP);
		}
		
		@Override
		@Transactional
	    public void updateChiffresShiftJour(Set<Long> listJrnIds, Set<Long> listShiftIds){
	    	EntityManager em = getEntityManager();
	    	//
			for (Long shiftId : listShiftIds) {
				CaisseJourneePersistant caisseJourneeP = findById(CaisseJourneePersistant.class, shiftId);
				setDataJourneeCaisseFromView(caisseJourneeP);
				
				em.merge(caisseJourneeP);
			}
			em.flush();
			//
			for (Long journeeId : listJrnIds) {
				JourneePersistant journeeImpactP = findById(JourneePersistant.class, journeeId);
				journeeService.setDataJourneeFromView(journeeImpactP);
				em.merge(journeeImpactP);
			}
	    }

		@Override
		@Transactional
		public void recalculMouvementsAchat() {
			EntityManager em = getEntityManager();
			
			List<MouvementPersistant> listAchat = getQuery("from MouvementPersistant where type_mvmnt='a'").getResultList();
			// MAj prix
			BigDecimal mttTotalHtTotal = null, 
					mttTotalTtcTotal = null, 
					mttTotalHtRemise = null,
					mttTotalTTCRemise = null;
			//
			for (MouvementPersistant mouvementP : listAchat) {
				List<MouvementArticlePersistant> listArt = mouvementP.getList_article();
				for (MouvementArticlePersistant mvmArtP : listArt) {
					BigDecimal mttTotalHt = BigDecimalUtil.multiply(mvmArtP.getPrix_ht(), mvmArtP.getQuantite());
					BigDecimal mttTotalTtc = BigDecimalUtil.multiply(mvmArtP.getPrix_ttc(), mvmArtP.getQuantite());
					boolean isPourcent = BooleanUtil.isTrue(mvmArtP.getIs_remise_ratio());
					//
					if(!BigDecimalUtil.isZero(mvmArtP.getRemise())){
						BigDecimal remise = null;
						if(isPourcent){
							remise  = BigDecimalUtil.divide(BigDecimalUtil.multiply(mttTotalHt, mvmArtP.getRemise()), BigDecimalUtil.get(100));
						} else{
							remise = mvmArtP.getRemise();					
						}
						mttTotalHtRemise = BigDecimalUtil.add(mttTotalHtRemise, remise);
						
						// Remise TTC
						BigDecimal tva = null, remiseTtc = remise;
						if(mvmArtP.getOpc_tva_enum() != null){
							ValTypeEnumPersistant tvaBean = valEnumService.findById(mvmArtP.getOpc_tva_enum().getId());
							tva = BigDecimalUtil.ZERO;
							//
							if(NumericUtil.isNum(tvaBean.getLibelle()) || NumericUtil.isDecimal(tvaBean.getLibelle())){
								tva = BigDecimalUtil.get(tvaBean.getLibelle());
							} else if(NumericUtil.isNum(tvaBean.getCode()) || NumericUtil.isDecimal(tvaBean.getCode())){
								tva = BigDecimalUtil.get(tvaBean.getCode());
							}
							remiseTtc = BigDecimalUtil.divide(BigDecimalUtil.multiply(remise, tva), BigDecimalUtil.get(100));
							remiseTtc = BigDecimalUtil.add(remiseTtc, remise);
						}
						mttTotalTTCRemise = BigDecimalUtil.add(mttTotalTTCRemise, remiseTtc);
						
						mttTotalHt = BigDecimalUtil.substract(mttTotalHt, remise);
						mttTotalTtc = BigDecimalUtil.substract(mttTotalTtc, remiseTtc);
					}
					mvmArtP.setPrix_ht_total(mttTotalHt);
					mvmArtP.setPrix_ttc_total(mttTotalTtc);
					
					// Calcul total mouvement
					mttTotalHtTotal = BigDecimalUtil.add(mttTotalHtTotal, mttTotalHt);
					mttTotalTtcTotal = BigDecimalUtil.add(mttTotalTtcTotal, mttTotalTtc);
			}
			mouvementP.setMontant_ht(mttTotalHtTotal);
			mouvementP.setMontant_tva(BigDecimalUtil.substract(mttTotalTtcTotal, mttTotalHtTotal));
			mouvementP.setMontant_ttc(mttTotalTtcTotal);
			mouvementP.setMontant_ht_rem(mttTotalHtRemise);
			mouvementP.setMontant_ttc_rem(mttTotalTTCRemise);
			
			em.merge(mouvementP);
		}
	}
		
	@Override
	@Transactional
	public void deleteCaisseInfoInv(Set<Long> listFamIds, boolean isRestau) {
		EntityManager em = getEntityManager();
		
		List<ArticleStockCaisseInfoPersistant> listFam = getQuery("from ArticleStockCaisseInfoPersistant "
				+ "where opc_article."+(isRestau ? "opc_famille_cuisine.id" : "opc_famille_stock.id")
				+" not in (:famIds)")
		.setParameter("famIds", listFamIds)
		.getResultList();
		
		for (ArticleStockCaisseInfoPersistant articleStockCaisseInfoP : listFam) {
			em.remove(articleStockCaisseInfoP);
		}
	}
	
	@Override
	@Transactional
	public void updateCaisseInfoInv(String[] famInvFilter,
			List<ArticleStockCaisseInfoPersistant> listInventaireDetIHM,
			CaisseMouvementPersistant CURRENT_COMMANDE) {
		EmplacementPersistant opcEmplacement = ContextAppliCaisse.getCaisseBean().getOpc_stock_cible();
		
		Map<Long, BigDecimal> mapQteCmdTmp = caisseWebService.getEtatStockCmdTmp(CURRENT_COMMANDE, ContextAppliCaisse.getJourneeBean().getId());
		
		//
		for (ArticleStockCaisseInfoPersistant inventaireDetailP : listInventaireDetIHM) {
			BigDecimal qte_reel = inventaireDetailP.getQte_reel();
			
			List<ArticleStockCaisseInfoPersistant> listArt = getQuery("from ArticleStockCaisseInfoPersistant "
					+ "where opc_emplacement.id=:emplId "
					+ "and opc_article.id=:artId")
					.setParameter("emplId", opcEmplacement.getId())
					.setParameter("artId", inventaireDetailP.getOpc_article().getId())
					.getResultList();
			inventaireDetailP = (listArt.size() > 0 ? listArt.get(0) : inventaireDetailP);
			inventaireDetailP.setQte_reel(BigDecimalUtil.add(qte_reel, mapQteCmdTmp.get(inventaireDetailP.getOpc_article().getId())));
			inventaireDetailP.setOpc_emplacement(opcEmplacement);
			//
			mergeEntity(inventaireDetailP);
		}
		
		String famStr = "";
		for (String fam : famInvFilter) {
			famStr += fam + ";";
		}
		
		// Sauvegarder conf ets
		EtablissementPersistant etsP = findById(EtablissementPersistant.class, ContextAppli.getEtablissementBean().getId());
		etsP.setFam_caisse_inv(famStr);
		mergeEntity(etsP);
	}
	
	@Override
	@Transactional
	@WorkModelMethodValidator
	public void delete(Long caisseId){
		getQuery("delete from ParametragePersistant where opc_terminal.id=:terminalId")
			.setParameter("terminalId", caisseId)
			.executeUpdate();
		
		if(ContextAppli.IS_RESTAU_ENV()) {
			getQuery("delete from CuisineJourneePersistant where opc_cuisine.id=:terminalId")
				.setParameter("terminalId", caisseId)
				.executeUpdate();
		}
		
		getQuery("delete from ArticleBalancePersistant where opc_caisse.id=:terminalId")
		.setParameter("terminalId", caisseId)
		.executeUpdate();
		
		getEntityManager().flush();
		
		super.delete(caisseId);
	}
}  

