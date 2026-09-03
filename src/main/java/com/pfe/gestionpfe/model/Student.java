 package com.pfe.gestionpfe.model;

import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Entity
public class Student {

    private static final String DEPARTEMENT_INFORMATIQUE = "Informatique";
    private static final Set<String> FILIERES_INFORMATIQUE = Set.of("GI", "ID", "TDIA");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le CNE est obligatoire.")
    @Pattern(
        regexp = "^[A-Za-z][A-Za-z0-9]*$",
        message = "Le CNE doit commencer par une lettre et contenir uniquement des lettres et chiffres."
    )
    @Column(nullable = false, unique = true, length = 50)
    private String cne;

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
        regexp = "^[A-Za-z0-9._%+-]+@etu\\.uae\\.ac\\.ma$",
        message = "L'email doit être au format xxx@etu.uae.ac.ma."
    )
    @Column(nullable = false, unique = true, length = 190)
    private String email;

    private String departement;

    @NotBlank(message = "La filière est obligatoire.")
    private String filiere;

    private String specialite;
    private String langueSoutenance; // FR / EN / AR
    private String themePfe;
    private Boolean affectationManuelle;

    @ManyToOne
    private Teacher encadrant;

    public Student() {
        this.affectationManuelle = false;
        this.departement = DEPARTEMENT_INFORMATIQUE;
    }

    @PrePersist
    @PreUpdate
    public void normalizeAndValidate() {
        this.departement = DEPARTEMENT_INFORMATIQUE;

        if (this.cne != null) {
            this.cne = this.cne.trim().toUpperCase();
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
        if (this.filiere != null) {
            this.filiere = this.filiere.trim().toUpperCase();
        }
        if (this.specialite != null) {
            this.specialite = this.specialite.trim();
        }
        if (this.langueSoutenance != null) {
            this.langueSoutenance = this.langueSoutenance.trim().toUpperCase();
        }
        if (this.themePfe != null) {
            this.themePfe = this.themePfe.trim();
        }

        if (this.filiere == null || !FILIERES_INFORMATIQUE.contains(this.filiere)) {
            throw new IllegalArgumentException("La filière doit être l'une des suivantes : GI, ID, TDIA.");
        }
    }

    public static Set<String> getFilieresInformatique() {
        return FILIERES_INFORMATIQUE;
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

    public String getCne() {
        return cne;
    }

    public void setCne(String cne) {
        this.cne = cne;
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

    public String getFiliere() {
        return filiere;
    }

    public void setFiliere(String filiere) {
        this.filiere = filiere;
    }

    public String getSpecialite() {
        return specialite;
    }

    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }

    public String getLangueSoutenance() {
        return langueSoutenance;
    }

    public void setLangueSoutenance(String langueSoutenance) {
        this.langueSoutenance = langueSoutenance;
    }

    public String getThemePfe() {
        return themePfe;
    }

    public void setThemePfe(String themePfe) {
        this.themePfe = themePfe;
    }

    public Boolean getAffectationManuelle() {
        return affectationManuelle;
    }

    public void setAffectationManuelle(Boolean affectationManuelle) {
        this.affectationManuelle = affectationManuelle;
    }

    public Teacher getEncadrant() {
        return encadrant;
    }

    public void setEncadrant(Teacher encadrant) {
        this.encadrant = encadrant;
    }
}
