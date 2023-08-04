<%@page import="framework.model.common.util.EncryptionUtil"%>
<%@ taglib uri="http://www.customtaglib.com/standard" prefix="std"%>
<%@ taglib uri="http://www.customtaglib.com/fmt" prefix="fmt"%>
<%@ taglib uri="http://www.customtaglib.com/html" prefix="html"%>
<%@ taglib uri="http://www.customtaglib.com/work" prefix="work"%>
<%@ taglib uri="http://www.customtaglib.com/c" prefix="c"%>
<%@page errorPage="/commun/error.jsp"%>

<style>
#journee_body .databox .databox-text {
	font-size: 12px;
}
</style>

<!-- Page Breadcrumb -->
<div class="page-breadcrumbs breadcrumbs-fixed">
	<ul class="breadcrumb">
		<li><i class="fa fa-home"></i> <a href="#">Accueil</a></li>
		<li>Gestion des journées</li>
		<li class="active">Fiche journée</li>
	</ul>
</div>

<div class="page-header position-relative">
	<div class="header-title" style="padding-top: 4px;"></div>
	<!--Header Buttons-->
	<jsp:include page="/WEB-INF/fragment/shortcut.jsp"></jsp:include>
	<!--Header Buttons End-->
</div>
<!-- /Page Header -->

<div class="page-body" id="journee_body">
	<div class="row">
		<jsp:include page="/WEB-INF/commun/center/banner_message.jsp"></jsp:include>
	</div>

	<!-- widget grid -->
	<div class="widget">
		<std:form name="data-form">
			<div class="row">
				<div class="col-lg-12 col-sm-12 col-xs-12">
					<%
					request.setAttribute("tabName", "cuisine");
					request.setAttribute("pAction", EncryptionUtil.encrypt("caisse.wizardInstall.initCaisse"));
					request.setAttribute("sAction", EncryptionUtil.encrypt("caisse.wizardInstall.initTicket"));
					%>
					<jsp:include page="wizard.jsp" />

					<div class="step-content" id="simplewizardinwidget-steps">
						<c:forEach items="${listParams }" var="parametre">
							<c:if test="${parametre.groupe == 'CAISSE_CUISINE_PIL'}">
								<div class="form-group">
									<std:label classStyle="control-label col-md-5" value="${parametre.libelle}" />
									<div class="col-md-7">
										<c:choose>
											<c:when test="${parametre.code=='ECRAN_CMD_VALIDE'}">
												<std:select type="string" name="param_${parametre.code}" required="true" data="${dataSourceStatut }" value="${parametre.valeur }" />
											</c:when>
											<c:when test="${parametre.code=='ECRAN_CMD_ENPREPARATION'}">
												<std:select type="string" name="param_${parametre.code}" required="true" data="${dataSourceStatut }" value="${parametre.valeur }" />
											</c:when>
											<c:when test="${parametre.code=='ECRAN_STRATEGIE'}">
												<std:select type="string" name="param_${parametre.code}" required="true" data="${dataStrategieEcran }" value="${parametre.valeur }" />
											</c:when>
											<c:when test="${parametre.code=='MODE_TRAVAIL_CUISINE'}">
												<std:select type="string" name="param_${parametre.code}" required="true" data="${listModeTravail }" value="${parametre.valeur }" />
											</c:when>
											<c:when test="${parametre.code=='ECRAN_STATUT'}">
												<std:select type="string[]" name="param_${parametre.code}" data="${dataStatut }" value="${paramStatutArray }" multiple="true" />
											</c:when>

											<c:when test="${parametre.type=='STRING'}">
												<std:text name="param_${parametre.code}" type="string" style="width:50%;float:left;" maxlength="120" value="${parametre.valeur}" />
											</c:when>
											<c:when test="${parametre.type=='TEXT'}">
												<std:textarea name="param_${parametre.code}" style="width:50%;float:left;" rows="3" value="${parametre.valeur}" />
											</c:when>
											<c:when test="${parametre.type=='NUMERIC'}">
												<std:text name="param_${parametre.code}" type="long" style="width:120px;float:left;" maxlength="120" value="${parametre.valeur}" />
											</c:when>
											<c:when test="${parametre.type=='DECIMAL'}">
												<std:text name="param_${parametre.code}" type="decimal" style="width:120px;float:left;" maxlength="120" value="${parametre.valeur}" />
											</c:when>
											<c:when test="${parametre.type=='BOOLEAN'}">
												<std:checkbox name="param_${parametre.code}" checked='${parametre.valeur }' />
											</c:when>
										</c:choose>
										<c:if test="${parametre.help != null && parametre.help != ''}">
											<img class="tooltip-lg" data-toggle="tooltip" data-placement="top" data-original-title="${parametre.help}" src="resources/framework/img/info.png" style="vertical-align: bottom;" />
										</c:if>
									</div>
								</div>
							</c:if>
						</c:forEach>
					</div>
				</div>
			</div>
		</std:form>
	</div>
</div>
