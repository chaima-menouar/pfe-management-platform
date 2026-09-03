package com.pfe.gestionpfe.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.transaction.annotation.Transactional;

import com.pfe.gestionpfe.model.Student;
import com.pfe.gestionpfe.service.AutoAssignmentService;
import com.pfe.gestionpfe.service.ParametrageData;
import com.pfe.gestionpfe.service.PlanningReport;
import com.pfe.gestionpfe.service.StudentService;
import com.pfe.gestionpfe.service.DefenseService;
import com.pfe.gestionpfe.service.TeacherService;

@Controller
public class StudentController {

    private static final List<String> FILIERES = List.of("GI", "ID", "TDIA");

    private final StudentService studentService;
    private final DefenseService defenseService;
    private final TeacherService teacherService;
    private final AutoAssignmentService autoAssignmentService;
    private final ParametrageData parametrageData;

    public StudentController(StudentService studentService,
                             DefenseService defenseService,
                             TeacherService teacherService,
                             AutoAssignmentService autoAssignmentService,
                             ParametrageData parametrageData) {
        this.studentService = studentService;
        this.defenseService = defenseService;
        this.teacherService = teacherService;
        this.autoAssignmentService = autoAssignmentService;
        this.parametrageData = parametrageData;
    }

    @GetMapping("/students")
    public String listStudents(@RequestParam(required = false) String filiere,
                               @RequestParam(required = false) String search,
                               Model model) {
        prepareStudentsPage(model, filiere, search, null, null, new ArrayList<>());
        return "students";
    }// required = false signifie que ces paramètres sont optionnels dans l'URL.

    @GetMapping("/students/new")
    public String showAddStudentForm(Model model) {
        Student student = new Student();
        student.setDepartement("Informatique");
        model.addAttribute("student", student);
        loadStudentFormData(model);
        return "add-student";
    }

    @PostMapping("/students/save")
    public String saveStudent(@Valid @ModelAttribute("student") Student student,
                              BindingResult bindingResult,
                              Model model) {

        if (bindingResult.hasErrors()) {
            loadStudentFormData(model);
            if (student.getId() != null) {
                return "edit-student";
            }
            return "add-student";
        }
        //Si la validation échoue (par exemple CNE vide), on réaffiche le formulaire avec les erreurs. On distingue "ajout" (id == null) de "modification" (id != null) pour retourner le bon template.

        try {
            studentService.saveStudent(student);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            loadStudentFormData(model);
            if (student.getId() != null) {
                return "edit-student";
            }
            return "add-student";
        }
        // 

        return "redirect:/students?filiere=" + encode(student.getFiliere());
    }

    @GetMapping("/students/edit/{id}")
    public String showEditStudentForm(@PathVariable Long id, Model model) {
        Student student = studentService.getStudentById(id);

        if (student == null) {
            model.addAttribute("error", "Étudiant introuvable.");
            prepareStudentsPage(model, null, null, "Étudiant introuvable.", null, new ArrayList<>());
            return "students";
        }

        model.addAttribute("student", student);
        loadStudentFormData(model);
        return "edit-student";
    }

    @PostMapping("/students/delete/{id}")
    public String deleteStudent(@PathVariable Long id, Model model) {
        Student student = studentService.getStudentById(id);
        String filiere = student != null ? student.getFiliere() : null;

        if (!studentService.canDeleteStudent(id)) {
            prepareStudentsPage(
                    model,
                    filiere,
                    null,
                    "Impossible de supprimer cet étudiant car il est déjà utilisé dans une soutenance.",
                    null,
                    new ArrayList<>()
            );
            return "students";
        }
        // On récupère d'abord la filière de l'étudiant AVANT de le supprimer — pour pouvoir rediriger vers la bonne filière après. canDeleteStudent vérifie si l'étudiant est lié à une soutenance : si oui, on ne peut pas supprimer car ça casserait la clé étrangère en base de données.

        studentService.deleteStudent(id);
        return "redirect:/students?filiere=" + encode(filiere);
    }

    @PostMapping("/students/delete-all")
    @Transactional
    public String deleteAllStudents() {
        defenseService.deleteAllDefenses();
        studentService.deleteAllStudents();
        return "redirect:/students?success=deleteAll";
    }

    @PostMapping("/students/auto-assign")
    public String autoAssignEncadrants(@RequestParam(required = false) String filiere,
                                       @RequestParam(required = false) String search,
                                       Model model) {

        // La répartition couvre toujours tout le département Informatique
        // (GI + ID + TDIA) en un seul calcul équilibré. Le paramètre `filiere`,
        // s'il est fourni, n'est plus exigé.
        PlanningReport report = autoAssignmentService.assignSupervisorsByDepartment("Informatique");

        List<String> finalReport = new ArrayList<>();
        finalReport.addAll(report.getPlannedMessages());
        finalReport.addAll(report.getSkippedMessages());
        finalReport.addAll(report.getUnplannedMessages());
        finalReport.addAll(report.getWarningMessages());

        String success = null;
        String error = null;

        if (!report.getPlannedMessages().isEmpty()) {
            success = "Répartition automatique des encadrants effectuée avec succès.";
        } else if (!report.getUnplannedMessages().isEmpty() || !report.getWarningMessages().isEmpty()) {
            error = "Aucune nouvelle affectation n'a été réalisée. Consultez le rapport ci-dessous.";
        }

        prepareStudentsPage(
                model,
                filiere,
                search,
                error,
                success,
                finalReport
        );

        return "students";
    }

    private void loadStudentFormData(Model model) {
        model.addAttribute("teachers", teacherService.getAllTeachers());
        model.addAttribute("filieres", FILIERES);
        model.addAttribute("specialitesByFiliere", parametrageData.getSpecialitesByFiliere());
    }

    private void prepareStudentsPage(Model model,
            String selectedFiliere,
            String search,
            String error,
            String success,
            List<String> report) {
		
		List<Student> allStudents = studentService.searchStudents(search, selectedFiliere);
		
		model.addAttribute("filieres", FILIERES);
		model.addAttribute("selectedFiliere", selectedFiliere);
		model.addAttribute("search", search);
		model.addAttribute("report", report);
		model.addAttribute("filiereColors", buildFiliereColors());
		
		if (error != null) {
		model.addAttribute("error", error);
		}
		
		if (success != null) {
		model.addAttribute("success", success);
		}
		
		if (selectedFiliere == null || selectedFiliere.isBlank()) {
		
		if (allStudents.isEmpty()) {
		model.addAttribute("students", new ArrayList<Student>());
		model.addAttribute("studentsByFiliere", new LinkedHashMap<String, List<Student>>());
		return;
		}
		
		Map<String, List<Student>> studentsByFiliere = new LinkedHashMap<>();
		for (Student student : allStudents) {
		String filiere = student.getFiliere();
		
		if (filiere == null || filiere.isBlank()) {
		filiere = "Non renseignée";
		}
		
		studentsByFiliere
		.computeIfAbsent(filiere, key -> new ArrayList<>())
		.add(student);
		}
		
		model.addAttribute("students", allStudents);
		model.addAttribute("studentsByFiliere", studentsByFiliere);
		return;
		}
		
		List<Student> filteredStudents = allStudents;
		
		Map<String, List<Student>> studentsByFiliere = new LinkedHashMap<>();
		for (Student student : filteredStudents) {
		String filiere = student.getFiliere();
		
		if (filiere == null || filiere.isBlank()) {
		filiere = "Non renseignée";
		}
		
		studentsByFiliere
		.computeIfAbsent(filiere, key -> new ArrayList<>())
		.add(student);
		}
		
		model.addAttribute("students", filteredStudents);
		model.addAttribute("studentsByFiliere", studentsByFiliere);
		}

    private Map<String, String> buildFiliereColors() {
        Map<String, String> colors = new LinkedHashMap<>();
        colors.put("GI", "#c5d9f1");
        colors.put("ID", "#f4b084");
        colors.put("TDIA", "#ffe699");
        return colors;
    }

    private String encode(String value) {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
