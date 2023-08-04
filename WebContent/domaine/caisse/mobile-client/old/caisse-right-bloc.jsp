<%@page import="appli.model.domaine.stock.service.IArticleService"%>
<%@page import="framework.controller.ContextGloabalAppli"%>
<%@page import="framework.model.common.util.BooleanUtil"%>
<%@page import="appli.controller.domaine.caisse.action.CaisseWebBaseAction"%>
<%@page import="appli.controller.domaine.util_erp.ContextAppli"%>
<%@page import="appli.model.domaine.vente.persistant.CaisseMouvementPersistant"%>
<%@page import="appli.controller.domaine.caisse.ContextAppliCaisse"%>
<%@page import="framework.model.common.util.StringUtil"%>
<%@page import="framework.model.common.util.BigDecimalUtil"%>
<%@page import="java.util.List"%>
<%@page import="appli.model.domaine.stock.persistant.ArticlePersistant"%>
<%@page import="java.util.Map"%>
<%@page import="framework.model.common.util.EncryptionUtil"%>
<%@page import="framework.controller.ControllerUtil"%>
<%@page import="framework.model.common.util.ServiceUtil"%>
<%@ taglib uri="http://www.customtaglib.com/complexe" prefix="cplx"%>
<%@ taglib uri="http://www.customtaglib.com/standard" prefix="std"%>
<%@ taglib uri="http://www.customtaglib.com/html" prefix="html"%>
<%@ taglib uri="http://www.customtaglib.com/work" prefix="work"%>
<%@ taglib uri="http://www.customtaglib.com/c" prefix="c"%>
<%@page errorPage="/commun/error.jsp"%>

<script type="text/javascript">

$(document).ready(function (){
	<%if(request.getAttribute("isLoadEvent")!=null){ %>
		$("#up_btn").hide();
	<%}%>
});
</script>
<style>
	.tab-titlz-stl{
	    height: 50px;
	    padding-top: 18px;
	    font-size: 15px;
	    color: #2196f3 !important;
	    line-height: 27px !important;
	}
</style>

	<div style="float: left;width: 100%;">
		<!-- Familles et menus -->
		<div style="float: left;width: 100%;" id="famille-div">
			
			<div class="tabbable">
                 <ul class="nav nav-tabs" id="myTab4">
                    <li class="active tab" style="height: 50px;">
                         <a data-toggle="tab" id="main_menu_tab" class="tab-titlz-stl" href="#tab_fam">NOS ARTICLES</a>
                     </li>
                     <li class="tab" style="height: 50px;">
                         <a data-toggle="tab" href="#tab_mnu" class="tab-titlz-stl">NOS MENUS</a>
                     </li>
                 </ul> 
				
                 <c:set var="caisseWeb" value="<%=new CaisseWebBaseAction() %>" />
                 <c:set var="encryptionUtil" value="<%=new EncryptionUtil() %>" />
                 
                 <div id="tab_det_fam" class="tab-content" style="overflow-y: auto;overflow-x: hidden;">
                     <div id="tab_fam" class="tab-pane in active">
	                   	<c:forEach var="famille" items="${listFamille }">
	                   		<div class="col-xs-12 col-sm-6 col-md-6 col-lg-4">
	                   			<std:link action="caisse-web.caisseWeb.familleEvent" targetDiv="right-div" workId="${famille.id }" classStyle="caisse-top-mobile-btn famille-btn" value="">
		         					<span class="span-img-stl">
		         						<img alt="" src="resourcesCtrl?elmnt=${encryptionUtil.encrypt(famille.getId().toString())}&path=famille&rdm=${famille.date_maj.getTime()}" class="img-caisse-stl">
		         					</span>
		         					<span class="span-libelle-stl">${famille.libelle }</span>
		                   		</std:link>
		                   	</div>
						</c:forEach>
	                </div>
	                <div id="tab_mnu" class="tab-pane">
				         <c:forEach var="menu" items="${listMenu }">
				         	<div class="col-xs-12 col-sm-6 col-md-6 col-lg-4">
					         	<std:link action="caisse-web.caisseWeb.loadMenu" targetDiv="right-div" workId="${menu.id }" classStyle="caisse-top-mobile-btn menu-btn" value="">
					         		<span class="span-img-stl">
					         			<img alt="" src="resourcesCtrl?elmnt=${encryptionUtil.encrypt(menu.getId().toString())}&path=menu&rdm=${menu.date_maj.getTime()}" class="img-caisse-stl">
					         		</span>
					         		<span class="span-libelle-stl">${menu.libelle }</span>
					         	</std:link>
					         </div>	
						</c:forEach>
                   </div>
                </div>
            </div>
		</div>
	</div>
		
<script type="text/javascript">
$(document).ready(function (){
	/*Handles Popovers*/
	var popovers = $('[data-toggle=popover]');
	$.each(popovers, function () {
	    $(this)
	        .popover({
	            html: true,
	            template: '<div class="popover ' + $(this)
	                .data("class") +
	                '"><div class="arrow"></div><h3 class="popover-title ' +
	                $(this)
	                .data("titleclass") + '">Popover right</h3><div class="popover-content"></div></div>'
	        });
	});

	var hoverpopovers = $('[data-toggle=popover-hover]');
	$.each(hoverpopovers, function () {
	    $(this)
	        .popover({
	            html: true,
	            template: '<div class="popover ' + $(this)
	                .data("class") +
	                '"><div class="arrow"></div><h3 class="popover-title ' +
	                $(this)
	                .data("titleclass") + '">Popover right</h3><div class="popover-content"></div></div>',
	            trigger: "hover"
	        });
	});
    });
 </script>   