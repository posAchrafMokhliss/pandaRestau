<%@page import="appli.controller.domaine.util_erp.ContextAppli"%>
<%@page import="framework.controller.ContextGloabalAppli"%>
<%@page import="appli.model.domaine.vente.persistant.CaisseMouvementPersistant"%>
<%@page import="appli.controller.domaine.caisse.ContextAppliCaisse"%>
<%@page import="framework.model.common.util.StringUtil"%>
<%@page import="framework.model.common.util.EncryptionUtil"%>
<%@ taglib uri="http://www.customtaglib.com/complexe" prefix="cplx"%>
<%@ taglib uri="http://www.customtaglib.com/standard" prefix="std"%>
<%@ taglib uri="http://www.customtaglib.com/html" prefix="html"%>
<%@ taglib uri="http://www.customtaglib.com/work" prefix="work"%>
<%@ taglib uri="http://www.customtaglib.com/c" prefix="c"%>
<%@page errorPage="/commun/error.jsp"%>
<%@page import="framework.controller.ControllerUtil"%>

<%
boolean isShowAttBtn = !StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("SHOW_MISE_ATT"));
boolean isCaisseVerouille = ControllerUtil.getMenuAttribute("IS_CAISSE_VERROUILLE", request) != null;
boolean isCaisseNotFermee = (ContextAppliCaisse.getJourneeCaisseBean() != null && ContextAppliCaisse.getJourneeCaisseBean().getStatut_caisse().equals("O"));
boolean isJourneeCaisseOuverte = !isCaisseVerouille && isCaisseNotFermee;
boolean isJourneeOuverte = (ContextAppliCaisse.getJourneeBean() != null && ContextAppliCaisse.getJourneeBean().getStatut_journee().equals("O"));

boolean isServeur = "SERVEUR".equals(ContextAppli.getUserBean().getOpc_profile().getCode());
boolean isCaissier = ContextAppli.getUserBean().isInProfile("CAISSIER");
boolean isManager = ContextAppli.getUserBean().isInProfile("MANAGER");
boolean isAdmin = ContextAppli.getUserBean().isInProfile("ADMIN");

CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant)ControllerUtil.getUserAttribute("CURRENT_COMMANDE", request);
boolean isCloseSaisieQte = StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("CLAC_NBR_SHOW"));
%>

<%if(isJourneeOuverte && isJourneeCaisseOuverte){ %>
<!-- Commande d&eacute;tail -->
	<div id="banner_foot_act" style="float: left;width: 100%;height: 50px;position: fixed;bottom: 50px;background-color: #d1d1d1;border-radius: 23px;border: 1px solid #FF9800;">
     <!-- TOTAL COMMANDE -->
	 <div id="cmd_total" class="label label-success" style="position: absolute;right: 20px;bottom: 10px;font-size: 22px;font-weight: bold;color: white;border-radius:15px !important;">
	 
	 </div>
			<std:link id="home_lnk" style="color: fuchsia;padding-top: 0px;padding-left: 7px;" classStyle="btn btn-default btn-circle btn-lg btn-menu shiny" action="caisse-web.caisseWeb.init_home" targetDiv="right-div" icon="fa-3x fa fa-home" value="" />
						
			<a href="javascript:void(0);" id="calc_lnk" style="color: #262626;font-weight: bold;" class="btn btn-default btn-circle btn-lg btn-menu shiny" data-container="body" data-titleclass="bordered-blue" data-class="" data-toggle="popover" data-placement="top" data-title="" 
				data-content="<%for(int i=0; i<10; i++){ %><a class='btn btn-blue btn-lg icon-only white btn-circle btn-calc ' href='javascript:void(0);'><%=i %></a><%} %><a class='btn btn-blue btn-lg icon-only white btn-circle btn-calc' href='javascript:void(0);'>C</a><%=!isCloseSaisieQte?"<a class='btn btn-blue btn-lg icon-only white btn-circle btn-calc' href='javascript:void(0);'>.</a><a class='btn btn-blue btn-lg icon-only white btn-circle btn-calc' style='color: red !important;' href='javascript:void(0);'>X</a>":""%>" data-original-title="" title="">
				<img src="resources/caisse/img/caisse-web/calculator_blue.png" />
				
			</a>
			<std:link id="up_btn" style="display:none;" action="caisse-web.caisseWeb.upButtonEvent" targetDiv="right-div" classStyle="btn btn-default btn-circle btn-lg btn-menu shiny" tooltip="Monter d'un niveau">
				<img src="resources/caisse/img/caisse-web/upload.png" />
			</std:link>
			
	 		<!-- AFFICHER CACHER LA COMMANDE -->
		<a onClick="$('#left-div').toggle(500);" href="javascript:" style="margin-right: 18px;padding-left: 6px;padding-top: 6px;border: 1px solid #2d0f04;margin-top: 1px;" class="btn btn-warning btn-circle btn-lg btn-menu shiny">
			<img src="resources/caisse/img/caisse-web/shopping_cart2.png" style="width:30px;" />
			<span id="span_cmd" class="badge badge-danger" style="color: #030303;background-color: #faa31b;font-weight: bold;">
			</span>
		</a>
</div>
<%} %>

<%if(isJourneeOuverte && isJourneeCaisseOuverte){ %>	
	<div style="height: 50px;width: 100%;line-height: 53px;padding-left: 10%;position: fixed;bottom: 0px;background-color: white;">
	    			
	    <%if(StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("CAISSE_TABLE"))){ %>
			<std:linkPopup action="caisse-web.caisseWeb.initPlan" params="isrp=0" style="padding-left: 6px;padding-top: 7px;" classStyle="btn btn-default btn-circle btn-lg btn-menu shiny" tooltip="Affecter une table">
				<img style="height: 33px;" src="resources/caisse/img/caisse-web/table2.png" /> 
			</std:linkPopup>
		<%} %>  
	    <%if(!isServeur || isCaissier || isManager || isAdmin){%>
			<std:linkPopup action="caisse-web.caisseWeb.initPaiement" style="padding-left: 6px;padding-top: 3px;width:62px;border-radius: 11px;background: #039be5 !important;" classStyle="btn btn-default btn-circle btn-lg btn-menu shiny" tooltip="Encaisser la commande">
				<img style="width: 40px;" src="resources/caisse/img/caisse-web/cash_register_sh.png" />
			</std:linkPopup>
		<%} %>
 
		<std:linkPopup id="send_cuis_pop" action="caisse-web.caisseWeb.miseEnAttente" style="display:none;padding-left: 6px;padding-top: 7px;" classStyle="btn btn-default btn-circle btn-lg btn-menu shiny" tooltip="Mettre la commande en attente" >
			<img style="height: 33px;" src="resources/caisse/img/caisse-web/hourglass.png" />
		</std:linkPopup>
		
<%-- 		<%if(isShowAttBtn){ %> --%>
		<std:link id="send_cuis" action="caisse-web.caisseWeb.miseEnAttente" targetDiv="left-div" style="display:none;padding-left: 6px;padding-top: 7px;" classStyle="btn btn-default btn-circle btn-lg btn-menu shiny" tooltip="Mettre la commande en attente">
			<img style="height: 33px;" src="resources/caisse/img/caisse-web/hourglass.png" />
		</std:link>
<%--          <%} %> --%>
         
         <a style="padding-left: 6px;padding-top: 2px;color: blue;" class="btn btn-default btn-circle btn-lg btn-menu shiny" targetDiv="right-div" wact="<%=EncryptionUtil.encrypt("caisse-web.caisseWeb.initHistorique")%>" params="isrp=1" title="Reprise d'une commande depuis l'historique" href="javascript:void(0);">
          	<i class="fa fa-reply-all"></i>
          </a>
         <%if(StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("CAISSE_TABLE"))){ %>    
	       	 <std:linkPopup classStyle="btn btn-default btn-circle btn-lg btn-menu shiny" style="padding-left: 6px;padding-top: 1px;color: blue;" action="caisse-web.caisseWeb.initPlan" params="isrp=1">
	       	 	<i class="fa fa-reply-all" style="    font-size: 26px;
    color: black;
    margin-top: -5px;
    position: absolute;
    margin-left: -7px;"></i>
	        	<i class="fa fa-th" style="margin-left: -7px;
			    font-size: 20px;
			    color: #000000;
			    position: absolute;
			    margin-top: 10px;"></i>
	          </std:linkPopup>
		<%} %> 
	</div>	
<%} %>		