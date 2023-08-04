<%@page import="appli.model.domaine.vente.persistant.CaisseMouvementPersistant"%>
<%@page import="framework.controller.ControllerUtil"%>
<%@page import="framework.model.common.util.EncryptionUtil"%>
<%@page import="framework.model.common.util.BigDecimalUtil"%>
<%@page import="java.math.BigDecimal"%>
<%@ taglib uri="http://www.customtaglib.com/standard" prefix="std"%>
<%@ taglib uri="http://www.customtaglib.com/html" prefix="html"%>
<%@ taglib uri="http://www.customtaglib.com/work" prefix="work"%>
<%@ taglib uri="http://www.customtaglib.com/fmt" prefix="fmt"%>
<%@ taglib uri="http://www.customtaglib.com/c" prefix="c"%>
<%@page errorPage="/commun/error.jsp"%>

<style>
.type-btn-paie{
    width: 137px;
    height: 58px;
    padding-top: 13px;
    font-weight: bold;
    margin-bottom: 5px;
    font-size: 20px;
 }
</style>

<%
BigDecimal mttTotal = (BigDecimal)request.getAttribute("mtt_total");
%>

<script type="text/javascript">
	var mttTotalCmd = <%=mttTotal %>;
<%

CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant) ControllerUtil.getUserAttribute("CURRENT_COMMANDE", request);

boolean isPoint = request.getAttribute("IS_POINT") != null;
boolean isPortefeuille = request.getAttribute("IS_PORTEFEUILLE") != null;

%>	
$(document).ready(function(){ 
	$("#div-type a").click(function(){
		$("#div-type a").attr("class", "btn btn-palegreen  shiny type-btn-paie");
		$(this).attr("class", "btn btn-warning shiny type-btn-paie");
		//
		$("#typeCmd").val($(this).attr("v"));
	});
	
	$("#div-type-paie a").click(function(){
		if($(this).attr("class").indexOf("btn-warning")  != -1){
			$(this).attr("class", "btn btn-blue shiny type-btn-paie");
			$("#modePaie").val("");
		} else{
			$("#div-type-paie a").attr("class", "btn btn-blue shiny type-btn-paie");
			$(this).attr("class", "btn btn-warning shiny type-btn-paie");
			$("#modePaie").val($(this).attr("v"));
		}
	});
});
function getValNum(elementId){
	return $("#"+elementId).val() ? parseFloat($("#"+elementId).val()) : 0;
}
function getValNumFormated(val){
	return (val && $.trim(val)!='') ? parseFloat(val).toFixed(2) : "0.00";
}
</script>

	<!-- Page Header -->
		<input type="hidden" id="typeCmd" name="typeCmd">
		<input type="hidden" id="modePaie" name="modePaie">
		
		<div class="row" style="margin-left: 0px;margin-right: 0px;">
			<jsp:include page="/WEB-INF/commun/center/banner_message.jsp"></jsp:include>
		</div>
         <div class="widget-body">
         
         	<div class="row" style="margin-left: 0px;margin-right: 0px;">
         		<!-- Type -->
         		<div style="width: 100%;float: left;" id="div-type">
         			<a href="javascript:void(0);" class="btn btn-palegreen shiny shiny type-btn-paie" v='P' style="width: 40%;" wact='<%=EncryptionUtil.encrypt("caisse-web.caisseWebClient.validerPaiement") %>'>SUR PLACE</a>
         			<a href="javascript:void(0);" class="btn btn-palegreen shiny shiny type-btn-paie" v='E' style="width: 40%;margin-left: 16%;" wact='<%=EncryptionUtil.encrypt("caisse-web.caisseWebClient.validerPaiement") %>'>A EMPORTER</a>
         		</div>
         	</div>
 			<c:if test="${not empty soldeFidelite or not empty soldePortefeuille }">  
				 <div class="row" style="width: 555px; background-color: #ececec;margin-bottom: 20px;margin-left: 0px;">
					<table class="table table-striped table-bordered table-hover" style="width: 100%;">
					  		<tbody>	  
				           	 <c:if test="${not empty soldeFidelite }">    
				                   <tr>
									 <td style="vertical-align: middle;"><h1 style="font-size: 15px;padding-left: 15px;">Solde Carte</h1></td>
									 <td align="right" style="vertical-align: middle;">
									 	<span id="sol-point" style="font-size: 20px;">
									 		<fmt:formatDecimal value="${soldeFidelite }" />
									 	</span>
									 </td>
				                  </tr>
				                  <tr>
									 <td style="vertical-align: middle;"><h1 style="font-size: 15px;padding-left: 15px;">Points Carte</h1></td>
									 <td align="right" style="vertical-align: middle;">
									 	<span id="sol-point" style="font-size: 20px;">
									 		<fmt:formatDecimal value="${pointFidelite }" />
									 	</span>
									 </td>
				                  </tr>
				              </c:if>
				              <c:if test="${not empty soldePortefeuille }">   
				                   <tr>
									 <td style="vertical-align: middle;"><h1 style="font-size: 15px;padding-left: 15px;">Solde Portefeuille</h1></td>
									 <td align="right" style="vertical-align: middle;">
									 	<span id="sol-portef" style="font-size: 20px;">
									 		<fmt:formatDecimal value="${soldePortefeuille }" />
									 	</span>
									 </td>
				                  </tr>
				                </c:if>
					         </tbody>     
						</table>
					</div>
				</c:if>
			<%if(isPoint || isPortefeuille){ %>
					<div class="row" style="margin-left: 0px;margin-right: 0px;">
						<div style="width: 100%;float: left;" id="div-type-paie">
							<%if(isPoint){%> 
								<a href="javascript:void(0);" class="btn btn-blue shiny type-btn-paie" v='F' style="width: 40%;">Utiliser les points</a>
							<%} %>	
							<%if(isPortefeuille){%>
			         			<a href="javascript:void(0);" class="btn btn-blue shiny type-btn-paie" v='PF' style="width: 40%;margin-left: 16%;">Utiliser portefeuille</a>
			         		<%} %>	
						</div>
				   </div>	
			<%} %>		   	    
				   	    
			<hr style="margin-top: 0px;margin-bottom: 15px;">
	<%-- 		<div class="row" style="text-align: center;">
				<div class="col-md-12">
					<button wact='<%=EncryptionUtil.encrypt("caisse-web.caisseWebClient.validerPaiement") %>' style="margin-top: 20px;top: -2px;left: 280px;height: 50px;width: 144px;font-size: 20px;font-weight: bold;border-radius: 47px;" class="btn btn-danger" type="button" targetDiv="main-div">VALIDER<i class="fa fa-long-arrow-right" style="margin-left: 9px;font-size: 22px"></i></button>
					<button wact='<%=EncryptionUtil.encrypt("caisse-web.caisseWebClient.backAuhentification") %>' noVal='true' style="margin-top: 20px;top: -2px;left: -296px;height: 50px;width: 144px;font-size: 20px;font-weight: bold;border-radius: 47px;background-color: black !important;border-color: black !important;" class="btn btn-danger" type="button" targetDiv="paie-div">RETOUR<i class="fa fa-mail-reply-all" style="margin-left: 9px;font-size: 22px"></i></button>
				</div>
			</div> --%>
			
			<button type="button" id="close_modal" class="btn btn-primary" data-dismiss="modal" style="margin-top: 2px;margin-right: 5px;text-align: center;margin-left: 18em;">
				<i class="fa fa-times"></i> Fermer
			</button>
		</div>
