<%@page import="framework.controller.ControllerUtil"%>
<%@page import="framework.model.common.util.EncryptionUtil"%>
<%@ taglib uri="http://www.customtaglib.com/complexe" prefix="cplx"%>
<%@ taglib uri="http://www.customtaglib.com/standard" prefix="std"%>
<%@ taglib uri="http://www.customtaglib.com/html" prefix="html"%>
<%@ taglib uri="http://www.customtaglib.com/work" prefix="work"%>
<%@ taglib uri="http://www.customtaglib.com/fmt" prefix="fmt"%>
<%@ taglib uri="http://www.customtaglib.com/c" prefix="c"%>
<%@ taglib uri="http://www.customtaglib.com/c" prefix="fn"%>
<%@page errorPage="/commun/error.jsp" %>

<script type="text/javascript">
	$(document).ready(function (){
		$(document).off('click', "a[id^='lnk_det']");
		$(document).on('click', "a[id^='lnk_det']", function(){
			// Remttre les plus/minus
			$("a[id='lnk_det']").each(function(){
				$(this).find("span").attr("class", "fa fa-plus");
			});
			var idx = $(this).attr("curr");
			if($("#tr_det_"+idx).css("display") == "none"){
				$("a[curr='"+idx+"']").find("span").attr("class", "fa fa-minus");
			} else{
				$("a[curr='"+idx+"']").find("span").attr("class", "fa fa-plus");
			}
			$("tr[id^='tr_det_']").each(function(){
				if($(this).attr("id") != "tr_det_"+idx){
					$(this).hide();
				}
			});
			$("#tr_det_"+$(this).attr("curr")).toggle(1000);
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
         <li>Gestion des cartes</li>
         <li class="active">Historique des consommations</li>
     </ul>
 </div>
  <!-- /Page Header -->
<div class="page-header position-relative">
	<div class="header-title" style="padding-top: 4px;">
	<%
	boolean isPortefeuilleMnu = ("POINT".equals(ControllerUtil.getUserAttribute("MNU_FIDELITE", request)) && ControllerUtil.getMenuAttribute("IS_MENU_CMLIENT", request) == null); 
	String act = (isPortefeuilleMnu ? "fidelite.carteFideliteClient.work_find" : "pers.client.work_find");
	%>
	
		<std:link classStyle="btn btn-default" action="<%=act %>" params="bck=1" icon="fa fa-3x fa-mail-reply-all" tooltip="Retour &agrave; la recherche" />
	</div>
	<jsp:include page="/WEB-INF/fragment/shortcut.jsp"></jsp:include>
</div>	
<!-- Page Body -->
<div class="page-body">
	<div class="row">
		<jsp:include page="/WEB-INF/commun/center/banner_message.jsp"></jsp:include>
	</div>
	<div class="row">
        <div class="col-lg-12 col-sm-12 col-xs-12">
	        <div class="row">
				<div class="col-lg-12 col-sm-12 col-xs-12">
					<% request.setAttribute("curMnu", "pointC"); %>
					<jsp:include page="/domaine/personnel/client_header_tab.jsp" />
				</div>
			</div>
				
			<div class="tab-content">  
				<std:form name="search-form">
					<!-- Liste des articles -->
					<cplx:table name="list_points_carte_conso" transitionType="simple" width="100%" title="List des points" initAction="fidelite.carteFideliteClient.findPointsConso" autoHeight="true" checkable="false">
						<cplx:header>
							<cplx:th type="date" value="Date consommation" field="carteFideliteConso.date_conso"/>
							<cplx:th type="decimal" value="Montant consomm&eacute;" field="carteFideliteConso.mtt_conso" width="120"/>
						</cplx:header>
						<cplx:body>
							<c:forEach items="${list_points_carte}" var="carteFideliteConso">
								<cplx:tr workId="${carteFideliteConso.id }">
									<cplx:td value="${carteFideliteConso.date_conso}"></cplx:td>
									<cplx:td align="right" style="font-weight:bold;" value="${carteFideliteConso.mtt_conso}"></cplx:td>
								</cplx:tr>	
							</c:forEach>
						</cplx:body>
					</cplx:table>
			 </std:form>			
			 </div>
		</div>
	</div>
</div>
