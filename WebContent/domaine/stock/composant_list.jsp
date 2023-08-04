<%@page import="framework.model.common.util.ServiceUtil"%>
<%@page import="appli.model.domaine.administration.service.IClientService"%>
<%@page import="appli.controller.domaine.util_erp.ContextAppli"%>
<%@page import="framework.model.common.util.EncryptionUtil"%>
<%@ taglib uri="http://www.customtaglib.com/complexe" prefix="cplx"%>
<%@ taglib uri="http://www.customtaglib.com/standard" prefix="std"%>
<%@ taglib uri="http://www.customtaglib.com/html" prefix="html"%>
<%@ taglib uri="http://www.customtaglib.com/work" prefix="work"%>
<%@ taglib uri="http://www.customtaglib.com/c" prefix="c"%>
<%@ taglib uri="http://www.customtaglib.com/c" prefix="fn"%>
<%@page errorPage="/commun/error.jsp" %>

<script type="text/javascript">
	$(document).ready(function (){
		$(document).off('click', "a[id^='lnk_det']").on('click', "a[id^='lnk_det']", function(){
			setTrContent($(this).attr("curr"), "<%=EncryptionUtil.encrypt("stock.composant.editTrArticle")%>");
		});
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
         <li>Gestion de stock</li>
         <li>Fiche des articles</li>
         <li class="active">Recherche</li>
     </ul>
 </div>
 
 <img class="imgBarCode" src="resources/framework/img/barcode_scanner.png" style="width: 20px;position: absolute;right: 17px;top: -27px;" title="Lecteur code barre utilisable sur cet écran">
 
 <std:form name="search-form">
<!-- /Page Breadcrumb -->
  <!-- Page Header -->
  <div class="page-header position-relative">
      <div class="header-title" style="padding-top: 4px;">
      	<c:if test="${empty isEditable or isEditable }">
	      	<std:link actionGroup="C" classStyle="btn btn-default" action="stock.composant.work_init_create" icon="fa-3x fa-plus" tooltip="Cr&eacute;er" />
	      	| 
	      	<div class="btn-group">
	            <a class="btn btn-azure dropdown-toggle" data-toggle="dropdown" aria-expanded="false">
	                Actions <i class="fa fa-angle-down"></i>
	            </a>
	            <ul class="dropdown-menu dropdown-azure">
	                <li>
	                    <std:link actionGroup="C" classStyle="" action="stock.composant.loadArticleFastSaisie" tooltip="Saisie rapide">
	                    	<i style="font-size: 14px;" class="fa fa-plus"></i> Saisie multi-articles
	                    </std:link>
	                </li>
	                <%if(isRestau){ %>
	                <li>
	                    <std:linkPopup actionGroup="C" classStyle="" action="stock.composant.init_dupliquer_fiche_article" tooltip="Générer les articles">
	                    	<i style="font-size: 14px;" class="fa fa-cubes"></i> Générer articles
	                    </std:linkPopup>
	                </li>
	                <%} %>
	                <li>
	                    <std:linkPopup actionGroup="C" classStyle="" action="stock.composant.importerComposants" tooltip="Importer les articles">
	                    	<i style="font-size: 14px;" class="fa fa-cloud-download"></i> Importer articles
	                    </std:linkPopup>
	                </li>
	                <li>
	                    <std:linkPopup actionGroup="C" classStyle="" action="stock.composant.exporterComposants" tooltip="Exporter les articles">
	                    	<i style="font-size: 14px;" class="fa fa-cloud-upload"></i> Exporter articles
	                    </std:linkPopup>
	                </li>
	                <li>
	                    <std:linkPopup actionGroup="C" classStyle="" action="stock.composant.familleComposants" tooltip="Changer la famille des articles">
	                    	<i style="font-size: 14px;" class="fa fa-edit"></i> Changer de famille
	                    </std:linkPopup>
	                </li>
	                <li class="divider"></li>
	                <%if(!isRestau){ %>
	                <li>
	                	<std:linkPopup actionGroup="C" classStyle="" action="stock.composant.sync_balance" tooltip="Synchroniser la balance">
	                		<i style="font-size: 14px;" class="fa fa-exchange"></i> Synchroniser balances
	                	</std:linkPopup>
	                </li> 
	                <%} %>
	                <li class="divider"></li>
	                <li>
	                    <std:linkPopup actionGroup="C" classStyle="" action="stock.article.init_print_fiche" tooltip="Configurer l'imprimante d'étiquettes">
	                    	<i style="font-size: 14px;" class="fa fa-print"></i> Configurer imprimantes
	                    </std:linkPopup>
	                </li>
	               <li>
	                    <std:link actionGroup="C" classStyle="" action="stock.composant.controle_marge" tooltip="Contrôler la marge (<10%)">
	                    	<i style="font-size: 14px;" class="fa fa-print"></i> Contôler la marge(<10%)
	                    </std:link>
	                </li>
	                <li class="divider"></li>
	                <li>
	                	<std:linkPopup noJsValidate="true" classStyle="" actionGroup="C" style='color:blue;' action="admin.dataForm.work_find" tooltip="Données formulaire" params="tp=COMPOSANT">
	                		<i style="font-size: 14px;" class="fa fa-cogs"></i> Champs formulaire
	                	</std:linkPopup>
	                </li>
	            </ul>
	        </div>
      	
           | <std:link actionGroup="C" classStyle="btn btn-success" action="stock.article.print_fiche_article" icon="fa-3x fa-print" tooltip="Imprimer les étiquette des prix" value="Imprimer étiquette prix" />
         	 <std:link actionGroup="C" classStyle="btn btn-magenta" action="stock.article.print_barre_article" icon="fa-3x fa-barcode" tooltip="Imprimer les étiquette code barre" value="Imprimer code barre" />
         	 
         	 <std:linkPopup noJsValidate="true" actionGroup="C" style='margin-left: 10px;' action="admin.dataForm.work_find" icon="fa fa-cogs" tooltip="Données formulaire" value="Formulaire" params="tp=COMPOSANT" />
      	</c:if>
     
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
	<!-- row -->
	<div class="row">
	
	<c:set var="encryptionUtil" value="<%=new EncryptionUtil() %>" />
	
	<!-- Liste des articles -->
	<cplx:table name="list_article" transitionType="simple" showDataState="true" checkable="${empty isEditable or isEditable }" width="100%" title="Liste des composants" initAction="stock.composant.work_find" autoHeight="true">
		<cplx:header>
			<cplx:th type="empty" />
			<cplx:th type="long[]" value="Famille" field="article.opc_famille_stock.id" groupValues="${listeFaimlle }" groupKey="id" groupLabel="libelle" width="0" filterOnly="true"/><!-- Filter only -->
			<cplx:th type="long[]" width="150" value="Code" field="article.code"/>
			<cplx:th type="long[]" value="Libellé" field="article.id" fieldExport="article.libelle" autocompleteAct="stock.mouvement.getListArticles" groupKey="id" groupLabel="code;'-';libelle"/>
			<cplx:th type="string" field="article.code_barre" value="Code barre" width="120"/>
			<cplx:th type="decimal" valueKey="article.prix_achat_ht" field="article.prix_achat_ht" width="100"/>
			<cplx:th type="string" valueKey="article.opc_unite_achat_enum" field="article.opc_unite_achat_enum.libelle" width="150"/>
			
			<%if(!isRestau){ %>
				<cplx:th type="decimal" value="Prix vente" field="article.prix_vente" width="100"/>
				<cplx:th type="string" value="Unité vente" field="article.opc_unite_vente_enum.libelle" width="150"/>
				<cplx:th type="string" value="Marge" width="150" filtrable="false" sortable="false" />
			<%} %>
			
			<c:forEach items="${listDataValueForm }" var="data">
				<cplx:th type="string" value="${data.opc_data_form.data_label }" filtrable="false" sortable="false" />
			</c:forEach>
			
			<c:if test="${empty isEditable or isEditable }">
				<cplx:th type="empty" />
			</c:if>
			<cplx:th type="empty" />
		</cplx:header>
		<cplx:body>
			<c:set var="oldfam" value="${null }"></c:set>
			<c:set var="minCol" value="<%=isRestau ? 7 : 10%>"/> 
			<c:set var="maxCol" value="<%=isRestau ? 9 : 12%>"/>
			
			<c:set var="clientService" value="<%=ServiceUtil.getBusinessBean(IClientService.class) %>" />
			
			<c:forEach items="${list_article }" var="article">
				<c:set var="listDataVal" value="${clientService.loadDataForm(article.id, 'COMPOSANT') }" />
			
				<c:if test="${article.familleStr.size() > 0}">
					<c:forEach var="i" begin="0" end="${article.familleStr.size()-1}">
						<c:if test="${empty oldfam or i>(oldfam.size()-1) or article.familleStr.get(i).code != oldfam.get(i).code}">
						     <tr>
								<td colspan="${((empty isEditable or isEditable)?maxCol:minCol)+listDataValueForm.size()}" noresize="true" class="separator-group" style="padding-left: ${article.familleStr.get(i).level*10}px;">
									<span class="fa fa-fw fa-folder-open-o separator-icon"></span> ${article.familleStr.get(i).code}-${article.familleStr.get(i).libelle}
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
					<cplx:td style="text-transform: uppercase;">
						<a href="javascript:" id="lnk_det" curr="${article.id}"><span class="fa fa-plus" style="color:green;"></span> ${article.code}</a>
						<c:if test="${article.is_fav_caisse }">
							<i class="fa fa-heart" title="Favoris caisse" style="color: green;"></i>
						</c:if>
						<!-- Image -->
						<img alt="" src='resourcesCtrl?elmnt=${encryptionUtil.encrypt(article.getId().toString())}&path=composant&rdm=${article.date_maj.getTime()}' width='24' height='24' onerror="this.onerror=null;this.remove();"/>
					</cplx:td>
					<cplx:td style="text-transform: uppercase;" value="${article.libelle}"></cplx:td>
					<cplx:td value="${article.code_barre}"></cplx:td>
					<cplx:td align="right" style="font-weight:bold;" value="${article.prix_achat_ht}"></cplx:td>
					<cplx:td value="${article.opc_unite_achat_enum.libelle}"></cplx:td>
					<%if(!isRestau){ %>
					<cplx:td align="right" style="font-weight:bold;" value="${article.prix_vente}"></cplx:td>
					<cplx:td value="${article.opc_unite_vente_enum.libelle}"></cplx:td>
					<cplx:td align="right" value="${article.getMarge_calcule()}"></cplx:td>
					<%} %>
					
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
                               <%if(!isRestau){ %>
                               <li>
                               	 <std:link action="caisse.article2.ajouterFavorisCaisse" workId="${article.id }" style="color:${article.is_fav_caisse?'green':'#8e24aa'};" actionGroup="C" icon="fa ${article.is_fav_caisse?'fa-heart':'fa-heart-o' }" classStyle="" value="${article.is_fav_caisse?'Retirer favoris caisse':'Ajouter favoris caisse'}" tooltip="${article.is_fav_caisse?'Retirer des favoris caisse':'Ajouter aux favoris caisse'}" />
							  </li>
							  <li>
  							  	<std:link actionGroup="U" params="art=${article.id }" classStyle="btn btn-default" action="stock.article.init_histo_prix" icon="fa-3x fa fa-copy" tooltip="Historique des prix" />
  							  </li>	
								<%} %>							  
							  <li>
								<std:link action="stock.composant.desactiver" workId="${article.id }" actionGroup="C" style="color:${article.is_disable?'green':'orange'};" icon="fa ${article.is_disable?'fa-unlock':'fa-lock' }" classStyle="" value="${article.is_disable?'Activer':'Désactiver'}" tooltip="${article.is_disable?'Activer':'Désactiver'}" />	
  							  </li>
                           </ul>
					 </cplx:td>
					 <c:if test="${empty isEditable or isEditable }">
						<cplx:td align="center">
								<work:delete-link />
						</cplx:td>
					</c:if>
				</cplx:tr>
				<tr style="display: none;" id="tr_det_${article.id}" class="sub">
					<td colspan="${listDataValueForm.size()+9 }" noresize="true" style="background-color: #fff4d3;" id="tr_consult_${article.id}">
						
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