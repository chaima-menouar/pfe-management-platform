package com.pfe.gestionpfe.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.pfe.gestionpfe.model.Student;
import com.pfe.gestionpfe.repository.DefenseRepository;
import com.pfe.gestionpfe.repository.StudentRepository;

@Service
public class StudentService {

    private static final String DEPARTEMENT_INFORMATIQUE = "Informatique";

    private final StudentRepository studentRepository;
    private final DefenseRepository defenseRepository;
    private final NlpService nlpService;

    public StudentService(StudentRepository studentRepository,
                          DefenseRepository defenseRepository,
                          NlpService nlpService) {
        this.studentRepository = studentRepository;
        this.defenseRepository = defenseRepository;
        this.nlpService = nlpService;
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll().stream()
                .filter(s -> s.getDepartement() != null
                        && s.getDepartement().equalsIgnoreCase(DEPARTEMENT_INFORMATIQUE))
                .toList();
    }

    public List<Student> getStudentsByFiliere(String filiere) {
        if (filiere == null || filiere.isBlank()) {
            return List.of();
        }
        return studentRepository.findByFiliereIgnoreCase(filiere).stream()
                .filter(s -> s.getDepartement() != null
                        && s.getDepartement().equalsIgnoreCase(DEPARTEMENT_INFORMATIQUE))
                .toList();
    }

    public List<Student> searchStudents(String query, String filiere) {
        List<Student> baseList;

        if (filiere == null || filiere.isBlank()) {
            baseList = getAllStudents();
        } else {
            baseList = getStudentsByFiliere(filiere);
        }

        if (query == null || query.isBlank()) {
            return baseList;
        }

        String normalizedQuery = query.trim();

        return baseList.stream()
                .filter(student -> matchesStudentSearch(student, normalizedQuery))
                .toList();
    }

    private boolean matchesStudentSearch(Student student, String query) {
        if (student == null) {
            return false;
        }

        boolean basicMatch =
                containsIgnoreCase(student.getNom(), query) ||
                containsIgnoreCase(student.getPrenom(), query) ||
                containsIgnoreCase(student.getCne(), query) ||
                containsIgnoreCase(student.getEmail(), query) ||
                containsIgnoreCase(student.getFiliere(), query) ||
                containsIgnoreCase(student.getSpecialite(), query) ||
                containsIgnoreCase(student.getThemePfe(), query) ||
                (student.getEncadrant() != null && (
                        containsIgnoreCase(student.getEncadrant().getNom(), query) ||
                        containsIgnoreCase(student.getEncadrant().getPrenom(), query) ||
                        containsIgnoreCase(student.getEncadrant().getSpecialite(), query) 
                ));

        if (basicMatch) {
            return true;
        }

        int scoreTheme = nlpService.calculateSimilarityScore(query, student.getThemePfe());
        int scoreSpecialite = nlpService.calculateSimilarityScore(query, student.getSpecialite());

        int scoreEncadrantSpecialite = 0;
        if (student.getEncadrant() != null) {
            scoreEncadrantSpecialite = nlpService.calculateSimilarityScore(
                    query,
                    student.getEncadrant().getSpecialite()
            );
        }

        return scoreTheme > 0 || scoreSpecialite > 0 || scoreEncadrantSpecialite > 0;
    }

    private boolean containsIgnoreCase(String source, String value) {
        if (source == null || value == null) {
            return false;
        }
        return source.toLowerCase().contains(value.toLowerCase());
    }

    public Student saveStudent(Student student) {
        if (student == null) {
            throw new IllegalArgumentException("L'étudiant est invalide.");
        }

        student.setDepartement(DEPARTEMENT_INFORMATIQUE);

        if (student.getAffectationManuelle() == null) {
            student.setAffectationManuelle(false);
        }

        if (Boolean.TRUE.equals(student.getAffectationManuelle()) && student.getEncadrant() == null) {
            throw new IllegalArgumentException("Une affectation manuelle exige un encadrant.");
        }

        if (student.getFiliere() == null || student.getFiliere().isBlank()) {
            throw new IllegalArgumentException("La filière est obligatoire.");
        }

        String filiere = student.getFiliere().trim().toUpperCase();
        if (!Student.getFilieresInformatique().contains(filiere)) {
            throw new IllegalArgumentException("La filière doit être GI, ID ou TDIA.");
        }
        student.setFiliere(filiere);

        if (student.getCne() != null) {
            studentRepository.findByCneIgnoreCase(student.getCne().trim())
                    .ifPresent(existing -> {
                        if (student.getId() == null || !existing.getId().equals(student.getId())) {
                            throw new IllegalArgumentException("Ce CNE existe déjà.");
                        }
                    });
        }

        if (student.getEmail() != null) {
            studentRepository.findByEmailIgnoreCase(student.getEmail().trim())
                    .ifPresent(existing -> {
                        if (student.getId() == null || !existing.getId().equals(student.getId())) {
                            throw new IllegalArgumentException("Cet email étudiant existe déjà.");
                        }
                    });
        }

        return studentRepository.save(student);
    }

    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .filter(s -> s.getDepartement() != null
                        && s.getDepartement().equalsIgnoreCase(DEPARTEMENT_INFORMATIQUE))
                .orElse(null);
    }

    public boolean canDeleteStudent(Long id) {
        return !defenseRepository.existsByStudentId(id);
    }

    public void deleteStudent(Long id) {
        studentRepository.deleteById(id);
    }

    public void clearAllEncadrants() {
        List<Student> students = studentRepository.findAll();
        for (Student student : students) {
            student.setEncadrant(null);
            student.setAffectationManuelle(false);
        }
        studentRepository.saveAll(students);
    }

    public void deleteAllStudents() {
        studentRepository.deleteAll();
    }

    public List<Student> getStudentsByDepartement(String departement) {
        return getAllStudents();
    }
}