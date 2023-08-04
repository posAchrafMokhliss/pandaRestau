<%@page import="framework.controller.ContextGloabalAppli"%>
<%@page import="appli.model.domaine.vente.persistant.JourneePersistant"%>
<%@page import="framework.model.common.util.EncryptionUtil"%>
<%@page import="framework.model.common.util.DateUtil"%>
<%@page import="appli.controller.domaine.util_erp.ContextAppli"%>
<%@page import="framework.model.common.util.StringUtil"%>
<%@page import="framework.controller.Context"%>
<%@page import="framework.model.common.util.StrimUtil"%>
<%@page import="framework.model.common.util.BigDecimalUtil"%>
<%@page import="java.math.BigDecimal"%>

<%
String devise = StrimUtil.getGlobalConfigPropertie("devise.symbole");
JourneePersistant journeeVente = (JourneePersistant)request.getAttribute("journeeVente");
if(journeeVente == null){
	journeeVente = new JourneePersistant();
}
BigDecimal fraisLivraison = journeeVente.getTarif_livraison();  
boolean isShiftRight = Context.isOperationAvailable("SHIFT");
boolean isPortefeuille =  StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("PORTEFEUILLE"));
boolean isPoints =  StringUtil.isTrue(ContextGloabalAppli.getGlobalConfig("POINTS"));
%>

<% 
if(isShiftRight){
	BigDecimal totalFraisLivraison = BigDecimalUtil.multiply(fraisLivraison, BigDecimalUtil.get(journeeVente.getNbr_livraison()==null?0:journeeVente.getNbr_livraison()));
%>
		
			<div class="header bordered-darkorange">Répartition des montants des ventes [
				<span style="color: green;font-style: italic;">
					<%if(journeeVente.getDate_journee() != null){ %>
						Journée du <%=DateUtil.dateToString(journeeVente.getDate_journee()) %>
					<%} else{ %>
						Aucune journée disponible
					<%} %>	
				</span>
				]
			</div>
			<div class="databox databox-xxlg databox-vertical databox-shadowed bg-white radius-bordered padding-5" style="height: 539px;">
				<div class="databox-top">
					<div class="databox-row row-12" style="border-bottom: 1px solid gray;height: 122%;">
						<div class="databox-cell cell-7 text-center">
							<div class="databox-number number-xxlg sonic-silver"><%=BigDecimalUtil.formatNumberZero(journeeVente.getMtt_total_net())%> DH</div>
							<div class="databox-text storm-cloud">Montant total TTC</div>
						</div>
						<div class="databox-cell cell-5 text-align-center">
							<div class="databox-number number-xxlg sonic-silver"><%=BigDecimalUtil.formatNumberZero(journeeVente.getNbr_vente())%></div>
							<div class="databox-text storm-cloud">Commandes<br></div>
						</div>
					</div>
				</div>
				<div class="databox-bottom">
					<div class="databox-row row-12">
						<div class="databox-cell cell-12 text-center  padding-12">
							<div id="pie-chart" class="chart chart"></div>
						</div>
					</div>
					
					<div class="databox-row row-12">	
						<div class="databox-cell cell-6 text-center no-padding-left padding-bottom-30">
							<div class="databox-row row-2 bordered-bottom bordered-ivory padding-10">
								<span class="databox-text sonic-silver pull-left no-margin" style="font-weight: bold;">Type de paiement</span> <span class="databox-text sonic-silver pull-right no-margin uppercase">DH</span>
							</div>
							<div class="databox-row row-2 bordered-bottom bordered-ivory padding-10">
								<span class="badge badge-empty pull-left margin-5" style="background-color: #2dc3e8;"></span> <span class="databox-text darkgray pull-left no-margin hidden-xs">Especes</span> <span
									class="databox-text darkgray pull-right no-margin uppercase"> <%=BigDecimalUtil.formatNumberZero(journeeVente.getMtt_espece())%> </span>
							</div>
							<%if(!BigDecimalUtil.isZero(journeeVente.getMtt_cheque())){ %>
							<div class="databox-row row-2 bordered-bottom bordered-ivory padding-10">
								<span class="badge badge-empty pull-left margin-5" style="background-color: #fb6e52;"></span> <span class="databox-text darkgray pull-left no-margin hidden-xs">Ch&egrave;ques</span> <span
									class="databox-text darkgray pull-right no-margin uppercase"><%=BigDecimalUtil.formatNumberZero(journeeVente.getMtt_cheque())%> </span>
							</div>
							<%} %>
							<%if(!BigDecimalUtil.isZero(journeeVente.getMtt_cb())){ %>
							<div class="databox-row row-2 bordered-bottom bordered-ivory padding-10">
								<span class="badge badge-empty pull-left margin-5" style="background-color: #ffce55;"></span> <span class="databox-text darkgray pull-left no-margin hidden-xs">Carte bancaire</span> <span
									class="databox-text darkgray pull-right no-margin uppercase"><%=BigDecimalUtil.formatNumberZero(journeeVente.getMtt_cb())%> </span>
							</div>
							<%} %>
							<%if(!BigDecimalUtil.isZero(journeeVente.getMtt_dej())){ %>
							<div class="databox-row row-2 bordered-bottom bordered-ivory padding-10">
								<span class="badge badge-empty pull-left margin-5" style="background-color: #e75b8d;"></span> <span class="databox-text darkgray pull-left no-margin hidden-xs">Ch&egrave;ques déj.</span> <span
									class="databox-text darkgray pull-right no-margin uppercase"><%=BigDecimalUtil.formatNumberZero(journeeVente.getMtt_dej())%> </span>
							</div>
							<%} %>
							<%if(isPoints && !BigDecimalUtil.isZero(journeeVente.getMtt_donne_point())){ %>
							<div class="databox-row row-2 bordered-bottom bordered-ivory padding-10">
								<span class="badge badge-empty pull-left margin-5" style="background-color: green;"></span> <span class="databox-text darkgray pull-left no-margin hidden-xs">Points fidélité</span> <span
									class="databox-text darkgray pull-right no-margin uppercase"><%=BigDecimalUtil.formatNumberZero(journeeVente.getMtt_donne_point())%> </span>
							</div>
							<%} %>
							<%if(isPortefeuille && !BigDecimalUtil.isZero(journeeVente.getMtt_portefeuille())){ %>
							<div class="databox-row row-2 bordered-bottom bordered-ivory padding-10">
								<span class="badge badge-empty pull-left margin-5" style="background-color: #a0d468;"></span> <span class="databox-text darkgray pull-left no-margin hidden-xs">Portefeuille virtuel</span> <span
									class="databox-text darkgray pull-right no-margin uppercase"><%=BigDecimalUtil.formatNumberZero(journeeVente.getMtt_portefeuille())%> </span>
							</div>
							<%} %>
						</div>
						<div class="databox-cell cell-6 text-center no-padding-left padding-bottom-30">	
							
							<div class="databox-row row-2 bordered-bottom bordered-ivory padding-10">
								<span class="databox-text sonic-silver pull-left no-margin" style="font-weight: bold;">Autre</span> <span class="databox-text sonic-silver pull-right no-margin uppercase">DH</span>
							</div>
							<div class="databox-row row-2 bordered-bottom bordered-ivory padding-10">
								<span class="badge badge-empty pull-left margin-5" style="background-color: #2196f3;"></span> <span class="databox-text darkgray pull-left no-margin hidden-xs">Ouverture</span> <span
									class="databox-text darkgray pull-right no-margin uppercase"><%=BigDecimalUtil.formatNumberZero(journeeVente.getMtt_ouverture())%> </span>
							</div>
							<%if(!BigDecimalUtil.isZero(journeeVente.getMtt_annule())){ %>
								<div class="databox-row row-2 bordered-bottom bordered-ivory padding-10">
									<span class="badge badge-empty pull-left margin-5" style="background-color: #2196f3;"></span> 
									<span class="databox-text darkgray pull-left no-margin hidden-xs">Annulées CMD</span> 
									<span style="color: red !important;" class="databox-text darkgray pull-right no-margin uppercase"><%=BigDecimalUtil.formatNumberZero(journeeVente.getMtt_annule())%> </span>
								</div>
							<%} %>
							<%if(!BigDecimalUtil.isZero(journeeVente.getMtt_annule_ligne())){ %>
								<div class="databox-row row-2 bordered-bottom bordered-ivory padding-10">
									<span class="badge badge-empty pull-left margin-5" style="background-color: #2196f3;"></span> <span class="databox-text darkgray pull-left no-margin hidden-xs">Annulées Ligne</span> 
									<span style="color: red !important;" class="databox-text darkgray pull-right no-margin uppercase"><%=BigDecimalUtil.formatNumberZero(journeeVente.getMtt_annule_ligne())%> </span>
								</div>
							<%} %>
							<%if(!BigDecimalUtil.isZero(journeeVente.getMtt_reduction())){ %>
								<div class="databox-row row-2 bordered-bottom bordered-ivory padding-10">
									<span class="badge badge-empty pull-left margin-5" style="background-color: #2196f3;"></span> <span class="databox-text darkgray pull-left no-margin hidden-xs">Réductions Cmd</span> <span
										class="databox-text darkgray pull-right no-margin uppercase"><%=BigDecimalUtil.formatNumberZero(journeeVente.getMtt_reduction())%> </span>
								</div>
							<%} %>
							<%if(!BigDecimalUtil.isZero(journeeVente.getMtt_art_reduction())){ %>
								<div class="databox-row row-2 bordered-bottom bordered-ivory padding-10">
									<span class="badge badge-empty pull-left margin-5" style="background-color: #2196f3;"></span> <span class="databox-text darkgray pull-left no-margin hidden-xs">Réductions Art</span> <span
										class="databox-text darkgray pull-right no-margin uppercase"><%=BigDecimalUtil.formatNumberZero(journeeVente.getMtt_art_reduction())%> </span>
								</div>
							<%} %>
							<%if(!BigDecimalUtil.isZero(journeeVente.getMtt_art_offert())){ %>
								<div class="databox-row row-2 bordered-bottom bordered-ivory padding-10">
									<span class="badge badge-empty pull-left margin-5" style="background-color: #2196f3;"></span> <span class="databox-text darkgray pull-left no-margin hidden-xs">Art. offerts</span> <span
										class="databox-text darkgray pull-right no-margin uppercase"><%=BigDecimalUtil.formatNumberZero(journeeVente.getMtt_art_offert())%> </span>
								</div>
							<%} %>
						<%
						if(fraisLivraison != null){
							String val = BigDecimalUtil.formatNumberZero(journeeVente.getNbr_livraison())  
									+" x "+BigDecimalUtil.formatNumber(fraisLivraison) + 
									devise;
						%>
						<!-- Livraison -->
						<div class="databox-row row-2 bordered-bottom bordered-ivory padding-10">
							<span class="badge badge-empty pull-left margin-5" style="background-color: black;"></span> <span
								class="databox-text darkgray pull-left no-margin hidden-xs">Livraisons <i style="color:blue;" class="fa fa-info-circle" title="<%=val%>"></i></span> 
								<span class="databox-text darkgray pull-right no-margin uppercase"><%=BigDecimalUtil.formatNumber(totalFraisLivraison) %>
								</span>
						</div>
						<%} %>
						<%if(!BigDecimalUtil.isZero(journeeVente.getMtt_total_achat())){ %>
							<div class="databox-row row-2 bordered-bottom bordered-ivory padding-10">
								<span class="badge badge-empty pull-left margin-5" style="background-color: #2196f3;"></span> <span class="databox-text darkgray pull-left no-margin hidden-xs">
									<b style="color: green;">Marge brute</b> <i class="fa fa-info-circle" title="Prix de vente - Prix d'achat"></i>
								</span> 
								<%
								// Calcul marge 
								BigDecimal mttMargeAll = BigDecimalUtil.substract(journeeVente.getMtt_total_net(), journeeVente.getMttLivraisonPartLivreur(), journeeVente.getMtt_total_achat());
								BigDecimal pourcentMarge = null;
								if(!BigDecimalUtil.isZero(journeeVente.getMtt_total_achat())){
									pourcentMarge = BigDecimalUtil.divide(BigDecimalUtil.multiply(mttMargeAll, BigDecimalUtil.get(100)), journeeVente.getMtt_total_achat());
								}
								%>	
								<span
									class="databox-text darkgray pull-right no-margin uppercase"><%=BigDecimalUtil.formatNumberZero(mttMargeAll) %> 
									 <i class="fa fa-info-circle" title="Total vente : <%=BigDecimalUtil.formatNumberZero(BigDecimalUtil.substract(journeeVente.getMtt_total_net(), journeeVente.getMttLivraisonPartLivreur())) %> | Total valeur achat : <%=BigDecimalUtil.formatNumberZero(journeeVente.getMtt_total_achat()) %> | % Marge brut : <%=BigDecimalUtil.formatNumberZero(pourcentMarge) %>%"></i>
									</span>
							</div>
						<%} %>
						</div>
						</div>
					</div>
				</div>
				
<%} %>
	
<script>
        // If you want to draw your charts with Theme colors you must run initiating charts after that current skin is loaded   
        $(window).ready(function () {
        	$(document).ready(function (){
        		$('.input-group-addon, #situation_dt_debut').datepicker({
        	    	clearBtn: true,
        		    language: "fr",
        		    autoclose: true,
        		    format: "mm/yyyy",
        		    startView: 1,
        		    minViewMode: 1
        	    });
        		$('.input-group-addon, #situation_dt_debut').datepicker().on("changeDate", function(e) {
        	        var currDate = $('#dateDebut').datepicker('getFormattedDate');
        	        submitAjaxForm('<%=EncryptionUtil.encrypt("dash.dashBoard.init_situation_chiffre")%>', 'dt='+currDate, $("#search-form"), $(this));
        	    });
        	});
			$(".databox-cell cell-4 no-padding text-align-center bordered-right bordered-platinum").css("background-color",themefifthcolor);

//-------------------------les commandes Pie Chart----------------------------------------//
		<%BigDecimal mttReel = journeeVente.getMtt_total_net();
		mttReel = (mttReel == null || mttReel.compareTo(BigDecimalUtil.ZERO)==0) ? BigDecimalUtil.get(1) : mttReel;
		%>
		var vEsp= <%=BigDecimalUtil.formatNumber(BigDecimalUtil.divide(BigDecimalUtil.multiply(journeeVente.getMtt_espece(), BigDecimalUtil.get(100)), mttReel)).replace(',', '.')%>;
		var vChq= <%=BigDecimalUtil.formatNumber(BigDecimalUtil.divide(BigDecimalUtil.multiply(journeeVente.getMtt_cheque(), BigDecimalUtil.get(100)), mttReel)).replace(',', '.')%>;
		var vCb= <%=BigDecimalUtil.formatNumber(BigDecimalUtil.divide(BigDecimalUtil.multiply(journeeVente.getMtt_cb(), BigDecimalUtil.get(100)), mttReel)).replace(',', '.')%>;
		var vPF= <%=BigDecimalUtil.formatNumber(BigDecimalUtil.divide(BigDecimalUtil.multiply(journeeVente.getMtt_portefeuille(), BigDecimalUtil.get(100)), mttReel)).replace(',', '.')%>;
		var vDej= <%=BigDecimalUtil.formatNumber(BigDecimalUtil.divide(BigDecimalUtil.multiply(journeeVente.getMtt_dej(), BigDecimalUtil.get(100)), mttReel)).replace(',', '.')%>;
		var vPoint= <%=BigDecimalUtil.formatNumber(BigDecimalUtil.divide(BigDecimalUtil.multiply(journeeVente.getMtt_donne_point(), BigDecimalUtil.get(100)), mttReel)).replace(',', '.')%>;
		var vTotal = vEsp+vChq+vCb+vPF+vDej+vPoint;
		<%if(isShiftRight){%>
		if(vTotal){
			var chartDom = document.getElementById('pie-chart');
			var myChart = echarts.init(chartDom, null,  {width: 400, height: 400});
			var option;	
		option = {
		  title: {
		    text: 'Répartition modes de paiements',
		    //subtext: '',
		    left: 'center'
		  },
		  tooltip: {
		    trigger: 'item'
		  },
		  legend: {
		    orient: 'vertical',
		    left: 'left',
		    show: false
		  },
		  series: [
		    {
		      name: 'Modes de paiement',
		      type: 'pie',
		      radius: '50%',
		      top: '-30%',
		      data: [
		        <%if(!BigDecimalUtil.isZero(journeeVente.getMtt_espece())){%>
		    	{ value: vEsp, name: 'Especes' },
		    	<%}%>
		    	<%if(!BigDecimalUtil.isZero(journeeVente.getMtt_cheque())){%>
		        { value: vChq, name: 'Chèque' },
		        <%}%>
		        <%if(!BigDecimalUtil.isZero(journeeVente.getMtt_cb())){%>
		        { value: vCb, name: 'Carte' },
		        <%}%>
		        <%if(!BigDecimalUtil.isZero(journeeVente.getMtt_portefeuille())){%>
		        { value: vPF, name: 'Portefeuille' },
		        <%}%>
		        <%if(!BigDecimalUtil.isZero(journeeVente.getMtt_dej())){%>
		        { value: vDej, name: 'Chèq. dej' },
		        <%}%>
		        <%if(!BigDecimalUtil.isZero(journeeVente.getMtt_donne_point())){%>
		        { value: vPoint, name: 'Points' }
		        <%}%>
		      ],
		      emphasis: {
			        itemStyle: {
			          shadowBlur: 10,
			          shadowOffsetX: 0,
			          shadowColor: 'rgba(0, 0, 0, 0.5)',
			          
			        }
			      },

			      itemStyle : {
		                normal : {
		                     label : {
		                        show: true, 
		                        position: 'inner',
		                        formatter : function (params){
		                              return params.name + '\n('+ params.percent + '%'+')'
		                        },
		                    },
		                    labelLine : {
		                        show : true
		                    }
		                }},
		    }
		  ]
		};

		option && myChart.setOption(option);
		}
      <%}%>
});
        </script>