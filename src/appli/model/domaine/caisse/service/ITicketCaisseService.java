/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appli.model.domaine.caisse.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import appli.controller.domaine.caisse.bean.TicketCaisseBean;
import appli.model.domaine.vente.persistant.TicketCaisseConfPersistant;
import framework.component.complex.table.RequestTableBean;
import framework.model.service.IGenericJpaService;

/**
 *
 * @author hp
 */
public interface ITicketCaisseService extends IGenericJpaService<TicketCaisseBean, Long> {

	void mergeConf(TicketCaisseConfPersistant attestConf);

	void deleteConf(Long workIdLong);

	Map getSituationLivreur(RequestTableBean cplxTable, Long livreurId, Date dateDebut, Date dateFin);

	Map<String, BigDecimal> getSituationClient(RequestTableBean cplxTable, Long clientId, Date dateDebut, Date dateFin);

	Map getSituationSocieteLivr(RequestTableBean cplxTable, Long societeLivrId, Date dateDebut, Date dateFin);

	Map getSituationLivreurErp(RequestTableBean cplxTable, Long livreurId, Date dateDebut, Date dateFin);

	Map getSituationSocieteLivrErp(RequestTableBean cplxTable, Long societeLivrId, Date dateDebut, Date dateFin);

	Map getSituationClientErp(RequestTableBean cplxTable, Long clientId, Date dateDebut, Date dateFin);
}
