<%@page import="framework.controller.ContextGloabalAppli"%>
<%@page import="framework.model.common.util.BooleanUtil"%>
<%@page import="framework.model.beanContext.AbonnementBean"%>
<%@page import="appli.model.domaine.vente.persistant.JourneePersistant"%>
<%@page import="appli.controller.domaine.util_erp.ContextAppli"%>
<%@ taglib uri="http://www.customtaglib.com/complexe" prefix="cplx"%>
<%@ taglib uri="http://www.customtaglib.com/standard" prefix="std"%>
<%@ taglib uri="http://www.customtaglib.com/html" prefix="html"%>
<%@ taglib uri="http://www.customtaglib.com/work" prefix="work"%>
<%@ taglib uri="http://www.customtaglib.com/c" prefix="c"%>
<%@page errorPage="/commun/error.jsp" %>

<%
	JourneePersistant lastJournee = (JourneePersistant)request.getAttribute("lastJournee");
%>
 <!-- Page Breadcrumb -->
 <div class="page-breadcrumbs breadcrumbs-fixed">
     <ul class="breadcrumb">
         <li>
             <i class="fa fa-home"></i>
             <a href="#">Accueil</a>
         </li>
         <li>Gestion de caisse</li>
         <li class="active">Recherche</li>
     </ul>
 </div>
<!-- /Page Breadcrumb --> 
  <!-- Page Header -->
  <div class="page-header position-relative">
      <div class="header-title" style="padding-top: 4px;">
          <std:link actionGroup="C" classStyle="btn btn-default" action="caisse.caisse.work_init_create" icon="fa-3x fa-plus" tooltip="Cr&eacute;er" />
          
          <c:choose>
		      <c:when test="${lastJournee != null && lastJournee.getStatut_journee()=='C' }">
		          <std:linkPopup actionGroup="C" classStyle="btn btn-success" action="caisse.journee.work_init_create" value="Ouvrir la journ&eacute;e" tooltip="Ouvrir la joun&eacute;e"/>
		      </c:when>
		      <c:when test="${lastJournee != null && lastJournee.getStatut_journee()=='O' }">
		      	  <std:linkPopup actionGroup="C" classStyle="btn btn-warning" action="caisse.journee.init_cloture" workId="<%=lastJournee.getId().toString()%>" value="Cl&ocirc;turer la journ&eacute;e" tooltip="Cl&ocirc;turer la journ&eacute;e"/>
		      </c:when>
	      </c:choose>
	      |
	      <std:linkPopup noJsValidate="true" actionGroup="C" style='margin-left: 10px;' action="admin.dataForm.work_find" icon="fa fa-cogs" tooltip="Données formulaire" value="Formulaire" params="tp=CAISSE" />
          <std:link actionGroup="C" classStyle="btn btn-primary" action="admin.parametrage.work_edit" params="tp=cais" icon="fa-3x fa-cogs" value="Paramétres" tooltip="Paramétres" />
	      
      </div>
      <!--Header Buttons-->
      <jsp:include page="/WEB-INF/fragment/shortcut.jsp"></jsp:include> 
	  <!--Header Buttons End-->
  <!-- /Page Header -->

<!-- Page Body -->
<div class="page-body">
	<div class="row">
		<jsp:include page="/WEB-INF/commun/center/banner_message.jsp"></jsp:include> 
	</div>

<c:set var="contextRestau" value="<%=new ContextAppli()%>" />
<%
AbonnementBean abonnementBean = ContextGloabalAppli.getAbonementBean();
boolean isStock = BooleanUtil.isTrue(abonnementBean.isOptStock());
%>
	<!-- row -->
	<div class="row">
<std:form name="search-form">
	<!-- Liste des caisses -->
	<cplx:table name="list_caisse" showDataState="true" transitionType="simple" width="100%" titleKey="caisse.list" initAction="caisse.caisse.work_find" checkable="false" autoHeight="true" paginate="false">
		<cplx:header>
			<cplx:th type="empty" />
			<cplx:th type="string" valueKey="caisse.adresse_mac" field="caisse.adresse_mac" width="200"/>
			<cplx:th type="string" valueKey="caisse.reference" field="caisse.reference" width="160"/>
			<cplx:th type="string" valueKey="caisse.marque" field="caisse.marque"/>
			<cplx:th type="string" value="Imprimante" field="caisse.imprimantes"/>
			<%if(isStock){ %>
				<cplx:th type="long[]" value="Stock cible" field="caisse.opc_stock_cible.id" groupValues="${listeEmplacement }" groupKey="id" groupLabel="titre"/>
			<%} %>
			<cplx:th type="string" valueKey="caisse.type_ecran" field="caisse.type_ecran" width="100" filterOnly="true" groupValues="${listType }"/>
			<cplx:th type="string" valueKey="caisseJournee.opc_statut_journee_enum" width="120" sortable="false" filtrable="false" />
			<cplx:th type="empty" width="200"/>
			<cplx:th type="empty"/>
		</cplx:header>
		<cplx:body>
			<c:set var="oldType" value="${null }" />
			<c:forEach items="${list_caisse }" var="caisse">
				<c:if test="${oldType != caisse.type_ecran }">
					<tr>
						<td colspan='<%=isStock?"10":"8" %>' noresize="true" style="font-size: 13px;font-weight: bold;background-color:#e3efff;">
							<span>${ contextRestau.getLibelleCaisse(caisse.type_ecran).toUpperCase() }</span>
						</td>
					</tr>	
				</c:if>
			
				<c:set var="oldType" value="${caisse.type_ecran }" />
				<cplx:tr workId="${caisse.id }">
					<cplx:td>
						<work:edit-link/>
					</cplx:td>
					<cplx:td value="${caisse.adresse_mac}" style="${caisse.is_desactive ? 'text-decoration: line-through;color: #FFC107;':'' };">
						<c:if test="${caisse.type_ecran=='AFFICHEUR'}">
							[<span style="color: red;font-size: 10px;">Afficheur : ${caisse.opc_caisse.reference }</span>]
						</c:if>
						<c:if test="${caisse.is_livraison }">
                    		<i class="fa fa-motorcycle" style="color: blue;" title="Caisse sp&eacute;ciale livraison"></i>
                    	</c:if>
					</cplx:td>
                    <cplx:td value="${caisse.reference}"></cplx:td>
					<cplx:td value="${caisse.marque}"></cplx:td>
					<cplx:td style="text-transform: uppercase;">
						<c:if test="${not empty caisse.imprimantes }">
							<i class="fa fa-print" style="color: blue;"></i> ${caisse.imprimantes}
						</c:if>
					</cplx:td>
					<%if(isStock){ %>
						<cplx:td style="text-transform: uppercase;">
							<c:if test="${not empty caisse.opc_stock_cible.titre }">
								<i class="fa fa-inbox" style="color:#882f28;"></i> ${caisse.opc_stock_cible.titre}
							</c:if>
						</cplx:td>
					<%} %>	
					<cplx:td align="center" style="text-transform: uppercase;">
						<c:if test="${caisse.type_ecran=='CAISSE' or caisse.type_ecran=='CAISSE_CLIENT' }">
							<c:set var="statut" value="${caisse.getStatutCaisse() }"></c:set>
							<c:choose>
								<c:when test="${caisse.is_desactive }">
									<span class="label" style="color:orange;font-weight: bold;">D&eacute;sactiv&eacute;e</span>
								</c:when>
								<c:when test="${statut == 'O' }">
									<span class="label" style="color:green;font-weight: bold;">Ouverte</span>
								</c:when>
								<c:when test="${statut == 'E' }">
									<span class="label" style="color:orange;font-weight: bold;">En cours de cl&ocirc;ture</span>
								</c:when>
								<c:when test="${statut == 'C' }">
									<span class="label" style="color:red;font-weight: bold;">Cl&ocirc;tur&eacute;e</span>
								</c:when>
								<c:otherwise>
									<span class="label" style="color:gray;font-weight: bold;">Non utilis&eacute;e</span>
								</c:otherwise>
							</c:choose>
						</c:if>
					</cplx:td>
					<cplx:td align="left">
						<std:link actionGroup="U" classStyle="btn btn-${caisse.is_desactive?'success':'warning' } btn-sm" action="caisse.caisse.activer_caisse" workId="${caisse.id }" value="${caisse.is_desactive?'Activer':'D&eacute;sactiver' }" />						
						<c:choose>
							<c:when test="${statut == 'O' and (caisse.type_ecran=='CAISSE' or caisse.type_ecran=='CAISSE_CLIENT')}">
								<std:linkPopup actionGroup="M" classStyle="btn btn-danger btn-sm" action="caisse.caisse.init_cloturer_definitive" workId="${caisse.id }" value="Cl&ocirc;turer" /> 
							</c:when>
							<c:when test="${statut == 'C' }">
								<c:if test="${lastJournee != null && lastJournee.getStatut_journee()=='O' && (caisse.type_ecran=='CAISSE' or caisse.type_ecran=='CAISSE_CLIENT')}">
									<std:linkPopup actionGroup="M" classStyle="btn btn-success btn-sm" action="caisse.caisse.init_ouverture_caisse" workId="${caisse.id }" value="Ouvrir" />
								</c:if>	
							</c:when>
							<c:otherwise>
								<c:if test="${lastJournee != null && lastJournee.getStatut_journee()=='O' && (caisse.type_ecran=='CAISSE' or caisse.type_ecran=='CAISSE_CLIENT') }">
									<std:linkPopup actionGroup="U" classStyle="btn btn-success btn-sm" action="caisse.caisse.init_ouverture_caisse" workId="${caisse.id }" value="Ouvrir" />
								</c:if>
							</c:otherwise>	
						</c:choose>
					</cplx:td>
					<cplx:td align="center">
						<work:delete-link />
					</cplx:td>
				</cplx:tr>
			</c:forEach>
		</cplx:body>
	</cplx:table>
 </std:form>			

 </div>
					<!-- end widget content -->

				</div>
				<!-- end widget div -->
