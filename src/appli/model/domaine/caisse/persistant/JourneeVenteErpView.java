package appli.model.domaine.caisse.persistant;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import appli.model.domaine.administration.persistant.UserPersistant;

@Entity
@Table(name = "jour_vente_view") 
public class JourneeVenteErpView implements Serializable {
	@Id
	@Column
	private Long mvm_id;
	@Temporal(TemporalType.TIMESTAMP)
    @Column
    private Date date_vente;

    @Column(length = 5)
	private Integer nbr_vente;
    @Column(length = 3)
	private Integer nbr_livraison;
    
     @Column(length = 15, scale = 6, precision = 15)
	private BigDecimal mtt_espece;
	 @Column(length = 15, scale = 6, precision = 15)
	private BigDecimal mtt_cheque;
	 @Column(length = 15, scale = 6, precision = 15)
	private BigDecimal mtt_cb;
	 @Column(length = 15, scale = 6, precision = 15)
	private BigDecimal mtt_total;// Hors réduction
	 @Column(length = 15, scale = 6, precision = 15)
	private BigDecimal mtt_total_net;// Avec prise en compte des réduction et des livraisons après clôture journée
	 @Column(length = 15, scale = 6, precision = 15)
	private BigDecimal mtt_annule;
	 @Column(length = 15, scale = 6, precision = 15)
	private BigDecimal mtt_reduction;
	 @Column(length = 15, scale = 6, precision = 15)
	private BigDecimal mtt_donne_point;
	 @Column(length = 15, scale = 6, precision = 15)
	private BigDecimal mtt_portefeuille;
	 
	 @Column(length = 15, scale = 6, precision = 15)
	private BigDecimal mtt_livraison;
	 @Column(length = 15, scale = 6, precision = 15)
	private BigDecimal mtt_art_offert;
	 @Column(length = 15, scale = 6, precision = 15)
	private BigDecimal mtt_art_reduction;
     
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName="id")
    private UserPersistant opc_user;
    
	public Date getDate_vente() {
		return date_vente;
	}

	public void setDate_vente(Date date_vente) {
		this.date_vente = date_vente;
	}

	public UserPersistant getOpc_user() {
		return opc_user;
	}

	public void setOpc_user(UserPersistant opc_user) {
		this.opc_user = opc_user;
	}

	public Integer getNbr_vente() {
		return nbr_vente;
	}

	public void setNbr_vente(Integer nbr_vente) {
		this.nbr_vente = nbr_vente;
	}

	public Integer getNbr_livraison() {
		return nbr_livraison;
	}

	public void setNbr_livraison(Integer nbr_livraison) {
		this.nbr_livraison = nbr_livraison;
	}

	public BigDecimal getMtt_espece() {
		return mtt_espece;
	}

	public void setMtt_espece(BigDecimal mtt_espece) {
		this.mtt_espece = mtt_espece;
	}

	public BigDecimal getMtt_cheque() {
		return mtt_cheque;
	}

	public void setMtt_cheque(BigDecimal mtt_cheque) {
		this.mtt_cheque = mtt_cheque;
	}

	public BigDecimal getMtt_cb() {
		return mtt_cb;
	}

	public void setMtt_cb(BigDecimal mtt_cb) {
		this.mtt_cb = mtt_cb;
	}

	public BigDecimal getMtt_total() {
		return mtt_total;
	}

	public void setMtt_total(BigDecimal mtt_total) {
		this.mtt_total = mtt_total;
	}

	public BigDecimal getMtt_total_net() {
		return mtt_total_net;
	}

	public void setMtt_total_net(BigDecimal mtt_total_net) {
		this.mtt_total_net = mtt_total_net;
	}

	public BigDecimal getMtt_annule() {
		return mtt_annule;
	}

	public void setMtt_annule(BigDecimal mtt_annule) {
		this.mtt_annule = mtt_annule;
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

	public BigDecimal getMtt_livraison() {
		return mtt_livraison;
	}

	public void setMtt_livraison(BigDecimal mtt_livraison) {
		this.mtt_livraison = mtt_livraison;
	}

	public BigDecimal getMtt_donne_point() {
		return mtt_donne_point;
	}

	public void setMtt_donne_point(BigDecimal mtt_donne_point) {
		this.mtt_donne_point = mtt_donne_point;
	}

	public BigDecimal getMtt_portefeuille() {
		return mtt_portefeuille;
	}

	public void setMtt_portefeuille(BigDecimal mtt_portefeuille) {
		this.mtt_portefeuille = mtt_portefeuille;
	}

	public Long getMvm_id() {
		return mvm_id;
	}

	public void setMvm_id(Long mvm_id) {
		this.mvm_id = mvm_id;
	}

	public BigDecimal getMtt_art_reduction() {
		return mtt_art_reduction;
	}

	public void setMtt_art_reduction(BigDecimal mtt_art_reduction) {
		this.mtt_art_reduction = mtt_art_reduction;
	}
}