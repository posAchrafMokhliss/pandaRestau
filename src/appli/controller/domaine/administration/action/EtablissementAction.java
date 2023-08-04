package appli.controller.domaine.administration.action;

import java.util.Map;

import javax.inject.Inject;

import appli.controller.domaine.administration.bean.EtablissementBean;
import appli.controller.domaine.util_erp.ContextAppli;
import appli.model.domaine.administration.service.IEtablissementService;
import appli.model.domaine.administration.service.IParametrageService;
import appli.model.domaine.administration.service.IValTypeEnumService;
import framework.controller.ActionBase;
import framework.controller.ActionUtil;
import framework.controller.annotation.WorkController;
import framework.model.beanContext.EtablissementPersistant;
import framework.model.beanContext.SocietePersistant;
import framework.model.common.constante.ProjectConstante;
import framework.model.common.util.ControllerBeanUtil;
import framework.model.util.ModelConstante;

@WorkController(nameSpace="admin", bean=EtablissementBean.class, jspRootPath="/domaine/administration/")
public class EtablissementAction extends ActionBase{
	@Inject
	private IParametrageService paramService;
	@Inject
	private IEtablissementService etablissementService;
	@Inject
	private IValTypeEnumService valEnumService;
	
	public void work_init(ActionUtil httpUtil) {
		if(ContextAppli.IS_SYNDIC_ENV()){	
			httpUtil.setRequestAttribute("list_mode_gestion", valEnumService.getListValeursByType(ModelConstante.ENUM_MODES_GESTION));
		}
	}
	
	public void work_edit(ActionUtil httpUtil){
		Long etsId = ContextAppli.getEtablissementBean().getId();
		
		httpUtil.setViewBean(etablissementService.findById(etsId));
		
		httpUtil.setDynamicUrl("/domaine/administration/etablissement_edit.jsp");
	}
		
	@Override
	public void work_merge(ActionUtil httpUtil) {
		Map<String, Object> params = (Map)httpUtil.getRequestAttribute(ProjectConstante.WORK_PARAMS);
		EtablissementPersistant etsP = (EtablissementPersistant) ControllerBeanUtil.mapToBean(EtablissementPersistant.class, params);
		Long societeId = ContextAppli.getSocieteBean().getId();
		//
		etsP.setId(httpUtil.getWorkIdLong());
		etsP.setOpc_societe(paramService.findById(SocietePersistant.class, societeId));
		
		paramService.mergeEtablissement(etsP, false);
		
		// Gestion de l'image ---------------------------------------------------
		managePieceJointe(httpUtil, etsP.getId(), "restau", 300, 300);// Image logo
		
		work_edit(httpUtil);
	}
	
	/**
	 * @param httpUtil
	 */
	public void desactiver(ActionUtil httpUtil) {
		etablissementService.activerDesactiverElement(httpUtil.getLongParameter("ets"));
		work_find(httpUtil);
	}
	
	public void work_post(ActionUtil httpUtil) {

	}
}
