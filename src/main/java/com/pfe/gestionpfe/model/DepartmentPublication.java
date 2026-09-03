package com.pfe.gestionpfe.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
public class DepartmentPublication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String departement;

    @Enumerated(EnumType.STRING)
    private PublicationType type;

    private LocalDateTime generatedAt;

    private String generatedBy;

    public DepartmentPublication() {
    }

    public DepartmentPublication(String departement, PublicationType type, LocalDateTime generatedAt, String generatedBy) {
        this.departement = departement;
        this.type = type;
        this.generatedAt = generatedAt;
        this.generatedBy = generatedBy;
    }

    public Long getId() {
        return id;
    }

    public String getDepartement() {
        return departement;
    }

    public void setDepartement(String departement) {
        this.departement = departement;
    }

    public PublicationType getType() {
        return type;
    }

    public void setType(PublicationType type) {
        this.type = type;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }
}