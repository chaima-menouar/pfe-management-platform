package com.pfe.gestionpfe.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.pfe.gestionpfe.model.Defense;
import com.pfe.gestionpfe.model.Student;
import com.pfe.gestionpfe.service.AutoPlanningService;
import com.pfe.gestionpfe.service.DefenseService;
import com.pfe.gestionpfe.service.ParametrageData;
import com.pfe.gestionpfe.service.PdfExportService;
import com.pfe.gestionpfe.service.PlanningReport;
import com.pfe.gestionpfe.service.RoomService;
import com.pfe.gestionpfe.service.StudentService;
import com.pfe.gestionpfe.service.TeacherService;

@Controller
public class DefenseController {

    private static final List<String> FILIERES = List.of("GI", "ID", "TDIA");

    private final DefenseService defenseService;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final RoomService roomService;
    private final AutoPlanningService autoPlanningService;
    private final ParametrageData parametrageData;
    private final PdfExportService pdfExportService;

    public DefenseController(DefenseService defenseService,
                             StudentService studentService,
                             TeacherService teacherService,
                             RoomService roomService,
                             AutoPlanningService autoPlanningService,
                             ParametrageData parametrageData,
                             PdfExportService pdfExportService) {
        this.defenseService = defenseService;
        this.studentService = studentService;
        this.teacherService = teacherService;
        this.roomService = roomService;
        this.autoPlanningService = autoPlanningService;
        this.parametrageData = parametrageData;
        this.pdfExportService = pdfExportService;
    }

    @GetMapping("/defenses") // Affiche page soutenances.
    public String listDefenses(@RequestParam(required = false) String filiere, Model model) {
        prepareDefensesPage(model, filiere, null, null, null);
        return "defenses";
    }

    @GetMapping("/defenses/new")
    public String showAddDefenseForm(Model model) {
        model.addAttribute("defense", new Defense());
        loadDefenseFormData(model);
        return "add-defense";
    }

    @PostMapping("/defenses/save")
    public String saveDefense(@Valid @ModelAttribute("defense") Defense defense,
                              BindingResult bindingResult,
                              Model model) {

        if (bindingResult.hasErrors()) {
            loadDefenseFormData(model);
            if (defense.getId() != null) {
                return "edit-defense";
            }
            return "add-defense";
        }

        try {
            defenseService.saveDefense(defense);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            loadDefenseFormData(model);// recharge form pour correction
            if (defense.getId() != null) {
                return "edit-defense";
            }
            return "add-defense";
        }

        String filiere = null;
        if (defense.getStudent() != null) {
            filiere = defense.getStudent().getFiliere();
        }

        return "redirect:/defenses?filiere=" + encode(filiere);
    }

    @GetMapping("/defenses/edit/{id}")
    public String showEditDefenseForm(@PathVariable Long id, Model model) {
        Defense defense = defenseService.getDefenseById(id);

        if (defense == null) {
            model.addAttribute("error", "Soutenance introuvable.");
            prepareDefensesPage(model, null, null, "Soutenance introuvable.", null);
            return "defenses";
        }

        model.addAttribute("defense", defense);
        loadDefenseFormData(model);
        return "edit-defense";
    }

    @PostMapping("/defenses/delete-all")
    public String deleteAllDefenses(@RequestParam(required = false) String filiere) {
        defenseService.deleteAllDefenses();
        return "redirect:/defenses?filiere=" + encode(filiere);
    }

    @PostMapping("/defenses/delete/{id}")
    public String deleteDefense(@PathVariable Long id) {
        Defense defense = defenseService.getDefenseById(id);

        String filiere = null;
        if (defense != null && defense.getStudent() != null) {
            filiere = defense.getStudent().getFiliere();
        }

        defenseService.deleteDefense(id);
        return "redirect:/defenses?filiere=" + encode(filiere);
    }

    @PostMapping("/defenses/auto-plan")
    public String autoPlanDefenses(@RequestParam(required = false) String filiere, Model model) {

        // La planification couvre toujours tout le département Informatique
        // (GI + ID + TDIA) en un seul calcul. Le paramètre `filiere`, s'il est
        // fourni, sert uniquement à l'affichage filtré après génération.
        PlanningReport report = autoPlanningService.generatePlanningByDepartment("Informatique");

        boolean hasUnplanned = report.hasUnplannedMessages();
        String success = hasUnplanned ? null
                : "Planification automatique effectuée pour GI + ID + TDIA.";
        String error = hasUnplanned
                ? "Planification non générée — consultez le rapport ci-dessous pour le diagnostic."
                : null;

        prepareDefensesPage(model, filiere, report, error, success);
        return "defenses";
    }

    private void loadDefenseFormData(Model model) {
        List<Student> allStudents = studentService.getAllStudents()
                .stream() // Transforme collection en flux de traitement.
                .filter(s -> s.getDepartement() != null && s.getDepartement().equalsIgnoreCase("Informatique"))
                .toList();

        model.addAttribute("students", allStudents);
        model.addAttribute("teachers", teacherService.getAllTeachers());
        model.addAttribute("rooms", roomService.getAllRooms());
        model.addAttribute("filieres", FILIERES);
        model.addAttribute("creneauxAutorises", List.of(
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                LocalTime.of(16, 0),
                LocalTime.of(17, 0)
        ));
        model.addAttribute("dateMin", parametrageData.getDateDebut());
        model.addAttribute("dateMax", parametrageData.getDateFin());
    }

    private void prepareDefensesPage(Model model,
                                     String selectedFiliere,
                                     PlanningReport report,
                                     String error,
                                     String success) {

        model.addAttribute("filieres", FILIERES);
        model.addAttribute("selectedFiliere", selectedFiliere);
        model.addAttribute("report", report);
        model.addAttribute("filiereColors", buildFiliereColors());

        if (error != null) {
            model.addAttribute("error", error);
        }

        if (success != null) {
            model.addAttribute("success", success);
        }

        // Plus de filtre obligatoire : on charge toujours toutes les soutenances
        // du département. Si une filière est fournie, on filtre l'affichage.
        List<Defense> filteredDefenses = (selectedFiliere == null || selectedFiliere.isBlank())
                ? defenseService.getAllDefenses()
                : defenseService.getDefensesByFiliere(selectedFiliere);

        Map<String, List<Defense>> defensesByFiliere = new LinkedHashMap<>();
        Map<LocalDate, List<Defense>> defensesByDate = new LinkedHashMap<>();

        for (Defense defense : filteredDefenses) {
            String filiere = defense.getStudent() != null ? defense.getStudent().getFiliere() : null;

            if (filiere == null || filiere.isBlank()) {
                filiere = "Non renseignée";
            }

            defensesByFiliere
                    .computeIfAbsent(filiere, key -> new ArrayList<>())
                    .add(defense);

            LocalDate date = defense.getDateSoutenance();
            defensesByDate
                    .computeIfAbsent(date, key -> new ArrayList<>())
                    .add(defense);
        }

        model.addAttribute("defenses", filteredDefenses);
        model.addAttribute("defensesByFiliere", defensesByFiliere);
        model.addAttribute("defensesByDate", defensesByDate);
        model.addAttribute("totalDefenses", filteredDefenses.size());
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

    // ========================= Fiche Evaluation PDF =========================
    @GetMapping("/defenses/fiche/{studentId}")
    public ResponseEntity<byte[]> downloadFicheEvaluation(@PathVariable Long studentId) {
        try {
            Student student = studentService.getStudentById(studentId);
            if (student == null) {
                return ResponseEntity.notFound().build();
            }

            byte[] pdfBytes = pdfExportService.exportFicheEvaluationPfe(studentId);

            String filename = "fiche_evaluation_" + student.getCne() + ".pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
