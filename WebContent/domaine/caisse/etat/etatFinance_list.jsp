<%@page import="appli.controller.domaine.administration.bean.EtatFinanceBean"%>
<%@page import="appli.controller.domaine.util_erp.ContextAppli"%>
<%@page import="framework.model.common.util.EncryptionUtil"%>
<%@page import="framework.model.common.util.StringUtil"%>
<%@page import="framework.controller.Context"%>
<%@page import="framework.model.common.util.StrimUtil"%>
<%@ taglib uri="http://www.customtaglib.com/complexe" prefix="cplx"%>
<%@ taglib uri="http://www.customtaglib.com/standard" prefix="std"%>
<%@ taglib uri="http://www.customtaglib.com/html" prefix="html"%>
<%@ taglib uri="http://www.customtaglib.com/work" prefix="work"%>
<%@ taglib uri="http://www.customtaglib.com/fmt" prefix="fmt"%>
<%@ taglib uri="http://www.customtaglib.com/c" prefix="c"%>
<%@page errorPage="/commun/error.jsp" %>

<%
EtatFinanceBean etatFinanceBean = (EtatFinanceBean) request.getAttribute("etatFinanceBean");
String devise = "&nbsp;"+StrimUtil.getGlobalConfigPropertie("devise.symbole");
boolean isBilanShow = Context.isOperationAvailable("BILAN");
%>
<script type="text/javascript">
$(document).ready(function (){
	manageDropMenu("list_etat_finance");
	
	$("#calc_lnk").click(function(){
		$("#calcul_span").show();
	});
	
	$('.input-group.date, #dateDebut').datepicker({
    	clearBtn: true,
	    language: "fr",
	    autoclose: true,
	    format: "mm/yyyy",
	    startView: 1,
	    startDate : '<%=request.getAttribute("minDate")%>m',
	    endDate : '<%=request.getAttribute("maxDate")%>m',
	    minViewMode: 1
    });
	$('.input-group.date').datepicker().on("changeDate", function(e) {
        var currDate = $('#dateDebut').datepicker('getFormattedDate');
        submitAjaxForm('<%=EncryptionUtil.encrypt("caisse.etatFinance.loadFinanceDetail")%>', 'dateDebut='+currDate, $("#search-form"), $(this));
    });

});
</script>

 <!-- Page Breadcrumb -->
 <div class="page-breadcrumbs breadcrumbs-fixed">
     <ul class="breadcrumb">
         <li>
             <i class="fa fa-home"></i>
             <a href="#">Accueil</a>
         </li>
         <li>Etat financier</li>
         <li class="active">Recherche</li>
     </ul>
 </div>
<!-- /Page Breadcrumb -->
  <!-- Page Header -->
  <div class="page-header position-relative">
      <div class="header-title" style="padding-top: 4px;">
      	<%if(isBilanShow && (etatFinanceBean==null || etatFinanceBean.getId() == null)){ %>
			<std:link actionGroup="X" id="calc_lnk" classStyle="btn btn-success shiny" action="caisse.etatFinance.loadFinanceDetail" icon="fa-3x fa-lock" value="Calculs du mois en cours" tooltip="Calculs pour le mois en cours" />
			<span id="calcul_span" style="color: #F44336;font-style: italic;display:none;">
				<img src='resources/framework/img/select2-spinner.gif' /> Veuillez patienter ... 
			</span>
		<%} %>
      </div>
      
      
	  	<div class="input-group date" style="width: 260px;position: absolute;right: 90px;top: 0px;">
	  		<span style="font-size: 16px;float: left;margin-top: 8px;margin-right: 5px;">Mois du bilan </span>
	  		
	  		<input type="text" class="form-control" name="dateDebut" id="dateDebut" style="font-size: 16px;color:green !important;font-weight: bold;border: 0px;width: 90px;" value="<%=StringUtil.getValueOrEmpty(request.getAttribute("dateDebut"))%>">
	  			<span class="input-group-addon" style="border: 1px solid #f3f3f3;padding-top: 9px;">
	  				<i class="fa fa-calendar" style="font-size: 22px;color: #9C27B0;"></i>
	  			</span>
		</div>
	
      <!--Header Buttons-->
      <jsp:include page="/WEB-INF/fragment/shortcut.jsp"></jsp:include>
      <!--Header Buttons End-->
  </div>
  <!-- /Page Header -->

<!-- Page Body -->
<div class="page-body">
	<div class="row">
		<jsp:include page="/WEB-INF/commun/center/banner_message.jsp"></jsp:include>
	</div>
<div class="row">
        <div class="col-lg-12 col-sm-12 col-xs-12">
              <div class="tabbable">

<!-- row -->
<std:form name="search-form">
	<!-- Liste des caisses -->
	<cplx:table name="list_etat_finance" transitionType="simple" width="100%" title="Etats mensuels clos" initAction="caisse.etatFinance.work_find" checkable="false" autoHeight="true">
		<cplx:header>
		<%if(isBilanShow){ %>
			<cplx:th type="empty" />
		<%} %>	
			<cplx:th type="date" value="Date &eacute;tat" field="etatFinance.date_etat"/>
			
			<%if(isBilanShow){ %>
			<cplx:th type="decimal" value="Recette" width="120" />
			<cplx:th type="decimal" value="D&eacute;penses" width="120" />
			<cplx:th type="decimal" value="R&eacute;sultat" width="120" />
			<cplx:th type="decimal" value="Etat stock" width="120" />
			<cplx:th type="decimal" value="Etat ch&egrave;que" field="etatFinance.mtt_resultat_net" width="120" />
			<cplx:th type="decimal" value="Etat banque" field="etatFinance.mtt_resultat_net" width="120" />
			<%} %>
			<cplx:th type="boolean" value="Purg&eacute;" field="etatFinance.is_purge" width="70" />
			<cplx:th type="empty" width="200"/>
		</cplx:header>
		<cplx:body>
			<c:forEach items="${list_etat_finance }" var="etatFinance">
				<cplx:tr workId="${etatFinance.id }">
					<%if(isBilanShow){ %>
					<cplx:td>
						<std:link actionGroup="X" classStyle="btn btn-sm btn-primary shiny" action="caisse.etatFinance.loadFinanceDetail" workId="${etatFinance.id}" icon="fa-3x fa-eye" />
					</cplx:td>
					<%} %>
					<cplx:td value="${etatFinance.date_etat }" />
					<%if(isBilanShow){ %>
						<c:set var="resultat" value="${etatFinance.calculTotalRecetteDepense() }" />
						<c:set var="etats" value="${etatFinance.calculTotalEtat() }" />
					
						<cplx:td align="right" value="${resultat[0] }" />
						<cplx:td align="right" value="${resultat[1]}" />
						<cplx:td align="right" style="font-weight:bold;" value="${resultat[2]}" />
						<cplx:td align="right" value="${etats[0] }" />
						<cplx:td align="right" value="${etats[1] }" />
						<cplx:td align="right" value="${etats[2] }" />
					<%} %>
					<cplx:td value="${etatFinance.is_purge }" align="center" />
					
					<cplx:td align="center">
						<std:link actionGroup="U" classStyle="btn btn-sm btn-warning shiny" action="caisse.etatFinance.annuler_clore_mois" workId="${etatFinance.id}" icon="fa-3x fa-lock" tooltip="Annuler la cl&ocirc;ture" />
						
<!-- 						<div class="btn-group"> -->
<%-- <%-- 				             <std:link actionGroup="X" classStyle="btn btn-sm btn-sky shiny" action="caisse.razPrint.imprimer_raz" params="isFromEtat=1&mode=MO" workId="${etatFinance.id}" icon="fa-3x fa-print" value="RAZ mensuelle" tooltip="Imprimer RAZ mensuelle" /> --%> --%>
<!-- 				             <a class="btn btn-sm btn-sky dropdown-toggle shiny" data-toggle="dropdown" href="javascript:void(0);" aria-expanded="true"><i class="fa fa-angle-down"></i></a> -->
<!-- 				             <ul class="dropdown-menu dropdown-primary"> -->
<!-- 				                 <li> -->
<%-- <%-- 				                     <std:link actionGroup="X" style="text-align: left;" classStyle="btn btn-sm btn-sky shiny" params="isFromEtat=1&mode=JRN" workId="${etatFinance.id}" action="caisse.razPrint.imprimer_raz" icon="fa-3x fa-print" value="RAZ jours" tooltip="Imprimer RAZ jours" /> --%> --%>
<!-- 				                 </li> -->
<!-- 				                 <li> -->
<%-- <%-- 				                     <std:link actionGroup="X" style="margin-top: 6px;text-align:left;" classStyle="btn btn-sm btn-sky shiny" params="isFromEtat=1&mode=BS" workId="${etatFinance.id}" action="caisse.razPrint.imprimer_raz" icon="fa-3x fa-print" value="RAZ boissons" tooltip="Imprimer la RAZ des boissons" /> --%> --%>
<!-- 				                 </li> -->
<!-- 				             </ul> -->
<!-- 				         </div> -->
					</cplx:td>
				</cplx:tr>
			</c:forEach>
			<%if(isBilanShow){ %>
		    <c:if test="${!list_etat_finance.isEmpty()}">
				<tr class="sub">
					<td colspan="2"></td>
					<td align="right">
						<span style="font-size: 14px !important;font-weight: bold;height: 28px;" class="badge badge-blue">
							<fmt:formatDecimal value="${etat_finance_total.mtt_recette_divers }"/>
						</span>
					</td>
					<td align="right">
						<span style="font-size: 14px !important;font-weight: bold;height: 28px;" class="badge badge-blue">
							<fmt:formatDecimal value="${etat_finance_total.mtt_depense_divers }"/>
						</span>
					</td>
					<td align="right">
						<span style="font-size: 14px !important;font-weight: bold;height: 28px;" class="badge badge-blue">
							<fmt:formatDecimal value="${etat_finance_total.mtt_resultat_net }"/>
						</span>
					</td>
					<td align="right">
						<span style="font-size: 14px !important;font-weight: bold;height: 28px;" class="badge badge-blue">
							<fmt:formatDecimal value="${etat_finance_total.mtt_achat }"/>
						</span>
					</td>
					<td align="right">
						<span style="font-size: 14px !important;font-weight: bold;height: 28px;" class="badge badge-blue">
							<fmt:formatDecimal value="${etat_finance_total.mtt_avoir }"/>
						</span>
					</td>
					<td align="right">
						<span style="font-size: 14px !important;font-weight: bold;height: 28px;" class="badge badge-blue">
							<fmt:formatDecimal value="${etat_finance_total.mtt_salaire }"/>
						</span>
					</td>
					<td></td>
				</tr>
			</c:if>	
			<%} %>
		</cplx:body>
		
<script type="text/javascript">
/*Handles ToolTips*/
$("[data-toggle=tooltip]")
    .tooltip({
        html: true
    });
</script> 
	</cplx:table>
 </std:form>			
</div>
 			</div>
		</div>
	</div>