package com.pfe.gestionpfe.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom de la salle est obligatoire.")
    @Size(max = 100, message = "Le nom de la salle ne peut pas dépasser 100 caractères.")
    @Column(nullable = false, unique = true, length = 100)
    private String nomSalle;

    @NotNull(message = "L'état de la salle est obligatoire.")
    private Boolean active;

    @NotNull(message = "La capacité est obligatoire.")
    @Min(value = 1, message = "La capacité doit être supérieure à zéro.")
    private Integer capacite;

    public Room() {
        this.active = true;
        this.capacite = 30;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNomSalle() {
        return nomSalle;
    }

    public void setNomSalle(String nomSalle) {
        this.nomSalle = nomSalle;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getCapacite() {
        return capacite;
    }

    public void setCapacite(Integer capacite) {
        this.capacite = capacite;
    }
}
