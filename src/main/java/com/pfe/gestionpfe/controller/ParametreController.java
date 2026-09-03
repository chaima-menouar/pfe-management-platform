package com.pfe.gestionpfe.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.pfe.gestionpfe.service.ParametrageData;

@Controller
public class ParametreController {

    private static final List<String> FILIERES_INFORMATIQUE = List.of("GI", "ID", "TDIA");

    private final ParametrageData parametrageData;

    public ParametreController(ParametrageData parametrageData) {
        this.parametrageData = parametrageData;
    }

    @GetMapping("/parametrage")
    public String showParametresPage(Model model) {
        prepareModel(model);
        return "parametrage";
    }

    @PostMapping("/parametrage")
    public String saveParametres(
            @RequestParam String dateDebut,
            @RequestParam String dateFin,
            @RequestParam int dureeSoutenanceMinutes,
            @RequestParam int dureeReposMinutes,
            @RequestParam String heureDebutMatin,
            @RequestParam String heureFinMatin,
            @RequestParam String heureDebutApresMidi,
            @RequestParam String heureFinApresMidi,
            @RequestParam int maxSoutenancesTeacherPerDay,
            @RequestParam int maxSoutenancesRoomPerDay,
            @RequestParam int minJoursPlanning,
            @RequestParam int maxJoursPlanning,
            @RequestParam(defaultValue = "false") boolean exclureWeekend,
            @RequestParam(defaultValue = "false") boolean exigerProfLangue,
            @RequestParam(defaultValue = "false") boolean autoriserProfLangueEncadrant,
            Model model) {

        try {
            LocalDate debut = LocalDate.parse(dateDebut);
            LocalDate fin = LocalDate.parse(dateFin);

            LocalTime hdm = LocalTime.parse(heureDebutMatin);
            LocalTime hfm = LocalTime.parse(heureFinMatin);
            LocalTime hdam = LocalTime.parse(heureDebutApresMidi);
            LocalTime hfam = LocalTime.parse(heureFinApresMidi);

            if (fin.isBefore(debut)) {
                throw new IllegalArgumentException("La date de fin doit être après la date de début.");
            }

            if (dureeSoutenanceMinutes <= 0) {
                throw new IllegalArgumentException("La durée de soutenance doit être positive.");
            }

            if (dureeReposMinutes < 0) {
                throw new IllegalArgumentException("La durée de repos ne peut pas être négative.");
            }

            if (!hfm.isAfter(hdm)) {
                throw new IllegalArgumentException("L'heure de fin matin doit être après le début matin.");
            }
            if (!hfam.isAfter(hdam)) {
                throw new IllegalArgumentException("L'heure de fin après-midi doit être après le début après-midi.");
            }
            if (!hdam.isAfter(hfm) && !hdam.equals(hfm)) {
                throw new IllegalArgumentException("Le début de l'après-midi doit être après la fin du matin.");
            }

            if (maxSoutenancesTeacherPerDay <= 0 || maxSoutenancesRoomPerDay <= 0) {
                throw new IllegalArgumentException("Les maxima par jour doivent être positifs.");
            }

            if (minJoursPlanning <= 0 || maxJoursPlanning <= 0) {
                throw new IllegalArgumentException("Le nombre de jours doit être positif.");
            }

            if (minJoursPlanning > maxJoursPlanning) {
                throw new IllegalArgumentException("Le minimum de jours ne peut pas dépasser le maximum.");
            }

            parametrageData.setDateDebut(debut);
            parametrageData.setDateFin(fin);
            parametrageData.setDureeSoutenanceMinutes(dureeSoutenanceMinutes);
            parametrageData.setDureeReposMinutes(dureeReposMinutes);
            parametrageData.setHeureDebutMatin(hdm);
            parametrageData.setHeureFinMatin(hfm);
            parametrageData.setHeureDebutApresMidi(hdam);
            parametrageData.setHeureFinApresMidi(hfam);
            parametrageData.setMaxSoutenancesTeacherPerDay(maxSoutenancesTeacherPerDay);
            parametrageData.setMaxSoutenancesRoomPerDay(maxSoutenancesRoomPerDay);
            parametrageData.setMinJoursPlanning(minJoursPlanning);
            parametrageData.setMaxJoursPlanning(maxJoursPlanning);
            parametrageData.setExclureWeekend(exclureWeekend);
            parametrageData.setExigerProfLangue(exigerProfLangue);
            parametrageData.setAutoriserProfLangueEncadrant(autoriserProfLangueEncadrant);

            List<LocalTime> slots = parametrageData.getWorkingSlots();
            if (slots.isEmpty()) {
                model.addAttribute("error", "Aucun créneau ne peut être généré. Vérifiez les plages horaires et la durée.");
            } else {
                model.addAttribute("success", "Paramètres mis à jour avec succès. " + slots.size()
                        + " créneau(x) générés : " + parametrageData.getWorkingSlotsAsString() + ".");
            }
        } catch (Exception e) {
            model.addAttribute("error", "Erreur : " + e.getMessage());
        }

        prepareModel(model);
        return "parametrage";
    }

    private void prepareModel(Model model) {
        model.addAttribute("parametrage", parametrageData);
        model.addAttribute("filieres", FILIERES_INFORMATIQUE);
        model.addAttribute("creneauxAutorises", parametrageData.getWorkingSlotsAsString());
    }
}
