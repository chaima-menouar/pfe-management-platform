package com.pfe.gestionpfe.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.pfe.gestionpfe.model.ParametrageEntity;
import com.pfe.gestionpfe.repository.ParametrageRepository;

import jakarta.annotation.PostConstruct;

@Component
public class ParametrageData {

    private final ParametrageRepository repository;
    private ParametrageEntity entity;

    private List<String> departements;
    private Map<String, List<String>> filieresByDepartement;
    private Map<String, List<String>> specialitesByFiliere;

    public ParametrageData(ParametrageRepository repository) {
        this.repository = repository;
        initAcademicData();
    }

    @PostConstruct
    public void init() {
        this.entity = repository.findById(1L).orElseGet(() -> {
            ParametrageEntity defaultEntity = new ParametrageEntity();
            return repository.save(defaultEntity);
        });
    }

    private void persist() {
        this.entity = repository.save(this.entity);
    }

    private void initAcademicData() {
        this.departements = new ArrayList<>();
        this.filieresByDepartement = new LinkedHashMap<>();
        this.specialitesByFiliere = new LinkedHashMap<>();

        departements.add("Informatique");

        filieresByDepartement.put("Informatique", List.of("GI", "ID", "TDIA"));

        specialitesByFiliere.put("GI", List.of("GL", "IA", "SE", "BD"));
        specialitesByFiliere.put("ID", List.of("BI", "DataMining", "Decisionnel"));
        specialitesByFiliere.put("TDIA", List.of("IA", "Transformation Digitale", "Data Science"));
    }

    /**
     * Calcule dynamiquement les créneaux disponibles à partir des plages
     * horaires matin/après-midi et de la durée d'une soutenance + repos.
     * Exemple : matin 9h-12h, AM 14h-18h, durée 60min, repos 0
     *  → 9, 10, 11, 14, 15, 16, 17
     */
    public List<LocalTime> getWorkingSlots() {
        List<LocalTime> slots = new ArrayList<>();
        int duree = entity.getDureeSoutenanceMinutes();
        int repos = entity.getDureeReposMinutes();
        int pas = duree + repos;

        if (pas <= 0) {
            return slots;
        }

        LocalTime t = entity.getHeureDebutMatin();
        while (t != null && !t.plusMinutes(duree).isAfter(entity.getHeureFinMatin())) {
            slots.add(t);
            t = t.plusMinutes(pas);
        }

        t = entity.getHeureDebutApresMidi();
        while (t != null && !t.plusMinutes(duree).isAfter(entity.getHeureFinApresMidi())) {
            slots.add(t);
            t = t.plusMinutes(pas);
        }

        return slots;
    }

    public List<String> getWorkingSlotsAsString() {
        List<String> result = new ArrayList<>();
        for (LocalTime t : getWorkingSlots()) {
            result.add(t.toString());
        }
        return result;
    }

    public List<String> getAllSpecialites() {
        List<String> all = new ArrayList<>();
        for (List<String> values : specialitesByFiliere.values()) {
            all.addAll(values);
        }
        return all;
    }

    public List<String> getAllSpecialitesInformatique() {
        return getAllSpecialites();
    }

    public List<String> getFilieresInformatique() {
        return filieresByDepartement.getOrDefault("Informatique", List.of());
    }

    public LocalDate getDateDebut() { return entity.getDateDebut(); }
    public void setDateDebut(LocalDate v) { entity.setDateDebut(v); persist(); }

    public LocalDate getDateFin() { return entity.getDateFin(); }
    public void setDateFin(LocalDate v) { entity.setDateFin(v); persist(); }

    public int getDureeSoutenanceMinutes() { return entity.getDureeSoutenanceMinutes(); }
    public void setDureeSoutenanceMinutes(int v) { entity.setDureeSoutenanceMinutes(v); persist(); }

    public int getDureeReposMinutes() { return entity.getDureeReposMinutes(); }
    public void setDureeReposMinutes(int v) { entity.setDureeReposMinutes(v); persist(); }

    public LocalTime getHeureDebutMatin() { return entity.getHeureDebutMatin(); }
    public void setHeureDebutMatin(LocalTime v) { entity.setHeureDebutMatin(v); persist(); }

    public LocalTime getHeureFinMatin() { return entity.getHeureFinMatin(); }
    public void setHeureFinMatin(LocalTime v) { entity.setHeureFinMatin(v); persist(); }

    public LocalTime getHeureDebutApresMidi() { return entity.getHeureDebutApresMidi(); }
    public void setHeureDebutApresMidi(LocalTime v) { entity.setHeureDebutApresMidi(v); persist(); }

    public LocalTime getHeureFinApresMidi() { return entity.getHeureFinApresMidi(); }
    public void setHeureFinApresMidi(LocalTime v) { entity.setHeureFinApresMidi(v); persist(); }

    public int getMaxSoutenancesTeacherPerDay() { return entity.getMaxSoutenancesTeacherPerDay(); }
    public void setMaxSoutenancesTeacherPerDay(int v) { entity.setMaxSoutenancesTeacherPerDay(v); persist(); }

    public int getMaxSoutenancesRoomPerDay() { return entity.getMaxSoutenancesRoomPerDay(); }
    public void setMaxSoutenancesRoomPerDay(int v) { entity.setMaxSoutenancesRoomPerDay(v); persist(); }

    public int getMinJoursPlanning() { return entity.getMinJoursPlanning(); }
    public void setMinJoursPlanning(int v) { entity.setMinJoursPlanning(v); persist(); }

    public int getMaxJoursPlanning() { return entity.getMaxJoursPlanning(); }
    public void setMaxJoursPlanning(int v) { entity.setMaxJoursPlanning(v); persist(); }

    public boolean isExclureWeekend() { return entity.isExclureWeekend(); }
    public void setExclureWeekend(boolean v) { entity.setExclureWeekend(v); persist(); }

    public boolean isExigerProfLangue() { return entity.isExigerProfLangue(); }
    public void setExigerProfLangue(boolean v) { entity.setExigerProfLangue(v); persist(); }

    public boolean isAutoriserProfLangueEncadrant() { return entity.isAutoriserProfLangueEncadrant(); }
    public void setAutoriserProfLangueEncadrant(boolean v) { entity.setAutoriserProfLangueEncadrant(v); persist(); }

    public List<String> getDepartements() { return departements; }
    public Map<String, List<String>> getFilieresByDepartement() { return filieresByDepartement; }
    public Map<String, List<String>> getSpecialitesByFiliere() { return specialitesByFiliere; }
}
