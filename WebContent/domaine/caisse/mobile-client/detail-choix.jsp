<%@page import="appli.model.domaine.vente.persistant.ListChoixPersistant"%>
<%@page import="appli.model.domaine.vente.persistant.ListChoixDetailPersistant"%>
<%@page import="appli.model.domaine.vente.persistant.MenuCompositionDetailPersistant"%>
<%@page import="appli.model.domaine.vente.persistant.MenuCompositionPersistant"%>
<%@page import="framework.model.common.util.BooleanUtil"%>
<%@page import="framework.controller.ControllerUtil"%>
<%@page import="framework.model.common.util.EncryptionUtil"%>
<%@page import="framework.model.common.util.StringUtil"%>
<%@page import="framework.model.common.util.BigDecimalUtil"%>
<%@page import="java.math.BigDecimal"%>
<%@page import="framework.model.util.FileUtil"%>
<%@page import="framework.model.common.util.ServiceUtil"%>
<%@page import="appli.model.domaine.stock.service.IArticleService"%>
<%@page import="appli.controller.domaine.caisse.action.CaisseWebBaseAction"%>
<%@page import="java.util.Map"%>
<%@page import="appli.model.domaine.stock.persistant.FamillePersistant"%>
<%@page import="appli.model.domaine.stock.persistant.ArticlePersistant"%>
<%@page import="java.util.List"%>
<%@ taglib uri="http://www.customtaglib.com/complexe" prefix="cplx"%>
<%@ taglib uri="http://www.customtaglib.com/standard" prefix="std"%>
<%@ taglib uri="http://www.customtaglib.com/html" prefix="html"%>
<%@ taglib uri="http://www.customtaglib.com/work" prefix="work"%>
<%@ taglib uri="http://www.customtaglib.com/c" prefix="c"%>

<script type="text/javascript">
	$(document).ready(function (){
		$("#back_btn, #up_btn").show();
		<%if(request.getAttribute("isLoadEvent")!=null){ %>
			$("#up_btn").hide();
		<%}%>
	});	
</script>

<%
List<FamillePersistant> listSousFamille = (List<FamillePersistant>)request.getAttribute("listFamille");
List<ArticlePersistant> listArticle = (List<ArticlePersistant>)request.getAttribute("listArticle");
Map<String, Object> listMenu = (Map<String, Object>)request.getAttribute("listMenu");

IArticleService service = (IArticleService)ServiceUtil.getBusinessBean(IArticleService.class);
List<MenuCompositionPersistant> listSousMenu = (List<MenuCompositionPersistant>)ControllerUtil.getUserAttribute("LIST_SOUS_MENU", request);
Integer step = (Integer)ControllerUtil.getUserAttribute("STEP_MNU", request);
%>
	
<%
if(listSousMenu != null && listSousMenu.size()>0){
%>
<div id="WiredWizard" class="wizard wizard-wired" data-target="#WiredWizardsteps">
   <ul class="steps">
   <%
	int i=0;
    int withLi = 100/listSousMenu.size();
	for(MenuCompositionPersistant sousMenu : listSousMenu){ 
		 String libelle = sousMenu.getLibelle().replaceAll("\\#", "");
	%>
            <li data-target="#wiredstep<%=i %>" class='<%=(step==i)?"active":((step>i)?"complete":"") %>' style="width: <%=withLi%>%;">
            	<a href="javascript:" wact='<%=EncryptionUtil.encrypt("caisse-web.caisseWeb.loadNextStep")%>' params="stepr=<%=(i+1) %>" targetDiv="right-div">
	            	<span class="step"><%=i+1 %></span>
	            	<span class="title" style="font-size: 10px;"><%=libelle %></span><span class="chevron"></span>
            	</a>
            </li>
	<%
		i++;
	} %>  
 	</ul>
</div>
<%} %> 

<%
if(listSousFamille != null){
	for(FamillePersistant familleP : listSousFamille){
		if(BooleanUtil.isTrue(familleP.getIs_disable())){
			continue;
		}
			String libelle = familleP.getLibelle().replaceAll("\\#", "");
	%>	
	<div class="col-xs-12 col-sm-6 col-md-6 col-lg-4">	
		<std:link action="caisse-web.caisseWeb.familleEvent" targetDiv="right-div" workId="<%=familleP.getId().toString() %>" classStyle="caisse-mobile-btn detail-famille-btn" value="">
			<span class="span-img-stl">
				<img alt="" src="resourcesCtrl?elmnt=<%=EncryptionUtil.encrypt(familleP.getId().toString()) %>&path=famille&rdm=<%=(familleP.getDate_maj()!=null?familleP.getDate_maj().getTime():"1")%>" class="img-caisse-stl">
			</span>	
       			<span class="caisse-lib-stl"><%=libelle %>&nbsp;&nbsp;</span>
		</std:link>	
	</div>	
	<%}
}%> 

<!-- Articles -->
<%
if(listArticle != null){
	for(ArticlePersistant articleP : listArticle){
		if(BooleanUtil.isTrue(articleP.getIs_disable())){
			continue;
		}
		String libelle = articleP.getLibelle().replaceAll("\\#", "");
		String prix = BigDecimalUtil.formatNumber(articleP.getPrix_vente());
		%>
		<div class="col-xs-12 col-sm-6 col-md-6 col-lg-4">
			<std:link action="caisse-web.caisseWeb.addArticleFamilleCmd" targetDiv="left-div" workId="<%=articleP.getId().toString() %>" classStyle="caisse-ligne-btn" value="">
				<span class="span-img-stl">
					<img alt="" onerror="replaceErrImg(this);" src="resourcesCtrl?elmnt=<%=EncryptionUtil.encrypt(articleP.getId().toString()) %>&path=article&rdm=<%=(articleP.getDate_maj()!=null?articleP.getDate_maj().getTime():"1")%>" class="caisse-img-stl">
				</span>	
	       		<span class="caisse-lib-stl">
	       			<span class="caisseSub-prix-stl"><%=libelle %></span>
					<span class="caisse-prix-stl"><%=prix %> MAD</span>	       		
	       		</span>
			</std:link>
		</div>	
<%	}
}%>

<%
if(listMenu != null){
	for(String cle : listMenu.keySet()){
		Object elementP = listMenu.get(cle);
		
		if(elementP instanceof MenuCompositionPersistant){
			MenuCompositionPersistant menuP = ((MenuCompositionPersistant)elementP);
			if(BooleanUtil.isTrue(menuP.getIs_desactive())){
				continue;
			}
			
			String libelle = ((MenuCompositionPersistant)elementP).getLibelle().replaceAll("\\#", "");
			String prix = BigDecimalUtil.isZero(menuP.getMtt_prix()) ? "":BigDecimalUtil.formatNumber(menuP.getMtt_prix());
			%>
			
			<div class="col-xs-12 col-sm-6 col-md-6 col-lg-4">
				<std:link style='<%=CaisseWebBaseAction.GET_STYLE_CONF("BUTTON_SOUS_MENU", "COULEUR_TEXT_DETAIL")%>' action="caisse-web.caisseWeb.menuCompoEvent" targetDiv="right-div" workId="<%=menuP.getId().toString() %>" classStyle="caisse-mobile-btn detail-menu-btn" value="">
					<span class="span-img-stl">
						<img alt="" onerror="replaceErrImg(this);" src="resourcesCtrl?elmnt=<%=EncryptionUtil.encrypt(menuP.getId().toString()) %>&path=menu&rdm=<%=(menuP.getDate_maj()!=null?menuP.getDate_maj().getTime():"1")%>" class="<%=StringUtil.isNotEmpty(prix) ? "caisse-img-stl":"img-caisse-stl" %>">
					</span>	
	        			<span class="caisse-lib-stl">
	        				<span class="caisseSub-prix-stl"><%=libelle %>&nbsp;&nbsp;</span>
		        			<%if(StringUtil.isNotEmpty(prix)){ %>
							<span class="caisse-prix-stl"><%=prix %> MAD</span>
							<%} %>
						</span>	
				</std:link>
			</div>	
	<% 	} 
		else if(elementP instanceof ArticlePersistant){
			String params = "mnu="+cle;
			ArticlePersistant artP = (ArticlePersistant)elementP;
			
			if(BooleanUtil.isTrue(artP.getIs_disable())){
				continue;
			}
			String libelle = artP.getLibelle().replaceAll("\\#", "");
		%>
		<div class="col-xs-12 col-sm-6 col-md-6 col-lg-4">
			<std:link params="<%=params %>" action="caisse-web.caisseWeb.addArticleMenuCmd" targetDiv="left-div" workId="<%=artP.getId().toString() %>" classStyle="caisse-ligne-btn" value="">
				<span class="span-img-stl">
					<img alt="" onerror="replaceErrImg(this);" src="resourcesCtrl?elmnt=<%=EncryptionUtil.encrypt(artP.getId().toString()) %>&path=article&rdm=<%=(artP.getDate_maj()!=null?artP.getDate_maj().getTime():"1")%>" class="img-caisse-stl">
				</span>	
	       		<span class="caisse-lib-stl"><%=libelle %>&nbsp;&nbsp;</span>
			</std:link>
		</div>	
	<%	} 
		else if(elementP instanceof MenuCompositionDetailPersistant){
			String params = "det="+((MenuCompositionDetailPersistant)elementP).getId()+"&tp=MC&mnu="+cle;
			MenuCompositionDetailPersistant mnuCompo = (MenuCompositionDetailPersistant)elementP;
			String prix = BigDecimalUtil.isZero(mnuCompo.getPrix()) ? "":BigDecimalUtil.formatNumber(mnuCompo.getPrix());
			
			if(mnuCompo.getOpc_article() != null){
				ArticlePersistant artP = mnuCompo.getOpc_article();
				if(BooleanUtil.isTrue(artP.getIs_disable())){
					continue;
				}
				String libelle = artP.getLibelle().replaceAll("\\#", "");
			%>
			<div class="col-xs-12 col-sm-6 col-md-6 col-lg-4">
				<std:link params="<%=params %>" action="caisse-web.caisseWeb.addArticleMenuCmd" targetDiv="left-div" workId="<%=artP.getId().toString() %>" classStyle="caisse-ligne-btn" value="">
					<span class="span-img-stl">
						<img alt="" onerror="replaceErrImg(this);" src="resourcesCtrl?elmnt=<%=EncryptionUtil.encrypt(artP.getId().toString()) %>&path=article&rdm=<%=(artP.getDate_maj()!=null?artP.getDate_maj().getTime():"1")%>" class="<%=StringUtil.isNotEmpty(prix) ? "caisse-img-stl":"img-caisse-stl" %>">
					</span>	
	        		<span class="caisse-lib-stl">
	        			<span class="caisseSub-prix-stl"><%=libelle %>&nbsp;&nbsp;</span>
		        		<%if(StringUtil.isNotEmpty(prix)){ %>
		        			<span class="caisse-prix-stl"><%=prix %> MAD</span>
		        		<%} %>
		        	</span>		
				</std:link>	
			</div>				
				<%} else if(((MenuCompositionDetailPersistant)elementP).getOpc_famille() != null){
					FamillePersistant famP = ((MenuCompositionDetailPersistant)elementP).getOpc_famille();
					if(BooleanUtil.isTrue(famP.getIs_disable())){
						continue;
					}
					
					String libelle = famP.getLibelle().replaceAll("\\#", "");
					%>
				<div class="col-xs-12 col-sm-6 col-md-6 col-lg-4">	
					<std:link action="caisse-web.caisseWeb.menuCompoDetailEvent" targetDiv="right-div" workId="<%=((MenuCompositionDetailPersistant)elementP).getId().toString() %>" classStyle="caisse-mobile-btn detail-famille-btn" value="">
						<span class="span-img-stl">
							<img alt="" onerror="replaceErrImg(this);" src="resourcesCtrl?elmnt=<%=EncryptionUtil.encrypt(famP.getId().toString()) %>&path=famille&rdm=<%=(famP.getDate_maj()!=null?famP.getDate_maj().getTime():"1")%>" class="<%=StringUtil.isNotEmpty(prix) ? "caisse-img-stl":"img-caisse-stl" %>">
						</span>	
		        		<span class="caisse-lib-stl">
		        			<span class="caisseSub-prix-stl"><%=libelle %>&nbsp;&nbsp;</span>
			        		<%if(StringUtil.isNotEmpty(prix)){ %>
			        			<span class="caisse-prix-stl"><%=prix %> MAD</span>
			        		<%} %>
			        	</span>	
					</std:link>
				</div>	
				<%} else if(((MenuCompositionDetailPersistant)elementP).getOpc_list_choix() != null){
					ListChoixPersistant listChoix = ((MenuCompositionDetailPersistant)elementP).getOpc_list_choix();
					if(BooleanUtil.isTrue(listChoix.getIs_disable())){
						continue;
					}
					
					String libelle = listChoix.getLibelle().replaceAll("\\#", "");
				%>
			<div class="col-xs-12 col-sm-6 col-md-6 col-lg-4">	
				<std:link style='<%=CaisseWebBaseAction.GET_STYLE_CONF("BUTTON_LIST_CHOIX", "COULEUR_TEXT_DETAIL")%>' action="caisse-web.caisseWeb.menuCompoDetailEvent" targetDiv="right-div" workId="<%=((MenuCompositionDetailPersistant)elementP).getId().toString() %>" classStyle="caisse-mobile-btn detail-choix-btn" value="">
	        		<span class="span-img-stl">&nbsp;</span>
	        		<span class="caisse-lib-stl">
	        			<span class="caisseSub-prix-stl"><%=libelle %>&nbsp;&nbsp;</span>
		        		<%if(StringUtil.isNotEmpty(prix)){ %>
		        			<span class="caisse-prix-stl"><%=prix %> MAD</span>
		        		<%} %>
		        	</span>		
				</std:link>
			</div>	
		<%
		}
	 } 
		else if(elementP instanceof ListChoixDetailPersistant){
			String params = "det="+((ListChoixDetailPersistant)elementP).getId()+"&tp=LC&mnu="+cle;
			
			if(((ListChoixDetailPersistant)elementP).getOpc_article() != null){
				ArticlePersistant articleP = ((ListChoixDetailPersistant)elementP).getOpc_article();
				if(BooleanUtil.isTrue(articleP.getIs_disable())){
					continue;
				}
				
				String libelle = articleP.getLibelle().replaceAll("\\#", "");
				%>
				<div class="col-xs-12 col-sm-6 col-md-6 col-lg-4">
					<std:link params="<%=params %>" action="caisse-web.caisseWeb.addArticleMenuCmd" targetDiv="left-div" workId="<%=articleP.getId().toString() %>" classStyle="caisse-ligne-btn" value="">
						<span class="span-img-stl">
							<img alt="" onerror="replaceErrImg(this);" src="resourcesCtrl?elmnt=<%=EncryptionUtil.encrypt(articleP.getId().toString()) %>&path=article&rdm=<%=(articleP.getDate_maj()!=null?articleP.getDate_maj().getTime():"1")%>" class="img-caisse-stl">
						</span>	
		        		<span class="caisse-lib-stl"><%=libelle %>&nbsp;&nbsp;</span>
					</std:link>
				</div>	
			<%} else if(((ListChoixDetailPersistant)elementP).getOpc_famille() != null){
				FamillePersistant familleP = ((ListChoixDetailPersistant)elementP).getOpc_famille();
				if(BooleanUtil.isTrue(familleP.getIs_disable())){
					continue;
				}
				String libelle = familleP.getLibelle().replaceAll("\\#", "");
				%>
				<div class="col-xs-12 col-sm-6 col-md-6 col-lg-4">
					<std:link action="caisse-web.caisseWeb.menuCompoChoixEvent" targetDiv="right-div" workId="<%=((ListChoixDetailPersistant)elementP).getId().toString() %>" classStyle="caisse-mobile-btn detail-famille-btn" value="">
						<span class="span-img-stl">
							<img alt="" onerror="replaceErrImg(this);" src="resourcesCtrl?elmnt=<%=EncryptionUtil.encrypt(familleP.getId().toString()) %>&path=famille&rdm=<%=(familleP.getDate_maj()!=null?familleP.getDate_maj().getTime():"1")%>" class="img-caisse-stl">
						</span>	
		        		<span class="caisse-lib-stl"><%=libelle %>&nbsp;&nbsp;</span>
					</std:link>
				</div>	
			<%} else if(((ListChoixDetailPersistant)elementP).getOpc_list_choix() != null){
				 ListChoixPersistant lisChoixP = ((ListChoixDetailPersistant)elementP).getOpc_list_choix();
				 if(BooleanUtil.isTrue(lisChoixP.getIs_disable())){
						continue;
					}
					String libelle = lisChoixP.getLibelle().replaceAll("\\#", "");
				 %>
				 <div class="col-xs-12 col-sm-6 col-md-6 col-lg-4">
					  <std:link style='<%=CaisseWebBaseAction.GET_STYLE_CONF("BUTTON_LIST_CHOIX", "COULEUR_TEXT_DETAIL")%>' action="caisse-web.caisseWeb.menuCompoChoixEvent" targetDiv="right-div" workId="<%=((ListChoixDetailPersistant)elementP).getId().toString() %>" classStyle="caisse-mobile-btn detail-choix-btn" value="">
		        		<span class="span-img-stl">&nbsp;</span>
		        		<span class="caisse-lib-stl"><%=libelle %>&nbsp;&nbsp;</span>
					  </std:link>
				</div>	  
	  <%	}
		}
	}
}%>

<%if(listSousMenu != null && listSousMenu.size()>0){ %>
<div class="row" id="footer_cmd_div">
 		<div class="btn-group" style='<%=step==0?"display: none;":""%>margin-right: 47px;'>
              <button id="btn-wizard-prev"  type="button" class="btn btn-danger btn-sm btn-prev" targetDiv="right-div" wact='<%=EncryptionUtil.encrypt("caisse-web.caisseWeb.loadNextStep")%>' params="stepr=<%=step %>">
              	<i style="font-size: 20px;" class="fa fa-angle-left"></i>PRÉCÉDENT
              </button>
          </div>
          <div class="btn-group" style='<%=step>=listSousMenu.size()-1?"display: none;":""%>margin-right: 47px;'>
              <button id="btn-wizard-next" type="button" class="btn btn-danger btn-sm btn-next" data-last="Finish" targetDiv="right-div" wact='<%=EncryptionUtil.encrypt("caisse-web.caisseWeb.loadNextStep")%>' params="step=<%=step %>">
              	SUIVANT
              	<i style="font-size: 20px;" class="fa fa-angle-right"></i>
              </button>
          </div>
          <button id="btn-wizard-end" class="btn btn-danger" type="button" onclick="$('#home_lnk').trigger('click');" targetDiv="xxxx" wact='<%=EncryptionUtil.encrypt("caisse-web.caisseWeb.finaliserMenuCmdStep")%>' params="step=<%=step %>">
          	TERMINER
          	<i class="fa fa-check-square-o"></i>
          </button>
 	</div>

	 <!--Page Related Scripts-->
    <script src="resources/framework/js/fuelux/wizard/wizard-custom.js"></script>
    <script src="resources/framework/js/toastr/toastr.js?v=1.0"></script>

    <script type="text/javascript">
        jQuery(function ($) {
            $('#WiredWizard').wizard();
        });
    </script>	
<%} %>  
	
