<%@page import="framework.model.common.util.DateUtil"%>
<%@page import="java.util.Calendar"%>
<%@page import="framework.model.common.util.BigDecimalUtil"%>
<%@page import="java.math.BigDecimal"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="framework.model.common.util.ServiceUtil"%>
<%@page import="framework.model.common.util.EncryptionUtil"%>
<%@page import="framework.controller.ControllerUtil"%>
<%@ taglib uri="http://www.customtaglib.com/complexe" prefix="cplx"%>
<%@ taglib uri="http://www.customtaglib.com/standard" prefix="std"%>
<%@ taglib uri="http://www.customtaglib.com/html" prefix="html"%>
<%@ taglib uri="http://www.customtaglib.com/work" prefix="work"%>
<%@ taglib uri="http://www.customtaglib.com/fmt" prefix="fmt"%>
<%@ taglib uri="http://www.customtaglib.com/c" prefix="c"%>
<%@ taglib uri="http://www.customtaglib.com/c" prefix="fn"%>
<%@page errorPage="/commun/error.jsp" %>

<style>
#list_vehicule th{
	background-color: #c8f4ff;
    text-align: left;
}
.bg-class{
    background-color: #f3f4f5; 
}
</style>
		
<%
Map<String, Object> mapData = (Map<String, Object>)request.getAttribute("list_data");
%>

 <!-- Page Breadcrumb -->
 <div class="page-breadcrumbs breadcrumbs-fixed">
     <ul class="breadcrumb">
         <li>
             <i class="fa fa-home"></i>
             <a href="#">Accueil</a>
         </li>
         <li>Parc auto</li>
         <li>Synthèse</li>
         <li class="active">Véhicules</li>
     </ul>
 </div>
<!-- /Page Breadcrumb -->
  <!-- Page Header -->
  <div class="page-header position-relative">
      <div class="header-title" style="padding-top: 4px;">
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
<std:form name="search-form">

<!-- Row Start -->
   <div class="col-lg-12 col-md-12">
     <div class="widget">
     	<div class="row">
     		<div class="form-group">
     			<std:label classStyle="col-sm-2 control-label" value="Employé" />
                 <div class="col-sm-4">
                   	<std:select type="string[]" name="employe_Ids" data="${listEmploye }" key="id" labels="nom;' ';prenom" width="80%" />
                  </div>
	  	    </div>
	  	         
	        <div class="form-group">
	        	<std:label classStyle="control-label col-md-2" value="Date début" />
	            <div class="col-md-2">
	                 <std:date name="dateDebut" value="${dateDebut }"/>
	            </div>
	            <div class="col-md-2" style="text-align: center;">
	            	<std:link action="pers.pointage.loadEmployeSynthese" params="prev=1" icon="fa fa-arrow-circle-left" tooltip="Mois précédent" />
	            	<std:link action="pers.pointage.loadEmployeSynthese" params="next=1" icon="fa fa-arrow-circle-right" tooltip="Mois suivant" />
	            </div>
	            
	            <std:label classStyle="control-label col-md-1" value="Date fin" />
	            <div class="col-md-2">
	                 <std:date name="dateFin" value="${dateFin }"/>
	            </div>
	            <div class="col-md-2">
	           	 	<std:button action="pers.pointage.loadEmployeSynthese" value="Filtrer" classStyle="btn btn-primary" />
	           	 </div>	
	       </div>
	   </div>
	   <hr>
<%-- 	   <% id = 0;%> --%>
  	   <div class="row">
       	 <div class="table-responsive">
           <table id="list_vehicule" class="table table-condensed table-striped table-bordered table-hover no-margin">
             <thead>
               <tr>
                 <th> Employé </th>
                 <th> Salaire </th>
                 <th style="width:130px;text-align: left;"> Frais </th>
                 <th style="width:130px;text-align: left;"> Congé </th>
                 <th style="width:130px;text-align: left;"> Prêt </th>
                 <th style="width:130px;text-align: left;"> Travail </th>
                 <th style="width:130px;text-align: left;"> Avance </th>
                 <th style="width:130px;text-align: left;"> Prime </th>
                 <th style="width:130px;text-align: left;"> Retenue </th>                            
               </tr>
             </thead>
             <tbody>
             
  <%   for (String key : mapData.keySet()) {%>
  		<tr>
  			<td style="border-bottom: 2px solid #f3f4f5;font-weight: bold;padding-left: 5px;">
  				<%=key %>
  			</td>
  		<%
		BigDecimal[] listData = (BigDecimal[]) mapData.get(key);     
    	for(BigDecimal value : listData){ %>
            <td style="border-bottom: 2px solid #f3f4f5;text-align: right;">
                <%=BigDecimalUtil.formatNumber(value) %>
             </td>
        <%}%>
       </tr>
		<%}%>
             </tbody>
           </table>
      		 <br>
         </div>
       </div>
     </div>
    </div> 
   </std:form>
   
 </div>
 
 <!-- Row End -->
</div>