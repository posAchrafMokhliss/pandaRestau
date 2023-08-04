<%@page import="framework.model.common.util.EncryptionUtil"%>
<%@page import="framework.model.common.util.StrimUtil"%>
<%@page import="appli.controller.domaine.util_erp.ContextAppli"%>
<%@page import="framework.model.common.util.StringUtil"%>
<%@page import="framework.controller.ContextGloabalAppli"%>
<%@page import="appli.controller.domaine.personnel.bean.PlanningBean"%>
<%@page import="framework.controller.ControllerUtil"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="framework.model.common.util.BooleanUtil"%>
<%@ taglib uri="http://www.customtaglib.com/complexe" prefix="cplx"%>
<%@ taglib uri="http://www.customtaglib.com/standard" prefix="std"%>
<%@ taglib uri="http://www.customtaglib.com/html" prefix="html"%>
<%@ taglib uri="http://www.customtaglib.com/work" prefix="work"%>
<%@ taglib uri="http://www.customtaglib.com/c" prefix="c"%>
<%@page errorPage="/commun/error.jsp"%>

<style>
#generic_modal_body{
	width: 680px;
	margin-left: -10%;
}
.note-editable{
	height: 95px !important;
}
#client_dic input{
	height: 25px !important;
	background-color: white;
}
</style>

<%
boolean isRestau = ContextAppli.IS_RESTAU_ENV();
%>
<script type="text/javascript">

	getTabElement("#generer_code").click(function(){
		executePartialAjax($(this), '<%=EncryptionUtil.encrypt("caisse-web.calendrier.generatTime")%>', 'heure_fin', true, true, null, true);
	});


	function manageLibCalendrier(){
		var tp = $("#planning\\.opc_type_planning\\.id option:selected").text();
		var empl = $("#planning\\.clients_array option:selected").text();
		var lieu = '';
		<%if(isRestau){ %>
			lieu = $("#planning\\.lieu_array option:selected").text();
		<%}%>
		var txt = ($("#heure_debut").val()!=''?$("#heure_debut").val():'')
							+ ($("#heure_fin").val()!=''?'-'+$("#heure_fin").val():'');
		
		$("#planning\\.titre").val(
				txt
				+ ($.trim(tp)!=''?" || "+tp:'')
				+ ($.trim(empl)!=''?" || "+empl:'')
				+ ($.trim(lieu)!=''?" || "+lieu:'')
			);	
	}
	//
	$(document).ready(function (){
		$("#client\\.opc_ville\\.id").select2();
	});
</script>

<%
PlanningBean planningB = (PlanningBean)request.getAttribute("planning");
String currLieu = (String)request.getAttribute("currLieu");
Map<String, List<String>> mapLieu = (Map<String, List<String>>)request.getAttribute("mapLieu");
Map<String, String> mapLieuDt = (Map<String, String>)request.getAttribute("mapLieuDt");
boolean isReadOnly = !ControllerUtil.isEditionWritePage(request);
%>
<%	
boolean isPortefeuille =  StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("PORTEFEUILLE"));
boolean isPoints =  StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("POINTS"));
boolean isServeur = ContextAppli.getUserBean().isInProfile("SERVEUR");
%>

<std:form name="data-form">
	<!-- widget grid -->
	<div class="widget">
		<div class="widget-header bordered-bottom bordered-blue">
			<span class="widget-caption">Fiche réservation</span>
			<div class="widget-buttons buttons-bordered" style="margin-bottom: 10px;">
        		<i class="fa fa-keyboard-o" style="font-size: 20px;"></i>         
        		<label>
                 <input class="checkbox-slider toggle colored-blue" type="checkbox" id="keyboard-activator" style="display: none;">
                 <span class="text"></span>
             </label>
        	</div>
        	
			<std:link actionGroup="U" targetDiv="generic_modal_body" classStyle="btn btn-default" action="caisse-web.calendrier.work_init_update" workId="${planning.id}" icon="fa fa-pencil" tooltip="Modifier" />
		</div>
		<div class="widget-body" style="padding-right: 45px;">
			<div class="row">
				<jsp:include page="/WEB-INF/commun/center/banner_message.jsp"></jsp:include>
			</div>
			<div class="row">
				<div class="form-group">
					<std:label classStyle="control-label col-md-3" value="Date d&eacute;but" />
					<div class="col-md-3">
						<std:date name="planning.date_debut" required="true" />
					</div>
					<div id="dateFin-div">
					<std:label classStyle="control-label col-md-3" value="Date fin" />
					<div class="col-md-3">
						<std:date name="planning.date_fin"/>
					</div>
					</div>
				</div>
				<div class="form-group" id="heure-div">
					<std:label classStyle="control-label col-md-3" value="Heure d&eacute;but" />
					<div class="col-md-3">
						<std:text mask="99:99" type="string" required="true" name="heure_debut" style="width:60px;" value="${heure_debut }"/>
					</div>
					<std:label classStyle="control-label col-md-3" value="Heure fin" />
					<div class="col-md-3">
						<std:text mask="99:99" type="string" name="heure_fin" style="width:60px;" value="${heure_fin }"/>
					    <a class="refresh-num" style=" position: absolute;right: 6em;top: 0.8em;" id="generer_code" href="javascript:" title="G&eacute;n&eacute;rer un nouveau code">
				            	<i class="fa fa-refresh"></i>
				        </a>
					</div>
				</div>
				<%if(request.getAttribute("tp") == "edit"){ %>
				<std:textarea name="all_planning" readOnly="true" style="width:500px;margin-left: 12em;" rows="3" value="${all_planning}" />
				<%} %>
				<div class="form-group">
					<std:label classStyle="control-label col-md-3" value="Type" />
					<div class="col-md-9">
						<std:select type="string[]" key="id" isTree="true" name="planning_opc_type_planning" data="${listTypePlanning }" labels="libelle" required="true" multiple="true"  width="100%"/>
					</div>
						
				</div>
				<div class="form-group">
					<std:label classStyle="control-label col-md-3" value="Client(s)" />
					<div class="col-md-9">
						<std:select type="string[]" name="planning.clients_array" multiple="true" data="${listClient }" labels="nom;' ';prenom" key="id" width="100%"/>
						<a href="javascript:" onclick="$('#client_dic').toggle(1000);">Ajouter</a>
					</div>
				</div>
				
				
				
				<div class="form-group" id="client_dic" style="display: none;margin-left: 51px;margin-right: 31px;border: 1px solid #aeaeae;padding-left: 26px;padding-right: 23px;border-radius: 10px;">
							<%
								request.setAttribute("colLab", 0); 
								request.setAttribute("colInput", 10);
								request.setAttribute("forceW", true);
							%>
							<div class="row" style="margin-top: 10px;margin-left: 16px;">
								<jsp:include page="/domaine/administration/dataValue_form.jsp" />
							</div>
							<div class="row" style="margin-top: 10px;">
								<div class="col-md-10">
									<std:text name="client.cin" type="string" placeholder="CIN" forceWriten="true" style="float: left;width: 130px;"/>
								</div>
							</div>
							<div class="row">	
								<div class="col-md-12">
									<std:text name="client.telephone" type="string" placeholder="T&eacute;l&eacute;phone" style="width:130px;float: left;" maxlength="20" forceWriten="true" />
								</div>
							</div>
							<div class="row">	
								<div class="col-md-10">
									<std:text name="client.nom" type="string" placeholder="Nom *" forceWriten="true" style="width:80%;float: left;border-bottom: 1px solid #FF9800 !important;border-bottom-style: dashed !important;"/>
								</div>
							</div>
							<div class="row">	
								<div class="col-md-10">
									<std:text name="client.prenom" type="string" placeholder="Pr&eacute;nom" forceWriten="true"/>
								</div>
							</div>	
							<div class="row">	
								<div class="col-md-12">
									<std:text name="client.mail" type="string" validator="email" placeholder="Mail" maxlength="50" forceWriten="true" />
								</div>
							</div>	
							<div class="row">	
								<div class="form-title">Adresse</div>
								<div class="col-md-12">
									<std:text name="client.adresse_rue" type="string" placeholder="Rue" maxlength="120" forceWriten="true" />
								</div>
							</div>
							<div class="row">	
								<div class="col-md-12">
									<std:text name="client.adresse_compl" type="string" placeholder="Compl&eacute;ment" maxlength="120" forceWriten="true" />
								</div>
							</div>
							<div class="row">	
								<div class="col-md-12">
									<std:select name="client.opc_ville.id" type="long" data="${listVille }" key="id" labels="libelle" classStyle="form-control" placeholder="Ville" groupKey="opc_region.id" groupLabels="opc_region.libelle" style="width:80%;" forceWriten="true" />
								</div>
							</div>
							
							<%
							if(isPortefeuille && isPoints && !isServeur){
							%>
							<div class="row">	
								<div class="form-title">Affecter carte</div>
								<div class="col-md-12">
									<std:select forceWriten="true" name="carte_id" type="long" data="${liste_carte }" key="id" labels="libelle" style="width:100%;" value="${carte_id }"/>
								</div>
							</div>
							<div class="row">	
								<div class="col-md-6">
									Activer portefeuille <std:checkbox forceWriten="true" name="client.is_portefeuille" checked="${is_portefeuille }" style="vertical-align: middle;" />
								</div>
								<div class="col-md-6">
									Autoriser solde négatif <std:checkbox forceWriten="true" name="client.is_solde_neg" value="${is_solde_neg }" style="vertical-align: middle;" />
								</div>
							</div>	
							<%
							}
							%>
				</div>
				
				
			<%if(isRestau){ %>	
				<div class="form-group">
					<std:label classStyle="control-label col-md-3" value="Lieu" />
					<div class="col-md-9">
						<select <%=isReadOnly?" disabled='disabled'":"" %> name="planning.lieu_array[]" id="planning.lieu_array" multiple="multiple" class="select2" style="width: 100%;">
						<%
							for(String lieu : mapLieu.keySet()){
								List<String> listDet = mapLieu.get(lieu);
						%>
						<optgroup label="<%=lieu%>"></optgroup>
						<% for(String det : listDet){ 
							String selected = "";
							if(planningB != null && planningB.getLieu_array() != null){
								for(String l : planningB.getLieu_array()){
									if(l.equals(det)){
										selected = " selected='selected'";
									}
								}
							}
							if(currLieu!=null && currLieu.equals(det)){
								selected = " selected='selected'";
							}
						%>
							<option <%=selected %> value="<%=det%>"><%=det+StringUtil.getValueOrEmpty(mapLieuDt.get(det)) %></option>
						<%} 
						}%>						
						
						</select>
					</div>
				</div>
		<%} %>		
				
				<%if(!isReadOnly){ %>
				<div class="form-group">
					<std:label classStyle="control-label col-md-3" value="Titre" />
					<div class="col-md-8">
						<std:text type="string" name="planning.titre" placeholder="Titre" required="true" maxlength="150" style="width:80%;float:left;"/>
						<a href="javascript:" onclick="$('#planning\\.titre').val('');" style="padding: 3px;
						    background-color: #4fc3f7;
						    line-height: 29px;
						    font-size: 20px;
						    border-radius: 8px;
						    margin-right: 5px;
						    float: left;"><i class="fa fa-fw fa-times-circle"></i></a>
					    <a href="javascript:" onclick="manageLibCalendrier();" style="padding: 3px;
						    background-color: #4fc3f7;
						    line-height: 29px;
						    font-size: 20px;
						    border-radius: 8px;
						    float: left;"><i class="fa fa-fw fa-refresh"></i></a>
					</div>
				</div>
				<%} %>
				<div class="form-group">
					<std:label classStyle="control-label col-md-3" value="Commentaire" />
					<div class="col-md-9">
						<std:textarea-rich name="planning.description" />
					</div>
				</div>
			</div>	
			<div class="row" style="text-align: center;">
				<div class="col-md-12">
					<std:button actionGroup="M" classStyle="btn btn-success" closeOnSubmit="true" action="caisse-web.calendrier.work_merge" targetDiv="right-div" workId="${planning.id }" icon="fa-save" value="Sauvegarder" />
					<std:button actionGroup="D" classStyle="btn btn-danger" closeOnSubmit="true" action="caisse-web.calendrier.work_delete" targetDiv="right-div" workId="${planning.id }" icon="fa fa-trash-o" value="Supprimer" />
					<button type="button" id="close_modal" class="btn btn-primary" data-dismiss="modal">
						<i class="fa fa-times"></i> Fermer
					</button>
				</div>
			</div>
		</div>
	</div>
</std:form>