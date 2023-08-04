<%@page import="appli.controller.domaine.util_erp.ContextAppli.SOFT_ENVS"%>
<%@page import="framework.controller.ContextGloabalAppli"%>
<%@page import="appli.model.domaine.vente.persistant.CaisseMouvementPersistant"%>
<%@page import="framework.model.common.util.StringUtil"%>
<%@page import="framework.model.common.util.StrimUtil"%>
<%@page import="appli.model.domaine.stock.service.IMouvementService"%>
<%@page import="java.util.Map"%>
<%@page import="framework.controller.ControllerUtil"%>
<%@page import="framework.model.util.FileUtil"%>
<%@page import="appli.controller.domaine.util_erp.ContextAppli"%>
<%@page import="appli.model.domaine.stock.service.IArticleService"%>
<%@page import="framework.model.common.util.ServiceUtil"%>
<%@page import="appli.controller.domaine.caisse.ContextAppliCaisse"%>
<%@page import="framework.model.common.util.EncryptionUtil"%>
<%@ taglib uri="http://www.customtaglib.com/complexe" prefix="cplx"%>
<%@ taglib uri="http://www.customtaglib.com/standard" prefix="std"%>
<%@ taglib uri="http://www.customtaglib.com/html" prefix="html"%>
<%@ taglib uri="http://www.customtaglib.com/work" prefix="work"%>
<%@ taglib uri="http://www.customtaglib.com/c" prefix="c"%>

<%
String soft = StrimUtil.getGlobalConfigPropertie("context.soft");
boolean isCloseSaisieQte = StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("CLAC_NBR_SHOW"));
boolean isRestau = SOFT_ENVS.restau.toString().equals(soft);
%>
		
		<style type="text/css">
		.context-menu-root .del {
				color: red;
			}
			.context-menu-root .off {
				color: green;
			}
			.context-menu-root .com{
				color: blue;
			}
			
			.context-menu-root .suite{
				color: #9c27b0;
			}
			.context-menu-root .ann-suite{
				color: #ea8b00;
			}
			
			.context-menu-active{
				background-color: yellow;
			}
			
		.btn_code_bar{
				font-size: 26px;
			    font-weight: bold;
			    color: black;
			    margin-bottom: 14px;
			}
			.btn.btn-circle {
			    width: 40px;
			    height: 40px;
			    padding: 0px 8px;
			}
			.btn-top-cai{
				height: 46px;
			}
			.btn-top-cai i{ 
				font-size: 25px !important;
			}
			.span-sub-cai{
			    font-size: 9px;
			    color: gray;
			    position: absolute;
			    left: 2px;
			    bottom: 0px;
			}
			.span-jour-title{
			    margin-left: 10px;
			    position: absolute;
			    width: 190px;
			    font-weight: bold;
			    transform: scale(1, 1.2);
			}
			
			 ::-webkit-scrollbar {
			    width: 1.5em;
			    height: 2em
			}
			::-webkit-scrollbar-button {
			    background: black;
			    height: 30px;
			}
			::-webkit-scrollbar-track-piece {
			    background: #888;
			}
			::-webkit-scrollbar-thumb {
			    background: #57b5e3
			}
		</style>
		
<%
boolean isCaisseVerouille = ControllerUtil.getMenuAttribute("IS_CAISSE_VERROUILLE", request) != null;
boolean isCaisseNotFermee = (ContextAppliCaisse.getJourneeCaisseBean() != null && "O".equals(ContextAppliCaisse.getJourneeCaisseBean().getStatut_caisse()));
boolean isJourneeCaisseOuverte = !isCaisseVerouille && isCaisseNotFermee;
int WIDTH_CMD = isJourneeCaisseOuverte ? 370 : 0;
int HEIGHT_DETAIL = 370;
%>
<script type="text/javascript">
<%-- Code barre --%>
var barcode = "";
//Timer présentoir
$(document).ready(function (){
	refreshSize();
	
    $(document).on('click', 'a[id="del_cli_lnk"]', function(){
    	$("#targ_link").attr("targetDiv", "left-div");
		showConfirmDeleteBox($(this).attr("act"), $(this).attr("params"), $("#targ_link"), "Ce client ainsi que ses articles seront supprim&eacute;s.<br>Voulez-vous confirmer ?", null, "Suppression client");
    });
    $(document).on('click', 'a[id="del_tab_lnk"]', function(){
    	$("#targ_link").attr("targetDiv", "left-div");
		showConfirmDeleteBox($(this).attr("act"), $(this).attr("params"), $("#targ_link"), "Cette table ainsi que ses articles et ses clients seront supprim&eacute;s.<br>Voulez-vous confirmer ?", null, "Suppression client");
    });
	
	$(document).on('click', '#annul_cmd_main', function(){
		$("#targ_link").attr("targetDiv", "left-div");
		showConfirmDeleteBox($(this).attr("act"), "tp=annul", $("#targ_link"), "Cette commande sera annul&eacute;e.<br>Voulez-vous confirmer ?", null, "Annulation commande");
	});
    
	 $("#zoom_slct").change(function(){
		writeLocalStorage('zoom_cai_cock', $(this).val());
		refreshSize();
	});
	
	 $(document).keydown(function(e) {
		//Ne pas déclencher si popup authentification ouverte
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
	        	$("#targ_link").attr("targetDiv", "left-div");
	        	submitAjaxForm('<%=EncryptionUtil.encrypt("caisse-web.caisseWeb.manageLecteursCarteBarre")%>', 'cb='+barcode, $("#data-form"), $("#targ_link"));
	    	}
	        barcode="";
	    } else{
			 barcode = barcode + String.fromCharCode(code);
	    }
	});
	
	//Plein &eacute;cran et resize
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
	<%-- Confirmer la deconnexion --%>
	$("#delog_lnk").click(function(){
        bootbox.dialog({
            message: $("#decon-modal-div").html(),
            title: "Accès caisse",
            className: "modal-darkorange"
        });
	});
	$("#lock_lnk").click(function(){
		clearInterval(intervalTime);
		showConfirmDeleteBox('<%=EncryptionUtil.encrypt("caisse-web.caisseWeb.verrouillerCaisse")%>', null, $("#targ_link"), "Vous &ecirc;tes sur le point de verouiller cette caisse.<br>Voulez-vous confirmer ?", null, "Verouillage de la caisse");
	});
	<%-- Click ligne tableau --%>
	$(document).on('click', '#context-mnu a[act]', function(){
		$("#targ_link").attr("targetDiv", "left-div");
		var tp = $(this).attr("tp");
		if(tp == 'grp'){
			$("#targ_link").attr("wact", $(this).attr("act")).trigger("click");
		}
	});
	
	<%-- Plein �cran de la partie droite --%>
	$("#toogle-detail").click(function(){
		if($("#right-div").attr("disp") != 'full'){
			$("#right-div").attr("disp", 'full');
			$("#left-div").css("display", "none");
			$("#right-div")
				.css("position", "fixed")
				.css("width", "100%")
				.css("height",  $(window).height()+"px")
		    	.css("top", "40px")
		    	.css("left", "0px");
		} else{
			$("#right-div").removeAttr("disp");
			$("#right-div").css("position", "relative").css("top", "0px");
			$("#left-div").css("display", "");
			refreshSize();
		}
	});
	
	// Gestion mise en veille -------------------------------
	<%
	boolean isVeille = StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("AFFICHER_IMAGE_VEILLE"));
	String imgBack = null;
	if(isVeille){
		IArticleService service = (IArticleService)ServiceUtil.getBusinessBean(IArticleService.class);
		Map<String, byte[]> dataimg = service.getDataImage(ContextAppli.getEtablissementBean().getId(), "paramFE");
		if(dataimg.size() > 0){
			imgBack = "data:image/jpeg;base64,"+FileUtil.getByte64(dataimg.entrySet().iterator().next().getValue());%>
			// Tempo pour effacer l'�cran
			var timer = startLockTimeOut();
			// Mise en veille de l'�cran --------------------------------------------------------
			$(document).ajaxComplete(function(e){
				timer = startLockTimeOut(timer);
			});
			//
			$(document).off('click', '#lock_caisse_div');
			$(document).on('click', '#lock_caisse_div', function(e){
				$("#lock_caisse_div").hide(1000);
				timer = startLockTimeOut(timer);
			});
		<%}
	}%>
	
	var barre_input_focus = $("#art\\.code_barre");
	$(document).ready(function (){
		init_keyboard_events();
		
		$("#search_tab").click(function(){
			$("#art\\.code_barre, #art\\.code, #art\\.code_pese").val('');
			setTimeout(function(){
				$("#art\\.code_barre").focus();
			}, 1000);
		});
		$(document).on('click', '#myTab4 a', function(){
			resetDetailCmd();
			barcode = "";
		});
		
		$(document).off('focusin', '#input_barre input');
		$(document).on('focusin', '#input_barre input', function(){
			barre_input_focus = $(this);
		});
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
		// Calc event
		$(document).off('click', '.btn-calc').on('click', '.btn-calc', function(){
			<%if(isCloseSaisieQte){%>
				if($(this).text() == 'Tbl'){
					$("#select_pln_lnk").attr("params", "is_saisie=1&is_calc=1&ref="+$("#qte_calc").val()).trigger("click");
					return;
				} else if($(this).text() == 'C'){
					clearClac();
					<%if(isRestau){%>
						$("#calc_lnk").hide();
					<%}%>
				} else{
					$("#calc_lnk").show().text($.trim($("#calc_lnk").text())+$.trim($(this).text()));
					$("#qte_calc").val($("#calc_lnk").text());
				}
				<%if(isRestau){%>
					$("#calc_lnk").trigger("click");
				<%}%>				
			<%} else {//--------------------------%>
				if($(this).text() == 'Tbl'){
					$("#select_pln_lnk").attr("params", "is_saisie=1&is_calc=1&ref="+$("#qte_calc").val()).trigger("click");
					return;
				} else if($(this).text() == 'X'){
					$("#calc_lnk").trigger("click");
					return;
				} else if($(this).text() == 'C'){
					clearClac();
				} else{
					$("#calc_lnk").text($.trim($("#calc_lnk").text())+$.trim($(this).text()));
					$("#qte_calc").val($("#calc_lnk").text());
				}
			<%} %>
		}); 
		$("#left-div").show();
		// Clear timer
		for (var i = 1; i < (interval_tracker_id+10); i++){
	        window.clearInterval(i);
		}
		refreshSize();
		
		intervalTime = setInterval(function() {
	        var momentNow = moment();
	        $('#time-part').html(momentNow.format('HH:mm:ss'));
	    }, 100);
	});
	
	<%
	CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant)ControllerUtil.getUserAttribute("CURRENT_COMMANDE", request);
	// Gestion mise en attente (aussi dans commande détail)
	if(CURRENT_COMMANDE == null || StringUtil.isEmpty(CURRENT_COMMANDE.getType_commande())){%>
		$("#att_pop_lnk").show();
		$("#att_std_lnk").hide();
	<%} else{%>
		$("#att_pop_lnk").hide();
		$("#att_std_lnk").show();
	<%} %>
	
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

function clearClac(){
		$("#calc_lnk").html('<img src="resources/caisse/img/caisse-web/calculator_blue.png">');
		$("#qte_calc").val('');
}

function startLockTimeOut(timer){
	window.clearTimeout(timer);
	timer = window.setTimeout(function(){
		$("#lock_caisse_div").show(1000);
	}, 90000);
	return timer;
}
function getWindowRatioZoom(){
	var ratioW = 1;
	var ratioH = 1;
	var dataZoom = readLocalStorage('zoom_cai_cock');
	if(dataZoom && dataZoom!=null && dataZoom!='' && dataZoom!='1'){
		if(dataZoom == '0.9'){
			ratioW = 9;
			ratioH = 9;
		} else if(dataZoom == '0.85'){
			ratioW = 5.6;
			ratioH = 5.6;
		} else if(dataZoom == '0.8'){
			ratioW = 4;
			ratioH = 4;
		} else if(dataZoom == '0.75'){
			ratioW = 3;
			ratioH = 3;
		} else if(dataZoom == '0.7'){
			ratioW = 2.35;
			ratioH = 2.35;
		} else if(dataZoom == '0.6'){
			ratioW = 1.5;
			ratioH = 1.5;
		} else if(dataZoom == '0.5'){
			ratioW = 1;
			ratioH = 1;
		}
	}
	var ratioArray = [ratioW, ratioH];
	
	return ratioArray;
}

function manageZoom(val){
	$("html").css("zoom", val).css("-moz-transform", "scale("+val+")").css("-moz-transform-origin", "0.0");
}

function resetDetailCmd(){
	$("#menu-detail-div").empty();
}

function managerFooterBanner(){
	$("#back_btn, #up_btn").hide();
}

function managerInitCaisseTmp(){
	if ($.active > 0) {
		setTimeout(function(){
			managerInitCaisseTmp();
		}, 1000);
		return;
	}
	if($("#top_msg_banner_det span").length==0){
		$("button[id^='close_modal']").each(function(){
			$(this).trigger('click');
			$(".modal-backdrop").hide();
		});
		managerFooterBanner();
	}
}

function managerInitCaisse(isImmediat){
	if(isImmediat){
		managerInitCaisseTmp();
	} else{
		setTimeout(function(){
			managerInitCaisseTmp();
		}, 1000);
	}
}

</script>

	<std:link id="select_pln_lnk" action="caisse-web.caisseWeb.selectPlan" targetDiv="left-div" />
	<a style="display: none;" id="lnk_cai_hist" targetDiv="menu5" wact="<%=EncryptionUtil.encrypt("caisse-web.caisseWeb.initHistorique") %>" params="isrp=0" href="javascript:void(0);"></a>
	<a style="display: none;" id="lnk_cai_rep" targetDiv="menu6" wact="<%=EncryptionUtil.encrypt("caisse-web.caisseWeb.initHistorique")%>" params="isrp=1" href="javascript:void(0);"></a>

	<a href="javascript:" id="targ_link" targetDiv="left-div"></a>
	<a href="javascript:" id="targ_link_pop" targetdiv="generic_modal_body" data-backdrop="static" data-keyboard="false" data-toggle="modal" data-target="#generic_modal"></a>
	
	<input type="hidden" id="is_confirm_mngr">
	
	<a href="#" class="navbar-brand">
	   <img alt="CaisseManager" src="resources/<%=StrimUtil.getGlobalConfigPropertie("context.soft") %>/img/logo_caisse_red.png?v=<%=StrimUtil.getGlobalConfigPropertie("version.resources")%>" style="
	   		height: 31px;
		    margin-top: -6px;
		    position: absolute;
		    left: 2px;
		    z-index: 9999999;" />
	</a>			
		<%
            IMouvementService mouvementService = (IMouvementService)ServiceUtil.getBusinessBean(IMouvementService.class);
            Map<String, byte[]> imagep = mouvementService.getDataImage(ContextAppli.getEtablissementBean().getId(), "restau");
            if(imagep.size() > 0){ %>
				<img src="data:image/jpeg;base64,<%=FileUtil.getByte64(imagep.entrySet().iterator().next().getValue())%>" alt="Caisse manager" style="height: 34px;
					z-index: 999;
				    position: absolute;
				    left: 91px;top:11px;" />                        
        <% } %>	

<div id="decon-modal-div" style="display:none;">
   <div class="row" style="text-align: center;min-height: 98px;margin-top: 44px;">
   	<std:link action="commun.login.disconnect" onClick="clearInterval(intervalTime);" targetDiv="right-div" style="height: 63px;margin-right: 39px;font-size: 20px;padding-top: 17px;" classStyle="btn btn-lg btn-danger shiny">
   		<i class="fa fa-3x fa-power-off"> </i> SE DECONNECTER
   	</std:link>
   	<%if(!isCaisseVerouille && isCaisseNotFermee){ %>
   		<std:link action="caisse-web.caisseWeb.verrouillerCaisse" onClick="clearInterval(intervalTime);" targetDiv="right-div" style="height: 63px;font-size: 20px;padding-top: 17px;" classStyle="btn btn-lg btn-warning shiny">
    		<i class="fa fa-3x fa fa-lock"> </i> VERROUILLER
    	</std:link>
    <%} %>
    </div>
</div>


	<%if(imgBack != null){ %>
		<div id="lock_caisse_div" style="opacity:0.9;z-index:10000;position: absolute;left: 0;top: 0;width: 100%;height: 100%;background-color: #ccc;display: none;">
			<div style="position: absolute;right: 13px;top: 90%;width:100%;text-align: right;">
				<img alt="Caisse manager" src="resources/<%=StrimUtil.getGlobalConfigPropertie("context.soft") %>/img/logo_caisse_red.png?v=1.66" style="height: 40px;"/>
			</div>
			<div style="position: absolute;height: 100%;width: 100%;text-align: center;">
	              <img src="<%=imgBack%>" alt="Caisse manager" style="width: 350px;margin-top:20%" />                     
	       </div>
		</div>
	<%} %>