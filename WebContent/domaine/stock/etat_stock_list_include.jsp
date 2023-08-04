<%@page import="framework.model.common.util.BigDecimalUtil"%>
<%@page import="framework.model.common.util.StrimUtil"%>
<%@page import="framework.model.common.constante.ProjectConstante"%>
<%@page import="framework.model.common.util.EncryptionUtil"%>
<%@page import="framework.controller.ControllerUtil"%>
<%@page import="framework.model.common.util.NumericUtil"%>
<%@page import="framework.model.common.util.StringUtil"%>
<%@page import="framework.component.complex.table.RequestTableBean"%>
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
	$("#list_article").show(1000);
	setTimeout(function(){ manageFullTh(false); }, 100);	
});
</script>

<div style="border: 1px solid #2dc3e8;">
<%
String tableName = "list_article";
RequestTableBean cplxTable = ControllerUtil.getRequestTableBean(tableName, request);
String currAct = EncryptionUtil.encrypt("stock.etatStock.work_find");
String jsFunction = "";
// *************************** Pager ******************************//
	int element_count = cplxTable.getDataSize();
	int line_count = cplxTable.getPageSize();
	int curent_page = cplxTable.getCurrentPage();
	int page_count = (int) Math.ceil((double) element_count / line_count);// Calculate
	int end = cplxTable.getLimitIndex();
	// page
	// count
	int start = cplxTable.getStartIndex();
	String[] rowsInpage = StringUtil.getElementsArray(ProjectConstante.ROWS_IN_PAGE, ",", true);
	// Re-ajust rows segment
	for (int i = 0; i < rowsInpage.length; i++) {
		if ((i > 0) && (rowsInpage[i - 1] == null)) {
			rowsInpage[i] = null;
		} else if ((NumericUtil.getIntOrDefault(rowsInpage[i]) > element_count)
				&& ((i > 0) && (NumericUtil.getIntOrDefault(rowsInpage[i - 1]) >= element_count))) {
			rowsInpage[i] = null;
		}
	}
	// Hidden fields
	String oldCp = (String)ControllerUtil.getParam(request, tableName+"_pager.cp");
	%>
	<input type='hidden' name="<%=tableName%>_pager.cp_old" value="<%= (StringUtil.isEmpty(oldCp) ? "1" : oldCp)%>"/>
	<input type='hidden' name="<%=tableName%>_pager.cp" id="<%=tableName%>_pager.cp"/>
	<input type='hidden' name="<%=tableName%>_pager.fie" id="<%=tableName%>_pager.fie" value="<%=StringUtil.getValueOrEmpty(ControllerUtil.getParam(request, tableName+"_pager.fie"))%>"/>

	<%
	jsFunction = "pagerAjaxTable('"+tableName+"','"+curent_page+"', '"+currAct+"');";
	String onClickRefresh = " onClick=\"" + jsFunction + "\"";
	%>
	<table cellspacing='0' cellpadding='0' class='inf' width='100%'>
		<tr>
			<td width='20%' nowrap='nowrap'>
				<%if (page_count >= 1) { %>
					<%=((start + 1) + "-" + end + " / " + element_count)%>
					&nbsp;
				<%} %>
				&nbsp;&nbsp;
			
			</td>
			<td width='60%' nowrap='nowrap' align='center'>
		<%
if (page_count >= 1) {
		jsFunction = "pagerAjaxTable('"+tableName+"','1', '"+currAct+"');";
		String onClickDeb = ((curent_page != 1) ? (" onClick=\"" + jsFunction) + "\"" : "");
		%>
			<div id='navigation'>
				<table>
					<tr>
						<td><!--  Debut -->
						   <a class="btn btn-default btn-xs shiny icon-only success" href="javascript:void(0);" style="margin-right:2px;margin-top: -2px;<%=(curent_page != 1) ?"":"background: #ccc;" %>" title="<%=StrimUtil.label("first.page")%>" <%=(curent_page != 1) ? onClickDeb:"" %>>
								<i class="fa fa-fast-backward"></i>
							</a>
						</td>

		<%jsFunction = "pagerAjaxTable('"+tableName+"','" + (curent_page - 1) + "', '"+currAct+"');";
		String onClickPrev = ((curent_page != 1) ? " onClick=\"" + jsFunction + "\"" : "");
		%>
				<td><!-- Precedent -->
					<a class="btn btn-default btn-xs shiny icon-only success" href="javascript:void(0);" style="margin-right:2px;margin-top: -2px;<%=(curent_page != 1) ?"":"background: #ccc;" %>" title="<%=StrimUtil.label("back.page")%>" <%=(curent_page != 1) ? onClickPrev:""%>>
						<i class="fa fa-chevron-left"></i>
					</a>
					
				</td>
				<td>Page</td>

		<%jsFunction = "pagerAjaxTable('"+tableName+"',this.value, '"+currAct+"');";
		String onChange = ((page_count > 1) ? " onChange=\"" + jsFunction + "\"" : " disabled='disabled'");
		%>
		<!-- Pages -->
		<td>
			<select class="flexselect" name="<%=tableName%>_pager.cp_sub" <%=onChange%>>
			<%for (int i = 1; i < (page_count + 1); i++) {%>
				<option value='<%=i %>' <%=((curent_page == i) ? " selected=selected" : "")%>><%=i %></option>
			<%}%>
			</select>
		</td>
		<td> / <%=page_count%></td>

		<%jsFunction = "pagerAjaxTable('"+tableName+"', '" + (curent_page + 1) + "', '"+currAct+"');";
		String onClickNext = ((curent_page < page_count) ? (" onClick=\"" + jsFunction) + "\"" : "");
		%>

			<td><!-- Next -->
				<a class="btn btn-default btn-xs shiny icon-only success" href="javascript:void(0);" style="margin-left:2px;margin-right:2px;margin-top: -2px;<%=(curent_page < page_count) ?"":"background: #ccc;" %>" title="<%=StrimUtil.label("next.page")%>" <%=(curent_page < page_count) ? onClickNext:"" %>>
					<i class="fa fa-chevron-right"></i>
				</a>	
			</td>

		<%jsFunction = "pagerAjaxTable('"+tableName+"', '" + (page_count) + "', '"+currAct+"');";
		String onClickLast = ((curent_page != page_count) ? (" onClick=\"" + jsFunction) + "\"" : "");
		%>

			<td><!-- Dernier -->
					<a class="btn btn-default btn-xs shiny icon-only success" href="javascript:void(0);" style="margin-top: -2px;<%=(curent_page != page_count) ?"":"background: #ccc;" %>" title="<%=StrimUtil.label("last.page")%>" <%=(curent_page != page_count) ? onClickLast:"" %>>
						<i class="fa fa-fast-forward"></i>
					</a>
				</td>
		</tr>
	</table>
	</div>
<%} %>	
	</td>

		<td width='20%' nowrap='nowrap' align='right'>
<%if (page_count >= 1) {
		jsFunction = "pagerAjaxTable('"+tableName+"', '"+curent_page+"', '"+currAct+"');";
		String state = (element_count < NumericUtil.toInteger(ProjectConstante.DEFAULT_LINE_COUNT)) ? "disabled='disabled'" : " onChange=\"" + jsFunction	+ "\"";
	%>		
		<%=StrimUtil.label("work.elmnt.page") %>
			<select class="flexselect" name="<%=tableName%>_pager.lc" <%=state%>>
		<!-- Nombre d'element par page -->
		<%for (String nbr : rowsInpage) {
			if (nbr != null) {%>
				<option value="<%=nbr %>" <%=((nbr.equals("" + line_count)) ? "selected=selected" : "")%>><%=nbr %></option>
			<%}
			} %>
			</select>

<%} %>
		</td>
</tr>
</table>
</div>
		
		<input type="hidden" name="<%=tableName %>_pager.cp_old" value="1">
		<input type="hidden" name="<%=tableName %>_pager.cp" id="<%=tableName %>_pager.cp">
		<input type="hidden" name="<%=tableName %>_pager.fie" id="<%=tableName %>_pager.fie" value="">
		
		<c:set var="bigDecimalUtil" value="<%=new BigDecimalUtil() %>" />
			
	<!-- Liste des articles -->
	<table border="1" style="width: 100%;display: none;" id="<%=tableName %>" class="table table-hover table-striped table-bordered table-condensed">
		<thead>
			<tr>
				<th rowspan="3">Article</th>
				<th rowspan="2" colspan="2">Prix unitaire</th>
				<th rowspan="2" colspan="2" style="background-color: #cddc39;" id="inventairePrec_det">Inventaire ${dateDebut}</th>	
				<!-- Mouvements in -->
				<th colspan="6" id="approvis_det">ENTREE</th>
				<th rowspan="2" colspan="2">TOTAL ENTREE
					<a href="javascript:" id="approvis"><span class="fa fa-plus" style="color:green;float: right;"></span></a>
				</th>
				
				<!-- Mouvements out -->
				<th colspan="14" style="width: 80px;" id="ventes_det">SORTIES</th>
				<th rowspan="2" colspan="2" >TOTAL SORTIES
					<a href="javascript:" id="ventes"><span class="fa fa-plus" style="color:green;float: right;"></span></a>
				</th>
				
				<th rowspan="2" colspan="2" style="background-color: #d17fb0de;">Situation Th&eacute;orique debut ${dateFin}</th>
				<th rowspan="2" colspan="2" style="background-color: #cddc39;" id="inventaireActu_det">Inventaire ${dateFin}</th> 
				<th rowspan="2" colspan="2">Ecarts</th>			
			</tr>
			
			<tr>
				<th colspan="2" id="approvis_det2">Achats</th>
				<th colspan="2" id="approvis_det3">Tranferts</th>
				<th colspan="2" id="approvis_det4">Transformations</th>
					
				<th colspan="2" id="ventes_det2">Ventes caisse</th>
				<th colspan="2" id="ventes_det4">Ventes hors caisse</th>
				<th colspan="2" id="ventes_det5">Transfert</th>
				<th colspan="2" id="ventes_det6">Transformation</th>
				<th colspan="2" id="ventes_det7">Avoirs</th>
				<th colspan="2" id="ventes_det8">Pertes</th>
				<th colspan="2" id="ventes_det9">Consommation</th>
			</tr>
			
			<tr id="tr_detail">
				<th style="width: 80px;">HT</th>
				<th style="width: 80px;">TTC</th>
				<!-- Inventaire -->
				<th style="width: 80px;background-color: #cddc39;">Quantit&eacute;</th>	
				<th style="width: 80px;background-color: #cddc39;" id="idx_approvis">Valeur TTC</th>
				
				<!-- Entree (achat, tranfert, transfo) -->
				<th style="width: 80px;">Quantit&eacute;</th>
				<th style="width: 80px;">Valeur TTC</th>
				<th style="width: 80px;">Quantit&eacute;</th>
				<th style="width: 80px;">Valeur TTC</th>
				<th style="width: 80px;">Quantit&eacute;</th>
				<th style="width: 80px;">Valeur TTC</th>
				<!-- Total entr�e -->
				<th style="width: 80px;">Quantit&eacute;</th>
				<th style="width: 80px;" id="idx_ventes">Valeur TTC</th>
				
				<!-- Sortie -->
				<th style="width: 80px;">Quantit&eacute;</th>
				<th style="width: 80px;">Valeur TTC</th>
				<th style="width: 80px;">Quantit&eacute;</th>
				<th style="width: 80px;">Valeur TTC</th>
				<th style="width: 80px;">Quantit&eacute;</th>
				<th style="width: 80px;">Valeur TTC</th>
				<th style="width: 80px;">Quantit&eacute;</th>
				<th style="width: 80px;">Valeur TTC</th>
				<th style="width: 80px;">Quantit&eacute;</th>
				<th style="width: 80px;">Valeur TTC</th>
				<th style="width: 80px;">Quantit&eacute;</th>
				<th style="width: 80px;">Valeur TTC</th>
				<th style="width: 80px;">Quantit&eacute;</th>
				<th style="width: 80px;">Valeur TTC</th>
				<!-- Total sortie -->				
				<th style="width: 80px;">Quantit&eacute;</th>
				<th style="width: 80px;">Valeur TTC</th>
				
				<!-- Situation th&eacute;orique -->
				<th style="width: 80px;background-color: #d17fb0de" >Quantit&eacute;</th>
				<th style="width: 80px;background-color: #d17fb0de">Valeur TTC</th>
				<!-- Inventaire -->
				<th style="width: 80px;">Quantit&eacute;</th>	
				<th style="width: 80px;">Valeur TTC</th>
				<!-- Ecart -->
				<th style="width: 80px;">Quantit&eacute;</th>
				<th style="width: 80px;">Valeur TTC</th>
			</tr>					
		</thead>
		<tbody>
			<c:set var="oldFam" value="${null }"></c:set>
			<c:set var="oldArt" value="${null }"></c:set>
			
			<c:forEach items="${map_article.keySet() }" var="key">
				<c:set var="artInfos" value="${map_article.get(key) }" />
				<c:set var="article" value="${artInfos.article }" />
				
				<!-- Ligne Totale -->
				<c:if test="${not empty oldArt and article.id != oldArt}">
					<c:set var="artTotaux" value="${mapRecapArt.get(oldArt) }" />
					<tr>
						<td></td>
						<td></td>
						<td></td>
						<!-- Inventaire -->
						<td style="font-weight:bold;background-color: orange;background-color: orange;" align="right">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qteInvPrec) }</td>
						<td style="font-weight:bold;background-color: orange;" align="right">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.mttInvPrec) }</td>
						
						<!-- Entr�e -->
						<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qteTotalAchat)}</td>
						<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.mttTotalAchat)}</td>
						<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qteTransfertIn)}</td>
						<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.mttTransfertIn)}</td>
						<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qteTransfoIn)}</td>
						<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.mttTransfoIn)}</td>
						<!-- Total -->
						<td align="right" style="font-weight:bold;background-color: orange;">${bigDecimalUtil.formatNumberZeroBd(artTotaux.qteTotalApprovisionnement)}</td>
						<td align="right" style="font-weight:bold;background-color: orange;">${bigDecimalUtil.formatNumberZeroBd(artTotaux.mttTotalApprovisionnement)}</td>
						
						<!--  Sortie -->
						<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qteVenteCaisse)}</td>
						<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.mttVenteCaisse)}</td>
						
						<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qteVenteHorsCaisse)}</td>
						<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.mttVenteHorsCaisse)}</td>
						
						<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qteTransfertOut)}</td>
						<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.mttTransfertOut)}</td>
						
						<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qteTransfoOut)}</td>
						<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qteTransfoOut)}</td>
						
						<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qteAvoirCaisse)}</td>
						<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.mttAvoirCaisse)}</td>
						
						<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qtePerte)}</td>
						<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.mttPerte)}</td>
						
						<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qteConsommation)}</td>
						<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.mttConsommation)}</td>
											
						<!-- Total Sorties Aout -->
						<td align="right" style="font-weight:bold;background-color: orange;">${bigDecimalUtil.formatNumberZeroBd(artTotaux.qteTotalSortie)}</td>
						<td align="right" style="font-weight:bold;background-color: orange;">${bigDecimalUtil.formatNumberZeroBd(artTotaux.mttTotalSortie)}</td>
						
						<!-- Situation Th&eacute;orique -->
						<td align="right" style="font-weight:bold;background-color: orange;">${bigDecimalUtil.formatNumberZeroBd(artTotaux.qteTotalSituation)}</td>
						<td align="right" style="font-weight:bold;background-color: orange;">${bigDecimalUtil.formatNumberZeroBd(artTotaux.mttTotalSituation)}</td>
						
						<td style="font-weight:bold;background-color: orange;" align="right">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qteInvAct) }</td>
						<td style="font-weight:bold;background-color: orange;" align="right">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.mttInvAct) }</td>	
						
						<td align="right" style="background-color: orange;font-weight:bold;color: ${artTotaux.qteEcart<0?'red':'green'};">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qteEcart)}</td>
						<td align="right" style="background-color: orange;font-weight:bold;color: ${artTotaux.mttEcart<0?'red':'green'};">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.mttEcart)}</td>
					</tr>
				</c:if>
				
				
				<!-- Famille -->
				<c:if test="${article.familleStr.size() > 0}">
					<c:forEach var="i" begin="0" end="${article.familleStr.size()-1}">
						<c:if test="${empty oldFam or i>(oldFam.size()-1) or article.familleStr.get(i).code != oldFam.get(i).code}">
						     <tr>
								<td id="familleTd" colspan="39" noresize="true" style="font-size: 11px;font-weight: bold;color:green;padding-left: ${article.familleStr.get(i).level<=1?0:article.familleStr.get(i).level*10}px;background-color:#e3efff;">
								<span class="fa fa-fw fa-folder-open-o separator-icon"></span>  ${article.familleStr.get(i).code}-${article.familleStr.get(i).libelle}
							</td>
							</tr>
						</c:if>		
					</c:forEach>
				</c:if>
				<c:set var="oldFam" value="${article.familleStr }"></c:set>
				
				<!-- Article -->
				<c:if test="${empty oldArt or article.id != oldArt}">
				     <tr>
						<td colspan="39" noresize="true" style="font-weight: bold;font-size: 16px;color: #E91E63;">
							${article.code}-${article.getLibelleDataVal()}
						</td>
					</tr>
				</c:if>		
				<c:set var="oldArt" value="${article.id }"></c:set>
				
				<tr>
					<td style="font-weight: bold;color: #9C27B0;">
						${artInfos.emplacement.titre}
					</td>
					<td align="right">${bigDecimalUtil.formatNumberZeroBlank(article.prix_achat_ht)}</td>
					<td align="right">${bigDecimalUtil.formatNumberZeroBlank(article.prix_achat_ttc)}</td>
					<!-- Inventaire -->
					<td style="font-weight:bold;background-color: #cddc39;" align="right">${bigDecimalUtil.formatNumberZeroBlank(artInfos.qteInvPrec) }</td>
					<td style="font-weight:bold;background-color: #cddc39;" align="right">${bigDecimalUtil.formatNumberZeroBlank(artInfos.mttInvPrec) }</td>
					
					<!-- Entr�e -->
					<td align="right">${bigDecimalUtil.formatNumberZeroBlank(artInfos.qteTotalAchat)}</td>
					<td align="right">${bigDecimalUtil.formatNumberZeroBlank(artInfos.mttTotalAchat)}</td>
					<td align="right">${bigDecimalUtil.formatNumberZeroBlank(artInfos.qteTransfertIn)}</td>
					<td align="right">${bigDecimalUtil.formatNumberZeroBlank(artInfos.mttTransfertIn)}</td>
					<td align="right">${bigDecimalUtil.formatNumberZeroBlank(artInfos.qteTransfoIn)}</td>
					<td align="right">${bigDecimalUtil.formatNumberZeroBlank(artInfos.mttTransfoIn)}</td>
					<!-- Total -->
					<td align="right" style="font-weight:bold;">${bigDecimalUtil.formatNumberZeroBd(artInfos.qteTotalApprovisionnement)}</td>
					<td align="right" style="font-weight:bold;">${bigDecimalUtil.formatNumberZeroBd(artInfos.mttTotalApprovisionnement)}</td>
					
					<!--  Sortie -->
					<td align="right">${bigDecimalUtil.formatNumberZeroBlank(artInfos.qteVenteCaisse)}</td>
					<td align="right">${bigDecimalUtil.formatNumberZeroBlank(artInfos.mttVenteCaisse)}</td>
					
					<td align="right">${bigDecimalUtil.formatNumberZeroBlank(artInfos.qteVenteHorsCaisse)}</td>
					<td align="right">${bigDecimalUtil.formatNumberZeroBlank(artInfos.mttVenteHorsCaisse)}</td>
					
					<td align="right">${bigDecimalUtil.formatNumberZeroBlank(artInfos.qteTransfertOut)}</td>
					<td align="right">${bigDecimalUtil.formatNumberZeroBlank(artInfos.mttTransfertOut)}</td>
					
					<td align="right">${bigDecimalUtil.formatNumberZeroBlank(artInfos.qteTransfoOut)}</td>
					<td align="right">${bigDecimalUtil.formatNumberZeroBlank(artInfos.qteTransfoOut)}</td>
					
					<td align="right">${bigDecimalUtil.formatNumberZeroBlank(artInfos.qteAvoirCaisse)}</td>
					<td align="right">${bigDecimalUtil.formatNumberZeroBlank(artInfos.mttAvoirCaisse)}</td>
					
					<td align="right">${bigDecimalUtil.formatNumberZeroBlank(artInfos.qtePerte)}</td>
					<td align="right">${bigDecimalUtil.formatNumberZeroBlank(artInfos.mttPerte)}</td>
					
					<td align="right">${bigDecimalUtil.formatNumberZeroBlank(artInfos.qteConsommation)}</td>
					<td align="right">${bigDecimalUtil.formatNumberZeroBlank(artInfos.mttConsommation)}</td>
										
					<!-- Total Sorties Aout -->
					<td align="right" style="font-weight:bold;">${bigDecimalUtil.formatNumberZeroBd(artInfos.qteTotalSortie)}</td>
					<td align="right" style="font-weight:bold;">${bigDecimalUtil.formatNumberZeroBd(artInfos.mttTotalSortie)}</td>
					
					<!-- Situation Th&eacute;orique -->
					<td align="right" style="font-weight:bold;background-color: #d17fb0de">${bigDecimalUtil.formatNumberZeroBd(artInfos.qteTotalSituation)}</td>
					<td align="right" style="font-weight:bold;background-color: #d17fb0de">${bigDecimalUtil.formatNumberZeroBd(artInfos.mttTotalSituation)}</td>
					
					<td style="font-weight:bold;background-color: #cddc39;" align="right">${bigDecimalUtil.formatNumberZeroBlank(artInfos.qteInvAct) }</td>
					<td style="font-weight:bold;background-color: #cddc39;" align="right">${bigDecimalUtil.formatNumberZeroBlank(artInfos.mttInvAct) }</td>	
					
					<td align="right" style="font-weight:bold;color: ${artInfos.qteEcart<0?'red':'green'};">${bigDecimalUtil.formatNumberZeroBlank(artInfos.qteEcart)}</td>
					<td align="right" style="font-weight:bold;color: ${artInfos.mttEcart<0?'red':'green'};">${bigDecimalUtil.formatNumberZeroBlank(artInfos.mttEcart)}</td>
				</tr>
			</c:forEach>
			
			<c:set var="artTotaux" value="${mapRecapArt.get(oldArt) }" />
			<tr>
				<td></td>
				<td></td>
				<td></td>
				<!-- Inventaire -->
				<td style="font-weight:bold;background-color: orange;" align="right">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qteInvPrec) }</td>
				<td style="font-weight:bold;background-color: orange;" align="right">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.mttInvPrec) }</td>
				
				<!-- Entr�e -->
				<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qteTotalAchat)}</td>
				<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.mttTotalAchat)}</td>
				<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qteTransfertIn)}</td>
				<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.mttTransfertIn)}</td>
				<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qteTransfoIn)}</td>
				<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.mttTransfoIn)}</td>
				<!-- Total -->
				<td align="right" style="font-weight:bold;background-color: orange;">${bigDecimalUtil.formatNumberZeroBd(artTotaux.qteTotalApprovisionnement)}</td>
				<td align="right" style="font-weight:bold;background-color: orange;">${bigDecimalUtil.formatNumberZeroBd(artTotaux.mttTotalApprovisionnement)}</td>
				
				<!--  Sortie -->
				<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qteVenteCaisse)}</td>
				<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.mttVenteCaisse)}</td>
				
				<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qteVenteHorsCaisse)}</td>
				<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.mttVenteHorsCaisse)}</td>
				
				<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qteTransfertOut)}</td>
				<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.mttTransfertOut)}</td>
				
				<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qteTransfoOut)}</td>
				<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qteTransfoOut)}</td>
				
				<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qteAvoirCaisse)}</td>
				<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.mttAvoirCaisse)}</td>
				
				<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qtePerte)}</td>
				<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.mttPerte)}</td>
				
				<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qteConsommation)}</td>
				<td align="right" style="background-color: orange;">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.mttConsommation)}</td>
									
				<!-- Total Sorties Aout -->
				<td align="right" style="font-weight:bold;background-color: orange;">${bigDecimalUtil.formatNumberZeroBd(artTotaux.qteTotalSortie)}</td>
				<td align="right" style="font-weight:bold;background-color: orange;">${bigDecimalUtil.formatNumberZeroBd(artTotaux.mttTotalSortie)}</td>
				
				<!-- Situation Th&eacute;orique -->
				<td align="right" style="font-weight:bold;background-color: orange;">${bigDecimalUtil.formatNumberZeroBd(artTotaux.qteTotalSituation)}</td>
				<td align="right" style="font-weight:bold;background-color: orange;">${bigDecimalUtil.formatNumberZeroBd(artTotaux.mttTotalSituation)}</td>
				
				<td style="font-weight:bold;background-color: orange;" align="right">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qteInvAct) }</td>
				<td style="font-weight:bold;background-color: orange;" align="right">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.mttInvAct) }</td>	
				
				<td align="right" style="background-color: orange;font-weight:bold;color: ${artTotaux.qteEcart<0?'red':'green'};">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.qteEcart)}</td>
				<td align="right" style="background-color: orange;font-weight:bold;color: ${artTotaux.mttEcart<0?'red':'green'};">${bigDecimalUtil.formatNumberZeroBlank(artTotaux.mttEcart)}</td>
			</tr>
		</tbody>
	</table>
<br><br>