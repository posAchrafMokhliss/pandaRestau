<%@page import="appli.controller.domaine.util_erp.ContextAppli"%>
<%@page import="framework.model.common.util.StrimUtil"%>
<%@ taglib uri="http://www.customtaglib.com/complexe" prefix="cplx"%>
<%@ taglib uri="http://www.customtaglib.com/standard" prefix="std"%>
<%@ taglib uri="http://www.customtaglib.com/html" prefix="html"%>
<%@ taglib uri="http://www.customtaglib.com/work" prefix="work"%>
<%@ taglib uri="http://www.customtaglib.com/c" prefix="c"%>
<%@page errorPage="/commun/error.jsp"%>

<%
	String tp = (String)request.getAttribute("tp");
String labelBtn = (tp.equals("ouv") ? "Clore": "Ouvrir")+" le shift";
boolean isServeurProfil = ContextAppli.getUserBean().isInProfile("SERVEUR");
%>

<std:form name="data-form">
	<!-- widget grid -->
	<div class="widget" id="shift-div">
		<div class="widget-header bordered-bottom bordered-blue">
			<span class="widget-caption"><%=labelBtn %></span>
			<div class="widget-buttons buttons-bordered" style="margin-bottom: 10px;">
         	</div>
		</div>
		<div class="widget-body">
			<div class="row">
				<jsp:include page="/WEB-INF/commun/center/banner_message.jsp"></jsp:include>
			</div>

		<div class="row" style="text-align: center;">
			<div class="col-md-12">
			<%
			tp = "tp="+tp;
			%>
				<std:button actionGroup="M" classStyle="btn btn-success" action="caisse-web.caisseWeb.ouvrirCloreShift" params="<%=tp %>" icon="fa-save" value="<%=labelBtn %>" />
				<button type="button" id="close_modal" class="btn btn-primary" data-dismiss="modal">
					<i class="fa fa-times"></i> Fermer
				</button>
			</div>
		</div>
	</div>
</div>
</std:form>