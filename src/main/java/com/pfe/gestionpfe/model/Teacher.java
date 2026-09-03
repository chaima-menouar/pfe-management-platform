package com.pfe.gestionpfe.model;

import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Entity
public class Teacher {

    private static final String DEPARTEMENT_INFORMATIQUE = "Informatique";
    private static final Set<String> LANGUES_AUTORISEES = Set.of("FR", "EN", "AR");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le PPR est obligatoire.")
    @Pattern(
        regexp = "^[0-9]+$",
        message = "Le PPR doit contenir uniquement des chiffres."
    )
    @Column(nullable = false, unique = true, length = 50)
    private String ppr;

    @NotBlank(message = "Le nom est obligatoire.")
    @Pattern(
        regexp = "^[A-Za-zÀ-ÿ\\- ]+$",
        message = "Le nom ne doit contenir que des lettres, espaces ou tirets."
    )
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire.")
    @Pattern(
        regexp = "^[A-Za-zÀ-ÿ\\- ]+$",
        message = "Le prénom ne doit contenir que des lettres, espaces ou tirets."
    )
    private String prenom;

    @NotBlank(message = "L'email est obligatoire.")
    @Pattern(
        regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
        message = "L'email n'est pas valide."
    )
    @Column(nullable = false, unique = true, length = 190)
    private String email;

    private String departement;

    private String specialite;
    private String langue; // FR / EN / AR

    private Boolean estProfLangue;
    private Integer maxEncadrement;
    private Integer maxJury;
    private Boolean actif;
    private Boolean disponibleEncadrement;
    private Boolean disponibleJury;

    @Transient
    private String accountPassword;

    public Teacher() {
        this.departement = DEPARTEMENT_INFORMATIQUE;
        this.maxEncadrement = 3;
        this.maxJury = 5;
        this.actif = true;
        this.disponibleEncadrement = true;
        this.disponibleJury = true;
        this.estProfLangue = false;
        this.langue = "FR";
    }

    @PrePersist
    @PreUpdate
    public void normalizeAndValidate() {
        this.departement = DEPARTEMENT_INFORMATIQUE;

        if (this.ppr != null) {
            this.ppr = this.ppr.trim();
        }
        if (this.nom != null) {
            this.nom = this.nom.trim();
        }
        if (this.prenom != null) {
            this.prenom = this.prenom.trim();
        }
        if (this.email != null) {
            this.email = this.email.trim().toLowerCase();
        }
        if (this.specialite != null) {
            this.specialite = this.specialite.trim();
        }
        if (this.langue != null) {
            this.langue = this.langue.trim().toUpperCase();
        }

        if (this.langue == null || !LANGUES_AUTORISEES.contains(this.langue)) {
            throw new IllegalArgumentException("La langue du professeur doit être FR, EN ou AR.");
        }

        if (this.maxEncadrement == null || this.maxEncadrement < 0) {
            this.maxEncadrement = 3;
        }

        if (this.maxJury == null || this.maxJury < 0) {
            this.maxJury = 5;
        }

        if (this.estProfLangue == null) {
            this.estProfLangue = false;
        }
        if (this.actif == null) {
            this.actif = true;
        }
        if (this.disponibleEncadrement == null) {
            this.disponibleEncadrement = true;
        }
        if (this.disponibleJury == null) {
            this.disponibleJury = true;
        }
    }

    public static String getDepartementInformatique() {
        return DEPARTEMENT_INFORMATIQUE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPpr() {
        return ppr;
    }

    public void setPpr(String ppr) {
        this.ppr = ppr;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartement() {
        return DEPARTEMENT_INFORMATIQUE;
    }

    public void setDepartement(String departement) {
        this.departement = DEPARTEMENT_INFORMATIQUE;
    }

    public String getSpecialite() {
        return specialite;
    }

    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }

    public String getLangue() {
        return langue;
    }

    public void setLangue(String langue) {
        this.langue = langue;
    }

    public Boolean getEstProfLangue() {
        return estProfLangue;
    }

    public void setEstProfLangue(Boolean estProfLangue) {
        this.estProfLangue = estProfLangue;
    }

    public Integer getMaxEncadrement() {
        return maxEncadrement;
    }

    public void setMaxEncadrement(Integer maxEncadrement) {
        this.maxEncadrement = maxEncadrement;
    }

    public Integer getMaxJury() {
        return maxJury;
    }

    public void setMaxJury(Integer maxJury) {
        this.maxJury = maxJury;
    }

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }

    public Boolean getDisponibleEncadrement() {
        return disponibleEncadrement;
    }

    public void setDisponibleEncadrement(Boolean disponibleEncadrement) {
        this.disponibleEncadrement = disponibleEncadrement;
    }

    public Boolean getDisponibleJury() {
        return disponibleJury;
    }

    public void setDisponibleJury(Boolean disponibleJury) {
        this.disponibleJury = disponibleJury;
    }

    public String getAccountPassword() {
        return accountPassword;
    }

    public void setAccountPassword(String accountPassword) {
        this.accountPassword = accountPassword;
    }
}
