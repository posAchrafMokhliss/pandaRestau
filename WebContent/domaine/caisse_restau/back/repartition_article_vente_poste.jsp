<%@page import="appli.controller.domaine.util_erp.ContextAppli"%>
<%@page import="appli.model.domaine.stock.service.impl.RepartitionBean"%>
<%@page import="java.math.BigInteger"%>
<%@page import="java.util.Arrays"%>
<%@page import="framework.controller.ControllerUtil"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.math.BigDecimal"%>
<%@page import="framework.model.common.util.BigDecimalUtil"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="framework.model.common.util.EncryptionUtil"%>
<%@ taglib uri="http://www.customtaglib.com/complexe" prefix="cplx"%>
<%@ taglib uri="http://www.customtaglib.com/standard" prefix="std"%>
<%@ taglib uri="http://www.customtaglib.com/fmt" prefix="fmt"%>
<%@ taglib uri="http://www.customtaglib.com/html" prefix="html"%>
<%@ taglib uri="http://www.customtaglib.com/work" prefix="work"%>
<%@ taglib uri="http://www.customtaglib.com/c" prefix="c"%>
<%@page errorPage="/commun/error.jsp"%>

<style>
	#tab_rep tr:hover{
		background: #DBEDF3;
	}
	#tab_rep td {
		border-bottom: 1px dashed #ff9900;
		padding-right: 5px;
	}
</style>

<%
BigDecimal mttTotalAll = null;
%>
<!-- Page Breadcrumb -->
<div class="page-breadcrumbs breadcrumbs-fixed">
	<ul class="breadcrumb">
		<li><i class="fa fa-home"></i> <a href="#">Accueil</a></li>
		<li>Suivi</li>
		<li class="active">R&eacute;parition des ventes par poste</li>
	</ul>
</div>
<!-- /Page Breadcrumb -->
<!-- Page Header -->
<div class="page-header position-relative">
	<div class="header-title" style="padding-top: 4px;">
	<%if(ControllerUtil.getMenuAttribute("IS_MNU", request) == null) { %>
	<%if(ControllerUtil.getMenuAttribute("IS_DASH_JRN", request) == null){ %>
		<std:link classStyle="btn btn-default" action="caisse.journee.work_find" params="bck=1" icon="fa fa-3x fa-mail-reply-all" tooltip="Retour &agrave; la recherche" />
		<%} %>
	<%} %>
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

	<div class="widget">
		<%if(ContextAppli.IS_RESTAU_ENV()){ %>
		<div class="tabbable">
			<ul class="nav nav-tabs" id="myTab9">
				<li><a data-toggle="tab" href="#repartitionVente" wact="<%=EncryptionUtil.encrypt("caisse.journee.find_repartition")%>"> R&eacute;partition des ventes </a></li>
				<li class="active"><a data-toggle="tab" href="#repartitionVente" params="is_poste=1" wact="<%=EncryptionUtil.encrypt("caisse.journee.find_repartition")%>"> R&eacute;partition par poste </a></li>
				<li><a data-toggle="tab" href="#repartitionVente" params="tp=mnu" wact="<%=EncryptionUtil.encrypt("caisse.journee.find_rep_stock")%>"> R&eacute;partition destockage </a></li>
				<li><a data-toggle="tab" href="#conf" wact="<%=EncryptionUtil.encrypt("caisse-web.caisseWeb.manage_reparition_conf")%>"> Configuration </a></li>
			</ul>
		</div>	
		<%} %>
	
		<std:form name="search-form">
         <div class="widget-body">
			<div class="row" style="margin-left: 10px;">
	<div class="row">
        <div class="form-group">
        	<std:label classStyle="control-label col-md-2" value="Date d&eacute;but" />
            <div class="col-md-2">
                 <std:date name="dateDebut" required="true" value="${dateDebut }"/>
            </div>
            <div class="col-md-2" style="text-align: center;">
            	<std:link action="caisse.journee.find_repartition" params="prev=1&is_fltr=1&is_poste=1" icon="fa fa-arrow-circle-left" tooltip="Journ&eacute;e pr&eacute;c&eacute;dente" />
            	<std:link action="caisse.journee.find_repartition" params="next=1&is_fltr=1&is_poste=1" icon="fa fa-arrow-circle-right" tooltip="Journ&eacute;e suivante" />
            </div>
            
            <std:label classStyle="control-label col-md-1" value="Date fin" />
            <div class="col-md-2">
                 <std:date name="dateFin" required="true" value="${dateFin }"/>
            </div>
       </div>
       <div class="form-group">
	       <std:label classStyle="control-label col-md-2" value="Famille" />
			<div class="col-md-7">	 
				<std:select type="string" name="curr_famille" data="${list_famille }" key="id" labels="libelle" width="100%;" value="${curr_famille }" isTree="true" />
			</div>
			<div class="col-md-2">
           	 	<std:button action="caisse.journee.find_repartition" value="Filtrer" params="is_fltr=1&is_poste=1" classStyle="btn btn-primary" />
           	 </div>	
		</div>	
   </div>
   <hr>
      <div class="row" style="width: 100%;">
   		<div class="alert alert-warning fade in">
             <button class="close" data-dismiss="alert">
                 x
             </button>
             <i class="fa-fw fa fa-warning"></i>
             <span>
             	Les montants affichés sont à titre indicatif. Ils ne prennent pas en compte le tarif 
             	dans un menu ni les offres globales sur les commandes ni l'heure de début et de fin de la journée.<br>
             	Pour les montants exactes, veuillez consulter la liste des journées via 
             	
             	<a href="#lmnu=cai-journee&rdm=qnqj" style="color: blue;">
             		CE LIEN
				</a>
             </span>
        </div>     
   </div>
   
   <div class="row">
        	<div class="widget" style="margin-right: 26px;">
				<div class="widget-header ">
					<span class="widget-caption">D&eacute;tail des ventes </span>
				</div>
				<div class="widget-body">
   
<%
Map<String, Map> mapDataRep = (Map)request.getAttribute("dataRepartion");
BigDecimal mttOffre = null;
BigDecimal mttLivraison = null;

for(String caisse : mapDataRep.keySet()){
	Map mapData = mapDataRep.get(caisse);

	Map<Long, RepartitionBean> mapMenu = (Map<Long, RepartitionBean>)mapData.get("MENU");
	if(mapMenu == null){
		mapMenu = new HashMap<>();
	}
	Map<Long, RepartitionBean> mapMenuArticle = (Map<Long, RepartitionBean>)mapData.get("MENU_ARTS");
	if(mapMenuArticle == null){
		mapMenuArticle = new HashMap<>();
	}
	Map<Long, RepartitionBean> mapArticle = (Map<Long, RepartitionBean>)mapData.get("ARTS");
	if(mapArticle == null){
		mapArticle = new HashMap<>();
	}
	mttOffre = BigDecimalUtil.add(mttOffre, (BigDecimal)mapData.get("OFFRE"));
	mttLivraison = BigDecimalUtil.add(mttLivraison, (BigDecimal)mapData.get("LIVRAISON"));
	
	if(mapMenu.size()==0 && mapArticle.size()==0){%>
		<span style="padding-left: 20px;">Aucune donn&eacute;e &agrave; afficher.</span>
	<%} else{%>

	<!-- Liste des articles -->
	<div class="row">
	<h3 style="padding-left: 30px;text-transform: uppercase;color: blue;"><b><%=caisse %></b></h3>
	
	<div class="col-md-6">
		<table style="background-color: white;width: 100%;border: 1px solid #2196F3;font-size: 11px;" id="tab_rep">
			<tr style="background-color: #c6cec6;">
				<th></th>
				<th style="text-align: right;width: 100px;">Qte</th>
				<th style="text-align: right;width: 100px;">Montant</th>
			</tr>
			<tr style="background-color: #fff59d;line-height: 37px;">
				<th colspan="3" style="font-weight: bold;font-size: 19px;">LES MENUS</th>
			</tr>	
			<%
			BigDecimal mttTotalMenu = BigDecimalUtil.ZERO, qteTotalMnu = BigDecimalUtil.ZERO;
			
			for(Long menuId : mapMenu.keySet()){
				RepartitionBean data = mapMenu.get(menuId);
				%>
				<tr>
					<td style="color: #777;padding-left: 25px;"><%=data.getLibelle() %></td>
					<td style="text-align: right;color: black;"><%=data.getQuantite() %></td>
					<td style="text-align: right;color: black;"><%=BigDecimalUtil.formatNumberZero(data.getMontant()) %></td>
				</tr>
				<%
				mttTotalMenu = BigDecimalUtil.add(mttTotalMenu, data.getMontant());
				qteTotalMnu = BigDecimalUtil.add(qteTotalMnu, data.getQuantite());
			}
			%>
				<tr style="background-color: #fdf8c9;font-size: 12px;">
					<td style="color: #777;padding-left: 5px;text-align:right;">TOTAL</td>
					<td style="text-align: right;color: black;font-weight: bold;"><%=qteTotalMnu.intValue() %></td>
					<td style="text-align: right;color: black;font-weight: bold;"><%=BigDecimalUtil.formatNumberZero(mttTotalMenu) %></td>
				</tr>
	
			<tr>
				<th colspan="3">&nbsp;</th>
			</tr>	
			<tr style="background-color: #fff59d;line-height: 37px;">
				<th colspan="3" style="font-weight: bold;font-size: 19px;">LES ARTICLES</th>
			</tr>	
			
			<%
			
			mttTotalAll = BigDecimalUtil.add(mttTotalAll, mttTotalMenu);
			
			String oldFamille = null;
			BigDecimal mttTotalArtBloc = BigDecimalUtil.ZERO, qteTotalArtBloc = BigDecimalUtil.ZERO;
			BigDecimal mttTotalArtAll = BigDecimalUtil.ZERO, qteTotalArtAll = BigDecimalUtil.ZERO;
			
			for(Long menuId : mapArticle.keySet()){
				RepartitionBean data = mapArticle.get(menuId);
				%>
				<%
				if(oldFamille == null || !oldFamille.equals(data.getFamille())){%>
				<!-- Sous total -->
					<%if(oldFamille != null){ %>
					<tr style="font-weight: bold;color: #FF5722;background-color: #fff385;">
						<td style="text-align: right;">SOUS TOTAL</td>
						<td style="font-weight: bold;text-align: right;"><%=qteTotalArtBloc.intValue() %></td>
						<td style="font-weight: bold;text-align: right;"><%=BigDecimalUtil.formatNumberZero(mttTotalArtBloc) %></td>
					</tr>
					<%
					mttTotalArtBloc = null;
					qteTotalArtBloc = null;
					} %>
					
				<tr>
					<td colspan="3" style="font-weight: bold;"><%=data.getFamille() %></td>
				</tr>	
				<%} %>
				<tr>
					<td style="color: #777;padding-left: 25px;"><%=data.getLibelle() %></td>
					<td style="text-align: right;color: black;"><%=(data.getQuantite()!=null?data.getQuantite().intValue():0) %></td>
					<td style="text-align: right;color: black;"><%=BigDecimalUtil.formatNumberZero(data.getMontant()) %></td>
				</tr>
				<%
				oldFamille = data.getFamille();
				mttTotalArtBloc = BigDecimalUtil.add(mttTotalArtBloc, data.getMontant());
				qteTotalArtBloc = BigDecimalUtil.add(qteTotalArtBloc, data.getQuantite());
				
				mttTotalArtAll = BigDecimalUtil.add(mttTotalArtAll, data.getMontant());
				qteTotalArtAll = BigDecimalUtil.add(qteTotalArtAll, data.getQuantite());
				
				mttTotalAll = BigDecimalUtil.add(mttTotalAll, data.getMontant());
			}
			%>
			<!-- Sous total -->
			<%if(oldFamille != null){ %>
				<tr style="font-weight: bold;color: #FF5722;background-color: #fff385;">
					<td style="text-align: right;">SOUS TOTAL</td>
					<td style="font-weight: bold;text-align: right;"><%=qteTotalArtBloc.intValue() %></td>
					<td style="font-weight: bold;text-align: right;"><%=BigDecimalUtil.formatNumberZero(mttTotalArtBloc) %></td>
				</tr>
			<%} %>	
			
			<!-- CUMUL DES ARTICLES -->
			<tr>
				<th colspan="3">&nbsp;</th>
			</tr>
			<tr style="background-color: #fdf8c9;font-size: 12px;">
				<td style="color: #777;padding-left: 5px;text-align:right;">TOTAL</td>
				<td style="text-align: right;color: black;font-weight: bold;"><%=qteTotalArtAll.intValue() %></td>
				<td style="text-align: right;color: black;font-weight: bold;"><%=BigDecimalUtil.formatNumberZero(mttTotalArtAll) %></td>
			</tr>
		</table>
	</div>
	<div class="col-md-6">
		<table style="background-color: white;width: 100%;border: 1px solid #2196F3;font-size: 11px;" id="tab_rep">
			<tr style="background-color: #fff59d;line-height: 37px;">
				<th colspan="4" style="font-weight: bold;font-size: 19px;">LES ARTICLES EN DETAIL [menu et hors menu]</th>
			</tr>	
			
			<tr style="background-color: #c6cec6;">
				<th></th>
				<th style="text-align: right;width: 50px;">Qte hors menu</th>
				<th style="text-align: right;width: 50px;">Qte menu</th>
				<th style="text-align: right;width: 50px;">Total</th>
			</tr>
		
			<%
			BigDecimal qteTotalArticleALL = BigDecimalUtil.ZERO;
			// Ajout articles menus manquants
			for(Long artId : mapMenuArticle.keySet()){
				if(mapArticle.get(artId) == null){
					mapArticle.put(artId, mapMenuArticle.get(artId));
				}
			}
			
			//
			for(Long menuId : mapArticle.keySet()){
				RepartitionBean data = mapArticle.get(menuId);
				RepartitionBean dataMnu = mapMenuArticle.get(menuId);
				
				BigDecimal qteHorsMnu = (data!=null && data.getQuantite()!=null ? data.getQuantite():BigDecimalUtil.get(0));
				BigDecimal qteMnu = (dataMnu!=null && dataMnu.getQuantite()!=null ? dataMnu.getQuantite() : BigDecimalUtil.get(0));
				
				if(oldFamille == null || !oldFamille.equals(data.getFamille())){%>
					<tr>
						<td colspan="7" style="font-weight: bold;"><%=data.getFamille() %></td>
					</tr>	
				<%}%>
				
				<tr>
					<td style="color: #777;padding-left: 25px;"><%=data.getLibelle() %></td>
					<td style="color: #777;padding-left: 25px;"><%=qteHorsMnu.intValue() %></td>
					<td style="text-align: right;color: black;"><%=qteMnu.intValue() %></td>
					<td style="text-align: right;color: black;"><%=BigDecimalUtil.add(qteHorsMnu, qteMnu).intValue() %></td>
				</tr>
				<%
				oldFamille = data.getFamille();
			}
			%>
		</table>
	</div>
	</div>
<%}
	}%>	
	<hr>
	<div class="row">
	<div class="col-md-12"> 
		<h4>Total ventes : <span style="color: blue;"><%=BigDecimalUtil.formatNumber(mttTotalAll) %></span></h4>
		<h4>Frais de livraisons : <span style="color: blue;"><%=BigDecimalUtil.formatNumber(mttLivraison) %></span></h4>
		<h4>Offres et réduction : <span style="color: blue;"><%=BigDecimalUtil.formatNumber(mttOffre) %></span></h4>
		<h4>Ventes net : <span style="color: blue;"><%=BigDecimalUtil.formatNumber(BigDecimalUtil.substract(mttTotalAll, mttLivraison, mttOffre)) %></span></h4>
		<br>
	</div>
	</div>
</div>
</div>
</div>
	</div>
</div>
</std:form>
</div>
</div>
