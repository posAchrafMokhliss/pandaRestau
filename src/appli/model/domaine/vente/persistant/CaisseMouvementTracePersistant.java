/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appli.model.domaine.vente.persistant;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import framework.model.beanContext.BasePersistant;

/**
 *
 * @author hp
 */
@Entity
@Table(name = "caisse_mouvement_trace", indexes={
		@Index(name="IDX_CAI_MVM_TRC_FUNC", columnList="code_func"),
		@Index(name="IDX_CAI_MVM_TRC_REF", columnList="ref_commande")
	})
public class CaisseMouvementTracePersistant extends BasePersistant {
	@Column(length = 50)
	private String code_barre;
	
	@Column(length = 20)
	private Long id_origine;// Commande d'origine
	@Column(length = 20)
	private Long journee_id;// Commande d'origine
	@Temporal(TemporalType.TIMESTAMP)
	@Column
	private Date date_vente;
	@Column(length = 30)
	private String ref_commande;
	@Column(length = 1)
	private String type_commande;// L=livraison, P=Sur place, E=A emporter
	@Column(length = 15, scale = 6, precision = 15) 
	private BigDecimal mtt_donne;
	@Column(length = 15, scale = 6, precision = 15)
	private BigDecimal mtt_donne_cheque;
	@Column(length = 15, scale = 6, precision = 15)
	private BigDecimal mtt_donne_cb;
	@Column(length = 15, scale = 6, precision = 15)
	private BigDecimal mtt_donne_dej;
	@Column(length = 15, scale = 6, precision = 15)
	private BigDecimal mtt_portefeuille;
	@Column(length = 15, scale = 6, precision = 15)
	private BigDecimal mtt_donne_point;
	@Column(length = 10)
	private Integer nbr_donne_point;
	@Column(length = 15, scale = 6, precision = 15)
	private BigDecimal mtt_reduction;
	 @Column(length = 15, scale = 6, precision = 15)
		private BigDecimal mtt_art_reduction;
	@Column(length = 15, scale = 6, precision = 15)
	private BigDecimal mtt_art_offert;	// Total des articles offerts
	@Column(length = 15, scale = 6, precision = 15)
	private BigDecimal mtt_marge_caissier;	
	@Column(length = 15, scale = 6, precision = 15)
	private BigDecimal mtt_livraison_ttl;// Total frais de livraison	
	@Column(length = 15, scale = 6, precision = 15)
	private BigDecimal mtt_livraison_livr;// Part livreur
	
	@Column(length = 15, scale = 6, precision = 15)
	private BigDecimal mtt_commande;// Montant de la commande hors réduction
	@Column(length = 15, scale = 6, precision = 15)
	private BigDecimal mtt_commande_net;// Montant de la commande avec prise en compte de la réduction
	
	@Column(length = 15, scale = 6, precision = 15)
	private BigDecimal mtt_a_rendre;
	@Column(length = 15, scale = 6, precision = 15)
	private BigDecimal mtt_donne_all;
	@Column(length = 120)
	private String caisse;
	@Column(length = 30)
	private String mode_paiement;
	@Column(length = 80)
	private String etablissement;
	@Column
	private Boolean is_annule;
	@Column
	private Boolean is_retour;
	@Column(length = 10)
	private String last_statut;// Dernier statut
	@Column
	@Lob
	private String caisse_cuisine;
	@Column
	private Boolean is_imprime;
	//----------------------------------------------------------------
	@Column
	private Date date_journee;
	@Column(length = 80)
	private String client;
	@Column(length = 80)
	private String livreur;
	@Column(length = 80)
	private String societe_livr;
	@Column(length = 80)
	private String employe;
	@Column(length = 80)
	private String user_encaiss;
	@Column(length = 80)
	private String user_annul;
	@Column(length = 80)
	private String serveur;	
	@Column
	@Lob
	private String offres;
	@Column
	@Lob
	private String articles;
	
	public String getCode_barre() {
		return code_barre;
	}
	public void setCode_barre(String code_barre) {
		this.code_barre = code_barre;
	}
	public Date getDate_vente() {
		return date_vente;
	}
	public void setDate_vente(Date date_vente) {
		this.date_vente = date_vente;
	}
	public String getRef_commande() {
		return ref_commande;
	}
	public void setRef_commande(String ref_commande) {
		this.ref_commande = ref_commande;
	}
	public String getType_commande() {
		return type_commande;
	}
	public void setType_commande(String type_commande) {
		this.type_commande = type_commande;
	}
	public BigDecimal getMtt_donne() {
		return mtt_donne;
	}
	public void setMtt_donne(BigDecimal mtt_donne) {
		this.mtt_donne = mtt_donne;
	}
	public BigDecimal getMtt_donne_cheque() {
		return mtt_donne_cheque;
	}
	public void setMtt_donne_cheque(BigDecimal mtt_donne_cheque) {
		this.mtt_donne_cheque = mtt_donne_cheque;
	}
	public BigDecimal getMtt_donne_cb() {
		return mtt_donne_cb;
	}
	public void setMtt_donne_cb(BigDecimal mtt_donne_cb) {
		this.mtt_donne_cb = mtt_donne_cb;
	}
	public BigDecimal getMtt_donne_dej() {
		return mtt_donne_dej;
	}
	public void setMtt_donne_dej(BigDecimal mtt_donne_dej) {
		this.mtt_donne_dej = mtt_donne_dej;
	}
	public BigDecimal getMtt_portefeuille() {
		return mtt_portefeuille;
	}
	public void setMtt_portefeuille(BigDecimal mtt_portefeuille) {
		this.mtt_portefeuille = mtt_portefeuille;
	}
	public BigDecimal getMtt_donne_point() {
		return mtt_donne_point;
	}
	public void setMtt_donne_point(BigDecimal mtt_donne_point) {
		this.mtt_donne_point = mtt_donne_point;
	}
	public Integer getNbr_donne_point() {
		return nbr_donne_point;
	}
	public void setNbr_donne_point(Integer nbr_donne_point) {
		this.nbr_donne_point = nbr_donne_point;
	}
	public BigDecimal getMtt_reduction() {
		return mtt_reduction;
	}
	public void setMtt_reduction(BigDecimal mtt_reduction) {
		this.mtt_reduction = mtt_reduction;
	}
	public BigDecimal getMtt_art_offert() {
		return mtt_art_offert;
	}
	public void setMtt_art_offert(BigDecimal mtt_art_offert) {
		this.mtt_art_offert = mtt_art_offert;
	}
	public BigDecimal getMtt_marge_caissier() {
		return mtt_marge_caissier;
	}
	public void setMtt_marge_caissier(BigDecimal mtt_marge_caissier) {
		this.mtt_marge_caissier = mtt_marge_caissier;
	}
	public BigDecimal getMtt_livraison_ttl() {
		return mtt_livraison_ttl;
	}
	public void setMtt_livraison_ttl(BigDecimal mtt_livraison_ttl) {
		this.mtt_livraison_ttl = mtt_livraison_ttl;
	}
	public BigDecimal getMtt_livraison_livr() {
		return mtt_livraison_livr;
	}
	public void setMtt_livraison_livr(BigDecimal mtt_livraison_livr) {
		this.mtt_livraison_livr = mtt_livraison_livr;
	}
	public BigDecimal getMtt_commande() {
		return mtt_commande;
	}
	public void setMtt_commande(BigDecimal mtt_commande) {
		this.mtt_commande = mtt_commande;
	}
	public BigDecimal getMtt_commande_net() {
		return mtt_commande_net;
	}
	public void setMtt_commande_net(BigDecimal mtt_commande_net) {
		this.mtt_commande_net = mtt_commande_net;
	}
	public BigDecimal getMtt_a_rendre() {
		return mtt_a_rendre;
	}
	public void setMtt_a_rendre(BigDecimal mtt_a_rendre) {
		this.mtt_a_rendre = mtt_a_rendre;
	}
	public BigDecimal getMtt_donne_all() {
		return mtt_donne_all;
	}
	public void setMtt_donne_all(BigDecimal mtt_donne_all) {
		this.mtt_donne_all = mtt_donne_all;
	}
	public String getMode_paiement() {
		return mode_paiement;
	}
	public void setMode_paiement(String mode_paiement) {
		this.mode_paiement = mode_paiement;
	}
	public Boolean getIs_annule() {
		return is_annule;
	}
	public void setIs_annule(Boolean is_annule) {
		this.is_annule = is_annule;
	}
	public Boolean getIs_retour() {
		return is_retour;
	}
	public void setIs_retour(Boolean is_retour) {
		this.is_retour = is_retour;
	}
	public String getLast_statut() {
		return last_statut;
	}
	public void setLast_statut(String last_statut) {
		this.last_statut = last_statut;
	}
	public String getCaisse_cuisine() {
		return caisse_cuisine;
	}
	public void setCaisse_cuisine(String caisse_cuisine) {
		this.caisse_cuisine = caisse_cuisine;
	}
	public Boolean getIs_imprime() {
		return is_imprime;
	}
	public void setIs_imprime(Boolean is_imprime) {
		this.is_imprime = is_imprime;
	}
	public Date getDate_journee() {
		return date_journee;
	}
	public void setDate_journee(Date date_journee) {
		this.date_journee = date_journee;
	}
	public String getClient() {
		return client;
	}
	public void setClient(String client) {
		this.client = client;
	}
	public String getLivreur() {
		return livreur;
	}
	public void setLivreur(String livreur) {
		this.livreur = livreur;
	}
	public String getSociete_livr() {
		return societe_livr;
	}
	public void setSociete_livr(String societe_livr) {
		this.societe_livr = societe_livr;
	}
	public String getEmploye() {
		return employe;
	}
	public void setEmploye(String employe) {
		this.employe = employe;
	}
	public String getUser_encaiss() {
		return user_encaiss;
	}
	public void setUser_encaiss(String user_encaiss) {
		this.user_encaiss = user_encaiss;
	}
	public String getServeur() {
		return serveur;
	}
	public void setServeur(String serveur) {
		this.serveur = serveur;
	}
	public String getOffres() {
		return offres;
	}
	public void setOffres(String offres) {
		this.offres = offres;
	}
	public String getArticles() {
		return articles;
	}
	public void setArticles(String articles) {
		this.articles = articles;
	}
	public Long getId_origine() {
		return id_origine;
	}
	public void setId_origine(Long id_origine) {
		this.id_origine = id_origine;
	}
	public String getEtablissement() {
		return etablissement;
	}
	public void setEtablissement(String etablissement) {
		this.etablissement = etablissement;
	}
	public String getUser_annul() {
		return user_annul;
	}
	public void setUser_annul(String user_annul) {
		this.user_annul = user_annul;
	}
	public String getCaisse() {
		return caisse;
	}
	public void setCaisse(String caisse) {
		this.caisse = caisse;
	}
	public Long getJournee_id() {
		return journee_id;
	}
	public void setJournee_id(Long journee_id) {
		this.journee_id = journee_id;
	}
	public BigDecimal getMtt_art_reduction() {
		return mtt_art_reduction;
	}
	public void setMtt_art_reduction(BigDecimal mtt_art_reduction) {
		this.mtt_art_reduction = mtt_art_reduction;
	}
}
