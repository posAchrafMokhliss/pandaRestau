<%@page import="appli.model.domaine.administration.service.IClientService"%>
<%@page import="framework.model.common.util.ServiceUtil"%>
<%@page import="framework.model.common.util.StrimUtil"%>
<%@page import="framework.controller.ContextGloabalAppli"%>
<%@page import="appli.controller.domaine.util_erp.ContextAppli"%>
<%@page import="framework.model.common.util.StringUtil"%>
<%@page import="framework.model.common.util.EncryptionUtil"%>
<%@ taglib uri="http://www.customtaglib.com/complexe" prefix="cplx"%>
<%@ taglib uri="http://www.customtaglib.com/standard" prefix="std"%>
<%@ taglib uri="http://www.customtaglib.com/html" prefix="html"%>
<%@ taglib uri="http://www.customtaglib.com/work" prefix="work"%>
<%@ taglib uri="http://www.customtaglib.com/fmt" prefix="fmt"%>
<%@ taglib uri="http://www.customtaglib.com/c" prefix="c"%>
<%@ taglib uri="http://www.customtaglib.com/c" prefix="fn"%>
<%@page errorPage="/commun/error.jsp" %>

<style>
#list_article_body tr:HOVER {
	cursor: move !important;
}
</style>

<script type="text/javascript">
	$(document).ready(function (){
		$(document).off('click', "a[id^='lnk_det']");
		$(document).on('click', "a[id^='lnk_det']", function(){
			setTrContent($(this).attr("curr"), "<%=EncryptionUtil.encrypt("stock.composant.editTrArticle")%>");
		});
		
		var tarDiv = $("#tabMnuNavContent .tab-pane[class*='active']").find("#body-content");
		manageCodeBarre('<%=EncryptionUtil.encrypt("stock.composant.work_find")%>', 'getTabElement("#body-content").empty().html(html);refreshSpecialComponents()');
		manageDropMenu("list_article");		
	});
</script>

<%
boolean isRestau = ContextAppli.IS_RESTAU_ENV();
%>

 <!-- Page Breadcrumb -->
 <div class="page-breadcrumbs breadcrumbs-fixed">
     <ul class="breadcrumb">
         <li>
             <i class="fa fa-home"></i>
             <a href="#">Accueil</a>
         </li>
         <li>Caisse enregistreuse</li>
         <li>Liste des articles</li>
         <li class="active">Recherche</li>
     </ul>
 </div>
<!-- /Page Breadcrumb -->
<std:form name="search-form">
  <!-- Page Header -->
  <div class="page-header position-relative">
      <div class="header-title" style="padding-top: 4px;">
      
      	   <c:if test="${empty isEditable or isEditable }">
           		<std:link actionGroup="C" style="float:left;" classStyle="btn btn-default" action="stock.article.work_init_create" icon="fa-3x fa-plus" tooltip="Créer" />
           </c:if>
           
           | <std:link actionGroup="C" classStyle="btn btn-success" action="stock.article.print_fiche_article" icon="fa-3x fa-print" tooltip="Imprimer les étiquette des prix" value="Imprimer étiquettes prix" />
          	<std:link actionGroup="C" classStyle="btn btn-magenta" action="stock.article.print_barre_article" icon="fa-3x fa-barcode" tooltip="Imprimer les étiquette code barre" value="Imprimer code barre" />
       		<std:linkPopup actionGroup="C" classStyle="btn btn-info" action="stock.article.init_print_fiche" icon="fa-3x fa-cogs" tooltip="Configurer l'imprimante d'étiquettes" value="Imprimantes" />
      	|
      	
      	 <%if(isRestau){ %>
              	<std:linkPopup actionGroup="C" classStyle="" action="stock.composant.sync_balance" tooltip="Synchroniser la balance">
              		<i style="font-size: 14px;" class="fa fa-exchange"></i> Synchroniser balances
              	</std:linkPopup>
              	|
          <%} %>
      	
		<std:linkPopup noJsValidate="true" actionGroup="C" style='margin-left: 10px;' action="admin.dataForm.work_find" icon="fa fa-cogs" tooltip="Données formulaire" value="Formulaire" params="tp=ARTICLE" />
		<img class="imgBarCode" src="resources/framework/img/barcode_scanner.png" style="width: 20px;position: absolute;top: -28px;right: 13px;" title="Lecteur code barre utilisable sur cet écran">
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

<c:set var="encryptionUtil" value="<%=new EncryptionUtil() %>" />
<input type='hidden' name="list_article_work_order" id="order-h">

	<!-- row -->
	<div class="row">

	<!-- Liste des articles -->
	<cplx:table name="list_article" transitionType="simple" showDataState="true" width="100%" dragable="true" title="Liste des articles" initAction="stock.article.work_find" autoHeight="true" checkable="true">
		<cplx:header>
			<cplx:th type="empty" />
			<cplx:th type="long[]" value="Famille" field='<%=isRestau ? "article.opc_famille_cuisine.id":"article.opc_famille_stock.id" %>' groupValues="${listeFaimlle }" groupKey="id" groupLabel="libelle" width="0" filterOnly="true"/>
			<cplx:th type="long[]" fieldExport="article.libelle" value="Article" field="article.id" autocompleteAct="stock.mouvement.getListArticlesNonStock" groupKey="id" groupLabel="code;'-';libelle"/>
			<cplx:th type="string" field="article.code_barre" value="Code barre" width="120"/>
			<cplx:th type="string" value="Composition" sortable="false" filtrable="false" width="50"/>
			<cplx:th type="string" valueKey="article.destination" field="article.destination" groupValues="${listeDestination }"/>
			<cplx:th type="decimal" valueKey="article.prix_vente" field="article.prix_vente" width="120"/>
			<cplx:th type="decimal" valueKey="article.taux_marge_caissier" field="article.taux_marge_caissier" width="80" />
			<cplx:th type="string" valueKey="article.opc_unite_vente_enum" field="article.opc_unite_vente_enum.libelle" width="120"/>
			
			<c:forEach items="${listDataValueForm }" var="data">
				<cplx:th type="string" value="${data.opc_data_form.data_label }" filtrable="false" sortable="false" />
			</c:forEach>
				
			<cplx:th type="empty" />
			<cplx:th type="empty" />
		</cplx:header>
		<cplx:body>
			<c:set var="oldfam" value="${null }"></c:set>
			<c:set var="clientService" value="<%=ServiceUtil.getBusinessBean(IClientService.class) %>" />
			
			<c:forEach items="${list_article }" var="article">
				<c:set var="listDataVal" value="${clientService.loadDataForm(article.id, 'ARTICLE') }" />
				
				<c:if test="${article.familleStr.size() > 0}">
					<c:forEach var="i" begin="0" end="${article.familleStr.size()-1}">
						<c:if test="${empty oldfam or i>(oldfam.size()-1) or article.familleStr.get(i).code != oldfam.get(i).code}">
						     <tr>
								<td colspan="${listDataValueForm.size()+11 }" noresize="true" class="separator-group" style="padding-left: ${article.familleStr.get(i).level<=1?0:article.familleStr.get(i).level*10}px;">
									<span class="fa fa-fw fa-folder-open-o separator-icon"></span>  ${article.familleStr.get(i).code}-${article.familleStr.get(i).libelle}
								</td>
							</tr>
						</c:if>		
					</c:forEach>
				</c:if>
			
			<c:set var="oldfam" value="${article.familleStr }"></c:set>
				
				<cplx:tr workId="${article.id }" style="${article.is_disable?'text-decoration: line-through;':'' }">
					<cplx:td>
						<work:edit-link/>
					</cplx:td>
					<cplx:td>
						<a href="javascript:" id="lnk_det" curr="${article.id}"><span class="fa fa-plus" style="color:green;"></span>  ${article.code}-${article.libelle}</a>
						<c:if test="${article.is_fav_caisse }">
							<i class="fa fa-heart" title="Favoris caisse" style="color: green;"></i>
						</c:if>
						<c:if test="${article.mtt_garantie > 0 }">
							<i class="fa fa-mail-reply-all" title="Garantie sur l'article de ${article.mtt_garantie }" style="color: fushia;"></i>
						</c:if>
						<!-- Image -->
						<img alt="" src='resourcesCtrl?elmnt=${encryptionUtil.encrypt(article.getId().toString())}&path=article&rdm=${article.date_maj.getTime()}' width='24' height='24' onerror="this.onerror=null;this.remove();"/>
					</cplx:td>
					<cplx:td value="${article.code_barre}"></cplx:td>
					<cplx:td align="center">
						<span class="badge badge-sky" data-placement="top">${article.list_article.size() }</span>
					</cplx:td>
					<cplx:td value="${article.destination}"></cplx:td>
					<cplx:td align="right" value="${article.prix_vente}"></cplx:td>
					<cplx:td align="right" value="${article.taux_marge_caissier}"></cplx:td>
					<cplx:td value="${article.opc_unite_vente_enum.libelle}"></cplx:td>
					
					<c:forEach items="${listDataValueForm }" var="dataV">		
						<c:forEach items="${listDataVal }" var="data">
							<c:if test="${dataV.opc_data_form.id==data.opc_data_form.id }">
								<c:set var="currDV" value="${data.data_value }" />
								<c:set var="currAlign" value="${(data.opc_data_form.data_type=='LONG' or data.opc_data_form.data_type=='DECIMAL') ? 'right':'center' }" />
							</c:if>
						</c:forEach>
						<cplx:td align="${currAlign }" value="${currDV }" />
					</c:forEach>
					
					 <cplx:td align="center">
					 	  <a class="btn btn-sm btn-palegreen dropdown-toggle shiny" data-toggle="dropdown" href="javascript:void(0);" aria-expanded="false"><i class="fa fa-angle-down"></i></a>
                          <ul class="dropdown-menu dropdown-primary">
                               <li>
                               	 <std:link action="caisse.article2.ajouterFavorisCaisse" workId="${article.id }" style="color:${article.is_fav_caisse?'green':'#8e24aa'};" actionGroup="C" icon="fa ${article.is_fav_caisse?'fa-heart':'fa-heart-o' }" classStyle="" value="${article.is_fav_caisse?'Retirer favoris caisse':'Ajouter favoris caisse'}" tooltip="${article.is_fav_caisse?'Retirer des favoris caisse':'Ajouter aux favoris caisse'}" />
							  </li>
							  <li>
								<std:link action="stock.article.desactiver" workId="${article.id }" actionGroup="C" style="color:${article.is_disable?'green':'orange'};" icon="fa ${article.is_disable?'fa-unlock':'fa-lock' }" classStyle="" value="${article.is_disable?'Activer':'Désactiver'}" tooltip="${article.is_disable?'Activer':'Désactiver'}" />	
  							  </li>
  							  <li>
  							  	<std:link actionGroup="U" params="art=${article.id }" classStyle="btn btn-default" action="stock.article.init_histo_prix" icon="fa-3x fa fa-copy" tooltip="Historique des prix" />
  							  </li>	
                           </ul>
					 </cplx:td>
					<cplx:td align="center">
						<span style="display:none;" id="span_ord" tr="${article.id }"></span>
						<work:delete-link />
					</cplx:td>
				</cplx:tr>
				<tr style="display: none;" id="tr_det_${article.id}" class="sub">
					<td colspan="${listDataValueForm.size()+11 }" noresize="true" style="background-color: #fff4d3;" id="tr_consult_${article.id}">
						
					</td>
				</tr>
			</c:forEach>
		</cplx:body>
	</cplx:table>

 </div>
					<!-- end widget content -->

				</div>
				<!-- end widget div -->
 </std:form>