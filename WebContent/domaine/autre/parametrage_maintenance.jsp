<%@page import="java.util.Map"%>
<%@page import="framework.model.common.util.BigDecimalUtil"%>
<%@page import="java.math.BigDecimal"%>
<%@page import="framework.model.common.util.EncryptionUtil"%>
<%@ taglib uri="http://www.customtaglib.com/complexe" prefix="cplx"%>
<%@ taglib uri="http://www.customtaglib.com/standard" prefix="std"%>
<%@ taglib uri="http://www.customtaglib.com/fmt" prefix="fmt"%>
<%@ taglib uri="http://www.customtaglib.com/html" prefix="html"%>
<%@ taglib uri="http://www.customtaglib.com/work" prefix="work"%>
<%@ taglib uri="http://www.customtaglib.com/fn" prefix="fn"%>
<%@ taglib uri="http://www.customtaglib.com/c" prefix="c"%>
<%@page errorPage="/commun/error.jsp"%>

<style>
   .form-title{ 
    	margin-left: 12px;
	}
</style>

<script type="text/javascript">
	$(document).ready(function (){
		$("#dump_lnk").click(function(){
			var triggeur = $(this);
            bootbox.prompt("Chemin pour stocker le fichier g&eacute;n&eacute;r&eacute;", function (result) {
                if (result === null) {
                   
                } else {
                	submitAjaxForm('<%=EncryptionUtil.encrypt("admin.parametrage.dumper_base")%>', 'path='+result, $("#data-form"), triggeur);
                }
            });
		});
		$("#purge_lnk").click(function(){
			if($("#calcul_span").css("display") != "none"){
				alertify.error("Un traitement de purge est d&eacute;j&agrave; en cours veuillez patienter.");
				return;
			}
			showConfirmDeleteBox('<%=EncryptionUtil.encrypt("admin.parametrage.purger_base")%>', 'etat='+$(this).attr('curr'), $(this), "Vous &ecirc;tes sur le point de purger les donn&eacute;es.<br>Voulez-vous confirmer ?", "startAnimPurge()", "Purge des donn&eacute;es");
		});
	});
	
	function startAnimPurge(){
		$("#calcul_span").show();
	}
</script>	
	
<!-- Page Breadcrumb -->
<div class="page-breadcrumbs breadcrumbs-fixed">
	<ul class="breadcrumb">
		<li><i class="fa fa-home"></i> <a href="#">Accueil</a></li>
		<li>Param&eacute;trage</li>
		<li class="active">Maintenance</li>
	</ul>
</div>

<div class="page-header position-relative">
	<div class="header-title" style="padding-top: 4px;">
	</div>
	<!--Header Buttons-->
	<jsp:include page="/WEB-INF/fragment/shortcut.jsp"></jsp:include>
	<!--Header Buttons End-->
</div>
<!-- /Page Header -->

<div class="page-body">
	<div class="row">
		<jsp:include page="/WEB-INF/commun/center/banner_message.jsp"></jsp:include>
	</div>
	<!-- widget grid -->
	<div class="widget">
		<std:form name="data-form">
		
			<div class="row">
		        <div class="col-lg-12 col-sm-12 col-xs-12">
		              <div class="tabbable">
		                    <ul class="nav nav-tabs" id="myTab">
		                          <li>
		                              <a data-toggle="tab" href="#data" wact="<%=EncryptionUtil.encrypt("admin.parametrage.work_edit")%>" params="tp=def">
		                               G&eacute;n&eacute;rale
		                              </a>
		                           </li>
		                           <li>
		                              <a data-toggle="tab" href="#data" wact="<%=EncryptionUtil.encrypt("admin.parametrage.work_edit")%>" params="tp=cai">
		                               Caisse
		                              </a>
		                           </li>
		                            <li>
		                              <a data-toggle="tab" href="#data" wact="<%=EncryptionUtil.encrypt("admin.token.work_find")%>">
		                               Tokens
		                              </a>
		                            </li>
		                            <li>
		                              <a data-toggle="tab" href="#data" wact="<%=EncryptionUtil.encrypt("admin.parametrage.work_edit")%>" params="tp=ihm">
		                               Interface graphique
		                              </a>
		                            </li>
		                            <li>
		                              <a data-toggle="tab" href="#data" wact="<%=EncryptionUtil.encrypt("admin.parametrage.work_edit")%>" params="tp=div">
		                               Divers
		                              </a>
		                            </li>
		                            <li>
					                   <a data-toggle="tab" href="#data" wact="<%=EncryptionUtil.encrypt("admin.parametrage.work_edit")%>" params="tp=rst">
					                    Etablissement
					                   </a>
					                 </li>
					                  <li>
                 					   <a data-toggle="tab" href="#data" wact="<%=EncryptionUtil.encrypt("admin.parametrage.work_edit")%>" params="tp=aff">
                    				    Afficheur
                   					   </a>
                 					 </li>
					                 <li class="active">
					                   <a data-toggle="tab" href="#data" wact="<%=EncryptionUtil.encrypt("admin.parametrage.work_edit")%>" params="tp=mnt">
					                    Maintenance
					                   </a>
					                 </li>
		                     </ul>
		                </div>
		          </div>
		      </div>
		
			<div class="widget-body">
				<div class="row">
					<div class="alert alert-warning fade in">
	                    <button class="close" data-dismiss="alert">
	                        x
	                    </button>
	                    <i class="fa-fw fa fa-warning"></i>
	                    <strong style="color: orange;text-align: center;">Attention :  Utiliser ce module avec pr&eacute;caution car il impacte la base de donn&eacute;es.</strong><br>
	                    <b>Purge des donn&eacute;es</b><br>
	                    <i class="fa fa-info-circle" style="color: red;"></i> La purge des donn&eacute;es est irr&eacute;versible.
	                    <i class="fa fa-info-circle" style="color: red;"></i> Les donn&eacute;es purg&eacute;es sont celles qui ne sont plus utiles pour les calculs car une fois le mois cl&ocirc;tur&eacute; car les chiffres calcul&eacute;s sont stock&eacute;s.
	                    <i class="fa fa-info-circle" style="color: red;"></i> Un mois purg&eacute; ne peut plus &ecirc;tre r&eacute;-ouvert.
	                    <i class="fa fa-info-circle" style="color: red;"></i> La purge doit se faire de pr&eacute;f&eacute;rence, quand aucune op&eacute;ration n'est en cours dans l'application.
	                    <br><b>Sauvegarde de la base</b><br>
	                    <i class="fa fa-info-circle" style="color: red;"></i> La sauvegarde de la base permet sa restitution en cas d'erreur de purge et avant la r&eacute;utilisation du systeme.
	                    <i class="fa fa-info-circle" style="color: red;"></i> La base sauvegard&eacute;e peut aussi servir pour consulter l'historique des donn&eacute;es purg&eacute;es.
	                    <i class="fa fa-info-circle" style="color: red;"></i> La base sauvegard&eacute;e doit &ecirc;tre mont&eacute;e avant son utilisation via une retauration technique.
	                </div>
	              </div>
	              <div class="row">
	              	<div class="col-lg-6 col-sm-6 col-xs-12">
		              	<div class="widget">
			                  <div class="widget-header bg-palegreen">
			                      <i class="widget-icon fa fa-check"></i>
			                      <span class="widget-caption">Informations application</span>
			                  </div><!--Widget Header-->
			                  <div class="widget-body">
			                   <%
			                  Map<String, String> systemInfos = (Map<String, String>)request.getAttribute("systemInfos");
			                  %>
			                  <table><tr><td>
			                  	Taille de la base de donn&eacute;es :  
			                  	</td>
			                  	<td style="font-weight: bold;text-align: right;">&nbsp; <%=systemInfos.get("DB_SIZE")%> Mo</td>
			                  	</tr>
			                  	<tr><td align="right">
			                    Taille des images et des fichiers : </td>
			                    <td style="font-weight: bold;text-align: right;">&nbsp; <%=systemInfos.get("FILES_SIZE")%> Mo</td>
			                    </tr>
			                     <tr><td>
			                    M&eacute;moire RAM application : </td>
			                    <td style="font-weight: bold;text-align: right;">&nbsp; <%=systemInfos.get("RAM_SYSTEM")%> Go</td>
			                    </tr>
			                    </table>
			                  </div><!--Widget Body-->
			              </div>
	              		</div>
	              		<div class="col-lg-6 col-sm-6 col-xs-12">
		              	<div class="widget">
			                  <div class="widget-header bg-palegreen">
			                      <i class="widget-icon fa fa-check"></i>
			                      <span class="widget-caption">Informations syst&egrave;me</span>
			                  </div><!--Widget Header-->
			                  <div class="widget-body">
			                  <table>
			                    <tr><td>
			                    Disque dur : </td>
			                    <td style="font-weight: bold;text-align: right;">&nbsp; <%=systemInfos.get("HDD_SIZE")%> Go</td>
			                    </tr>
			                    <tr><td>
			                    M&eacute;moire RAM : </td>
			                    <td style="font-weight: bold;text-align: right;">&nbsp; <%=systemInfos.get("RAM_SERVEUR_SIZE")%> Go</td>
			                    </tr>
			                    <tr><td>
			                    Syst&egrave;me : </td>
			                    <td style="font-weight: bold;text-align: right;">&nbsp; <%=systemInfos.get("OS_NAME")%></td>
			                    </tr>
			                    </table>
			                  </div><!--Widget Body-->
			              </div>
	              		</div>
	              </div>
	              <div class="row">
	              	<div class="col-lg-6 col-sm-6 col-xs-12">
		              	<div class="widget">
			                  <div class="widget-header bg-lightred">
			                      <i class="widget-icon fa fa-check"></i>
			                      <span class="widget-caption">Sauvegarde des donn&eacute;es</span>
			                  </div><!--Widget Header-->
			                  <div class="widget-body">
			                     <a id="dump_lnk" style="width: 250px;text-align: left;" class="btn btn-primary" href="javascript:void(0);"><i class="fa fa-shopping-cart"></i> Sauvegarder la base de donn&eacute;es</a>
			                     <img class="tooltip-lg" data-toggle="tooltip" data-placement="top" data-original-title="Exporter un dump de la base de donn&eacute;es." src="resources/framework/img/info.png" style="vertical-align: bottom;"/>
			                  </div><!--Widget Body-->
			              </div>
	              		</div>
	              		<div class="col-lg-6 col-sm-6 col-xs-12">
	              			<div class="widget">
			                  <div class="widget-header bg-gold">
			                      <i class="widget-icon fa fa-check"></i>
			                      <span class="widget-caption">Purge des donn&eacute;es</span>
			                  </div><!--Widget Header-->
			                  <div class="widget-body">
			                  	<c:choose>
			                  		<c:when test="${empty moisPurge }">
			                  			Aucun mois n'est disponible pour la purge des donn&eacute;es.
			                  		</c:when>
			                  		<c:otherwise>
			                  			<table style="width: 100%;">
			                  				<tr style="border-bottom: 1px dashed #427fed;">
			                  					<td>Ventes caisses</td>
			                  					<td><std:checkbox name="purge_VEC" checked="true" forceWriten="true" /></td>
			                  					<td>Ventes hors caisses</td>
			                  					<td><std:checkbox name="purge_VEA" checked="true" forceWriten="true"/></td>
			                  				</tr>
			                  				<tr style="border-bottom: 1px dashed #427fed;">	
			                  					<td>Achats</td>
			                  					<td><std:checkbox name="purge_ACH" checked="true" forceWriten="true"  /></td>
			                  					<td>D&eacute;penses</td>
			                  					<td><std:checkbox name="purge_DEP" checked="true" forceWriten="true"  /></td>
			                  				</tr>
			                  				<tr style="border-bottom: 1px dashed #427fed;">	
			                  					<td>Recettes</td>
			                  					<td><std:checkbox name="purge_REC" checked="true" forceWriten="true"  /></td>
			                  					<td>Mouvements stock
			                  					<i class="fa fa-info-circle" data-toggle="tooltip" data-placement="top" data-original-title="Cette purge concerne : avoirs, pr&eacute;parations, transferts, consommations, ch&egrave;ques fournisseurs"></i>
			                  					</td>
			                  					<td><std:checkbox name="purge_MVM" checked="true" forceWriten="true"  /></td>
			                  				</tr>
			                  				<tr style="border-bottom: 1px dashed #427fed;">	
			                  					<td>Inventaires</td>
			                  					<td><std:checkbox name="purge_INV" checked="true" forceWriten="true"  /></td>
			                  					<td>Ecritures comptables</td>
			                  					<td><std:checkbox name="purge_ECR" checked="true" forceWriten="true"  /></td>
			                  				</tr>
			                  				<tr style="border-bottom: 1px dashed #427fed;">	
			                  					<td>Journ&eacute;es</td>
			                  					<td><std:checkbox name="purge_JOU" checked="true" forceWriten="true"  /></td>
			                  				</tr>	
			                  			</table>
			                  		
			                  		
			                  			<c:set var="encryptUtil" value="<%=new EncryptionUtil() %>" />
			                     		<a id="purge_lnk" class="btn btn-danger" href="javascript:void(0);" style="margin-left: 25%;margin-top:12px;" curr="${encryptUtil.encrypt(moisPurge.id) }"><i class="fa fa-times"></i> 
			                     			Purger les donn&eacute;es du mois [<span style="color:yellow;font-size: 13px !important;height: 22px;font-weight: bold;"><fmt:formatDate value="${moisPurge.date_etat }" pattern="MM/yyyy"/></span>]
			                     		</a>
			                     		<span id="calcul_span" style="width: 100%;float: left;margin-bottom: 17px;color: #F44336;font-style: italic;display:none;"><img src='resources/framework/img/select2-spinner.gif' /> Patientez, cette op&eacute;ration prendra plusieurs minutes ... </span>
			                     	<br>
			                     	<span style="font-size: 11px;color: fuchsia;">* Par pr&eacute;caution, pensez &agrave; r&eacute;aliser un inventaire et &agrave; faire une sauvegarde des donn&eacute;es</span>
			                  		</c:otherwise>
			                  	</c:choose>
			                  
			                  </div><!--Widget Body-->
			              </div>
	              		</div>
	              	</div>
			</div>	
			<hr>
			<div class="form-actions">
				<div class="row" style="text-align: center;" class="col-md-12" id="action-div">
					<std:button actionGroup="M" classStyle="btn btn-success" action="admin.parametrage.work_update" params="tp=div" icon="fa-save" value="Sauvegarder" />
				</div>
			</div>
		</std:form>
	</div>
</div>

<script type="text/javascript">
/*Handles ToolTips*/
$("[data-toggle=tooltip]")
    .tooltip({
        html: true
    });
</script>    