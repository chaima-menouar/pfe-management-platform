package com.pfe.gestionpfe.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.pfe.gestionpfe.model.Defense;
import com.pfe.gestionpfe.model.Teacher;
import com.pfe.gestionpfe.repository.DefenseRepository;
import com.pfe.gestionpfe.repository.RoomRepository;
import com.pfe.gestionpfe.repository.StudentRepository;
import com.pfe.gestionpfe.repository.TeacherRepository;

@Service
public class DashboardService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final RoomRepository roomRepository;
    private final DefenseRepository defenseRepository;
    private final ParametrageData parametrageData;

    public DashboardService(StudentRepository studentRepository,
                            TeacherRepository teacherRepository,
                            RoomRepository roomRepository,
                            DefenseRepository defenseRepository,
                            ParametrageData parametrageData) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.roomRepository = roomRepository;
        this.defenseRepository = defenseRepository;
        this.parametrageData = parametrageData;
    }

    public DashboardData buildDashboard() {
        DashboardData data = new DashboardData();

        List<Defense> defenses = defenseRepository.findAll();
        List<Teacher> teachers = teacherRepository.findAll();

        // ── Stats globales ──
        data.setTotalStudents(studentRepository.count());
        data.setTotalTeachers(teacherRepository.count());
        data.setTotalRooms(roomRepository.count());
        data.setTotalDefenses(defenseRepository.count());
        data.setPlanifiedDefenses(defenseRepository.countByStatut("PLANIFIEE"));

        long nonPlanified = Math.max(0, studentRepository.count() - defenseRepository.count());
        data.setNonPlanifiedStudents(nonPlanified);

        // ── Défenses par département ──
        Map<String, Long> defensesByDepartment = new LinkedHashMap<>();
        defensesByDepartment.put("Informatique", defenseRepository.countInformatiqueDefenses());
        data.setDefensesByDepartment(defensesByDepartment);

        // ── Étudiants par département ──
        Map<String, Long> studentsByDepartment = new LinkedHashMap<>();
        studentsByDepartment.put("Informatique", studentRepository.count());
        data.setStudentsByDepartment(studentsByDepartment);

        // ── Défenses par salle ──
        Map<String, Long> roomLoad = new LinkedHashMap<>();
        defenses.stream()
                .filter(d -> d.getRoom() != null)
                .sorted(Comparator.comparing(d -> d.getRoom().getNomSalle()))
                .forEach(d -> roomLoad.merge(d.getRoom().getNomSalle(), 1L, Long::sum));
        data.setDefensesByRoom(roomLoad);

        // ── Charge totale par enseignant (encadrant + jury1 + jury2) ──
        Map<String, Long> loadByTeacher = new LinkedHashMap<>();
        for (Teacher t : teachers) {
            long load = defenses.stream()
                    .filter(d ->
                            (d.getEncadrant() != null && t.getId().equals(d.getEncadrant().getId())) ||
                            (d.getJury1() != null && t.getId().equals(d.getJury1().getId())) ||
                            (d.getJury2() != null && t.getId().equals(d.getJury2().getId())))
                    .count();
            loadByTeacher.put(teacherFullName(t), load);
        }
        List<Map.Entry<String, Long>> sortedLoads = new ArrayList<>(loadByTeacher.entrySet());
        sortedLoads.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        data.setTeacherLoads(sortedLoads);

        long maxLoad = sortedLoads.stream().map(Map.Entry::getValue).max(Long::compareTo).orElse(0L);
        long minLoad = sortedLoads.stream().map(Map.Entry::getValue).min(Long::compareTo).orElse(0L);
        data.setMaxTeacherLoad(maxLoad);
        data.setMinTeacherLoad(minLoad);

        double balanceScore;
        if (maxLoad == 0) {
            balanceScore = 100.0;
        } else {
            balanceScore = 100.0 - (((double) (maxLoad - minLoad) / (double) maxLoad) * 100.0);
            if (balanceScore < 0) balanceScore = 0;
        }
        data.setBalanceScore(Math.round(balanceScore * 100.0) / 100.0);

        // ══════════════════════════════════════════════════════
        // NOUVEAU 1 : Étudiants encadrés par professeur
        // ══════════════════════════════════════════════════════
        Map<String, Long> studentsPerTeacher = new LinkedHashMap<>();
        for (Teacher t : teachers) {
            long count = studentRepository.countByEncadrantId(t.getId());
            if (count > 0) {
                studentsPerTeacher.put(teacherFullName(t), count);
            }
        }
        // Trier par nombre décroissant
        List<Map.Entry<String, Long>> studentsPerTeacherSorted = new ArrayList<>(studentsPerTeacher.entrySet());
        studentsPerTeacherSorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        data.setStudentsPerTeacher(studentsPerTeacherSorted);

        // ══════════════════════════════════════════════════════
        // NOUVEAU 2 : Soutenances par professeur (encadrant uniquement)
        // ══════════════════════════════════════════════════════
        Map<String, Long> defensesByTeacher = new LinkedHashMap<>();
        for (Teacher t : teachers) {
            long count = defenses.stream()
                    .filter(d -> d.getEncadrant() != null && t.getId().equals(d.getEncadrant().getId()))
                    .count();
            if (count > 0) {
                defensesByTeacher.put(teacherFullName(t), count);
            }
        }
        List<Map.Entry<String, Long>> defensesByTeacherSorted = new ArrayList<>(defensesByTeacher.entrySet());
        defensesByTeacherSorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        data.setDefensesByTeacher(defensesByTeacherSorted);

        // ══════════════════════════════════════════════════════
        // NOUVEAU 3 : Soutenances par filière
        // ══════════════════════════════════════════════════════
        Map<String, Long> defensesByFiliere = new LinkedHashMap<>();
        for (Defense d : defenses) {
            if (d.getStudent() != null && d.getStudent().getFiliere() != null) {
                String filiere = normalizeFiliere(d.getStudent().getFiliere());
                defensesByFiliere.merge(filiere, 1L, Long::sum);
            }
        }
        // Trier alphabétiquement par filière
        List<Map.Entry<String, Long>> defensesByFiliereSorted = new ArrayList<>(defensesByFiliere.entrySet());
        defensesByFiliereSorted.sort(Map.Entry.comparingByKey());
        data.setDefensesByFiliere(defensesByFiliereSorted);

        return data;
    }

    private String teacherFullName(Teacher t) {
        String name = ((t.getNom() == null ? "" : t.getNom()) + " " +
                       (t.getPrenom() == null ? "" : t.getPrenom())).trim();
        return name.isBlank() ? "Prof #" + t.getId() : name;
    }
    
    private String normalizeFiliere(String filiere) {
        if (filiere == null) return "AUTRE";
        String upper = filiere.trim().toUpperCase();
        if (upper.contains("GI") || upper.contains("GÉNIE") || upper.contains("GENIE")) return "GI";
        if (upper.contains("ID") || upper.contains("DONNÉES") || upper.contains("DONNEES") || upper.contains("INGÉNIERIE")) return "ID";
        if (upper.contains("TDIA") || upper.contains("TRANSFORM") || upper.contains("DIGIT")) return "TDIA";
        return upper;
    }
}
	