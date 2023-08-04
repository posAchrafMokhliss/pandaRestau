<%@page
	import="appli.model.domaine.vente.persistant.MenuCompositionPersistant"%>
<%@page
	import="appli.model.domaine.vente.persistant.ListChoixPersistant"%>
<%@page
	import="appli.model.domaine.vente.persistant.MenuCompositionDetailPersistant"%>
<%@page
	import="appli.model.domaine.vente.persistant.ListChoixDetailPersistant"%>
<%@page import="framework.controller.ControllerUtil"%>
<%@page import="framework.model.common.util.EncryptionUtil"%>
<%@page import="framework.model.common.util.StringUtil"%>
<%@page import="framework.model.common.util.BigDecimalUtil"%>
<%@page import="java.math.BigDecimal"%>
<%@page import="framework.model.common.util.ServiceUtil"%>
<%@page import="appli.model.domaine.stock.service.IArticleService"%>
<%@page
	import="appli.controller.domaine.caisse.action.CaisseWebBaseAction"%>
<%@page import="java.util.Map"%>
<%@page import="appli.model.domaine.stock.persistant.FamillePersistant"%>
<%@page import="appli.model.domaine.stock.persistant.ArticlePersistant"%>
<%@page import="java.util.List"%>
<%@ taglib uri="http://www.customtaglib.com/complexe" prefix="cplx"%>
<%@ taglib uri="http://www.customtaglib.com/standard" prefix="std"%>
<%@ taglib uri="http://www.customtaglib.com/html" prefix="html"%>
<%@ taglib uri="http://www.customtaglib.com/work" prefix="work"%>
<%@ taglib uri="http://www.customtaglib.com/c" prefix="c"%>

<%
	List<FamillePersistant> listSousFamille = (List<FamillePersistant>) request.getAttribute("listFamille");
List<ArticlePersistant> listArticle = (List<ArticlePersistant>) request.getAttribute("listArticle");
Map<String, Object> listMenu = (Map<String, Object>) request.getAttribute("listMenu");

IArticleService service = (IArticleService) ServiceUtil.getBusinessBean(IArticleService.class);
List<MenuCompositionPersistant> listSousMenu = (List<MenuCompositionPersistant>) ControllerUtil
		.getUserAttribute("LIST_SOUS_MENU", request);
Integer step = (Integer) ControllerUtil.getUserAttribute("STEP_MNU", request);
%>
<%
	if (listSousMenu != null && listSousMenu.size() > 0) {
%>
<div id="WiredWizard" class="wizard wizard-wired"
	data-target="#WiredWizardsteps">
	<ul class="steps">
		<%
			int i = 0;
		int withLi = 100 / listSousMenu.size();
		for (MenuCompositionPersistant sousMenu : listSousMenu) {
			String libelle = sousMenu.getLibelle().replaceAll("\\#", "");
		%>
		<li data-target="#wiredstep<%=i%>"
			class='<%=(step == i) ? "active" : ((step > i) ? "complete" : "")%>'
			style="width: <%=withLi%>%;"><span class="step"><%=i + 1%></span>
			<span class="title"><%=libelle%></span><span class="chevron"></span>
		</li>
		<%
			i++;
		}
		%>
	</ul>
</div>
<%
	}
%>


<%
	if (listSousFamille != null) {
	for (FamillePersistant familleP : listSousFamille) {
%>
<%
	String libelle = familleP.getLibelle().replaceAll("\\#", "");
%>
<std:link
	style='background-color: #FFF4E0!important;box-shadow: 2px 3px 3px #000000;
    color: #000000 !important;'
	action="caisse-web.caisseWeb.familleEvent" targetDiv="menu-detail-div"
	workId="<%=familleP.getId().toString()%>"
	classStyle="caisse-btn detail-famille-btn" value="">
	<span class="span-img-stl"
		style="width: 20%;margin-left: 5%; height: 69%; margin-top: 3%;"> <img alt="" onerror="this.onerror=null;this.remove();"
		style="border-radius: 40%; transform: scale(1.3);"
		src="resourcesCtrl?elmnt=<%=EncryptionUtil.encrypt(familleP.getId().toString())%>&path=famille&rdm=<%=(familleP.getDate_maj() != null ? familleP.getDate_maj().getTime() : "1")%>"
		class="img-caisse-stl">
	</span>
	<span class="span-libelle-stl" style="font-size: 2em; float: left;"><%=libelle%>&nbsp;&nbsp;</span>
</std:link>
<%
	}
}
%>

<!-- Articles -->
<%
	if (listArticle != null) {
	for (ArticlePersistant articleP : listArticle) {
		String libelle = articleP.getLibelle().replaceAll("\\#", "");
		String prix = BigDecimalUtil.formatNumber(articleP.getPrix_vente());
%>
<std:link
	style='background-color: #ffd890!important;
    color: #000000 !important;
    box-shadow: 2px 3px 3px #000000;
    border: none;'
	action="caisse-web.caisseWeb.addArticleFamilleCmd" targetDiv="left-div"
	workId="<%=articleP.getId().toString()%>" classStyle="caisse-btn"
	value="">
	<span class="span-img-stl"
		style="width: 75px; height: 51px; margin-top: 13px; margin-left: 36px;">
		<img alt="" onerror="this.onerror=null;this.remove();"
		style="border: 1px solid; width: 75px; height: 51px; margin-top: -7px;"
		src="resourcesCtrl?elmnt=<%=EncryptionUtil.encrypt(articleP.getId().toString())%>&path=article&rdm=<%=(articleP.getDate_maj() != null ? articleP.getDate_maj().getTime() : "1")%>"
		class="img-caisse-det-stl">
	</span>
	<span class="span-libelle-stl"
		style="margin-top: 4px; font-size: 1.5em; text-align: left;"><i
		class="fa fa-tag" style='color: red;'></i><%=libelle%>&nbsp;&nbsp;</span>
	<span class="span-prix-stl"
		style="width: 36%; font-size: 2em; margin-left: 3em; color: #403d3d;"><%=prix%>DHs
	</span>
</std:link>
<%
	}
}
%>

<%
	if (listMenu != null) {
	for (String cle : listMenu.keySet()) {
		Object elementP = listMenu.get(cle);

		if (elementP instanceof MenuCompositionPersistant) {
	MenuCompositionPersistant menuP = ((MenuCompositionPersistant) elementP);
	String libelle = menuP.getLibelle().replaceAll("\\#", "");
	String prix = BigDecimalUtil.isZero(menuP.getMtt_prix())
			? ""
			: BigDecimalUtil.formatNumber(menuP.getMtt_prix());
%>

<std:link
	style='background-color: #FFF4E0!important;box-shadow: 2px 3px 3px #000000;
    color: #000000 !important;'
	action="caisse-web.caisseWeb.menuCompoEvent"
	targetDiv="menu-detail-div" workId="<%=menuP.getId().toString()%>"
	classStyle="caisse-btn detail-menu-btn" value="">
	<span class="span-img-stl"
		style="width: 20%; margin-top: 10px; height: 68%;"> <img alt="" onerror="this.onerror=null;this.remove();"
		style="border-radius: 40%; margin-top: 3px; transform: scale(1.5);"
		src="resourcesCtrl?elmnt=<%=EncryptionUtil.encrypt(menuP.getId().toString())%>&path=menu&rdm=<%=(menuP.getDate_maj() != null ? menuP.getDate_maj().getTime() : "1")%>"
		class="<%=StringUtil.isNotEmpty(prix) ? "img-caisse-det-stl" : "img-caisse-stl"%>">
	</span>
	<span class="span-libelle-stl"
		style="float: left; margin-top: 4px; font-size: 2em; float: left;"><%=libelle%>&nbsp;&nbsp;</span>
	<%
		if (StringUtil.isNotEmpty(prix)) {
	%>
	<span class="span-prix-stl"
		style="width: 45%; font-size: 2em; margin-right: 3em;">[<%=prix%>]
	</span>
	<%
		}
	%>
</std:link>
<%
	} else if (elementP instanceof ArticlePersistant) {
String params = "mnu=" + cle;
ArticlePersistant artP = (ArticlePersistant) elementP;
String libelle = artP.getLibelle().replaceAll("\\#", "");
%>


<std:link
	style='background-color: #ffd890!important;
    color: #000000 !important;box-shadow: 2px 3px 3px #000000;
    border: none;'
	action="caisse-web.caisseWeb.addArticleFamilleCmd" targetDiv="left-div"
	workId="<%=artP.getId().toString()%>" classStyle="caisse-btn" value="">
	<span class="span-img-stl"
		style="width: 75px; height: 51px; margin-top: 13px; margin-left: 36px;">
		<img alt="" onerror="this.onerror=null;this.remove();"
		style="border: 1px solid; width: 75px; height: 51px; margin-top: -7px;"
		src="resourcesCtrl?elmnt=<%=EncryptionUtil.encrypt(artP.getId().toString())%>&path=article&rdm=<%=(artP.getDate_maj() != null ? artP.getDate_maj().getTime() : "1")%>"
		class="img-caisse-det-stl">
	</span>
	<span class="span-libelle-stl"
		style="margin-top: 4px; font-size: 1.5em; text-align: left;"><i
		class="fa fa-tag" style='color: red;'></i><%=libelle%>&nbsp;&nbsp;</span>
</std:link>

<%
	} else if (elementP instanceof MenuCompositionDetailPersistant) {
String params = "det=" + ((MenuCompositionDetailPersistant) elementP).getId() + "&tp=MC&mnu=" + cle;
MenuCompositionDetailPersistant mnuCompo = (MenuCompositionDetailPersistant) elementP;
String prix = BigDecimalUtil.isZero(mnuCompo.getPrix()) ? "" : BigDecimalUtil.formatNumber(mnuCompo.getPrix());

if (mnuCompo.getOpc_article() != null) {
	ArticlePersistant artP = mnuCompo.getOpc_article();
	String libelle = artP.getLibelle().replaceAll("\\#", "");
%>


<std:link
	style='background-color: #ffd890!important;
    color: #000000 !important;box-shadow: 2px 3px 3px #000000;
    border: none;'
	action="caisse-web.caisseWeb.addArticleFamilleCmd" targetDiv="left-div"
	workId="<%=artP.getId().toString()%>" classStyle="caisse-btn" value="">
	<span class="span-img-stl"
		style="width: 75px; height: 51px; margin-top: 13px; margin-left: 36px;">
		<img alt="" onerror="this.onerror=null;this.remove();"
		style="border: 1px solid; width: 75px; height: 51px; margin-top: -7px;"
		src="resourcesCtrl?elmnt=<%=EncryptionUtil.encrypt(artP.getId().toString())%>&path=article&rdm=<%=(artP.getDate_maj() != null ? artP.getDate_maj().getTime() : "1")%>"
		class="img-caisse-det-stl">
	</span>
	<span class="span-libelle-stl"
		style="margin-top: 4px; font-size: 1.5em; text-align: left;"><i
		class="fa fa-tag" style='color: red;'></i><%=libelle%>&nbsp;&nbsp;</span>
	<span class="span-prix-stl"
		style="width: 36%; font-size: 2em; margin-left: 3em; color: #403d3d;"><%=prix%>DHs
	</span>
</std:link>

<%
	} else if (((MenuCompositionDetailPersistant) elementP).getOpc_famille() != null) {
FamillePersistant famP = ((MenuCompositionDetailPersistant) elementP).getOpc_famille();
String libelle = famP.getLibelle().replaceAll("\\#", "");
%>
<std:link
	style='background-color: #FFF4E0!important;box-shadow: 2px 3px 3px #000000;
    color: #000000 !important;'
	action="caisse-web.caisseWeb.menuCompoDetailEvent"
	targetDiv="menu-detail-div"
	workId="<%=((MenuCompositionDetailPersistant) elementP).getId().toString()%>"
	classStyle="caisse-btn detail-famille-btn" value="">
	<span class="span-img-stl"
		style="width: 20%;margin-left: 5%; height: 69%; margin-top: 3%;"> <img alt="" onerror="this.onerror=null;this.remove();"
		style="border-radius: 40%; transform: scale(1.3);"
		src="resourcesCtrl?elmnt=<%=EncryptionUtil.encrypt(famP.getId().toString())%>&path=famille&rdm=<%=(famP.getDate_maj() != null ? famP.getDate_maj().getTime() : "1")%>"
		class="<%=StringUtil.isNotEmpty(prix) ? "img-caisse-det-stl" : "img-caisse-stl"%>">
	</span>
	<span class="span-libelle-stl"
		style="margin-top: 4px; font-size: 2em; float: left;"><%=libelle%>&nbsp;&nbsp;</span>
	<%
		if (StringUtil.isNotEmpty(prix)) {
	%>
	<span class="span-prix-stl"
		style="width: 45%; font-size: 2em; margin-left: 3em;">[<%=prix%>]
	</span>
	<%
		}
	%>
</std:link>
<%
	} else if (((MenuCompositionDetailPersistant) elementP).getOpc_list_choix() != null) {
String libelle = ((MenuCompositionDetailPersistant) elementP).getOpc_list_choix().getLibelle().replaceAll("\\#", "");
%>
<std:link
	style='<%=CaisseWebBaseAction.GET_STYLE_CONF("BUTTON_LIST_CHOIX", "COULEUR_TEXT_DETAIL")%>'
	action="caisse-web.caisseWeb.menuCompoDetailEvent"
	targetDiv="menu-detail-div"
	workId="<%=((MenuCompositionDetailPersistant) elementP).getId().toString()%>"
	classStyle="caisse-btn detail-choix-btn" value="">
	<span class="span-img-stl"
		style="width: 30%; height: 100%; margin-top: 3%;">&nbsp;</span>
	<span class="span-libelle-stl"
		style="margin-top: 4px; font-size: 2em; float: left;"><%=libelle%>&nbsp;&nbsp;</span>
	<%
		if (StringUtil.isNotEmpty(prix)) {
	%>
	<span class="span-prix-stl"
		style="width: 45%; font-size: 2em; margin-left: 3em;">[<%=prix%>]
	</span>
	<%
		}
	%>
</std:link>
<%
	}
} else if (elementP instanceof ListChoixDetailPersistant) {
String params = "det=" + ((ListChoixDetailPersistant) elementP).getId() + "&tp=LC&mnu=" + cle;

if (((ListChoixDetailPersistant) elementP).getOpc_article() != null) {
ArticlePersistant articleP = ((ListChoixDetailPersistant) elementP).getOpc_article();
String libelle = articleP.getLibelle().replaceAll("\\#", "");
%>

<std:link
	style='background-color: #ffd890!important;
    color: #000000 !important;box-shadow: 2px 3px 3px #000000;
    border: none;'
	action="caisse-web.caisseWeb.addArticleFamilleCmd" targetDiv="left-div"
	workId="<%=articleP.getId().toString()%>" classStyle="caisse-btn"
	value="">
	<span class="span-img-stl"
		style="width: 75px; height: 51px; margin-top: 13px; margin-left: 36px;">
		<img alt="" onerror="this.onerror=null;this.remove();"
		style="border: 1px solid; width: 75px; height: 51px; margin-top: -7px;"
		src="resourcesCtrl?elmnt=<%=EncryptionUtil.encrypt(articleP.getId().toString())%>&path=article&rdm=<%=(articleP.getDate_maj() != null ? articleP.getDate_maj().getTime() : "1")%>"
		class="img-caisse-det-stl">
	</span>
	<span class="span-libelle-stl"
		style="margin-top: 4px; font-size: 1.5em; text-align: left;"><i
		class="fa fa-tag" style='color: red;'></i><%=libelle%>&nbsp;&nbsp;</span>
</std:link>
<%
	} else if (((ListChoixDetailPersistant) elementP).getOpc_famille() != null) {
FamillePersistant familleP = ((ListChoixDetailPersistant) elementP).getOpc_famille();
String libelle = familleP.getLibelle().replaceAll("\\#", "");
%>
<std:link
	style='background-color: #FFF4E0!important;box-shadow: 2px 3px 3px #000000;
    color: #000000 !important;'
	action="caisse-web.caisseWeb.menuCompoChoixEvent"
	targetDiv="menu-detail-div"
	workId="<%=((ListChoixDetailPersistant) elementP).getId().toString()%>"
	classStyle="caisse-btn detail-famille-btn" value="">
	<span class="span-img-stl"
		style="width: 20%;margin-left: 5%; height: 69%; margin-top: 3%;"> <img alt="" onerror="this.onerror=null;this.remove();"
		style="border-radius: 40%; transform: scale(1.3);"
		src="resourcesCtrl?elmnt=<%=EncryptionUtil.encrypt(familleP.getId().toString())%>&path=famille&rdm=<%=(familleP.getDate_maj() != null ? familleP.getDate_maj().getTime() : "1")%>"
		class="img-caisse-stl">
	</span>
	<span class="span-libelle-stl" style="font-size: 2em; float: left;"><%=libelle%>&nbsp;&nbsp;</span>
</std:link>
<%
	} else if (((ListChoixDetailPersistant) elementP).getOpc_list_choix() != null) {
ListChoixPersistant lisChoixP = ((ListChoixDetailPersistant) elementP).getOpc_list_choix();
String libelle = lisChoixP.getLibelle().replaceAll("\\#", "");
%>
<std:link
	style='<%=CaisseWebBaseAction.GET_STYLE_CONF("BUTTON_LIST_CHOIX", "COULEUR_TEXT_DETAIL")%>'
	action="caisse-web.caisseWeb.menuCompoChoixEvent"
	targetDiv="menu-detail-div"
	workId="<%=((ListChoixDetailPersistant) elementP).getId().toString()%>"
	classStyle="caisse-btn detail-choix-btn" value="">
	<span class="span-img-stl"
		style="width: 30%; height: 100%; margin-top: 3%;">&nbsp;</span>
	<span class="span-libelle-stl" style="font-size: 2em; float: left;"><%=libelle%>&nbsp;&nbsp;</span>
</std:link>
<%
	}
}
}
}
%>

<%
	if (listSousMenu != null && listSousMenu.size() > 0) {
%>
<div class="row" id="footer_cmd_div"
	style="width: 75%; height: 70px; text-align: center; position: fixed; bottom: 12%; margin-top: 15px;">
	<div class="btn-group" style='<%=step == 0 ? "display: none;" : ""%>'>
		<button id="btn-wizard-prev" type="button"
			class="btn btn-danger btn-sm btn-prev" targetDiv="menu-detail-div"
			wact='<%=EncryptionUtil.encrypt("caisse-web.caisseWeb.loadNextStep")%>'
			params="stepr=<%=step%>"
			style="height: 44px; width: 190px; font-size: 23px; font-weight: bold; margin-right: 9px; border-radius: 40px;">
			<i style="font-size: 30px;" class="fa fa-angle-left"></i>Pr&eacute;c&eacute;dent
		</button>
	</div>
	<div class="btn-group"
		style='<%=step >= listSousMenu.size() - 1 ? "display: none;" : ""%>'>
		<button id="btn-wizard-next" type="button"
			class="btn btn-danger btn-sm btn-next" data-last="Finish"
			targetDiv="menu-detail-div"
			wact='<%=EncryptionUtil.encrypt("caisse-web.caisseWeb.loadNextStep")%>'
			params="step=<%=step%>"
			style="margin-left: 10%; height: 44px; width: 150px; font-weight: bold; font-size: 23px; border-radius: 40px;">
			Suivant <i style="font-size: 30px;" class="fa fa-angle-right"></i>
		</button>
	</div>
	<%
		// if (step >= listSousMenu.size()-1) {
	%>
	<button class="btn btn-danger" type="button"
		onclick="$('#menu-detail-div').html('');" targetDiv="left-div"
		wact='<%=EncryptionUtil.encrypt("caisse-web.caisseWeb.finaliserMenuCmdStep")%>'
		params="step=<%=step%>"
		style="margin-left: 20%; height: 44px; width: 170px; font-weight: bold; font-size: 23px; border-radius: 40px; background-color: #529609 !important;">
		Terminer</i>
	</button>
	<%
		//}
	%>

</div>

<!--Page Related Scripts-->
<script src="resources/framework/js/fuelux/wizard/wizard-custom.js"></script>
<script src="resources/framework/js/toastr/toastr.js?v=1.0"></script>

<script type="text/javascript">
	jQuery(function($) {
		$('#WiredWizard').wizard();
	});
</script>
<%
	}
%>
