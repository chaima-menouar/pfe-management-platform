package com.pfe.gestionpfe.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "parametrage")
public class ParametrageEntity {

    @Id
    private Long id = 1L;

    private LocalDate dateDebut;
    private LocalDate dateFin;

    private int dureeSoutenanceMinutes = 60;
    private int dureeReposMinutes = 0;

    private LocalTime heureDebutMatin = LocalTime.of(9, 0);
    private LocalTime heureFinMatin = LocalTime.of(12, 0);
    private LocalTime heureDebutApresMidi = LocalTime.of(14, 0);
    private LocalTime heureFinApresMidi = LocalTime.of(18, 0);

    private int maxSoutenancesTeacherPerDay = 7;
    private int maxSoutenancesRoomPerDay = 7;

    private int minJoursPlanning = 3;
    private int maxJoursPlanning = 5;

    private boolean exclureWeekend = true;
    private boolean exigerProfLangue = false;
    private boolean autoriserProfLangueEncadrant = false;

    public ParametrageEntity() {
        this.dateDebut = LocalDate.now()
                .plusWeeks(4)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        this.dateFin = this.dateDebut.plusDays(11);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public int getDureeSoutenanceMinutes() { return dureeSoutenanceMinutes; }
    public void setDureeSoutenanceMinutes(int v) { this.dureeSoutenanceMinutes = v; }

    public int getDureeReposMinutes() { return dureeReposMinutes; }
    public void setDureeReposMinutes(int v) { this.dureeReposMinutes = v; }

    public LocalTime getHeureDebutMatin() { return heureDebutMatin; }
    public void setHeureDebutMatin(LocalTime v) { this.heureDebutMatin = v; }

    public LocalTime getHeureFinMatin() { return heureFinMatin; }
    public void setHeureFinMatin(LocalTime v) { this.heureFinMatin = v; }

    public LocalTime getHeureDebutApresMidi() { return heureDebutApresMidi; }
    public void setHeureDebutApresMidi(LocalTime v) { this.heureDebutApresMidi = v; }

    public LocalTime getHeureFinApresMidi() { return heureFinApresMidi; }
    public void setHeureFinApresMidi(LocalTime v) { this.heureFinApresMidi = v; }

    public int getMaxSoutenancesTeacherPerDay() { return maxSoutenancesTeacherPerDay; }
    public void setMaxSoutenancesTeacherPerDay(int v) { this.maxSoutenancesTeacherPerDay = v; }

    public int getMaxSoutenancesRoomPerDay() { return maxSoutenancesRoomPerDay; }
    public void setMaxSoutenancesRoomPerDay(int v) { this.maxSoutenancesRoomPerDay = v; }

    public int getMinJoursPlanning() { return minJoursPlanning; }
    public void setMinJoursPlanning(int v) { this.minJoursPlanning = v; }

    public int getMaxJoursPlanning() { return maxJoursPlanning; }
    public void setMaxJoursPlanning(int v) { this.maxJoursPlanning = v; }

    public boolean isExclureWeekend() { return exclureWeekend; }
    public void setExclureWeekend(boolean v) { this.exclureWeekend = v; }

    public boolean isExigerProfLangue() { return exigerProfLangue; }
    public void setExigerProfLangue(boolean v) { this.exigerProfLangue = v; }

    public boolean isAutoriserProfLangueEncadrant() { return autoriserProfLangueEncadrant; }
    public void setAutoriserProfLangueEncadrant(boolean v) { this.autoriserProfLangueEncadrant = v; }
}
