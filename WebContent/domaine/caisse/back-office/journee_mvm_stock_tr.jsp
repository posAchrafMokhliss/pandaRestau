<%@page import="framework.model.common.util.BigDecimalUtil"%>
<%@page import="appli.controller.domaine.stock.bean.MouvementBean"%>
<%@page import="framework.controller.ControllerUtil"%>
<%@ taglib uri="http://www.customtaglib.com/complexe" prefix="cplx"%>
<%@ taglib uri="http://www.customtaglib.com/standard" prefix="std"%>
<%@ taglib uri="http://www.customtaglib.com/html" prefix="html"%>
<%@ taglib uri="http://www.customtaglib.com/work" prefix="work"%>
<%@ taglib uri="http://www.customtaglib.com/fmt" prefix="fmt"%>
<%@ taglib uri="http://www.customtaglib.com/c" prefix="c"%>
<%@page errorPage="/commun/error.jsp"%>

<%
	MouvementBean mvmBean = (MouvementBean)request.getAttribute("mouvementBean");
%>
<style>
	.control-label{
	margin-top: -4px;
	}
</style>
	<!-- widget grid -->
	<div class="widget" style="width: 95%;margin: 5px;">
         <div class="widget-body" style="border-radius: 15px;">
			<div class="row">
				<table style="width: 97%;margin-left: 20px;">
					<tr>
						<th>Composant</th>
						<th width="100px;" style="text-align: center;">Quantit&eacute;</th>
						<th width="140px" style="text-align: center;">Valeur achat</th>
					</tr>
					
					<c:set var="totalAchat" />
					
					<c:set var="bigDecimalUtil" value="<%=new BigDecimalUtil() %>" />
					
					<c:forEach items="${mouvementBean.list_article }" var="articleMvm">
						<c:set var="totalAchat" value="${bigDecimalUtil.add(totalAchat, articleMvm.prix_ttc_total) }" />
						
						<tr style="height: 10px;">
							<td style="padding-top: 5px; padding-right: 10px;" valign="top">
								${articleMvm.opc_article.getLibelleDataVal()}
							</td>
							<td style="padding-top: 5px; padding-right: 10px;" valign="top" align="right">
								<fmt:formatDecimal value="${articleMvm.quantite}" />
							</td>
							<td style="padding-top: 5px; padding-right: 10px;" valign="top" align="right">
								<fmt:formatDecimal value="${articleMvm.prix_ttc_total}"/>
							</td>
						</tr>
					</c:forEach>
						<tr>
							<td align="right" style="font-weight: bold;">Total</td>
							<td></td>
							<td align="right" style="font-weight: bold;padding-top: 5px; padding-right: 10px;"><fmt:formatDecimal value="${totalAchat }"/></td>
						</tr>
				</table>
			</div>
		</div>
	</div>
<br>