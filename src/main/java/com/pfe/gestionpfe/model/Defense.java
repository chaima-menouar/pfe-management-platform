package com.pfe.gestionpfe.model;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
public class Defense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "L'étudiant est obligatoire.")
    @ManyToOne
    private Student student;

    @NotNull(message = "L'encadrant est obligatoire.")
    @ManyToOne
    private Teacher encadrant;

    @NotNull(message = "Le jury 1 est obligatoire.")
    @ManyToOne
    private Teacher jury1;

    @NotNull(message = "Le jury 2 est obligatoire.")
    @ManyToOne
    private Teacher jury2;

    @NotNull(message = "La salle est obligatoire.")
    @ManyToOne
    private Room room;

    @NotNull(message = "La date de soutenance est obligatoire.")
    private LocalDate dateSoutenance;

    @NotNull(message = "L'heure de début est obligatoire.")
    private LocalTime heureDebut;

    @NotNull(message = "L'heure de fin est obligatoire.")
    private LocalTime heureFin;

    @NotBlank(message = "Le statut est obligatoire.")
    private String statut; // PLANIFIEE / NON_PLANIFIEE / VALIDEE

    @NotBlank(message = "La langue est obligatoire.")
    private String langue; // FR / EN / AR

    public Defense() {
        this.statut = "PLANIFIEE";
    }

    @PrePersist
    @PreUpdate
    public void validateBusinessRules() {
        if (this.langue != null) {
            this.langue = this.langue.trim().toUpperCase();
        }
        if (this.statut != null) {
            this.statut = this.statut.trim().toUpperCase();
        }

        if (this.student != null) {
            this.encadrant = this.student.getEncadrant();
            if (this.langue == null || this.langue.isBlank()) {
                this.langue = this.student.getLangueSoutenance();
            }
        }

        if (this.heureDebut != null && this.heureFin == null) {
            this.heureFin = this.heureDebut.plusHours(1);
        }

        if (this.heureDebut != null && this.heureFin != null && !this.heureFin.isAfter(this.heureDebut)) {
            throw new IllegalArgumentException("L'heure de fin doit être après l'heure de début.");
        }

        if (this.student != null && this.encadrant != null && this.student.getEncadrant() != null
                && !this.student.getEncadrant().getId().equals(this.encadrant.getId())) {
            throw new IllegalArgumentException("L'encadrant de la soutenance doit être l'encadrant affecté à l'étudiant.");
        }

        if (this.student != null) {
            if (!"Informatique".equalsIgnoreCase(this.student.getDepartement())) {
                throw new IllegalArgumentException("La soutenance doit concerner un étudiant du département Informatique.");
            }
        }

        if (this.jury1 != null && this.encadrant != null && this.jury1.getId().equals(this.encadrant.getId())) {
            throw new IllegalArgumentException("Le jury 1 doit être différent de l'encadrant.");
        }

        if (this.jury2 != null && this.encadrant != null && this.jury2.getId().equals(this.encadrant.getId())) {
            throw new IllegalArgumentException("Le jury 2 doit être différent de l'encadrant.");
        }

        if (this.jury1 != null && this.jury2 != null && this.jury1.getId().equals(this.jury2.getId())) {
            throw new IllegalArgumentException("Le jury 1 et le jury 2 doivent être différents.");
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Teacher getEncadrant() {
        return encadrant;
    }

    public void setEncadrant(Teacher encadrant) {
        this.encadrant = encadrant;
    }

    public Teacher getJury1() {
        return jury1;
    }

    public void setJury1(Teacher jury1) {
        this.jury1 = jury1;
    }

    public Teacher getJury2() {
        return jury2;
    }

    public void setJury2(Teacher jury2) {
        this.jury2 = jury2;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public LocalDate getDateSoutenance() {
        return dateSoutenance;
    }

    public void setDateSoutenance(LocalDate dateSoutenance) {
        this.dateSoutenance = dateSoutenance;
    }

    public LocalTime getHeureDebut() {
        return heureDebut;
    }

    public void setHeureDebut(LocalTime heureDebut) {
        this.heureDebut = heureDebut;
    }

    public LocalTime getHeureFin() {
        return heureFin;
    }

    public void setHeureFin(LocalTime heureFin) {
        this.heureFin = heureFin;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getLangue() {
        return langue;
    }

    public void setLangue(String langue) {
        this.langue = langue;
    }
}