package com.pfe.gestionpfe.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.pfe.gestionpfe.model.Defense;
import com.pfe.gestionpfe.model.Student;
import com.pfe.gestionpfe.repository.DefenseRepository;

@Service
public class DefenseService {

    private static final String DEPARTEMENT_INFORMATIQUE = "Informatique";

    private final DefenseRepository defenseRepository;
    private final ParametrageData parametrageData;

    public DefenseService(DefenseRepository defenseRepository, ParametrageData parametrageData) {
        this.defenseRepository = defenseRepository;
        this.parametrageData = parametrageData;
    }

    public List<Defense> getAllDefenses() {
        return defenseRepository.findAllInformatiqueOrdered();
    }

    public List<Defense> getDefensesByFiliere(String filiere) {
        if (filiere == null || filiere.isBlank()) {
            return List.of();
        }
        return defenseRepository.findByStudentFiliereOrderByDateSoutenanceAscHeureDebutAsc(filiere);
    }

    public Defense saveDefense(Defense defense) {

        if (defense == null) {
            throw new IllegalArgumentException("La soutenance est invalide.");
        }

        if (defense.getStudent() != null) {
            Student student = defense.getStudent();

            if (!DEPARTEMENT_INFORMATIQUE.equalsIgnoreCase(student.getDepartement())) {
                throw new IllegalArgumentException("Seuls les étudiants du département Informatique sont autorisés.");
            }

            if (student.getEncadrant() == null) {
                throw new IllegalArgumentException("L'étudiant doit avoir un encadrant avant d'ajouter une soutenance.");
            }

            defense.setEncadrant(student.getEncadrant());

            if (defense.getLangue() == null || defense.getLangue().isBlank()) {
                defense.setLangue(student.getLangueSoutenance());
            }
        }

        if (defense.getStatut() == null || defense.getStatut().isBlank()) {
            defense.setStatut("PLANIFIEE");
        }

        if (defense.getHeureDebut() != null) {
            List<LocalTime> creneaux = parametrageData.getWorkingSlots();
            if (!creneaux.contains(defense.getHeureDebut())) {
                throw new IllegalArgumentException("Les horaires autorisés sont : "
                        + parametrageData.getWorkingSlotsAsString() + ".");
            }

            defense.setHeureFin(defense.getHeureDebut().plusMinutes(parametrageData.getDureeSoutenanceMinutes()));
        }

        validateDefense(defense);

        return defenseRepository.save(defense);
    }

    private void validateDefense(Defense defense) {

        if (defense.getStudent() == null) {
            throw new IllegalArgumentException("L'étudiant est obligatoire.");
        }
        if (defense.getEncadrant() == null) {
            throw new IllegalArgumentException("L'encadrant est obligatoire.");
        }
        if (defense.getJury1() == null) {
            throw new IllegalArgumentException("Le jury 1 est obligatoire.");
        }
        if (defense.getJury2() == null) {
            throw new IllegalArgumentException("Le jury 2 est obligatoire.");
        }
        if (defense.getRoom() == null) {
            throw new IllegalArgumentException("La salle est obligatoire.");
        }
        if (defense.getDateSoutenance() == null) {
            throw new IllegalArgumentException("La date de soutenance est obligatoire.");
        }
        if (defense.getHeureDebut() == null) {
            throw new IllegalArgumentException("L'heure de début est obligatoire.");
        }
        if (defense.getHeureFin() == null) {
            throw new IllegalArgumentException("L'heure de fin est obligatoire.");
        }
        if (defense.getLangue() == null || defense.getLangue().isBlank()) {
            throw new IllegalArgumentException("La langue est obligatoire.");
        }
        if (defense.getStatut() == null || defense.getStatut().isBlank()) {
            throw new IllegalArgumentException("Le statut est obligatoire.");
        }

        if (!DEPARTEMENT_INFORMATIQUE.equalsIgnoreCase(defense.getStudent().getDepartement())) {
            throw new IllegalArgumentException("Seules les soutenances Informatique sont autorisées.");
        }

        validateDateAgainstParametres(defense.getDateSoutenance());

        if (!parametrageData.getWorkingSlots().contains(defense.getHeureDebut())) {
            throw new IllegalArgumentException("L'heure de début doit respecter les créneaux autorisés ("
                    + parametrageData.getWorkingSlotsAsString() + ").");
        }

        if (!defense.getHeureFin().equals(defense.getHeureDebut().plusMinutes(parametrageData.getDureeSoutenanceMinutes()))) {
            throw new IllegalArgumentException("L'heure de fin ne respecte pas la durée fixée dans les paramètres.");
        }

        if (!defense.getHeureFin().isAfter(defense.getHeureDebut())) {
            throw new IllegalArgumentException("L'heure de fin doit être après l'heure de début.");
        }

        if (defense.getJury1().getId().equals(defense.getJury2().getId())) {
            throw new IllegalArgumentException("Le jury 1 et le jury 2 doivent être différents.");
        }

        if (defense.getEncadrant().getId().equals(defense.getJury1().getId())
                || defense.getEncadrant().getId().equals(defense.getJury2().getId())) {
            throw new IllegalArgumentException("L'encadrant ne peut pas être jury 1 ou jury 2.");
        }

        // Règle métier : un étudiant doit avoir une seule soutenance au total.
        if (defense.getStudent() != null && defense.getStudent().getId() != null) {
            List<Defense> allDefenses = defenseRepository.findAll();
            for (Defense existing : allDefenses) {
                if (defense.getId() != null && defense.getId().equals(existing.getId())) {
                    continue;
                }
                if (existing.getStudent() != null
                        && existing.getStudent().getId() != null
                        && existing.getStudent().getId().equals(defense.getStudent().getId())) {
                    throw new IllegalArgumentException("Cet étudiant a déjà une soutenance. Un étudiant ne peut avoir qu'une seule soutenance.");
                }
            }
        }

        List<Defense> sameDate = defenseRepository.findByDateSoutenance(defense.getDateSoutenance());

        for (Defense existing : sameDate) {

            if (defense.getId() != null && defense.getId().equals(existing.getId())) {
                continue;
            }

            if (isOverlap(defense.getHeureDebut(), defense.getHeureFin(),
                    existing.getHeureDebut(), existing.getHeureFin())) {

                if (existing.getRoom() != null
                        && defense.getRoom() != null
                        && existing.getRoom().getId().equals(defense.getRoom().getId())) {
                    throw new IllegalArgumentException("La salle est déjà occupée dans ce créneau.");
                }
            }

            if (isOverlapWithPause(defense.getHeureDebut(), defense.getHeureFin(),
                    existing.getHeureDebut(), existing.getHeureFin())) {

                if (sameTeacher(defense.getEncadrant(), existing.getEncadrant())
                        || sameTeacher(defense.getEncadrant(), existing.getJury1())
                        || sameTeacher(defense.getEncadrant(), existing.getJury2())
                        || sameTeacher(defense.getJury1(), existing.getEncadrant())
                        || sameTeacher(defense.getJury1(), existing.getJury1())
                        || sameTeacher(defense.getJury1(), existing.getJury2())
                        || sameTeacher(defense.getJury2(), existing.getEncadrant())
                        || sameTeacher(defense.getJury2(), existing.getJury1())
                        || sameTeacher(defense.getJury2(), existing.getJury2())) {
                    throw new IllegalArgumentException("Un enseignant est déjà occupé ou n'a pas au moins 1h de pause entre deux soutenances.");
                }
            }
        }
    }

    private void validateDateAgainstParametres(LocalDate date) {
        if (parametrageData.getDateDebut() == null || parametrageData.getDateFin() == null) {
            throw new IllegalArgumentException("Veuillez d'abord configurer les paramètres du planning.");
        }

        if (date.isBefore(parametrageData.getDateDebut()) || date.isAfter(parametrageData.getDateFin())) {
            throw new IllegalArgumentException("La date de soutenance doit être comprise entre "
                    + parametrageData.getDateDebut() + " et " + parametrageData.getDateFin() + ".");
        }

        if (Boolean.TRUE.equals(parametrageData.isExclureWeekend())) {
            DayOfWeek day = date.getDayOfWeek();
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                throw new IllegalArgumentException("Les week-ends sont exclus par les paramètres.");
            }
        }
    }

    private boolean sameTeacher(com.pfe.gestionpfe.model.Teacher a, com.pfe.gestionpfe.model.Teacher b) {
        return a != null && b != null && a.getId() != null && a.getId().equals(b.getId());
    }

    private boolean isOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return false;
        }
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    private boolean isOverlapWithPause(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return false;
        }
        int pauseMinutes = parametrageData.getDureeReposMinutes();
        return isOverlap(start1, end1, start2.minusMinutes(pauseMinutes), end2.plusMinutes(pauseMinutes));
    }

    public Defense getDefenseById(Long id) {
        return defenseRepository.findById(id).orElse(null);
    }

    public void deleteDefense(Long id) {
        defenseRepository.deleteById(id);
    }

    public void deleteAllDefenses() {
        defenseRepository.deleteAll();
    }

    public long countAll() {
        return defenseRepository.countInformatiqueDefenses();
    }

    public long countPlanifiees() {
        return defenseRepository.countByStatut("PLANIFIEE");
    }

    public long countByFiliere(String filiere) {
        if (filiere == null || filiere.isBlank()) {
            return 0;
        }
        return defenseRepository.countByStudentFiliere(filiere);
    }
    public List<Defense> getDefensesByDepartement(String departement) {
        return defenseRepository.findAllInformatiqueOrdered();
    }
}