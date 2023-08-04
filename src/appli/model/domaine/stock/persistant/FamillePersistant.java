package appli.model.domaine.stock.persistant;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;

import framework.model.beanContext.BasePersistant;

@Entity @Inheritance(strategy=InheritanceType.SINGLE_TABLE) @DiscriminatorColumn(name="type")
@Table(name = "famille", indexes={
		@Index(name="IDX_FAM_CODE", columnList="code"),
		@Index(name="IDX_FAM_FUNC", columnList="code_func")
	})
@NamedQuery(name="famille_find", query="from FamillePersistant famille" +
		" order by famille.b_left")
public class FamillePersistant extends BasePersistant  {
	@Column(length = 80, nullable = false)
	private String code;
	
	@Column(length = 5)
	private Integer nbrPersonne;
	
	@Column(length = 200, nullable = false)
	private String libelle;
	
	@Column(length = 5)
	private Integer b_left;
	
	@Column(length = 5)
	private Integer b_right;
	
	@Transient
	private Integer idx_order;

	@Column(length = 5)
	private Integer level;
	
	@Column(length = 200)
	private String admin_synchro;// Synchronisation avec l'admin [origine_id|opération|code fonctionnel]

	@Column(name="type",insertable=false,updatable=false)
	private String type;

	@Column(length=50)
	public String caisse_target;// Les caisse ou sera uniquement affiché
	@Column
	private Boolean is_noncaisse;// ne pas afficher dans la caisse
	@Column
	private Boolean is_disable;
	
	@Column(length = 255)
	private String description;
	
	public String getLibelle() {
		return libelle;
	}
	public void setLibelle(String libelle) {
		this.libelle = libelle;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public Integer getB_left() {
		return b_left;
	}
	public void setB_left(Integer b_left) {
		this.b_left = b_left;
	}
	public Integer getB_right() {
		return b_right;
	}
	public void setB_right(Integer b_right) {
		this.b_right = b_right;
	}
	public Integer getLevel() {
		return level;
	}
	public void setLevel(Integer level) {
		this.level = level;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Boolean getIs_disable() {
		return is_disable;
	}
	public void setIs_disable(Boolean is_disable) {
		this.is_disable = is_disable;
	}
	public Integer getIdx_order() {
		return idx_order;
	}
	public void setIdx_order(Integer idx_order) {
		this.idx_order = idx_order;
	}
	public String getCaisse_target() {
		return caisse_target;
	}
	public void setCaisse_target(String caisse_target) {
		this.caisse_target = caisse_target;
	}

	public Boolean getIs_noncaisse() {
		return is_noncaisse;
	}
	public void setIs_noncaisse(Boolean is_noncaisse) {
		this.is_noncaisse = is_noncaisse;
	}
	public String getAdmin_synchro() {
		return admin_synchro;
	}
	public void setAdmin_synchro(String admin_synchro) {
		this.admin_synchro = admin_synchro;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Integer getNbrPersonne() {
		return nbrPersonne;
	}
	public void setNbrPersonne(Integer nbrPersonne) {
		this.nbrPersonne = nbrPersonne;
	}
}
