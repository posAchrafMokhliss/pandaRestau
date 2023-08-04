<%@page import="framework.controller.ContextGloabalAppli"%>
<%@page import="framework.model.common.util.StrimUtil"%>
<%@page import="appli.controller.domaine.caisse.action.CaisseWebBaseAction"%>
<%@page import="framework.model.util.FileUtil"%>
<%@page import="appli.model.domaine.stock.service.IArticleService"%>
<%@page import="framework.model.common.util.ServiceUtil"%>
<%@page import="appli.controller.domaine.util_erp.ContextAppli.STATUT_JOURNEE"%>
<%@page import="framework.model.common.util.DateUtil"%>
<%@page import="framework.model.common.util.StringUtil"%>
<%@page import="appli.model.domaine.personnel.persistant.EmployePersistant"%>
<%@page import="framework.model.common.util.EncryptionUtil"%>
<%@page import="appli.model.domaine.vente.persistant.CaisseMouvementPersistant"%>
<%@page import="framework.model.common.util.BigDecimalUtil"%>
<%@page import="appli.controller.domaine.util_erp.ContextAppli"%>
<%@ taglib uri="http://www.customtaglib.com/complexe" prefix="cplx"%>
<%@ taglib uri="http://www.customtaglib.com/standard" prefix="std"%>
<%@ taglib uri="http://www.customtaglib.com/html" prefix="html"%>
<%@ taglib uri="http://www.customtaglib.com/work" prefix="work"%>
<%@ taglib uri="http://www.customtaglib.com/c" prefix="c"%>
<%@page errorPage="/commun/error.jsp"%>
<%@page import="framework.controller.ControllerUtil"%>

<!DOCTYPE html>
<html lang="fr-fr" style="overflow: hidden;">	
<%
// Purge
ControllerUtil.cleanAll(request);
%>	
	
<!-- Head -->	
	<head>
		<title>Gestion de la caisse automatique</title>
		<jsp:include page="/WEB-INF/fragment/header-resources.jsp"></jsp:include>
		
		<link rel="stylesheet" type="text/css" href="resources/caisse/css/caisse.css?v=<%=StrimUtil.getGlobalConfigPropertie("version.resources")%>15">
	
		<style type="text/css">
			.btn_code_bar{
				font-size: 26px;
			    font-weight: bold;
			    color: black;
			    margin-bottom: 14px;
			}
			.btn.btn-circle {
			    width: 47px;
			    height: 47px;
			    padding: 1px 10px;
			}
		</style>

	<script type="text/javascript">
		function manageZoom(val){
			$("html").css("zoom", val).css("-moz-transform", "scale("+val+")").css("-moz-transform-origin", "0.0");
		}
	
		var tempo = 20000;
		// Tempo pour effacer l'�cran
		var timer = window.setTimeout(function(){}, tempo)
	
		function restCounterDiv(){
			// Mise en veille de l'�cran
			window.clearTimeout(timer);
			$("#menu-detail-div").html('');
			$('#cb_srh').val('');
			
			timer = window.setTimeout(function(){
				$("#menu-detail-div").html('');
				$('#cb_srh').val('');
			}, tempo)
		}
	
		
		var barre_input_focus = $("#cb_srh");
		$(document).ready(function (){
			$("#zoom_slct").change(function(){
				$.cookie('zoom_lect_cock', $(this).val());
				manageZoom($(this).val());
			});
			var zoomCook = $.cookie('zoom_lect_cock');
			if(zoomCook && zoomCook!=null && zoomCook!=''){
				manageZoom(zoomCook);
				$("#zoom_slct").val(zoomCook);
			}
			
			// Empecher session out
			setInterval(function() {
				var url = 'front?w_uact=<%=EncryptionUtil.encrypt("caisse-web.lecteurPrix.work_init")%>';
				callBackJobAjaxUrl(url, false);
			  }, 60000);
			
			
			$("#input_barre input").focusin(function(){
				barre_input_focus = $(this);
			});
			
			//
			$("#code_keys a").click(function(){
				if($(this).attr("id") == 'reset'){
					barre_input_focus.val('');
				} else if ($(this).attr("id") == 'back'){
					barre_input_focus.val(barre_input_focus.val().substring(0, barre_input_focus.val().length-1));
				} else{
					barre_input_focus.val(barre_input_focus.val()+$(this).text());
				}
				barre_input_focus.focus();
			});
			$(document).keydown(function(e) {
		        var code = (e.keyCode ? e.keyCode : e.which);
		        //
		        if(code==13){
		        	e.preventDefault();
		        	$("#load_lnk").trigger("click");
		        	return;
		        }
		    });
		});
		
		
		$(document).ready(function (){
			<%-- Confirmer la deconnexion --%>
			$("#delog_lnk").click(function(){
				showConfirmDeleteBox('<%=EncryptionUtil.encrypt("commun.login.disconnect")%>', null, $("#targ_link"), "Vous &ecirc;tes sur le point de vous d&eacute;connecter.<br>La commande en cours sera <b>perdue</b>. Voulez-vous confirmer ?", null, "Quitter la caisse");
			});
			
			<%-- Code barre --%>
			var barcode="";
		    $(document).keydown(function(e) {
		    	<%-- Ne pas d�clencher si popup authentification ouverte --%>
		    	if($("#generic_modal").length == 1 && ($("#generic_modal").css("display") != "none")){
		    		return;
		    	}
		        var code = (e.keyCode ? e.keyCode : e.which);
		        var sourceEvent = $(e.target).prop('nodeName');
		        var isInput = (sourceEvent == 'INPUT') ? true : false;
		        //
		        if(!isInput && code==13 && $.trim(barcode) != ''){
		        	if($.trim(barcode).length > 5){
		        		e.preventDefault();
		        		//
		        		restCounterDiv();
			        	submitAjaxForm('<%=EncryptionUtil.encrypt("caisse-web.lecteurPrix.loadArtCodeBarre")%>', 'cb='+barcode, $("#data-form"), $("#tmp_lnk"));
		        	}
		            barcode="";
		        } else{
		  			 barcode = barcode + String.fromCharCode(code);
		        }
		    });
			
		    <%-- Plein &eacute;cran et resize --%>
			$("#fullscreen-toggler").click(function(){
				setTimeout(function(){
					refreshSize();
				}, 1000);
			});

			var doit;
			window.onresize = function(){
			  clearTimeout(doit);
			  doit = setTimeout(refreshSize, 100);
			};
			
			refreshSize();
		});
		
		function resetDetailCmd(){
			$("#menu-detail-div").empty();
		}
		
		function refreshSize(){
			var widowHeight = $(window).height();
			var widowWidth = $(window).width();
			
			// Commande
			$("#div_body").css("height", (widowHeight-40)+"px");
		}
		</script>		
	</head>
<!-- /Head -->

<!-- Body -->
<body>
	<std:link id="tmp_lnk" action="caisse-web.lecteurPrix.loadArtCodeBarre" targetDiv="menu-detail-div" style="display:none;" />
	
	<div class="row">
		<jsp:include page="/WEB-INF/commun/center/banner_message.jsp"></jsp:include>
	</div>
			
    <!-- Main Container -->
    <div class="main-container container-fluid">
        <!-- Page Container -->
        <div class="page-container">
			<!-- Page Breadcrumb --> 
				<div class="page-header position-relative" style="left: 0px;top: 0px;<%=StringUtil.isEmpty(CaisseWebBaseAction.GET_STYLE_CONF("PANEL_ENETETE", null))?"background-image: linear-gradient(#fbfbfb, #ddd)":"background:"+ContextGloabalAppli.getGlobalConfig("PANEL_ENETETE")%>">
					<div class="header-title" style="padding-top: 4px;">
				        <std:link classStyle="btn btn-default shiny" id="delog_lnk" style="color: red;" targetDiv="menu-detail-div" icon="fa-3x fa-sign-out" tooltip="Quitter" />
			      	</div>
			      <!--Header Buttons-->
			      <div class="header-buttons">
				        <span style="color: white!important;">Zoom </span>
				        <select id="zoom_slct" style="background-color: transparent;color: #90caf9 !important;">
			      			<option value="1">100%</option>
			      			<option value="0.9">90%</option>
			      			<option value="0.8">80%</option>
			      			<option value="0.7">70%</option>
			      			<option value="0.6">60%</option>
			      			<option value="0.5">50%</option>
			      		</select>
			      		| 
				     <a class="refresh" id="refresh-toggler" href="javascript:" onclick="location.reload();">
				         <i class="glyphicon glyphicon-refresh"></i>
				     </a>
				     <a class="fullscreen" id="fullscreen-toggler" href="#">
				         <i class="glyphicon glyphicon-fullscreen"></i>
				     </a>
				 </div>
			      <!--Header Buttons End-->
			  </div>
			  <!-- /Page Header -->
			
			<div class="page-body" style="position:fixed; margin: 0px;padding: 0px;margin-top: 40px;width: 100%;">
				<div class="widget">
				<std:form name="data-form">
			         <div class="widget-body" id="div_body">
			         	<div class="row">
			         		<div class="col-md-3">
								<!-- Panneau gauche -->
					        	<div class="col-md-12" style="margin-left: 5px;text-align: center;">
					        		<img src="resources/img/caisse/normal/bar-code-scanner.png" style="width: 175px;" />
					        	</div>
					        	<div class="col-md-12" style="margin-top: 10px;">
						         	<div class="col-md-12" id="input_barre" style="margin-right: 2%;">
										<std:text name="cb_srh" placeholder="Code barre" type="string" style="border-radius: 25px !important;font-weight: bold;height:50px;font-size: 25px;float:left;" maxlength="15" />
									</div>	
									<div class="col-md-12" id="code_keys" style="border-left: 1px solid #e1e1e1;padding-left: 2%;margin-top: 10px;">
										<%for(int i=0; i<10; i++){ %>
											<a href="javascript:void(0);" class="btn btn-info btn-circle num_auth_stl btn_code_bar"><%=i %></a>
										<%} %>
										<a href="javascript:void(0);" id="back" class="btn btn-warning btn-circle num_auth_stl btn_code_bar" style="font-size: 12px;margin-left: 20px;"><i class="fa fa-mail-reply"></i></a>
										<a href="javascript:void(0);" id="reset" class="btn btn-warning btn-circle num_auth_stl btn_code_bar" style="font-size: 12px;"><i class="fa fa-times"></i></a>
									</div>
									<div class="col-md-12" style="text-align: center;margin-top: 20px;">
										<std:link id="load_lnk" targetDiv="menu-detail-div" classStyle="btn btn-danger btn-lg shiny" icon="fa fa-search" action="caisse-web.lecteurPrix.loadArtCodeBarre" value="CHERCHER L'ARTICLE" style="font-weight:bold;" />
									</div>
								</div>
							</div>	
							<!-- Panneau droit -->
				        	<div class="col-md-8" id="menu-detail-div" style="overflow-y: auto;overflow-x: hidden;border: 1px solid #9e9e9e;margin-left: 3%;mitext-align: center;">
				        		
				        	</div>	
		               	 </div>
					</div>
				</std:form>
			</div>
			</div>
        </div>
        <!-- /Page Container -->
    </div>
    <!-- Main Container -->
    
    <jsp:include page="/WEB-INF/fragment/footer-resources.jsp"></jsp:include>
	<jsp:include page="/WEB-INF/fragment/static-panels.jsp"/>
    
    <script src="resources/framework/js/keyboard/my_keyboard.js?v=<%=StrimUtil.getGlobalConfigPropertie("version.resources")%>"></script>
    <jsp:include page="/commun/keyboard-popup.jsp" />
    <jsp:include page="/commun/keyboard-popup-num.jsp" />
    
  </body>
    
</html>