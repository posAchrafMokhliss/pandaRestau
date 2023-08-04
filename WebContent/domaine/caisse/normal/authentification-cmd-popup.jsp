<%@page import="framework.model.common.util.StrimUtil"%>
<%@page import="appli.controller.domaine.util_erp.ContextAppli"%>
<%@page import="framework.model.common.util.StringUtil"%>
<%@page import="framework.model.common.util.EncryptionUtil"%>
<%@ taglib uri="http://www.customtaglib.com/complexe" prefix="cplx"%>
<%@ taglib uri="http://www.customtaglib.com/standard" prefix="std"%>
<%@ taglib uri="http://www.customtaglib.com/html" prefix="html"%>
<%@ taglib uri="http://www.customtaglib.com/work" prefix="work"%>
<%@ taglib uri="http://www.customtaglib.com/c" prefix="c"%>
<%@page errorPage="/commun/error.jsp"%>

<style>
#generic_modal_body{
	width: 700px;
}
</style>

<script type="text/javascript">
	$(document).ready(function() {
		init_keyboard_events();
		
		var barcodeCheck2="";
		$(document).off('keydown').on('keydown', function (e) {
	        var code = (e.keyCode ? e.keyCode : e.which);
	        var sourceEvent = $(e.target).prop('nodeName');
	        var isInput = (sourceEvent == 'INPUT') ? true : false;
	        //
	        if(!isInput && code==13 && $.trim(barcodeCheck2) != ''){
	        	barcodeCheck2 = barcodeCheck2.substring(barcodeCheck2.length-10);
	        	if(barcodeCheck2.length==10){
	        		submitAjaxForm('<%=EncryptionUtil.encrypt("caisse-web.caisseWeb.confirmAnnuleCmd")%>', 'tkn='+barcodeCheck2+'&'+$("#confirmAnnulCmdBtnPop").attr("params"), $("#data-form"), $("#trg-lnk"));
	        		return false;
	        	}
	        	barcodeCheck2="";
	        } else{
	        	barcodeCheck2 = barcodeCheck2 + String.fromCharCode(code);
	        }
	    });
	    $("#checkFoc2").focus();
	    
	    $("#cmd\\.user\\.id, #cmd\\.password").keypress(function(e){
			if(e.which == 13) {
				$("#confirmAnnulCmdBtnPop").trigger("click");
				e.stopPropagation();
				return false;
		    }
		});
	});
</script>
	
<%
		String typeAct = (String)request.getAttribute("typeAct");
	boolean isDelockCaisse = "delock".equals(typeAct);
	%>	

<a href="javascript:void(0)" targetDiv="${tp=='histo' ? 'right-div':'left-div'}" onComplete="$('#close_modal').trigger('click');" id="trg-lnk" style="display: none;"></a>

<std:form name="data-form">
	<input type="hidden" name="checkFoc2" id="checkFoc2">
	<!-- widget grid -->
	<div class="widget">
		<div class="widget-header bordered-bottom bordered-blue">
			<span class="widget-caption">Authentification mot de passe OU badge</span>
			<img src="resources/framework/img/badge_scanner.png" style="width: 20px;margin-top: 8px;margin-right: 5px;" title="Lecteur badge utilisable sur cet écran">
		</div>
		<div class="widget-body">
			<div class="row">
				<jsp:include page="/WEB-INF/commun/center/banner_message.jsp"></jsp:include>
			</div>
			<div class="row">
				<div class="form-group">
					<div class="col-md-6">
						<std:label classStyle="control-label col-md-3" value="Login" style="font-weight:bold;font-size: 19px;"/>&nbsp;
						<div class="col-md-9" style="margin-top: -15px;">
							<std:select name="cmd.user.id" type="long" style="width:100%;font-size: 25px;" required="true" data="${listUser }" key="id" labels="login" />
						</div>
					</div>
					<div class="col-md-6">
						<std:label classStyle="control-label col-md-5" value="Mot de passe" style="font-weight:bold;font-size: 19px;"/>&nbsp;
						<div class="col-md-7">
							<std:password name="cmd.password" placeholder="Mot de passe" type="string" style="width:140px;font-size: 18px;margin-top: -15px;" required="true" maxlength="80" />
						</div>
					</div>
				</div>
			</div>
			<div class="widget-buttons buttons-bordered" style="margin-bottom: 10px;">
         		<i class="fa fa-keyboard-o" style="font-size: 20px;"></i>         
         		<label>
	                 <input class="checkbox-slider toggle colored-blue" type="checkbox" id="keyboard-activator" style="display: none;">
	                 <span class="text"></span>
	             </label>
         	</div>
         	<br>
			<div class="row">
				<div class="col-md-12" style="margin-left:10px;margin-top: -14px;margin-bottom: 11px;">
					<span style="font-size:11px;color:orange;">
				<%
					if(isDelockCaisse){
				%>
					* Cette caisse peut &ecirc;tre débloquée par <b><i><%=ContextAppli.getUserBean().getLogin()%></i></b> ou le <b><i>manager</i></b>
				<%} else{ %>
					* Une authentification du <b>manager</b> est nécessaire pour annuler cette commade
				<%} %>
				</span>
				</div>	
			</div>
			<div class="row" style="text-align: center;">
				<div class="col-md-12">
					<%if("delrow".equals(typeAct) || "offrir".equals(typeAct)){ %>
						<std:button actionGroup="M" id="confirmAnnulCmdBtnPop" style="border-radius: 37px;height: 52px;font-size: 21px;" classStyle="btn btn-lg btn-success" action="caisse-web.caisseWeb.confirmAnnuleCmd" targetDiv="left-div" params="tpact=${tp}&${trParams.replace('**', '&') }" closeOnSubmit="true" icon="fa-save" value="S'authentifier" />
					<%} else{ %>
						<c:choose>
							<c:when test="${tp=='histo' }">
								<c:set var="tarDiv" value="right-div" /> 
							</c:when>
							<c:when test="${tp=='cmd' }">
								<c:set var="tarDiv" value="left-div" /> 
							</c:when>
							<c:when test="${tp=='reduce' }">
								<c:set var="tarDiv" value="left-div" /> 
							</c:when>
							<c:otherwise>
								<c:set var="tarDiv" value="" />
							</c:otherwise>
						</c:choose>
						
						<std:button id="confirmAnnulCmdBtnPop" actionGroup="M" style="border-radius: 37px;height: 52px;font-size: 21px;" classStyle="btn btn-lg btn-success" action="caisse-web.caisseWeb.confirmAnnuleCmd" targetDiv="${tarDiv }" params="tp=${tp}&workId=${mvm }&caisse.id=${caisse }" closeOnSubmit="true" workId="${cmdWid }" icon="fa-save" value="S'authentifier" />
					<%} %>
					<button type="button" id="close_modal" style="border-radius: 37px;height: 52px;font-size: 21px;" class="btn btn-lg btn-primary" data-dismiss="modal">
						<i class="fa fa-times"></i> Fermer
					</button>
				</div>
			</div>
		</div>
	</div>
</std:form>