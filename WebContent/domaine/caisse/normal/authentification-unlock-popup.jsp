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
div#lockscreen {
width: 1000%;
    height: 1000%;
    position: fixed;
    top: -1000px;
    left: -999px;
    z-index: 0;
    background-color: #c8c8c845;
}
</style>

<script type="text/javascript">
	$(document).ready(function() {
		init_keyboard_events();
	
		$("#unlock\\.login, #unlock\\.password").keypress(function(e){
			if(e.which == 13) {
				e.preventDefault();
				$("#unlock_cmd_lnk").trigger('click');
				return false;
		    }
		});
		
		var barcodeLock="";
		
		$(document).off('keydown').on('keydown', function (e) {
	        var code = (e.keyCode ? e.keyCode : e.which);
	        var sourceEvent = $(e.target).prop('nodeName');
	        var isInput = (sourceEvent == 'INPUT') ? true : false;
	        
	        //
	        if(!isInput && code==13 && $.trim(barcodeLock) != ''){
	        	barcodeLock = barcodeLock.substring(barcodeLock.length-10);
	        	
	        	if(barcodeLock.length==10){
	        		submitAjaxForm('<%=EncryptionUtil.encrypt("caisse-web.caisseWeb.quitterLockMode")%>', 'unlock.tkn='+barcodeLock, $("#data-form"), $("#trg-lnk"));
	        		return false;
	        	}
	        	barcodeLock="";
	        } else{
	        	barcodeLock = barcodeLock + String.fromCharCode(code);
	        }
	    });
	    $("#checkFoc").focus();
	});
</script>
	
<a href="javascript:void(0)" targetDiv="${tp=='histo' ? 'right-div':'left-div'}" id="trg-lnk" style="display: none;"></a>

<div id="lockscreen"></div>

<std:form name="data-form">
	<input type="hidden" name="checkFoc" id="checkFoc">
	<!-- widget grid -->
	<div class="widget">
		<div class="widget-header bordered-bottom bordered-blue">
			<span class="widget-caption">Authentification mot de passe OU badge</span>
			
			<div class="widget-buttons buttons-bordered" style="margin-bottom: 10px;">
         		<i class="fa fa-keyboard-o" style="font-size: 20px;"></i>         
         		<label>
	                 <input class="checkbox-slider toggle colored-blue" type="checkbox" id="keyboard-activator" style="display: none;">
	                 <span class="text"></span>
	             </label>
         	</div>
			
			
			<img src="resources/framework/img/badge_scanner.png" style="width: 20px;margin-top: 8px;margin-right: 5px;" title="Lecteur badge utilisable sur cet écran">
		</div>
		<div class="widget-body">
			<div class="row">
				<jsp:include page="/WEB-INF/commun/center/banner_message.jsp"></jsp:include>
			</div>
			<div class="row" style="margin-left: 0px;margin-right: 0px;">
				<div class="form-group">
					<div class="col-md-12">
						<std:label classStyle="control-label col-md-4" value="Login" style="font-weight:bold;font-size: 19px;"/>&nbsp;
						<div class="col-md-8" style="margin-top: -15px;">
							<std:select name="unlock.login" type="long" style="width:100%;font-size: 25px;" required="true" data="${listUser }" key="id" labels="login" />
						</div>
					</div>
				</div>
				<div class="row" style="margin-left: 0px;margin-right: 0px;">	
					<div class="col-md-12">
						<std:label classStyle="control-label col-md-4" value="Mot de passe" style="font-weight:bold;font-size: 19px;"/>&nbsp;
						<div class="col-md-8">
							<std:password name="unlock.password" placeholder="Mot de passe" type="string" style="width:140px;font-size: 18px;margin-top: -15px;" required="true" maxlength="80" />
						</div>
					</div>
				</div>
			</div>
			
         	<br>
			<div class="row" style="text-align: center;">
				<div class="col-md-12">
					<std:button id="unlock_cmd_lnk" actionGroup="M" style="border-radius: 37px;height: 52px;font-size: 21px;" closeOnSubmit="true" targetDiv="left-div" classStyle="btn btn-lg btn-success" action="caisse-web.caisseWeb.quitterLockMode" icon="fa-save" value="S'authentifier" />
					<button style="display: none;" type="button" id="close_modal_unlock" class="btn btn-primary" data-dismiss="modal">
						</button>
				</div>
			</div>
		</div>
	</div>
</std:form>
