
package appli.controller.domaine.administration.action;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Order;
import org.hibernate.proxy.HibernateProxy;

import appli.controller.domaine.administration.ParametrageRightsConstantes;
import appli.controller.domaine.administration.bean.EtablissementBean;
import appli.controller.domaine.administration.bean.UserBean;
import appli.controller.domaine.personnel.action.paie.PointageAction;
import appli.controller.domaine.util_erp.ContextAppli;
import appli.controller.domaine.util_erp.ContextAppli.APPLI_ENV;
import appli.controller.domaine.util_erp.ContextAppli.SOFT_ENVS;
import appli.controller.domaine.util_erp.ContextAppli.TYPE_CAISSE_ENUM;
import appli.controller.util_ctrl.ApplicationListener;
import appli.model.domaine.administration.persistant.NotificationPersistant;
import appli.model.domaine.administration.service.IEtablissementService;
import appli.model.domaine.administration.service.IParametrageService;
import appli.model.domaine.administration.service.IUserService;
import appli.model.domaine.administration.service.impl.ParametrageService;
import appli.model.domaine.compta.service.IExerciceService;
import appli.model.domaine.habilitation.persistant.ProfileMenuPersistant;
import appli.model.domaine.habilitation.persistant.ProfilePersistant;
import appli.model.domaine.habilitation.service.IProfileMenuService;
import appli.model.domaine.vente.persistant.CaisseJourneePersistant;
import appli.model.domaine.vente.persistant.CaisseMouvementPersistant;
import appli.model.domaine.vente.persistant.CaissePersistant;
import appli.model.domaine.vente.persistant.JourneePersistant;
import framework.controller.ActionUtil;
import framework.controller.ContextGloabalAppli;
import framework.controller.ControllerUtil;
import framework.controller.FileUtilController;
import framework.controller.annotation.WorkController;
import framework.controller.annotation.WorkForward;
import framework.model.beanContext.AbonnePersistant;
import framework.model.beanContext.AbonnementBean;
import framework.model.beanContext.EtablissementPersistant;
import framework.model.beanContext.ExerciceBean;
import framework.model.beanContext.SocietePersistant;
import framework.model.common.constante.ProjectConstante;
import framework.model.common.service.MessageService;
import framework.model.common.util.BooleanUtil;
import framework.model.common.util.DateUtil;
import framework.model.common.util.DateUtil.TIME_ENUM;
import framework.model.common.util.EncryptionEtsUtil;
import framework.model.common.util.NumericUtil;
import framework.model.common.util.ReflectUtil;
import framework.model.common.util.ServiceUtil;
import framework.model.common.util.StrimUtil;
import framework.model.common.util.StringUtil;
import framework.model.util.audit.ReplicationGenerationEventListener;

/**
 * @author 
 *
 */
@WorkController(nameSpace="commun") 
public class LoginAction {
	@Inject
	private IUserService userService;
	@Inject
	private IProfileMenuService profilMenuService;
	@Inject
	private IParametrageService paramsService;
	@Inject
	private IExerciceService exerciceService;
	@Inject
	private IProfileMenuService profileMenuService;
	@Inject
	private IEtablissementService etablissementService;
	
	private final static Logger LOGGER = Logger.getLogger(LoginAction.class);
	
    public void connect(ActionUtil httpUtil) throws Exception{
    	
    	boolean isIntenetAvailable = FileUtilController.isInternetAvailable();
    	
		HttpServletRequest request = httpUtil.getRequest();
		String mail = (String)ControllerUtil.getParam(request, "login");
		String pw = (String)ControllerUtil.getParam(request, "password");
		String env = (String)ControllerUtil.getParam(request, "env");
		if(StringUtil.isEmpty(env)) {
			env = httpUtil.getRequest().getParameter("env");
		}
		
		request.getSession(true).removeAttribute("ENV_MOBILE");// Pour ne pas mélanger les session avec espace mobile	
		
		String badge = (String)ControllerUtil.getParam(request, "tkn");
		String soft = StrimUtil.getGlobalConfigPropertie("context.soft");
		boolean isMobile = APPLI_ENV.cais_mob.toString().equals(env);
		
		EncryptionEtsUtil encryptionUtil = new EncryptionEtsUtil(EncryptionEtsUtil.getDecrypKey());
		
		if(!SOFT_ENVS.erp.toString().equals(soft)
				&& !SOFT_ENVS.syndic.toString().equals(soft)
				&& !SOFT_ENVS.agri.toString().equals(soft)){
			if(StringUtil.isEmpty(env)) {
				MessageService.addGrowlMessage("Environnement", "<b>Veuillez sélectionner un environnement.<b>");
				return;
			}
		}
		UserBean userBean = null;
		if(StringUtil.isNotEmpty(badge)){
			userBean = userService.getUserByBadge(badge.trim()); 
			if(userBean == null){
				MessageService.addGrowlMessage("Badge non enregistré", "Ce badge n'a pas été encore enregistré.");
				return;
			}
			// Si le compte est désactivé
			if(BooleanUtil.isTrue(userBean.getIs_desactive())){
				MessageService.addGrowlMessage("Compte désactivé", "Cet utilisateur est désactivé");
				return;
			}
		} else {
			mail = (mail != null) ? mail.trim() : null; 
			pw = (pw != null) ? pw.trim() : null;
			//
			userBean = userService.getUserByLoginAndPw(mail, encryptionUtil.encrypt(pw)); 
			if(userBean == null){
				if(isMobile){
					MessageService.addGrowlMessage("", "Le login ou le mot de passse est erroné");	
				} else{
					MessageService.addFieldMessage("login", "Le login ou le mot de passse est erroné");
				}
				
				return;
			}
			// Si le compte est désactivé
			if(BooleanUtil.isTrue(userBean.getIs_desactive())){
				if(isMobile){
					MessageService.addGrowlMessage("", "L'utilisateur est désactivé");
				} else{
					MessageService.addFieldMessage("login", "L'utilisateur est désactivé");
				}
				return;
			}
		}
		
		ProfilePersistant opc_profile1 = userBean.getOpc_profile();
		
		if(BooleanUtil.isTrue(opc_profile1.getIs_desactive())) {
			MessageService.addGrowlMessage("Profile désactivé", "Le profile de cet utilisateur est désactivé");
			return;
		}
		
		// Bloquer l'accès aux client web
		if("CLIENT".equals(opc_profile1.getCode())) {
			MessageService.addGrowlMessage("Profile", "Votre profile ne permet d'accèder à cet environnement");
			return;
		}
		
		// Controler les droits d'accès
		if(StringUtil.isNotEmpty(userBean.getAllEnvs())) {
			if(!checkDoitProfileEnv(userBean.getAllEnvs(), env)) {
				MessageService.addGrowlMessage("Profile", "Votre profile ne permet d'accèder à cet environnement");
				return;
			}
		} else {
			if(userBean.isInProfile("LIVREUR") || userBean.isInProfile("SERVEUR") || userBean.isInProfile("CAISSIER")){
				if(!(env.equals(ContextAppli.APPLI_ENV.cais.toString()) || env.equals(ContextAppli.APPLI_ENV.cais_mob.toString()))){
					MessageService.addGrowlMessage("Profile", "Votre profile ne permet d'accèder à cet environnement");
					return;
				}
			} else if(!userBean.isInProfile("ADMIN") 
					&& !userBean.isInProfile("SUPERVISEUR") 
					&& !userBean.isInProfile("GESTIONNAIRE")
					&& !userBean.isInProfile("MANAGER")) {
				MessageService.addGrowlMessage("Profile", "Votre profile ne permet d'accèder à cet environnement");
				return;
			}
		}
				
		MessageService.getGlobalMap().put(ProjectConstante.SESSION_GLOBAL_USER, userBean); 
		CaissePersistant caisse = null;		
		//
		if(StringUtil.isNotEmpty(env)){
			String path = "";
			if(SOFT_ENVS.restau.toString().equals(soft)){
				path = "caisse_restau";
			} else if(SOFT_ENVS.market.toString().equals(soft)){
				path = "caisse_market";
			} else if(SOFT_ENVS.pharma.toString().equals(soft)){
				path = "caisse_pharma";
			}
			
			ControllerUtil.setUserAttribute("CURRENT_ENV", env, request);
			APPLI_ENV _ENV_TP = APPLI_ENV.valueOf(env);
			
			String jspPath = _ENV_TP.getJspPath();
			jspPath = (jspPath != null ? jspPath.replaceAll("§§", path) : jspPath);
			httpUtil.setUserAttribute("PATH_JSP_CAISSE", jspPath);		
			httpUtil.setUserAttribute("PATH_JSP_CM", _ENV_TP.equals(APPLI_ENV.cais_mob) ? "mobile" : "normal");
			
			if(!SOFT_ENVS.erp.toString().equals(soft)){
				String caisseRef =(String)ControllerUtil.getParam(request, "caisse_ref");
				if(!env.equals(ContextAppli.APPLI_ENV.back.toString())){
					JourneePersistant journeeOuverte = userService.getLastJourne();
		            String typeEcran = _ENV_TP.getTypeEcran();
		            
					String adresseIp = null;
					if(StringUtil.isEmpty(caisseRef)){
			            adresseIp = request.getRemoteAddr();
			            if (adresseIp.equalsIgnoreCase("0:0:0:0:0:0:0:1")) {// Si on est sur la même machine (local ost)
			                InetAddress inetAddress = InetAddress.getLocalHost();
			                adresseIp = inetAddress.getHostAddress();
			            }
		            } else{
		            	adresseIp = caisseRef;
		            }
		            
		            caisse = (CaissePersistant) paramsService.getSingleResult(paramsService.getQuery("from CaissePersistant where adresse_mac=:mac "
		            		+ "and type_ecran=:typeEcran")
		                  .setParameter("mac", adresseIp)
		                  .setParameter("typeEcran", typeEcran));
		            if(caisse == null) {
		            	MessageService.addGrowlMessage("Machine non reconnue", "Cette machine (<b>"+adresseIp+"</b>) n'est pas encore paramétrée dans le back-office.");
		            	MessageService.getGlobalMap().remove(ProjectConstante.SESSION_GLOBAL_USER);
		            	return;
		            }
		            
		            if( (env.equals(ContextAppli.APPLI_ENV.affi_caisse.toString()) 
		            		&& !TYPE_CAISSE_ENUM.AFFICHEUR.toString().equals(caisse.getType_ecran()))
		            	|| (env.equals(ContextAppli.APPLI_ENV.affi_salle.toString()) 
		            			&& !TYPE_CAISSE_ENUM.AFFICLIENT.toString().equals(caisse.getType_ecran()))	
		            		) {
		            	MessageService.addGrowlMessage("Problème afficheur", "Cet appareil ne correspond pas à un afficheur.");
		            	MessageService.getGlobalMap().remove(ProjectConstante.SESSION_GLOBAL_USER);
		            	return;
		    		}
		            
		            CaisseJourneePersistant journeeCaisseOuverte = userService.getJourneCaisseOuverte(caisse.getId());
		            
		            MessageService.getGlobalMap().put("CURRENT_CAISSE", caisse);
		            MessageService.getGlobalMap().put("CURRENT_JOURNEE_CAISSE", journeeCaisseOuverte);
		            
		            MessageService.getGlobalMap().put("CURRENT_JOURNEE", journeeOuverte);
		            
			         // Liste des afficheurs
					if(env.equals(ContextAppli.APPLI_ENV.cais.toString())) {
						ProfileMenuPersistant profileMenuP = profileMenuService.getProfileMenuByMenuAndProfile("caisse-right", opc_profile1.getId());
						if(profileMenuP != null) {
							Map<String, String> mapDroitCaisse = profileMenuService.getCaisseRight(profileMenuP);
							for (String right : mapDroitCaisse.keySet()) {
								httpUtil.setUserAttribute("RIGHT_"+right.substring(right.indexOf("param_")+6), mapDroitCaisse.get(right));
							}
						}
					} 
				}
			}
		}
		
		EtablissementPersistant etablissement = getEtablissement();
		
		if(StringUtil.isNotEmpty(etablissement.getFlag_maj())){
			if(encryptionUtil.decrypt(etablissement.getFlag_maj()).equals("ABON_DOWN")){
				MessageService.addGrowlMessage("Application désactivée", "<h3>L'application est désactivée suite à un désabonnement de notre service.</h3>");
				MessageService.getGlobalMap().remove(ProjectConstante.SESSION_GLOBAL_USER);
				return;
			}
		}
		//----------------------------- Contrôle licence ----------------------------------
		// Maj date échéance dans le retaurant
		boolean isAppliValide = checkEtatConfAndAbonnement(httpUtil, isIntenetAvailable);
		if(!isAppliValide){
			MessageService.getGlobalMap().remove(ProjectConstante.SESSION_GLOBAL_USER);
			return;
		}
		
		if(BooleanUtil.isTrue(etablissement.getIs_disable())) {
			MessageService.addGrowlMessage("Application désactivée", "<h3>L'application est désactivée suite depuis le Cloud.</h3>");
			MessageService.getGlobalMap().remove(ProjectConstante.SESSION_GLOBAL_USER);
			return;
		}
		
		Map<String, String> mapAbonnement = getMapAbonnement(encryptionUtil);
		env = (env == null ? ContextAppli.APPLI_ENV.back.toString() : env);
		
		String sattelite = mapAbonnement.get("sat")==null ? "" : mapAbonnement.get("sat");
		if(!SOFT_ENVS.erp.toString().equals(soft)){
			if(env.equals(ContextAppli.APPLI_ENV.bal.toString()) && sattelite.indexOf("SAT_BALANCE;") == -1){
				MessageService.addGrowlMessage("", "Vous n'êtes pas encore abonné à ce service. Veuillez contacter le service commerciale pour s'abonner.");
				MessageService.getGlobalMap().remove(ProjectConstante.SESSION_GLOBAL_USER);
				return;
			} else if(env.equals(ContextAppli.APPLI_ENV.lect.toString()) && sattelite.indexOf("SAT_LECTEUR_BARRE;") == -1){
				MessageService.addGrowlMessage("", "Vous n'êtes pas encore abonné à ce service. Veuillez contacter le service commerciale pour s'abonner.");
				MessageService.getGlobalMap().remove(ProjectConstante.SESSION_GLOBAL_USER);
				return;
			} else if(env.equals(ContextAppli.APPLI_ENV.affi_caisse.toString()) && sattelite.indexOf("SAT_AFFICHEUR_CAISSE;") == -1){
				MessageService.addGrowlMessage("", "Vous n'êtes pas encore abonné à ce service. Veuillez contacter le service commerciale pour s'abonner.");
				MessageService.getGlobalMap().remove(ProjectConstante.SESSION_GLOBAL_USER);
				return;
			}
//			if(env.equals(ContextAppli.APPLI_ENV.cuis.toString()) && sattelite.indexOf("SAT_CUISINE;") == -1){
//				MessageService.addGrowlMessage("", "Vous n'êtes pas encore abonné à ce service. Veuillez contacter le service commerciale pour s'abonner.");
//				MessageService.getGlobalMap().remove(ProjectConstante.SESSION_GLOBAL_USER);
//				return;
//			} 
			else if(env.equals(ContextAppli.APPLI_ENV.pres.toString()) && sattelite.indexOf("SAT_CUISINE;") == -1){
				MessageService.addGrowlMessage("", "Vous n'êtes pas encore abonné à ce service. Veuillez contacter le service commerciale pour s'abonner.");
				MessageService.getGlobalMap().remove(ProjectConstante.SESSION_GLOBAL_USER);
				return;
			} else if(env.equals(ContextAppli.APPLI_ENV.pil.toString()) && sattelite.indexOf("SAT_CUISINE;") == -1){
				MessageService.addGrowlMessage("", "Vous n'êtes pas encore abonné à ce service. Veuillez contacter le service commerciale pour s'abonner.");
				MessageService.getGlobalMap().remove(ProjectConstante.SESSION_GLOBAL_USER);
				return;
			} else if(env.equals(ContextAppli.APPLI_ENV.affi_caisse.toString()) && sattelite.indexOf("SAT_AFFICHEUR_CAISSE;") == -1){
				MessageService.addGrowlMessage("", "Vous n'êtes pas encore abonné à ce service. Veuillez contacter le service commerciale pour s'abonner.");
				MessageService.getGlobalMap().remove(ProjectConstante.SESSION_GLOBAL_USER);
				return;
			} else if(env.equals(ContextAppli.APPLI_ENV.affi_salle.toString()) && sattelite.indexOf("SAT_AFFICHEUR_CLIENT;") == -1){
				MessageService.addGrowlMessage("", "Vous n'êtes pas encore abonné à ce service. Veuillez contacter le service commerciale pour s'abonner.");
				MessageService.getGlobalMap().remove(ProjectConstante.SESSION_GLOBAL_USER);
				return;
			} else if(env.equals(ContextAppli.APPLI_ENV.cais_cli.toString()) && sattelite.indexOf("SAT_CAISSE_AUTONOME;") == -1){
				MessageService.addGrowlMessage("", "Vous n'êtes pas encore abonné à ce service. Veuillez contacter le service commerciale pour s'abonner.");
				MessageService.getGlobalMap().remove(ProjectConstante.SESSION_GLOBAL_USER);
				return;
			}
		}	
		
		loadAbonnement(); 
		
		AbonnementBean abnBean = (AbonnementBean) MessageService.getGlobalMap().get("ABONNEMENT_BEAN");
		
		if(etablissement != null){
			if(BooleanUtil.isTrue(abnBean.isOptPlusSynchroCloud())) {
				EtablissementBean etsB = etablissementService.findById(etablissement.getId());
				etsB.setIs_synchro_cloud(true);
				etablissementService.merge(etsB);
				etablissement = etablissementService.findById(EtablissementPersistant.class, etablissement.getId());
			}
			
			if(!ReplicationGenerationEventListener._IS_CLOUD_SYNCHRO_INSTANCE){
				boolean isUpdated = false;
				//
				AbonnePersistant abonneP = etablissement.getOpc_abonne();
				if(abonneP == null){
					List<AbonnePersistant> listAbonne = userService.findAll(AbonnePersistant.class);
					abonneP = (listAbonne.size() > 0) ? listAbonne.get(0) : null;
					isUpdated = true;
				} else {
					abonneP = paramsService.findById(AbonnePersistant.class, abonneP.getId());
				}
				if (abonneP instanceof HibernateProxy) {
					abonneP = (AbonnePersistant) ((HibernateProxy) abonneP).getHibernateLazyInitializer().getImplementation();
				}
				if(abonneP != null) {
					MessageService.getGlobalMap().put("GLOBAL_ABONNE", ReflectUtil.cloneBean(abonneP));
				}
				SocietePersistant societeP = etablissement.getOpc_societe();
				if(societeP == null){
					List<SocietePersistant> listSociete = userService.findAll(SocietePersistant.class);
					societeP = (listSociete.size() > 0) ? listSociete.get(0) : null;
					isUpdated = true;
				} else {
					societeP = paramsService.findById(SocietePersistant.class, societeP.getId());
				}
				if (societeP instanceof HibernateProxy) {
					societeP = (SocietePersistant) ((HibernateProxy) societeP).getHibernateLazyInitializer().getImplementation();
				}
				if(societeP != null) {
					MessageService.getGlobalMap().put("GLOBAL_SOCIETE", ReflectUtil.cloneBean(societeP));
				}
				//
				if(isUpdated) {
					EtablissementBean etsB = etablissementService.findById(etablissement.getId());
					etsB.setOpc_societe(societeP);
					etsB.setOpc_abonne(abonneP);
					etablissementService.merge(etsB);
					etablissement = etablissementService.findById(EtablissementPersistant.class, etablissement.getId());
				}
			}

			if (etablissement instanceof HibernateProxy) {
				etablissement = (EtablissementPersistant) ((HibernateProxy) etablissement).getHibernateLazyInitializer().getImplementation();
			}
			
			MessageService.getGlobalMap().put("GLOBAL_ETABLISSEMENT", ReflectUtil.cloneBean(etablissement));
		}
		
		 // Liste des afficheurs
		if(abnBean.isSatAffCaisse() && env.equals(ContextAppli.APPLI_ENV.cais.toString())) {
			httpUtil.setUserAttribute("LIST_AFFICHEUR", userService.getListAfficheurs(caisse.getId()));
		}
		
		// Paramétrage
		List<ExerciceBean> listExercice = exerciceService.findAll(Order.desc("date_debut"));
		MessageService.getGlobalMap().put("CURRENT_EXERCICE", listExercice.size()>0?ServiceUtil.persistantToBean(ExerciceBean.class,listExercice.get(0)):null);
		
		// Load and save right in session
		loadMenuRightData();
		
		// Maintenir la session ouverte
		boolean isSaveSession = httpUtil.getParameter("session_save") != null;
		if(isSaveSession){
			HttpSession session = httpUtil.getRequest().getSession(false);
			if(session != null){
				session.setMaxInactiveInterval(-1);				
			}
		}
		
		//Maj date derniere connexion
		userBean.setDate_connexion(new Date()); 
		userService.update(userBean);
		
		// Charger les paramètres ----------------------------------------------
		ParametrageRightsConstantes.loadAllMapGlobParams(httpUtil.getRequestAttribute("ABN_UPDATED")!=null);
		ParametrageRightsConstantes.loadAllMapSpecParams(httpUtil.getRequestAttribute("ABN_UPDATED")!=null);
		
        // Imprimante RAZ cas caisse
		Map mapConf = (Map)MessageService.getGlobalMap().get("GLOBAL_CONFIG");
		if(caisse != null){
	        String[] listImprimante = StringUtil.getArrayFromStringDelim(caisse.getImprimantes(), "|");
	        if(listImprimante != null && listImprimante.length > 0 && StringUtil.isNotEmpty(listImprimante[0])){
	        	mapConf.put("PRINT_RAZ", listImprimante[0]);            	
	        }
		}
		
		//
		if(env != null && env.indexOf("cais") != -1){
			Map<String, String> startCodeBarre = paramsService.getCodeBarreBalanceStart();
			MessageService.getGlobalMap().put("LIST_CODE_BALANCE", startCodeBarre);
		}
		boolean isRestau = SOFT_ENVS.restau.toString().equals(soft);
		if(isRestau){
			setCuisinePresPilotageInfos(httpUtil, env);
		}
		
		if(env.equals(ContextAppli.APPLI_ENV.back.toString())) {
			String cloudUrlBase = StrimUtil.getGlobalConfigPropertie("caisse.cloud.url");
			String cloudUrl = cloudUrlBase+"/update";
			String codeAuth = ParametrageService.getEtsCodeAuth();
			
			if(StringUtil.isNotEmpty(cloudUrlBase) && isIntenetAvailable) {
				try {
					String retourCloud = FileUtilController.callURL(cloudUrl+"?mt=notifs&auth=" + codeAuth);
					List<NotificationPersistant> listNotif = ControllerUtil.getObjectFromJson(retourCloud, NotificationPersistant.class);
					userService.synchroniseNotifications(listNotif);
					
					// Style et image de fond
					String retourTheme = FileUtilController.callURL(cloudUrl+"?mt=theme&auth=" + codeAuth);
					if(StringUtil.isNotEmpty(retourTheme)) {
						EtablissementBean etablissementRef = etablissementService.findById(etablissement.getId());
						etablissementRef.setTheme_site(retourTheme);
						etablissementService.merge(etablissementRef);
						
						try(InputStream in = new URL(cloudUrl+"?mt=themeImg&auth=" + codeAuth).openStream()){
							String path = StrimUtil.BASE_FILES_PATH+"/restau/fond/"+etablissementRef.getId();
							File dir = new File(path);
							if(!dir.exists()) {
								dir.mkdirs();
							} else {
								for(String f : dir.list()) {
									FileUtils.forceDelete(new File(path+"/"+f));
								}
							}
						    Files.copy(in, Paths.get(path+"/"+etablissementRef.getThemeDet("img_fond")), StandardCopyOption.REPLACE_EXISTING);
						}
					}
					// Maj flag cloud
					FileUtilController.callURL(cloudUrl+"?mt=theme&auth=" + codeAuth+"&isUpd=1");
				} catch(Exception e) {
					System.out.println(e.getMessage());
				}
			}
			httpUtil.setUserAttribute("listNotif", userService.getListNotification(userBean));			
			
			// TMP ==> A supprimer plus tard
			//try {
				//userService.updateNewLivreurInfosTmp();
			//} catch(Exception e) {
				
			//}
		}
		
		// Mode impression
		String tkn = "";
		
		if(ReplicationGenerationEventListener._IS_CLOUD_SYNCHRO_INSTANCE
				|| (caisse != null && BooleanUtil.isTrue(caisse.getIs_local_print()))) {
			if(mapConf != null) {
				mapConf.put("CAISSE_PRINT", "false");
			}
			
			tkn = "?jtn="+etablissement.getToken()+"&isLog=1";
		} else {
			if(mapConf != null) {
				mapConf.put("CAISSE_PRINT", "true");
			}
		}
		
		// From Mobile
		String contextPath = request.getServletContext().getContextPath();
		
//		if(envMobile != null) {
//			envMobile = envMobile.replaceAll("mobile-", "mob-");
//			httpUtil.writeResponse("REDIRECT:"+contextPath+"/"+envMobile + tkn);
//			return;
//		}
		
		// ---------------------------------------------------------------------
		if(env == null || env.equals(ContextAppli.APPLI_ENV.back.toString())) {
			boolean isPointeuse = (StringUtil.isNotEmpty(ContextGloabalAppli.getEtablissementBean().getPointeuse_db_path()) || 
					(StringUtil.isNotEmpty(ContextGloabalAppli.getEtablissementBean().getPointeuse_ip()) 
					&& StringUtil.isNotEmpty(ContextGloabalAppli.getEtablissementBean().getPointeuse_port())));
			
			if(isPointeuse){
				// Pointage
				if(!ReplicationGenerationEventListener._IS_CLOUD_SYNCHRO_INSTANCE){
					new Thread(() -> {
						new PointageAction().runUploadPointeuseZktEcoIp(httpUtil);
					}).start();
				}
			}
			httpUtil.writeResponse("REDIRECT:"+contextPath + tkn);
		} else {
			httpUtil.writeResponse("REDIRECT:"+contextPath+"/caisse" + tkn);
		}
    }
    
   public static void loadAbonnement() {
	   EncryptionEtsUtil encryptionUtil = new EncryptionEtsUtil(EncryptionEtsUtil.getDecrypKey());
	   AbonnementBean abnBean = new AbonnementBean();
	   Map<String, String> mapAbonnement = getMapAbonnement(encryptionUtil);
		
	   String sattelite = mapAbonnement.get("sat")==null ? "" : mapAbonnement.get("sat");
	    String packAbon = mapAbonnement.get("pack");
		String optionsAbon = mapAbonnement.get("opt");
		String optionsPlusAbon = mapAbonnement.get("optp");
		String nbrAbon = mapAbonnement.get("nbr");
		
		// cais, cliaff, caisauto, caiscuis, caisaff
		Map<String, Integer> mapNbrTerminaux = new HashMap<>();
		String[] det = StringUtil.getArrayFromStringDelim(nbrAbon, ";");
		
		if(det != null){
			for (String val : det) {
				if(StringUtil.isEmpty(val)) {
					continue;
				}
				String[] detNbr = StringUtil.getArrayFromStringDelim(val, "-");
				mapNbrTerminaux.put(detNbr[0], StringUtil.isEmpty(detNbr[1]) ? 99 : Integer.valueOf(detNbr[1]));
			}
			abnBean.setNbrCaisse(mapNbrTerminaux.get("cais"));
			abnBean.setNbrSatCaisseMob(mapNbrTerminaux.get("caismob"));
			abnBean.setNbrSatCaisseAuto(mapNbrTerminaux.get("caisauto"));
			
			abnBean.setNbrSatAffClient(mapNbrTerminaux.get("cliaff"));
			abnBean.setNbrSatAffClient(mapNbrTerminaux.get("caisaff"));
			
			abnBean.setNbrSatCuisine(mapNbrTerminaux.get("caiscuis"));
			abnBean.setNbrSatPilotage(mapNbrTerminaux.get("caispil"));
			abnBean.setNbrSatPresentoire(mapNbrTerminaux.get("caispres"));
			
			abnBean.setNbrSatBalance(mapNbrTerminaux.get("caisbal"));
			abnBean.setNbrSatLecteurBarre(mapNbrTerminaux.get("lecprix"));
		}
		boolean isOpt = StringUtil.isNotEmpty(optionsAbon);
		boolean isNotPack = StringUtil.isEmpty(packAbon);
		
		boolean isBasicPack = (packAbon != null && packAbon.equals("PACK_TACTILE"));// Caisse tactile uniquement
		
		boolean isOptPlusCmdVitrine = isNotPack || (optionsPlusAbon != null && optionsPlusAbon.indexOf("OPTPLUS_COMMANDE_VITRINE") != -1);
		boolean isOptPlusPageVitrine = isNotPack || (optionsPlusAbon != null && optionsPlusAbon.indexOf("OPTPLUS_PAGE_VITRINE") != -1);
		boolean isOptPlusBackup = isNotPack || (optionsPlusAbon != null && optionsPlusAbon.indexOf("OPTPLUS_BACKUP_BASE") != -1);
		boolean isOptPlusRemote = isNotPack || (optionsPlusAbon != null && optionsPlusAbon.indexOf("OPTPLUS_ACCESS_DISTANT") != -1);
		
		// Si c'est un etablissement centrale
		boolean isOptPlusEtsCentrale = isNotPack || (optionsPlusAbon != null && optionsPlusAbon.indexOf("OPTPLUS_ETS_CENTRALE") != -1);
		// Si la synchro centrale est activee pour cet etabliment
		boolean isOptPlusSyncCentrale = isNotPack || (optionsPlusAbon != null && optionsPlusAbon.indexOf("OPTPLUS_SYNC_CENTRALE") != -1);
		
		if(isOptPlusEtsCentrale) {
			isOptPlusSyncCentrale = false;// Si etablissement centrale (master) alors pas de synchro (slave)
		}
		
		boolean isOptPlusSynchroCloud = (optionsPlusAbon != null && optionsPlusAbon.indexOf("OPTPLUS_SYNCHRO_CLOUD") != -1);
		boolean isOptPlusOptimisation = isNotPack || (optionsPlusAbon != null && optionsPlusAbon.indexOf("OPTPLUS_OPTIMISATION") != -1);
		
		boolean isOptCompta = isNotPack || (isOpt && optionsAbon.indexOf("OPT_COMPTABILITE;") != -1);
		boolean isOptRh = isNotPack || (isOpt && optionsAbon.indexOf("OPT_RH;") != -1);
		boolean isOptAuto = isNotPack || (isOpt && optionsAbon.indexOf("OPT_AUTO;") != -1);
		boolean isOptCommercial = isNotPack || (isOpt && optionsAbon.indexOf("OPT_COMMERCIAL;") != -1);
		boolean isOptLivraison = isNotPack || (isOpt && optionsAbon.indexOf("OPT_LIVRAISON;") != -1);
		boolean isOptControle = isNotPack || (isOpt && optionsAbon.indexOf("OPT_CONTROLE;") != -1);
		boolean isOptStock = !isBasicPack || (isOpt && optionsAbon.indexOf("OPT_STOCK;") != -1);
		
		abnBean.setSatCuisine(isNotPack || sattelite.indexOf("SAT_CUISINE") != -1);
		abnBean.setSatAffCaisse(isNotPack || sattelite.indexOf("SAT_AFFICHEUR_CAISSE") != -1);
		abnBean.setSatAffClient(isNotPack || sattelite.indexOf("SAT_AFFICHEUR_CLIENT") != -1);
		abnBean.setSatCaisseAuto(isNotPack || sattelite.indexOf("SAT_CAISSE_AUTONOME") != -1);
		abnBean.setSatLecteurBarre(isNotPack || sattelite.indexOf("SAT_LECTEUR_BARRE") != -1);
		abnBean.setSatBalance(isNotPack || sattelite.indexOf("SAT_BALANCE;") != -1);
		
		abnBean.setPackType(packAbon);
		
		abnBean.setOptStock(isOptStock);
		abnBean.setOptRh(isOptRh);
		abnBean.setOptCommercial(isOptCommercial);
		abnBean.setOptLivraison(isOptLivraison);
		abnBean.setOptControle(isOptControle);
		abnBean.setOptCompta(isOptCompta);
		
		abnBean.setOptPlusRemote(isOptPlusRemote);
		abnBean.setOptPlusBackup(isOptPlusBackup);
		abnBean.setOptPlusCmdVitrine(isOptPlusCmdVitrine);
		abnBean.setOptPlusPageVitrine(isOptPlusPageVitrine);
		abnBean.setOptPlusSynchroCloud(isOptPlusSynchroCloud);
		abnBean.setOptPlusEtsCentrale(isOptPlusEtsCentrale);
		abnBean.setOptPlusSyncCentrale(isOptPlusSyncCentrale);
		abnBean.setOptPlusOptimisation(isOptPlusOptimisation);
	
		MessageService.getGlobalMap().put("ABONNEMENT_BEAN", abnBean);
	}

/**
    * @param envs
    * @param currEnv
    * @return
    */
    public static boolean checkDoitProfileEnv(String envs, String currEnv) {
    	if(StringUtil.isEmpty(envs)) {
    		return true;
    	}
    	APPLI_ENV currEnvEnum = APPLI_ENV.valueOf(currEnv);
    	String[] envArray = StringUtil.getArrayFromStringDelim(envs, ";");
    	if(envArray != null) {
    		for (String env : envArray) {
				String typeEcran = currEnvEnum.getTypeEcran();
				typeEcran = (typeEcran == null && APPLI_ENV.back.equals(currEnvEnum)) ? TYPE_CAISSE_ENUM.BACKOFFICE.toString() : typeEcran;
				
				if(typeEcran.equals(env)) {
					return true;
				}
			}
    	}
    	return false;
    }
    
    /**
     * @param httpUtil
     */
    public void updateNotification(ActionUtil httpUtil) {
    	boolean isChecked = httpUtil.getParameter("newsNotShow") != null;
    	
    	if(isChecked) {
	    	Long userId = ContextAppli.getUserBean().getId();
	    	String[] notifIds = StringUtil.getArrayFromStringDelim(httpUtil.getParameter("notif"), ";");
	    	//
	    	if(notifIds != null) {
	    		for(String notifId : notifIds) {
	    			if(StringUtil.isNotEmpty(notifId)) {
	    				userService.updateNotification(userId, Long.valueOf(notifId));
	    			}
	    		}
	    	}
    	}
    }
    
    /**
     * @param httpUtil
     * @param env
     */
    private void setCuisinePresPilotageInfos(ActionUtil httpUtil, String env){
    	//boolean IS_ALERT_SONORE = StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig(ContextAppli.PARAM_APPLI_ENUM.ALERT_SONOR_CUISINE.toString()));
        
		if(env.equals(ContextAppli.APPLI_ENV.cuis.toString()) 
				|| env.equals(ContextAppli.APPLI_ENV.pil.toString()) 
				|| env.equals(ContextAppli.APPLI_ENV.pres.toString())
				|| env.equals(ContextAppli.APPLI_ENV.affi_caisse.toString())
				|| env.equals(ContextAppli.APPLI_ENV.affi_salle.toString())) {//----------------------------------------------------------------------------
			// Alert sonore
		   	// if(IS_ALERT_SONORE) {
		         int DELAIS_ALERT_MINUTE = 0;
		         if (StringUtil.isNotEmpty(ContextGloabalAppli.getGlobalConfig(ContextAppli.PARAM_APPLI_ENUM.DELAIS_ALERT_CUISINE.toString()))) {
		         	DELAIS_ALERT_MINUTE = Integer.valueOf(ContextGloabalAppli.getGlobalConfig(ContextAppli.PARAM_APPLI_ENUM.DELAIS_ALERT_CUISINE.toString()));
		         }
		         if (DELAIS_ALERT_MINUTE < 3) {
		         	DELAIS_ALERT_MINUTE = 3;// 3 Minutes minimum 
		         }
		         httpUtil.setUserAttribute("DELAIS_ALERT_MINUTE", DELAIS_ALERT_MINUTE);
		   //	 }
		   	 // Refreshissement écran
	        int delais = 0;
	        if (StringUtil.isNotEmpty(ContextGloabalAppli.getGlobalConfig(ContextAppli.PARAM_APPLI_ENUM.DELAIS_REFRESH_ECRAN.toString()))) {
	            delais = Integer.valueOf(ContextGloabalAppli.getGlobalConfig(ContextAppli.PARAM_APPLI_ENUM.DELAIS_REFRESH_ECRAN.toString()));
	            delais = delais * 1000;
	        }
	        // Sécurité au cas ou erreur dans la paramétrage
	        if(delais < 5000){
	           delais = 5000; //5 secondes minumum
	        }
	        httpUtil.setUserAttribute("DELAIS_REFRESH_ECRAN_SECONDE", delais);
		}
    }
    
    private static Map<String, String> getMapAbonnement(EncryptionEtsUtil encryptionUtil){
    	EtablissementPersistant restauP = getEtablissement();
    	String abonnement = encryptionUtil.decrypt(restauP.getAbonnement());
    	Map<String, String> mapData = new HashMap<>();
    	
    	if(StringUtil.isNotEmpty(abonnement)){
    		String[] abonnArray = StringUtil.getArrayFromStringDelim(abonnement, "$$");
    		for (String data : abonnArray) {
    			String[] dataArray = StringUtil.getArrayFromStringDelim(data, ":");
    			mapData.put(dataArray[0], dataArray[1]);
			}
    	}
    	
    	return mapData;
    }
	
    /**
     * @return
     */
    private static EtablissementPersistant getEtablissement(){
    	IUserService userService = ServiceUtil.getBusinessBean(IUserService.class);
    
    	EtablissementPersistant etablissement = null;
 		if(ReplicationGenerationEventListener._IS_CLOUD_SYNCHRO_INSTANCE){
 			Long etsId = ContextAppli.getEtablissementBean().getId();
 			etablissement = userService.getEntityManager().find(EtablissementPersistant.class, etsId);
 		} else{
 			etablissement = userService.findAll(EtablissementPersistant.class).get(0);
 	    	etablissement = userService.findById(EtablissementPersistant.class, etablissement.getId());	
 		}
		
		return etablissement;
    }
    
    /**
     * @param httpUtil
     */
    private boolean checkEtatConfAndAbonnement(ActionUtil httpUtil, boolean isIntenetAvailable) {
//    	if("localConf".equals(StrimUtil.getGlobalConfigPropertie("context.install"))) {
//    		return true;
//    	}
		try {
			EtablissementPersistant ets = getEtablissement();
			String retourCheckCaisse = null;
			String codeAuth = ParametrageService.getEtsCodeAuth();			
			String cloudBaseUrl = StrimUtil.getGlobalConfigPropertie("caisse.cloud.url");
			String cloudUrl = cloudBaseUrl+"/update";
			//
			if(StringUtil.isNotEmpty(cloudBaseUrl) && isIntenetAvailable) {
				retourCheckCaisse = FileUtilController.callURL(cloudUrl+"?mt=checkd&auth=" + codeAuth);
			}
			// Si cloud injoignable
			if(StringUtil.isEmpty(retourCheckCaisse) // Pas de réponse du cloud
					&& !BooleanUtil.isTrue(ets.getIs_disable())// Etablissement actif en local
					&& ets.getTarget_endDecrypt() != null // Date echeance alimentée
					&& ets.getTarget_endDecrypt().after(DateUtil.addSubstractDate(new Date(), TIME_ENUM.DAY, -15))//Date échéance plud de 15j 
					&& ets.getDate_synchro() != null 
					&& DateUtil.getDiffDays(ets.getDate_synchro(), new Date()) < 20){// Date synchro au moins 20 jours 
				return true;
			}
			
			boolean isCaisseValable = (""+retourCheckCaisse).trim().equals("true");
			
			if(!NumericUtil.isNum(retourCheckCaisse) && !isCaisseValable){// Si non date et false
				if(!BooleanUtil.isTrue(ets.getIs_disable())) {
					EtablissementPersistant etsDb = userService.findById(EtablissementPersistant.class, ets.getId());
					etsDb.setIs_disable(true);
					userService.mergeEntity(etsDb);
				}
				MessageService.addGrowlMessage("Application désactivée", "<h3>Votre application n'est pas activée. Veuillez contacter le support.</h3>");
				return false;
			} else {
				if(BooleanUtil.isTrue(ets.getIs_disable())) {
					EtablissementPersistant etsDb = userService.findById(EtablissementPersistant.class, ets.getId());
					etsDb.setIs_disable(false);
					userService.mergeEntity(etsDb);
				}
			}
			
			Date dateEcheance = DateUtil.stringToDate(retourCheckCaisse, "ddMMyyyy");
			
			if(ets.getTarget_end() == null || ets.getTarget_endDecrypt().compareTo(dateEcheance) != 0){
				userService.updateEcheanceRestaurant(dateEcheance);
			}
			
			// Maj date synchronisation
			userService.updateLastCheckCloud(ets.getId());

			if(ReplicationGenerationEventListener._IS_CLOUD_SYNCHRO_INSTANCE) {
				return true;
			}
			
			// Sauvegarder les options d'abonnement + base de données
			if(StringUtil.isNotEmpty(cloudBaseUrl) && isIntenetAvailable) {
				String retourAbonnement = FileUtilController.callURL(cloudUrl+"?mt=abonmnt&auth="+codeAuth);
				
				// 0: abonnement,     1:conf,            2:cle crytptage client               3:isNewVersion
				if(StringUtil.isNotEmpty(retourAbonnement)){
					String[] retourArray = retourAbonnement.split("\\|");
					// Maj abonnnement
					if(StringUtil.isNotEmpty(retourArray[0])){
						httpUtil.setRequestAttribute("ABN_UPDATED", true);
						userService.updateAboonement(retourArray[0]);	
					}
					// Maj conf
					if(StringUtil.isNotEmpty(retourArray[1])){
						ApplicationListener.updateConf(retourArray[1], retourArray[2], httpUtil.getRequest().getContextPath());
					}
					
					if(StringUtil.isTrue(retourArray[3])) {
						httpUtil.setUserAttribute("IS_NEW_VERSION", true);
					}
				}
			}
		} catch (Exception e) {
		    if (!e.getClass().equals(ConnectException.class)) {
		        LOGGER.error(e.getMessage());
		        e.printStackTrace();
		    } else {
		        e.printStackTrace();
		    }
		}
		
		EtablissementPersistant etsP = getEtablissement();
		
		if(!ReplicationGenerationEventListener._IS_CLOUD_SYNCHRO_INSTANCE){// Non cloud
			/** Si pas de connexion on va verifier si la derniere journee est coherente avec la date 
			system en cas d'absence de connexion
			*/ 
			// Si la date synchro depasse la date en cours donc probleme
			Date dateJourAfter = DateUtil.addSubstractDate(new Date(), TIME_ENUM.DAY, 1);
			
			if(etsP.getDate_synchro() != null && etsP.getDate_synchro().compareTo(dateJourAfter) > 0){
				MessageService.addGrowlMessage("Date system incohérente", "<h3>La date systeme est incohérente avec la date de synchronisation."
						+ "<br> Veuillez régler la date de votre serveur.</h3>");
				return false;
			} else{
				// Si date max journée supérieur à la date du jour de 5 jours alors problème
//				Date maxDateJournee = caisseWebService.getMaxJourneeDate();
//				if(maxDateJournee != null){
//					int daysDiff = DateUtil.getDiffDays(maxDateJournee, new Date());
//					if(daysDiff < 0 && (daysDiff*-1) > 5){
//						MessageService.addGrowlMessage("Date system incohérente", "La date system est incohérente avec la date de synchronisation (J)."
//								+ "<br> Veuillez régler la date de votre serveur.");
//						return false;
//					}
//				}
			}
		}
		//
		
		if(etsP.getTarget_end()  != null){
			Date date_target_endDecrypt = etsP.getTarget_endDecrypt();
			
			if(date_target_endDecrypt == null) {
				MessageService.addGrowlMessage("Application désactivée", "<h3>Votre échéance d'abonnement n'a pas été mise à jour.<br>Veuillez contacter le support.</h3>");
				return false;
			}
			int daysDiff = DateUtil.getDiffDays(date_target_endDecrypt, new Date());
			if(daysDiff >= 0 && daysDiff < 15){
				httpUtil.setUserAttribute("CHECK_DIFF_DAYS", (daysDiff==0?1:daysDiff));
			} else if(daysDiff >= 15){// Plus de 15 jours, on désactive
				//MessageService.addGrowlMessage("Application désactivée", "<h3>Votre échéance d'abonnement a expiré depuis plus de 15 jours ("+DateUtil.dateToString(date_target_endDecrypt)+").<br>Veuillez régler votre abonnement.</h3>");
				//return false;
				httpUtil.setUserAttribute("CHECK_DIFF_DAYS", daysDiff);
			}
		}
		
		return true;
    }
    
	private void loadMenuRightData(){
		UserBean userBean = (UserBean) MessageService.getGlobalMap().get(ProjectConstante.SESSION_GLOBAL_USER);
		Map<String, Map<String, Integer>> mapMenuRights = loadMenuRights(userBean);
		MessageService.getGlobalMap().put(ProjectConstante.SESSION_GLOBAL_RIGHT, mapMenuRights);
	}
	
	/**
	 * @param httpUtil
	 */
	@WorkForward(useBean=false, useFormValidator=false)
	public void disconnect(ActionUtil httpUtil){
		HttpServletRequest request = httpUtil.getRequest();
		HttpSession session = request.getSession(false);
		
		String currEnv = (String)ControllerUtil.getUserAttribute("CURRENT_ENV", request);
		// Ne pas pouvoir se déconnecter si une commande est encours
		if(currEnv != null && (currEnv.equals(ContextAppli.APPLI_ENV.cais.toString()) 
				|| currEnv.equals(ContextAppli.APPLI_ENV.cais_mob.toString()))){
			CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) httpUtil.getUserAttribute("CURRENT_COMMANDE");
			if(CURRENT_COMMANDE != null && CURRENT_COMMANDE.getList_article().size() > 0) {
				MessageService.addGrowlMessage("", "<h3>Vous devez d'abord annuler ou mettre en attente la commande en cours.<h3>");
				return;
			}
			
			// Libérer les commandes
			userService.unlockCommandes(null, ContextAppli.getUserBean().getId());
		}
		// ------------------------------------------------------------
		String envMobile = (String) request.getSession(true).getAttribute("ENV_MOBILE");
		
		// If session exists, destroy it
		if(session != null){
			session.invalidate();
		}
		request.getSession(true).setAttribute("ENV_MOBILE", envMobile);
		
		httpUtil.setDynamicUrl("/commun/secure_page.jsp");
	}


	/**
	 * Load right for current user
	 * @param menuId
	 * @return
	 */
	private Map<String, Map<String, Integer>> loadMenuRights(UserBean userBean){
		if(userBean == null){
			return null;
		}
		ProfilePersistant opc_profile = userBean.getOpc_profile();
		ProfilePersistant opc_profile2 = userBean.getOpc_profile2();
		ProfilePersistant opc_profile3 = userBean.getOpc_profile3();
		
		List<ProfileMenuPersistant> listProfileMenu = profilMenuService.getListMenuProfileByProfile(opc_profile.getId());
		if(opc_profile2 != null) {
			listProfileMenu.addAll(profilMenuService.getListMenuProfileByProfile(opc_profile2.getId()));
		}
		if(opc_profile3 != null) {
			listProfileMenu.addAll(profilMenuService.getListMenuProfileByProfile(opc_profile3.getId()));
		}
		
		Map<String, Map<String, Integer>> mapMenuRights = new HashMap<String, Map<String, Integer>>();
		
		// Add from data base
		for(ProfileMenuPersistant pmenuPersistant : listProfileMenu){
			Map<String, Integer> mapRights = new HashMap<String, Integer>();
			String rights = pmenuPersistant.getRights();
			//
			if(StringUtil.isNotEmpty(rights)){
				String[] rightsArray = StringUtil.getArrayFromStringDelim(rights, ";");
				//
				if(rightsArray != null){
					for(String r : rightsArray){
						String[] valuesArray = StringUtil.getArrayFromStringDelim(r, ":");
						mapRights.put(valuesArray[0], NumericUtil.toInteger(valuesArray[1]));
					}
				}
			}
			//
			mapMenuRights.put(pmenuPersistant.getMenu_id(), mapRights);
		}

		return mapMenuRights;
	}
	
	/**
	 * @param httpUtil
	 */
	public void checkCodeAbonnement(ActionUtil httpUtil){
		String codeAbonnement = httpUtil.getParameter("tkn");
		boolean retour = userService.checkCodeAbonnement(codeAbonnement);
		//
		if(retour){
			httpUtil.writeResponse("MSG_CUSTOM:Votre abonnement a été <b>prolongé</b> avec succès.");
		} else{
			MessageService.addGrowlMessage("Validation abonnement", "Le code saisi n'est pas <b>valide</b>.");
		}
	}
	
	/**
	 * @param httpUtil 
	 */
	public void load_histo_maj(ActionUtil httpUtil){
		httpUtil.setDynamicUrl("/historique_maj.jsp");
	}
	
	public void triggerMajApplication(ActionUtil httpUtil){
		try {
			String tomcatDir = StrimUtil.getGlobalConfigPropertieIgnoreErreur("caisse.tomcat.dir");
			if(StringUtil.isEmpty(tomcatDir)
					|| !new File(tomcatDir).exists()) {
				FileUtilController.callURL("http://localhost:8000/cm-serveur?act=maj");
			} else {
				updateBoApplication(); 
			}
		} catch (IOException e) {
		}
		httpUtil.writeResponse("OK");
	}
	
	/**
	 * 
	 */
	private void updateBoApplication() {
		IUserService userService = ServiceUtil.getBusinessBean(IUserService.class);
    	try {
	    	String cloudUrl = StrimUtil.getGlobalConfigPropertie("caisse.cloud.url")+"/update";
	    	String codeAuth = ParametrageService.getEtsCodeAuth();
			String retourCheckCaisse = FileUtilController.callURL(cloudUrl+"?mt=checkv&auth="+codeAuth);
			boolean isCaisseUpdate = (""+retourCheckCaisse).trim().equals("true");
			
			if(!isCaisseUpdate) {
				return;
			}
			
			String tomcatPath = StrimUtil.getGlobalConfigPropertie("caisse.tomcat.dir");
	    	String tempPath = StrimUtil.getGlobalConfigPropertie("caisse.dir.temp");
	    	
			// Création du répertoire si'il n'existe pas
			if (!new File(tempPath).exists()) {
				new File(tempPath).mkdirs();
			}
			
			String callURL = FileUtilController.callURL(cloudUrl+"?mt=info&auth="+codeAuth);
			
			if(StringUtil.isEmpty(callURL)) {
				return;
			}
			String[] retourInfos = StringUtil.getArrayFromStringDelim(callURL, "|");
			String instanceName = retourInfos[0];
			String typeAppli = retourInfos[1];
			String version = retourInfos[2];
			Date dateVersion = DateUtil.stringToDate(retourInfos[3], "ddMMyyyy");
	
			// Maj caisse
			File pathTempBackTarget = new File(tempPath + "/" + instanceName+".war");
			FileUtils.copyURLToFile(new URL(cloudUrl + "?mt=download&auth="+codeAuth), pathTempBackTarget);
		
			// Copier dans le répertoire 
			File pathBackTarget = new File(tomcatPath+"/webapps/"+instanceName+".war");
			
			if(pathBackTarget.exists()) {
				String backupDir = ""; 
				if(tomcatPath.indexOf("/") != -1) {
					backupDir = tomcatPath + "/BACKUP";	
				} else {
					backupDir = tomcatPath + "\\BACKUP";
				}
				
				if(!new File(backupDir).exists()){
					new File(backupDir).mkdirs();
				}
				// Copier le fichier pour les cas ou la maj à échouée
				File warBackup = new File(backupDir+"/"+instanceName+"_backup.war");
				FileUtilController.copyFile(pathBackTarget, warBackup);
				
				// Supprimer le war de tomcat
				if(pathBackTarget.exists()){
					FileUtils.forceDelete(pathBackTarget);
				}
	    	}
			
			// Maj version locale + Ajouter pour executer le script init au prochain démarrage
			userService.updateFlagUpdate(version, dateVersion);

			// Maj cloud pour la maj
			FileUtilController.callURL(cloudUrl+"?mt=update&auth="+codeAuth);
			
			// Copier le fichier en modifiant la date
			pathBackTarget.setLastModified(new Date().getTime());
			FileUtilController.copyFile(pathTempBackTarget, pathBackTarget);
			
			// Suppression du fichier
			//FileUtils.forceDelete(pathTempBackTarget);
			
			// Tempo de 30 secode pour redemarrage tomcat
			Thread.sleep(30000);
			
			// Passage des scripts de maj
			// Comparaison des versions
			//paramsService.executerInitScript(currentDateVersion);
			
			// Maj restau session
			//MessageService.getGlobalMap().put("GLOBAL_RESTAURANT", ets);
		} catch (Exception e) {
 			e.printStackTrace();
		}
    }
}
