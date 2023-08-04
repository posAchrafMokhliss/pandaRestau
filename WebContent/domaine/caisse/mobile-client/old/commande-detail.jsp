<%@page import="appli.model.domaine.util_srv.raz.RazService"%>
<%@page import="framework.model.common.constante.ProjectConstante"%>
<%@page import="appli.controller.domaine.administration.bean.UserBean"%>
<%@page import="appli.model.domaine.personnel.persistant.SocieteLivrPersistant"%>
<%@page import="appli.controller.domaine.caisse.action.CaisseWebBaseAction"%>
<%@page import="appli.model.domaine.vente.persistant.CaisseMouvementOffrePersistant"%>
<%@page import="framework.model.util.FileUtil"%>
<%@page import="appli.controller.domaine.util_erp.ContextAppli.TYPE_LIGNE_COMMANDE"%>
<%@page import="appli.model.domaine.vente.persistant.CaisseMouvementArticlePersistant"%>
<%@page import="appli.controller.domaine.util_erp.ContextAppli"%>
<%@page import="appli.controller.domaine.caisse.ContextAppliCaisse"%>
<%@page import="framework.controller.ContextGloabalAppli"%>
<%@page import="appli.model.domaine.vente.persistant.CaisseMouvementPersistant"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.HashMap"%>
<%@page import="framework.model.common.util.ServiceUtil"%>
<%@page import="appli.model.domaine.stock.service.IArticleService"%>
<%@page import="java.util.Map"%>
<%@page import="java.math.BigDecimal"%>
<%@page import="framework.model.common.service.MessageService"%>
<%@page import="framework.controller.bean.message.GrowlMessageBean"%>
<%@page import="framework.model.common.util.EncryptionUtil"%>
<%@page import="framework.controller.ControllerUtil"%>
<%@page import="framework.model.common.util.BigDecimalUtil"%>
<%@page import="framework.model.common.util.BooleanUtil"%>
<%@page import="framework.model.common.util.StringUtil"%>
<%@page import="java.util.List"%>
<%@ taglib uri="http://www.customtaglib.com/standard" prefix="std"%>

<script type="text/javascript">
	$(document).ready(function() {
		var total = document.getElementById('det_total').innerHTML;
		if(document.getElementById('cmd_total')){
			document.getElementById('cmd_total').innerHTML = (parseFloat(total)==0?'':total);
		}
		
		$(".spinbox-up").click(function(){
			var art = $(this).attr("art");
			$("#quantite_custom_"+art).val(parseInt($("#quantite_custom_"+art).val())+1);
			$("#quantite_custom").val(parseInt($("#quantite_custom_"+art).val()));
			$("#changeQteBtn").attr("params", $(this).attr("params"));
			$("#changeQteBtn").trigger("click");
		});
		$(".spinbox-down").click(function(){
			var art = $(this).attr("art");
			var trParam = $(this).closest("tr").attr("par");
			var qte = ($("#quantite_custom_"+art).val() == '' ? 0 : parseInt($("#quantite_custom_"+art).val()));
			
			if(qte > 1){ 
				$("#quantite_custom_"+art).val(parseInt($("#quantite_custom_"+art).val())-1);
				$("#quantite_custom").val(parseInt($("#quantite_custom_"+art).val()));
				$("#changeQteBtn").attr("params", $(this).attr("params"));
				$("#changeQteBtn").trigger("click");
			} else{// Remove 
				$("#del-cmd-lnk").attr("params", trParam).trigger("click");
			}
		});
	});
 </script>	

<%
	String CURRENT_ITEM_ADDED = (String)ControllerUtil.getUserAttribute("CURRENT_ITEM_ADDED", request);
	String CURRENT_MENU_NUM = (String)ControllerUtil.getUserAttribute("CURRENT_MENU_NUM", request);
	CaisseMouvementPersistant CURRENT_COMMANDE = (CaisseMouvementPersistant)ControllerUtil.getUserAttribute("CURRENT_COMMANDE", request);
	if(CURRENT_COMMANDE == null){
		CURRENT_COMMANDE = new CaisseMouvementPersistant(); 
	}
	List<CaisseMouvementArticlePersistant> listSortedArticle = CURRENT_COMMANDE.getList_article();
	if(listSortedArticle == null){
		listSortedArticle = new ArrayList<>();
	}
	Integer NBR_NIVEAU_CAISSE = StringUtil.isNotEmpty(ContextGloabalAppli.getGlobalConfig("NBR_NIVEAU_CAISSE"))?Integer.valueOf(ContextGloabalAppli.getGlobalConfig("NBR_NIVEAU_CAISSE")):null;
	IArticleService service = (IArticleService)ServiceUtil.getBusinessBean(IArticleService.class);
	UserBean userBean = (UserBean)MessageService.getGlobalMap().get(ProjectConstante.SESSION_GLOBAL_USER);
	%>
	
	<std:link action="caisse-web.caisseWeb.deleteRow" targetDiv="left-div" id="del-cmd-lnk" style="display:none;" />  
	
<div style="background-color: rgb(255, 250, 201);">
<!-- R&eacute;cap commande -->
<div style="height: 70px;border: 1px solid #cdc9c9;background-color: black;color: white;">

	<div class="btn-group" id="context-mnu" style="margin-left: 5px;">
	   <a class="btn btn-xs btn-succes dropdown-toggle" data-toggle="dropdown" aria-expanded="false" style="border-radius: 20px;padding-top: 0px;height: 40px;width: 57px;margin-top: -29px;font-size: 22px;font-weight: bold;">
	   		<i class="fa fa-ellipsis-v"></i>
	   		<i class="fa fa-angle-down"></i>
	   </a>
	   <ul class="dropdown-menu dropdown-yellow" role="menu">
	       <li>
	       		<a href="javascript:" targetDiv="left-div" wact="<%=EncryptionUtil.encrypt("caisse-web.caisseWeb.ajouterGroupe")%>" >
	       		<i class="fa fa-male" style="color: black;font-size: 16px;"></i> Ajouter client</a>
	       	</li>
	    </ul>
	</div>
<!-- Total -->
	<span style="font-weight: bold;font-size: 40px;margin-left: 15%;line-height: 65px;" id="det_total"> 
		<%=CURRENT_COMMANDE!=null?BigDecimalUtil.formatNumberZeroBd(CURRENT_COMMANDE.getMtt_commande_net()):""%>
	</span>
	
	<button type="button" id="close_modal" onclick="$('#left-div').hide(500);" style="height:35px; right: 4px;top: 16px;position: absolute;" class="btn btn-danger btn-xs" data-dismiss="modal">
		&nbsp;&nbsp;<i class="fa fa-times"></i>&nbsp;&nbsp;
	</button>
</div>
<std:hidden name="quantite_custom" id="quantite_custom"/>
<std:button actionGroup="X" id="changeQteBtn" style="display: none" action="caisse-web.caisseWeb.changeQuantite" targetDiv="left-div"/>
	                      
 <div style="overflow: auto;
    border: 1px dashed #cdc9c9;
    width: 99%;background: rgb(255 255 255);" id="div_detail_cmds">    		
	<table style="width: 100%;color: black;" id="cmd-table">

	<%
		if(request.getAttribute("mtt_rendu") != null){
	%>
	<tr>
		<td>
			<div class="alert alert-success fade in radius-bordered alert-shadowed" style="font-size: 24px;margin-top: 50px;">
		         <button class="close" data-dismiss="alert" style="margin-top: 10px;">X</button>
		         <i class="fa-fw fa fa-check"></i>
		         A rendre :<strong> <%=BigDecimalUtil.formatNumber(BigDecimalUtil.get(""+request.getAttribute("mtt_rendu")))%></strong> 
		     </div>
     	</td>
     </tr>
	<%
		}else if(StringUtil.isNotEmpty(CURRENT_COMMANDE.getRef_commande()) && CURRENT_COMMANDE.getRef_commande().indexOf("-") != -1 && CURRENT_COMMANDE.getList_article().size()>0){
	%>
	<tr style="background-color: #f4f4f4;">
		<td colspan="3" style="text-align: center;color: blue;font-style: italic"><%=CURRENT_COMMANDE.getRef_commande()%></td>
	</tr>
	<%
		}
	%>
<%
	// Total par menu
String currentTableRef = (String)ControllerUtil.getUserAttribute("CURRENT_TABLE_REF", request);

Map<String, BigDecimal> mttMenuMap = new HashMap<>();
List<String> mttTableClient = new ArrayList<>();
Map<String, Integer> nbrCouvertTable = (Map<String, Integer>)ControllerUtil.getMenuAttribute("COUVERTS_TABLE", request);
if(nbrCouvertTable == null){
	nbrCouvertTable = new HashMap<>();
}

List<String> listTables = new ArrayList<>();
List<Integer> listIdxClient = new ArrayList<>();
for(CaisseMouvementArticlePersistant caisseMvmP : listSortedArticle){
	if(BooleanUtil.isTrue(caisseMvmP.getIs_annule())){
		continue;
	}
	if(!listIdxClient.contains(caisseMvmP.getIdx_client()) && caisseMvmP.getIdx_client() != null){
		listIdxClient.add(caisseMvmP.getIdx_client()); 
	}
	// Clients par table
	if(!mttTableClient.contains(caisseMvmP.getIdx_client()+"_"+caisseMvmP.getRef_table())){
		mttTableClient.add(caisseMvmP.getIdx_client()+"_"+caisseMvmP.getRef_table());
	}
	
	// Ajout tables
	 if(caisseMvmP.getRef_table() != null && !listTables.contains(caisseMvmP.getRef_table())){
		 listTables.add(caisseMvmP.getRef_table());
	 }
	// Nombre de couverts
	if(caisseMvmP.getNbr_couvert() != null){
		nbrCouvertTable.put(caisseMvmP.getRef_table(), caisseMvmP.getNbr_couvert());
	}
	
	if(caisseMvmP.getMenu_idx() == null){
	     continue;
	 }
	 mttMenuMap.put(caisseMvmP.getMenu_idx(),  BigDecimalUtil.add( mttMenuMap.get(caisseMvmP.getMenu_idx()), caisseMvmP.getMtt_total()));
}
	// Trier les tables
	if(listTables.size() > 0){
		Collections.sort(listTables);
	}
	if(listIdxClient.size() > 0){
		Collections.sort(listIdxClient);
	}
	
	if(listIdxClient != null && listIdxClient.size()>0 && listIdxClient.get(listIdxClient.size()-1) > CURRENT_COMMANDE.getMax_idx_client()){
		CURRENT_COMMANDE.setMax_idx_client(listIdxClient.get(listIdxClient.size()-1));
	}
	
	if(currentTableRef == null){
		if(listTables.size() == 0){
			listTables.add("XX");
		}
	} else if(!listTables.contains(currentTableRef)){
		listTables.add(currentTableRef);
	}

	Integer idxArticle = 0;
	int nbrNiveau = 0;
	BigDecimal sousTotal = null;
	BigDecimal sousTotalTable = null;
	Integer currentIdxClient = (Integer)ControllerUtil.getUserAttribute("CURRENT_IDX_CLIENT", request);

	// Tables -------------
	int idxTable = 0;
	for(String refTable : listTables){
		idxTable++;
		boolean isTablePassed = true;
		
		// Clients
		for(int i=1; i<=CURRENT_COMMANDE.getMax_idx_client(); i++){
	if(currentIdxClient != null && currentIdxClient!=i && !listIdxClient.contains(i)){
		continue;
	}

	// Si client de la table
	if(!"XX".equals(refTable) 
			&& !refTable.equals(currentTableRef) 
			&& !mttTableClient.contains(i+"_"+refTable)
		){
		continue;
	}
	
	boolean isFamillePassed = false;
	if(listIdxClient.size() > 1){
		    	   if(i != listIdxClient.get(0)){
%>
		    		   <tr style="color: #d73d32;font-weight: normal;background-color: #eeeeee;" class="client-root-style">
			       			<td colspan="2" align="right" style="font-weight: bold;">SOUS TOTAL CLIENT</td>
			       			<td align="right" style="font-weight: bold;"><%=BigDecimalUtil.formatNumber(sousTotal)%></td>
			      	   </tr>
		    	 <%
		    	 	}
		    	 		    	 
		    	 		    	 if(isTablePassed && !"XX".equals(refTable)){
		    	 	    	   if(idxTable != 1){
		    	 %>
			    		   <tr style="color: black;font-weight: normal;background-color: #ccc;" class="client-root-style">
				       			<td colspan="2" align="right" style="font-weight: bold;">SOUS TOTAL TABLE</td>
				       			<td align="right" style="font-weight: bold;"><%=BigDecimalUtil.formatNumber(sousTotalTable)%></td>
				      	   </tr>
			    	 <%
			    	 	sousTotalTable = null;
			    	 	    	   }
			    	 	      }
			    	 	  }
			    	 %>
			  
			  <%
			  			  	if(isTablePassed && !"XX".equals(refTable)){
			  			  %>
		    	 <tr style="color: black;background-color: #262626;" class="client-root-style" par="istab=1&ref_tab=<%=refTable%>&tp=TAB">
		       		<td colspan="3" style="border-radius: 15px;font-weight: bold;color: #fbfbfb;" align="center">
		       			<%=(currentTableRef!=null && currentTableRef.equals(refTable) ? "<i style='color:#ff5722;' class='fa fa-arrow-circle-right'></i>":"")%> <%=refTable%> <%=nbrCouvertTable.get(refTable)!=null?" ("+nbrCouvertTable.get(refTable)+" couverts)":""%>
		       		</td>
		       </tr>
		    	 <%
		    	 	}
		    	 %> 
		   	 <%
 		   	 	if(currentIdxClient != null && (currentIdxClient!=1 || request.getAttribute("is_cliIdxAdded")!=null || listIdxClient.size()>0)){
 		   	 %> 
		    	   <tr style="color: black;background-color: #dddddd;" class="client-root-style" par="iscli=1&idx_cli=<%=i%>&tp=CLI">
			       		<td colspan="3" style="padding-left: 10px;"> 
			       			<i class="fa fa-street-view" style="color: #585858"></i> 
			       			<%=((currentIdxClient!=null && currentIdxClient==i) ? "<i style='color:#ff5722;' class='fa fa-arrow-circle-right'></i>":"")%>  CLIENT <%=i%>
			       		</td>
			       </tr>
		      <%
		      	}
		      		    sousTotal = null;
		      		    isTablePassed = false;
		      		    //
		      	for(CaisseMouvementArticlePersistant caisseMvmP : listSortedArticle){
		      		       if(BooleanUtil.isTrue(caisseMvmP.getIs_annule()) || (caisseMvmP.getIdx_client() != null && caisseMvmP.getIdx_client()!=i) || (caisseMvmP.getRef_table()!=null && !caisseMvmP.getRef_table().equals(refTable))){
		      		           continue;
		      		       }
		      		      sousTotal = BigDecimalUtil.add(sousTotal, (BooleanUtil.isTrue(caisseMvmP.getIs_offert()) ? null:caisseMvmP.getMtt_total()));
		      		      sousTotalTable = BigDecimalUtil.add(sousTotalTable, (BooleanUtil.isTrue(caisseMvmP.getIs_offert()) ? null:caisseMvmP.getMtt_total()));
		      		       
		      		       // Ajout du num�ro dans le tableau
		      		       String type = caisseMvmP.getType_ligne();
		      		       String libCmd = caisseMvmP.getLibelle();
		      		       if(type == null){
		      		           type = "XXX";
		      		       }
		      		       
		      		       if(type.equals(TYPE_LIGNE_COMMANDE.MENU.toString()) && (caisseMvmP.getLevel() == null || caisseMvmP.getLevel() > 1)) {
		      		           idxArticle++;
		      		           libCmd = idxArticle + " - " + libCmd;
		      		       } else if(type.equals(TYPE_LIGNE_COMMANDE.ART.toString())){
		      		           idxArticle++;
		      		           libCmd = idxArticle + " - " + libCmd;
		      		       }
		      		       
		      		       if(StringUtil.isNotEmpty(caisseMvmP.getCommentaire())){
		      		           libCmd = libCmd + " <i class='fa fa-comments-o' style='color:orange;font-size: 17px;cursor:pointer;' data-container='body' data-titleclass='bordered-blue' data-class='' data-toggle='popover' data-placement='top' data-title='Commentaire' data-content='"+caisseMvmP.getCommentaire()+"' data-original-title='' title=''></i>";
		      		       }
		      		       if(BooleanUtil.isTrue(caisseMvmP.getIs_client_pr())){
		      		    	   libCmd = libCmd + " <i class='fa fa-long-arrow-down' style='color:#4caf50;font-size: 17px;cursor:pointer;' data-container='body' data-titleclass='bordered-blue' data-class='' data-toggle='popover' data-placement='top' data-title='Prix r&eacute;duit. Intial : ' data-content='"+BigDecimalUtil.formatNumber(caisseMvmP.getOpc_article().getPrix_vente())+"' data-original-title='' title=''></i>";
		      		       }
		      		       
		      		       String qte = RazService.getQteFormatted(caisseMvmP.getQuantite());
		      		       
		      		       String mttTotal = "";
		      		       if( BooleanUtil.isTrue(caisseMvmP.getIs_offert())){
		      		    	   mttTotal = "<i isoff='1' class='fa fa-gift'style='color:green;font-size: 17px;cursor:pointer;' data-container='body' data-titleclass='bordered-blue' data-class='' data-toggle='popover' data-placement='top' data-title='Article offert' data-content='Prix : "+BigDecimalUtil.formatNumber(caisseMvmP.getMtt_total())+"' data-original-title='' title=''></i>";
		      		       } else if(!BigDecimalUtil.isZero(caisseMvmP.getMtt_total())){
		      		    	   mttTotal = BigDecimalUtil.formatNumber(caisseMvmP.getMtt_total());
		      		       }
		      		       
		      		       String styleTd = "";
		      		       boolean isSelected = false;
		      		       if(CURRENT_ITEM_ADDED != null){
		      		    	   String parentCode = (caisseMvmP.getParent_code() != null && caisseMvmP.getParent_code().indexOf("_")!=-1 ? caisseMvmP.getParent_code().substring(caisseMvmP.getParent_code().indexOf("_")+1) : caisseMvmP.getParent_code());
		      		    	   String currElmntPath = caisseMvmP.getIdx_client()
		      		    			   						+ "-" + caisseMvmP.getElementId()
		      		    			   						+ "-" + caisseMvmP.getType_ligne()
		      		    			   						+ "-" + parentCode+(caisseMvmP.getMenu_idx()==null ?"":"-"+caisseMvmP.getMenu_idx())
		      		    			   						+ (StringUtil.isNotEmpty(caisseMvmP.getRef_table()) ? "-"+caisseMvmP.getRef_table() : "");
		      		    	   if(CURRENT_ITEM_ADDED.equals(currElmntPath)){
		      		    		   isSelected = true;
		      		       		}
		      		       }
		      		       
		      		       String classType = "";
		      		       String height = "35px";
		      		       boolean isRootMenu = false;
		      		       boolean isMenu = false;
		      		      if(caisseMvmP.getMenu_idx() != null || type.equals(TYPE_LIGNE_COMMANDE.MENU.toString())){
		      		    	  if(type.equals(TYPE_LIGNE_COMMANDE.MENU.toString())) {
		      		    		  // Ajouter une ligne e s�paration
		      	       	  // if(!isMenuPassed){
		      %>
	<!-- 		       	       <tr style="color: black;" class="menu-root-style"> -->
	<!-- 		       	       		<td colspan="3" align="center">Menus</td> -->
	<!-- 		       	       </tr> -->
			       	       <%
			       	       	//   isMenuPassed = true;
			       	       	       	 //  }
			       	       		    		  if(caisseMvmP.getLevel() != null && caisseMvmP.getLevel() == 1){
			       	       		    			  isRootMenu = true;
			       	       		    			  classType = "menu-cat-style cat-cmd-detail";
			       	       		    		  } else{
			       	       		    			  if(BooleanUtil.isTrue(caisseMvmP.getIs_menu())){
			       	       		    			 	 isMenu = true;
			       	       		    			  }
			       	       		        	   	  classType = "menu-style cat-cmd-detail";
			       	       		    		  }
			       	       		           } else if(type.equals(TYPE_LIGNE_COMMANDE.ART_MENU.toString())) {
			       	       		        	   classType = "ligne-style";
			       	       		        	   height = "30px";
			       	       		        	   nbrNiveau = 0;
			       	       		           } else if(type.equals(TYPE_LIGNE_COMMANDE.GROUPE_MENU.toString())){
			       	       		        	   classType = "group-style-fam";//"group-style cat-cmd-detail";
			       	       		        	   height = "5px";
			       	       		        	   nbrNiveau++;
			       	       		           } else if(type.equals(TYPE_LIGNE_COMMANDE.GROUPE_FAMILLE.toString())){
			       	       		        	   classType = "group-style-fam";//"group-style cat-cmd-detail";
			       	       		        	   height = "5px";
			       	       		        	   nbrNiveau++;
			       	       		           }
			       	       		       } else{//---------------------------------------------------------
			       	       		    	   if(type.equals(TYPE_LIGNE_COMMANDE.ART.toString())){
			       	       		    		   classType = "ligne-fam-style";
			       	       		    		   height = "30px";
			       	       		    		   nbrNiveau = 0;
			       	       		           } else if(type.equals(TYPE_LIGNE_COMMANDE.GROUPE_FAMILLE.toString())){
			       	       		        	   // Ajouter une ligne e s�paration
			       	       		        	   if(!isFamillePassed){
			       	       %>
		        	       <tr style="height:3px;color: black;background-color:<%=isSelected?"#fff3f0;":"#dddddd"%>;" <%=isSelected?" isSel='1' ":""%> class="menu-root-style">
		        	       		<td colspan="3" align="center"></td>
		        	       </tr>
		        	       <%
		        	       	isFamillePassed = true;
		        	       		        	   }
		        	       		        	   classType = "group-style-fam";//"famille-style cat-cmd-famille";
		        	       		        	   height = "5px";
		        	       		        	   nbrNiveau++;
		        	       		           } 
		        	       		       }
		        	       		       
		        	       		       boolean isArticle = (type.equals(TYPE_LIGNE_COMMANDE.ART.toString()) || type.equals(TYPE_LIGNE_COMMANDE.ART_MENU.toString()));
		        	       		       boolean isToAdd = (isArticle || NBR_NIVEAU_CAISSE == null || nbrNiveau <= NBR_NIVEAU_CAISSE) && !caisseMvmP.getLibelle().startsWith("#");
		        	       		       String params = (BooleanUtil.isTrue(caisseMvmP.getIs_offert())?"isaf=1&":"")
		        	       		    		   				+ "cli="+i
		        	       		    		   				+ "&cd="+caisseMvmP.getCode()
		        	       		    		   				+ "&elm="+caisseMvmP.getElementId()
		        	       		    		   				+ "&par="+StringUtil.getValueOrEmpty(caisseMvmP.getParent_code())
		        	       		    		   				+ "&mnu="+caisseMvmP.getMenu_idx()
		        	       		    		   				+ "&tbl="+StringUtil.getValueOrEmpty(caisseMvmP.getRef_table())
		        	       		    		   				+ "&tp="+caisseMvmP.getType_ligne();
		        	       		       if(caisseMvmP.getLevel() != null && caisseMvmP.getLevel() > 1){
		        	       		    	   styleTd = styleTd + "padding-left:"+(7*caisseMvmP.getLevel())+"px;";
		        	       		       } else if(isArticle){
		        	       		    	   styleTd = styleTd + "padding-left:10px;";
		        	       		       }
		        	       		      
		        	       		       if(isToAdd){
		        	       		    	   boolean isNotArt = (!type.equals(TYPE_LIGNE_COMMANDE.ART.toString()) && !type.equals(TYPE_LIGNE_COMMANDE.ART_MENU.toString()));
		        	       		       
		        	       		       		boolean isToClolapse = (caisseMvmP.getMenu_idx()!= null 
		        	       		       										&& !type.equals(TYPE_LIGNE_COMMANDE.MENU.toString())
		        	       		       										&& (CURRENT_MENU_NUM == null || !CURRENT_MENU_NUM.equals(caisseMvmP.getMenu_idx())));
		        	       %>
		       
		       <%
		       		       	if(isRootMenu){
		       		       %>
		       	<tr id="<%=isToClolapse ? "tr_"+caisseMvmP.getMenu_idx():""%>" class="<%=classType%>" style="color:#969696;background-color: white;font-size: 16px;height: <%=height%>; <%=isSelected?"background-color:#fff3f0;":""%>" <%=isSelected?" isSel='1' ":""%> par="<%="isroot=1&"+params%>">
		       		<td colspan="3" style="<%=styleTd%>;<%="5px".equals(height)?"":"line-height:"+height%>;text-align:left;">
		       			<%=libCmd%>
		       		</td>
		       	</tr>
		       <%
		       	} else{ 
		       		       String mttTotalMenu = BigDecimalUtil.formatNumber(mttMenuMap.get(caisseMvmP.getMenu_idx()));
		       		       boolean isMtt = (!BigDecimalUtil.isZero(caisseMvmP.getMtt_total()) && !BooleanUtil.isTrue(caisseMvmP.getIs_offert()));
		       %>
		         <tr id="<%=isToClolapse ? "tr_"+caisseMvmP.getMenu_idx():""%>" class="<%=classType%> <%=isMenu?(" mnu_"+caisseMvmP.getMenu_idx()):""%>" style="height: <%=height%>;<%=isToClolapse?"display:none;":""%> <%=isSelected?"background-color:#fff3f0;":""%><%=isArticle?"font-weight:normal;":""%>" <%=isSelected?" isSel='1' ":""%> par="<%=(isMtt?"isart=1&":"")+params%>">
		       		<td style="<%=styleTd%>;<%="5px".equals(height)?"":"line-height:"+height%>;">
		       			<%
		       				if(type.equals(TYPE_LIGNE_COMMANDE.MENU.toString()) && (CURRENT_MENU_NUM == null || !CURRENT_MENU_NUM.equals(caisseMvmP.getMenu_idx())) && caisseMvmP.getLevel()>1 ){
		       			%>
		       				<a style="border: 0px;" class="btn btn-default btn-xs icon-only white lnk_mnu" href="javascript:void(0);" mnu="<%=caisseMvmP.getMenu_idx()%>">
		       					<i class="fa fa-plus" style="font-size: 13px;color: #03A9F4;"></i>
			       			</a>
			       			<%
			       				Map<String, byte[]> dataimg = service.getDataImage(caisseMvmP.getElementId(), "menu");
			       					       				String stl = "";
			       					       				if(dataimg.size() > 0){
			       					       					stl = "width: 33px;height:33px;float: right;background-size: 33px 33px !important;background: url(data:image/jpeg;base64,"+FileUtil.getByte64(dataimg.entrySet().iterator().next().getValue())+") no-repeat;";
			       			%>
		       					<span style="<%=stl%>"></span>
		       					<%
		       						}
		       							       			} else if(isArticle){
		       							       				Map<String, byte[]> dataimg = service.getDataImage(caisseMvmP.getElementId(), "article");
		       							       				String stl = "";
		       							       				if(dataimg.size() > 0){
		       							       					stl = "width: 23px;height:23px;float: right;background-size: 23px 23px !important;background: url(data:image/jpeg;base64,"+FileUtil.getByte64(dataimg.entrySet().iterator().next().getValue())+") no-repeat;";
		       					%>
		       					<span style="<%=stl%>"></span>
		       					<%
		       						}
		       							       			}
		       					%>
		       		
		       			<%=libCmd%>
		       		</td>
		       		<td align="right">
		       			<%
		       				if(BooleanUtil.isTrue(caisseMvmP.getIs_encaisse()) && isMtt){
		       			%>
		       					<i isenc="1" class="fa fa-check-circle-o" style="font-size: 17px;color: #53a93f;"></i>
		       			<%
		       				}
		       			%>
		       			<%if(!isNotArt){
		       				String id_qte = "quantite_custom_"+caisseMvmP.getCode();
		       			%>
		       			<% 
		       			String onClick = "";
		       			if(BigDecimalUtil.isZero(CURRENT_COMMANDE.getMtt_commande_net())){
		       				onClick = "$('#left-div').hide(500);";
		       			} 
		       			%>
		       				<div style="display: flex">
                         <%-- <% if(id_qte < 1 && )%> --%>
 				       			<a href="javascript:" class="spinbox-down" art="<%=caisseMvmP.getCode() %>" params="<%=params%>" targetDiv="left-div" style="font-size: 16px;" onclick="<%=onClick%>">    <%--    onclick="$('#left-div').hide(500);"    --%>
				       				<i class="fa fa-minus-square" style="color: #2c272d;padding-top: 3px;"></i>
				       			</a>
								<std:text name="<%=id_qte %>" style="background-color: transparent;" classStyle="qte_input" type="long" value="<%=qte %>"/>
								<a href="javascript:" class="spinbox-up" art="<%=caisseMvmP.getCode() %>" params="<%=params%>" targetDiv="left-div" style="font-size: 16px;">
				       				<i class="fa fa-plus-square" style="color: #2c272d;padding-top: 3px;"></i>
				       			</a>
			       			</div>
			       		<%} %>		       			
		       			
		       		</td>
		       		<td align="right" style="font-weight: bold;">
		       			<%
		       				if(caisseMvmP.getMenu_idx() == null){
		       			%>
		       				<%=mttTotal%>
		       			<%
		       				} else{ 
		       					boolean isCurrMnu = (CURRENT_MENU_NUM!=null && CURRENT_MENU_NUM.equals(caisseMvmP.getMenu_idx()));
		       			%>
		       				<span class="span_mtt_art" style='display:<%=isToClolapse || isCurrMnu ? "":"none"%>;'><%=mttTotal%></span>
		       			<%
		       					if(BooleanUtil.isTrue(caisseMvmP.getIs_offert())){
		       						 mttTotalMenu = "<i isoff='1' class='fa fa-gift'style='color:green;font-size: 17px;cursor:pointer;' data-container='body' data-titleclass='bordered-blue' data-class='' data-toggle='popover' data-placement='top' data-title='Article offert' data-content='Prix : "+mttTotalMenu+"' data-original-title='' title=''></i>";
		       					} %>	
		       				<span class="span_mtt_mnu" style='display:<%=isToClolapse || isCurrMnu ? "none":""%>;'><%=isMenu ? mttTotalMenu : ""%></span>
		       			<%
		       				}
		       			%>
		       		</td>
		       </tr>
		       <%
		       					}
		       		    	}
		       			}
		       	   }
		       	}
		       	
		       	if(listIdxClient.size() > 1){ %>
			 		 <tr style="color: #d73d32;background-color: #eeeeee;font-weight: normal;" class="menu-root-style">
				     	<td colspan="2" align="right" style="font-weight: bold;">SOUS TOTAL CLIENT</td>
				     	<td align="right" style="font-weight: bold;"><%=BigDecimalUtil.formatNumber(sousTotal)%></td>
				     </tr>
   			<% }
   	
   				if(listTables.size()>0 && !listTables.get(0).equals("XX")){ %>
	 <tr style="color: black;background-color: #ccc;font-weight: normal;" class="menu-root-style">
    	<td colspan="2" align="right" style="font-weight: bold;">SOUS TOTAL TABLE</td>
    	<td align="right" style="font-weight: bold;"><%=BigDecimalUtil.formatNumber(sousTotalTable)%></td>
    </tr>
	<%
		}
		
   	List<CaisseMouvementOffrePersistant> listOffres = RazService.getListOffre(CURRENT_COMMANDE);
	   // Les offres
	if(listOffres != null && listOffres.size() > 0){
	%>
       <tr style="color: green;" class="menu-root-style">
       		<td colspan="3" align="center">Offres</td>
       </tr>
       <%
       	for (CaisseMouvementOffrePersistant offreDet : listOffres) {
       	       if(offreDet.getIs_annule() != null && offreDet.getIs_annule()){
       	           continue;
       	       }
       	       Long idOffre = offreDet.getOpc_offre().getId();
       	       String libelleOffre = offreDet.getOpc_offre().getLibelle();
       	       boolean idMajoration = offreDet.getMtt_reduction()!=null && offreDet.getMtt_reduction().compareTo(BigDecimalUtil.ZERO)<0;
             	   String params = "cd="+idOffre+"&elm="+offreDet.getOpc_offre().getId()+"&tp=OFFRE";
       %>
	       <tr style="color: <%=idMajoration?"green":"red"%>;" par="<%=params%>" class="ligne-style">
	       		<td><%="**"+libelleOffre%></td>
	       		<td></td>
	       			       		
	       		<%
	       	String mtt = idMajoration ? BigDecimalUtil.formatNumber(offreDet.getMtt_reduction().abs()) : "-"+BigDecimalUtil.formatNumber(offreDet.getMtt_reduction());
	       	%>
	       		<td align="right"><%=mtt%></td>
	       </tr>
	       <%
	       	}
	          }
	       %>     
</table>
</div>
	<div id="footer-cmd-div">
			<std:linkPopup id="btn_encaiss_val" action="caisse-web.caisseWeb.initPaiement" classStyle="btn btn-default btn-circle btn-lg btn-menu shiny" tooltip="Encaisser la commande">
					<i class="fa fa-money"></i> ENCAISSER
			</std:linkPopup>

			<a id="annul_cmd_main" class="btn btn-danger btn-circle btn-lg btn-menu shiny" act="<%=EncryptionUtil.encrypt("caisse-web.caisseWeb.annulerCommande")%>" targetDiv="left-div" title="Annuler la commande">
				<i class="fa fa-times"></i> ANNULER
			</a>
		</div>	
</div>

<script type="text/javascript">
	$(document).ready(function (){
		
		$("#annul_cmd_Pop").hide();
		$(".modal-backdrop").hide();
		
		$("#paiement_partiel_lnk").click(function(){
			$("input[id^='checkcli_'], input[id^='checktab_']").prop("checked", false).toggle(1000);
		});
		
		<% if(CURRENT_COMMANDE!=null && CURRENT_COMMANDE.getList_article().size()>0){%>
			$("#lnk_panier_det").show();
		<%} else{%>
			$("#lnk_panier_det").hide();
		<%} %>
		
		$(".lnk_mnu").click(function(){
			var mnuTr = $(".mnu_"+$(this).attr("mnu"));
			//
			if($(this).find("i").attr("class") == "fa fa-plus"){
				$(this).find("i").attr("class", "fa fa-minus");
				mnuTr.find(".span_mtt_art").show();
				mnuTr.find(".span_mtt_mnu").hide();
			} else{
				$(this).find("i").attr("class", "fa fa-plus");
				mnuTr.find(".span_mtt_art").hide();
				mnuTr.find(".span_mtt_mnu").show();
			}
			
			$("tr[id='tr_"+$(this).attr("mnu")+"']").toggle();
		});
		
		$('#annul_cmd').click(function(){
			showConfirmDeleteBox($(this).attr("act"), "", $("#targ_link"), "Cette commande sera annul&eacute;e.<br>Voulez-vous confirmer ?", null, "Annulation commande");
		});
		//Scroll
		var selectedTr = $('#cmd-table tr[isSel]');
		if(selectedTr && selectedTr.length > 0){
			selectedTr.get(0).scrollIntoView();
		}
		<%
		// List growl message
		List<GrowlMessageBean> listGrowlMessage = MessageService.getListGrowlMessageBean();
		if((listGrowlMessage != null) && (listGrowlMessage.size() > 0)){
			for(GrowlMessageBean growlBean : listGrowlMessage) { %>
				showPostAlert("<%=growlBean.getTitle()%>", "<%=growlBean.getMessage() %>" , "<%=growlBean.getType().toString()%>");
			<%}
		}
		MessageService.clearMessages();
		%>
		
		<%if(request.getAttribute("NXT_STEP") != null){%>
		$("#btn-wizard-next").trigger("click");
	<%} else if(request.getAttribute("UP_MNU") != null){%>
		$("#up_btn").attr("params", 'up=<%=request.getAttribute("UP_MNU")%>').show().trigger("click");
		$("#up_btn").removeAttr("params");
	<%}%>
		<%if(request.getAttribute("PAGE_JS") != null){%>
			<%=request.getAttribute("PAGE_JS")%>
		<%}%>
		$("#span_pers").html('');
		$("#generic_modal").modal("hide");
	});

</script>
