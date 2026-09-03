package com.pfe.gestionpfe.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pfe.gestionpfe.model.Student;
import com.pfe.gestionpfe.model.Teacher;
import com.pfe.gestionpfe.repository.StudentRepository;
import com.pfe.gestionpfe.repository.TeacherRepository;

@Service
public class AutoAssignmentService {

    private static final String DEPARTEMENT_INFORMATIQUE = "Informatique";
    private static final Set<String> FILIERES_AUTORISEES = Set.of("GI", "ID", "TDIA");

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final NlpService nlpService;
    private final ParametrageData parametrageData;

    public AutoAssignmentService(StudentRepository studentRepository,
                                 TeacherRepository teacherRepository,
                                 NlpService nlpService,
                                 ParametrageData parametrageData) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.nlpService = nlpService;
        this.parametrageData = parametrageData;
    }

    @Transactional
    public PlanningReport assignSupervisorsByDepartment(String departement) {
        PlanningReport report = new PlanningReport();

        if (departement == null || !departement.equalsIgnoreCase(DEPARTEMENT_INFORMATIQUE)) {
            report.addWarningMessage("Le système est configuré uniquement pour le département Informatique.");
            return report;
        }

        List<Student> students = studentRepository.findByAffectationManuelleFalse()
                .stream()
                .filter(this::isStudentEligible)
                .sorted(studentComparator())
                .toList();

        return assignBalanced(students, report, "département Informatique");
    }

    @Transactional
    public PlanningReport assignSupervisorsByFiliere(String filiere) {
        PlanningReport report = new PlanningReport();

        if (!isValidFiliere(filiere)) {
            report.addWarningMessage("La filière doit être l'une des suivantes : GI, ID, TDIA.");
            return report;
        }

        String filiereNorm = filiere.trim().toUpperCase();

        // IMPORTANT : même si l'utilisateur clique depuis une filière, on recalcule
        // toutes les affectations automatiques du département Informatique.
        // Sinon GI peut être équilibrée seule, puis ID/TDIA gardent les anciennes
        // affectations et le dashboard affiche encore 2..5.
        List<Student> students = studentRepository.findByAffectationManuelleFalse()
                .stream()
                .filter(this::isStudentEligible)
                .sorted(studentComparator())
                .toList();

        report.addWarningMessage("Répartition relancée depuis la filière " + filiereNorm
                + " : recalcul global GI + ID + TDIA pour garantir un écart maximum de 1.");
        return assignBalanced(students, report, "département Informatique");
    }

    /**
     * Répartition équitable stricte :
     * - Les professeurs de langue peuvent être exclus ou autorisés comme encadrants selon le paramétrage.
     * - Les affectations automatiques existantes du périmètre sont recalculées pour éviter les anciens déséquilibres.
     * - La différence de charge entre deux encadrants utilisés est au maximum 1 lorsque la capacité le permet.
     */
    private PlanningReport assignBalanced(List<Student> students, PlanningReport report, String scopeLabel) {
        if (students.isEmpty()) {
            report.addWarningMessage("Aucun étudiant non manuel à traiter pour " + scopeLabel + ".");
            return report;
        }

        List<Teacher> teachers = teacherRepository.findByDisponibleEncadrementTrueAndActifTrue()
                .stream()
                .filter(this::isTeacherEligibleForSupervision)
                .sorted(teacherComparator())
                .toList();

        if (teachers.isEmpty()) {
            if (parametrageData.isAutoriserProfLangueEncadrant()) {
                report.addUnplannedMessage("Aucun enseignant disponible pour l'encadrement.");
            } else {
                report.addUnplannedMessage("Aucun enseignant informatique disponible pour l'encadrement. Les professeurs de langue sont exclus par le paramétrage.");
            }
            return report;
        }

        // Nettoyer les anciennes affectations automatiques du périmètre afin que le nouveau lancement soit vraiment équilibré.
        List<Student> studentsToReset = new ArrayList<>();
        for (Student student : students) {
            if (student.getEncadrant() != null && !Boolean.TRUE.equals(student.getAffectationManuelle())) {
                student.setEncadrant(null);
                studentsToReset.add(student);
            }
        }
        if (!studentsToReset.isEmpty()) {
            studentRepository.saveAll(studentsToReset);
        }

        Map<Long, Integer> runLoad = new HashMap<>();
        int existingAssignments = 0;
        for (Teacher teacher : teachers) {
            int load = (int) Math.min(Integer.MAX_VALUE,
                    studentRepository.countByEncadrantId(teacher.getId()));
            runLoad.put(teacher.getId(), load);
            existingAssignments += load;
        }

        int balancedQuota = (int) Math.ceil(
                (double) (students.size() + existingAssignments) / (double) teachers.size());
        balancedQuota = Math.max(1, balancedQuota);

        for (Student student : students) {
            Teacher selected = findBalancedTeacherForStudent(student, teachers, runLoad, balancedQuota, false);
            if (selected == null) {
                // Dernière tentative : on garde l'équilibrage mais on dépasse le quota si le nombre d'étudiants l'impose.
                selected = findBalancedTeacherForStudent(student, teachers, runLoad, balancedQuota, true);
            }

            if (selected == null) {
                report.addUnplannedMessage("Aucun encadrant adéquat pour : " + studentFullName(student));
                continue;
            }

            student.setEncadrant(selected);
            student.setAffectationManuelle(false);
            studentRepository.save(student);

            runLoad.merge(selected.getId(), 1, Integer::sum);

            int nlpScore = nlpService.calculateSimilarityScore(student.getThemePfe(), selected.getSpecialite());
            report.addPlannedMessage(
                    studentFullName(student)
                            + " -> " + teacherFullName(selected)
                            + " | charge=" + runLoad.get(selected.getId())
                            + " | quota équilibré=" + balancedQuota
                            + " | filière=" + safe(student.getFiliere())
                            + " | spécialité étudiant=" + safe(student.getSpecialite())
                            + " | spécialité encadrant=" + safe(selected.getSpecialite())
                            + " | score NLP=" + nlpScore
            );
        }

        addBalanceSummary(report, teachers, runLoad);
        return report;
    }

    private Teacher findBalancedTeacherForStudent(Student student,
                                                  List<Teacher> teachers,
                                                  Map<Long, Integer> runLoad,
                                                  int balancedQuota,
                                                  boolean allowQuotaOverflow) {
        return teachers.stream()
                .filter(this::isTeacherEligibleForSupervision)
                .filter(t -> {
                    int limit = allowQuotaOverflow
                            ? configuredMaxEncadrement(t)
                            : Math.min(configuredMaxEncadrement(t), balancedQuota);
                    return runLoad.getOrDefault(t.getId(), 0) < limit;
                })
                .map(t -> new TeacherScore(t, scoreTeacherForStudent(student, t), runLoad.getOrDefault(t.getId(), 0)))
                .sorted(Comparator
                        .comparingLong(TeacherScore::load)
                        .thenComparing(Comparator.comparingInt(TeacherScore::score).reversed())
                        .thenComparing(ts -> safe(ts.teacher().getNom()))
                        .thenComparing(ts -> safe(ts.teacher().getPrenom())))
                .map(TeacherScore::teacher)
                .findFirst()
                .orElse(null);
    }

    private int configuredMaxEncadrement(Teacher teacher) {
        if (teacher.getMaxEncadrement() == null) {
            return 3;
        }
        return Math.max(0, teacher.getMaxEncadrement());
    }

    private int scoreTeacherForStudent(Student student, Teacher teacher) {
        int score = 0;
        if (equalsIgnoreCase(student.getSpecialite(), teacher.getSpecialite())) {
            score += 35;
        }
        if (equalsIgnoreCase(student.getLangueSoutenance(), teacher.getLangue())) {
            score += 10;
        }

        int nlpScore = nlpService.calculateSimilarityScore(student.getThemePfe(), teacher.getSpecialite());
        score += nlpScore * 15;

        return score;
    }

    private void addBalanceSummary(PlanningReport report, List<Teacher> teachers, Map<Long, Integer> runLoad) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int used = 0;
        for (Teacher teacher : teachers) {
            int load = runLoad.getOrDefault(teacher.getId(), 0);
            if (load > 0) {
                used++;
                min = Math.min(min, load);
                max = Math.max(max, load);
            }
        }
        if (used > 0) {
            report.addPlannedMessage("Équilibrage encadrants : min=" + min + ", max=" + max + ", différence=" + (max - min) + ".");
        }
    }

    private boolean isStudentEligible(Student student) {
        return student != null
                && equalsIgnoreCase(student.getDepartement(), DEPARTEMENT_INFORMATIQUE)
                && isValidFiliere(student.getFiliere());
    }

    private boolean isTeacherEligibleForSupervision(Teacher teacher) {
        return teacher != null
                && (teacher.getActif() == null || teacher.getActif())
                && (teacher.getDisponibleEncadrement() == null || teacher.getDisponibleEncadrement())
                && equalsIgnoreCase(teacher.getDepartement(), DEPARTEMENT_INFORMATIQUE)
                && (parametrageData.isAutoriserProfLangueEncadrant() || !isLanguageTeacher(teacher));
    }

    private boolean isLanguageTeacher(Teacher teacher) {
        if (teacher == null) {
            return false;
        }
        if (Boolean.TRUE.equals(teacher.getEstProfLangue())) {
            return true;
        }
        String specialite = normalize(teacher.getSpecialite());
        return specialite.contains("anglais")
                || specialite.contains("english")
                || specialite.contains("francais")
                || specialite.contains("français")
                || specialite.equals("fr")
                || specialite.equals("en");
    }

    private boolean isValidFiliere(String filiere) {
        if (filiere == null) {
            return false;
        }
        return FILIERES_AUTORISEES.contains(filiere.trim().toUpperCase());
    }

    private Comparator<Student> studentComparator() {
        return Comparator
                .comparing(Student::getFiliere, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(Student::getSpecialite, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(Student::getNom, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(Student::getPrenom, Comparator.nullsLast(String::compareToIgnoreCase));
    }

    private Comparator<Teacher> teacherComparator() {
        return Comparator
                .comparing(Teacher::getNom, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(Teacher::getPrenom, Comparator.nullsLast(String::compareToIgnoreCase));
    }

    private boolean equalsIgnoreCase(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private String studentFullName(Student s) {
        return (safe(s.getNom()) + " " + safe(s.getPrenom())).trim();
    }

    private String teacherFullName(Teacher t) {
        return (safe(t.getNom()) + " " + safe(t.getPrenom())).trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized;
    }

    private record TeacherScore(Teacher teacher, int score, long load) {
    }
}
