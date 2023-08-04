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
         <li class="active">Historique des gains</li>
     </ul>
 </div>
<!-- /Page Breadcrumb -->
  <!-- /Page Header -->
<div class="page-header position-relative">
	<div class="header-title" style="padding-top: 4px;">
		<std:linkPopup actionGroup="C" classStyle="btn btn-success" action="fidelite.carteFideliteClient.initOffre" workId="${carteFideliteClient.id }" icon="fa-3x fa fa-gift" value="Offrir des points" tooltip="Offrir des points" />
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
					<% request.setAttribute("curMnu", "pointG"); %>
					<jsp:include page="/domaine/personnel/client_header_tab.jsp" />
				</div>
			</div>
		<div class="tab-content">  
		<std:form name="search-form">
	<!-- Liste des articles -->
	<cplx:table name="list_points_carte" transitionType="simple" width="100%" title="List des points" initAction="fidelite.carteFideliteClient.findPointsGain" autoHeight="true" checkable="false">
		<cplx:header>
			<cplx:th type="date" value="Date gain" field="carteFidelitePoints.date_gain" width="120"/>
			<cplx:th type="string" value="Source du gain" field="carteFidelitePoints.source" />
			<cplx:th type="decimal" value="Montant gagn&eacute;" field="carteFidelitePoints.mtt_gain" width="120"/>	
			<cplx:th type="empty"/>
		</cplx:header>
		<cplx:body>
			<c:forEach items="${list_client_carte}" var="carteFidelitePoints">
				<cplx:tr workId="${carteFidelitePoints.id }">
					<cplx:td align="center" value="${carteFidelitePoints.date_gain}"></cplx:td>
					<cplx:td style="color:green;">
						<c:choose>
							<c:when test="${carteFidelitePoints.source=='CMD'}">Commande</c:when>
							<c:when test="${carteFidelitePoints.source=='CDE'}">Offert</c:when>
						</c:choose>
					</cplx:td>
					<cplx:td align="right" style="font-weight:bold;" value="${carteFidelitePoints.mtt_gain}"></cplx:td>
					<cplx:td>
						<std:link actionGroup="D" forceShow="true" action="fidelite.carteFideliteClient.deletePoints" workId="${carteFidelitePoints.id }" params="tp=gain" classStyle="btn btn-sm btn-danger" icon="fa fa-trash-o"/>
					</cplx:td>
				</cplx:tr>	
			</c:forEach>
		</cplx:body>
	</cplx:table>
 </std:form>			
	</div>
	</div>
	</div>
	</div>
