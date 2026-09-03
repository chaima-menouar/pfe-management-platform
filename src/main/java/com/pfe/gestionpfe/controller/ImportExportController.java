package com.pfe.gestionpfe.controller;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.pfe.gestionpfe.service.ExcelImportService;
import com.pfe.gestionpfe.service.PdfExportService;

@Controller
public class ImportExportController {

    private final ExcelImportService excelImportService;
    private final PdfExportService pdfExportService;

    public ImportExportController(ExcelImportService excelImportService,
                                  PdfExportService pdfExportService) {
        this.excelImportService = excelImportService;
        this.pdfExportService = pdfExportService;
    }

    @GetMapping("/import-export")
    public String page() {
        return "import-export";
    }

    @GetMapping("/import")
    public String showImportPage() {
        return "import-export";
    }

    @PostMapping("/import-export/import/students-excel")
    public String importStudentsExcel(@RequestParam("files") MultipartFile[] files,
                                      RedirectAttributes redirectAttributes) {
        if (files == null || files.length == 0) {
            redirectAttributes.addFlashAttribute("error", "Aucun fichier sélectionné.");
            return "redirect:/import-export";
        }

        int total = 0;
        java.util.List<String> successDetails = new java.util.ArrayList<>();
        java.util.List<String> errors = new java.util.ArrayList<>();

        for (org.springframework.web.multipart.MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            String name = file.getOriginalFilename() == null ? "(fichier)" : file.getOriginalFilename();
            try {
                int count = excelImportService.importStudentsFromExcel(file);
                total += count;
                successDetails.add(name + " : " + count + " étudiant(s)");
            } catch (Exception e) {
                errors.add(name + " : " + e.getMessage());
            }
        }

        if (total > 0) {
            redirectAttributes.addFlashAttribute("success",
                    total + " étudiant(s) importé(s) au total — " + String.join(" | ", successDetails));
        }
        if (!errors.isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                    "Erreurs d'import : " + String.join(" ; ", errors));
        }
        if (total == 0 && errors.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Aucun fichier valide n'a été traité.");
        }

        return "redirect:/students";
    }

    @PostMapping("/import-export/import/teachers-excel")
    public String importTeachersExcel(@RequestParam("file") MultipartFile file,
                                      RedirectAttributes redirectAttributes) {
        try {
            int count = excelImportService.importTeachersFromExcel(file);
            redirectAttributes.addFlashAttribute("success", count + " enseignant(s) importé(s) depuis Excel.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur import Excel enseignants : " + e.getMessage());
        }
        return "redirect:/teachers";
    }

    @PostMapping("/import-export/import/rooms-excel")
    public String importRoomsExcel(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {
        try {
            int count = excelImportService.importRoomsFromExcel(file);
            redirectAttributes.addFlashAttribute("success", count + " salle(s) importée(s) depuis Excel.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur import Excel salles : " + e.getMessage());
        }
        return "redirect:/rooms";
    }

    // ===================== EXPORT PAR FILIÈRE =====================
    // Utilisés depuis les interfaces Étudiants et Soutenances.
    // Ils exportent uniquement la filière affichée.

    @GetMapping("/import-export/export/assignments-filiere/pdf")
    public ResponseEntity<ByteArrayResource> exportAssignmentsByFilierePdf(@RequestParam String filiere) throws Exception {
        byte[] data = pdfExportService.exportAssignmentsPdf(filiere);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=affectation_encadrants_Informatique_" + safeFilePart(filiere) + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(data.length)
                .body(new ByteArrayResource(data));
    }

    @GetMapping("/import-export/export/planning-filiere/pdf")
    public ResponseEntity<ByteArrayResource> exportPlanningByFilierePdf(@RequestParam String filiere) throws Exception {
        byte[] data = pdfExportService.exportPlanningPdf(filiere);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=planning_soutenances_Informatique_" + safeFilePart(filiere) + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(data.length)
                .body(new ByteArrayResource(data));
    }

    // ===================== EXPORT GLOBAL =====================
    // Utilisés uniquement depuis la page Import / Export.
    // Ils exportent toutes les filières GI + ID + TDIA dans un seul PDF.

    @GetMapping("/import-export/export/assignments-global/pdf")
    public ResponseEntity<ByteArrayResource> exportAssignmentsGlobalPdf() throws Exception {
        byte[] data = pdfExportService.exportAssignmentsPdf(null);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=affectation_encadrants_Informatique_GLOBAL.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(data.length)
                .body(new ByteArrayResource(data));
    }

    @GetMapping("/import-export/export/planning-global/pdf")
    public ResponseEntity<ByteArrayResource> exportPlanningGlobalPdf() throws Exception {
        byte[] data = pdfExportService.exportPlanningPdf(null);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=planning_soutenances_Informatique_GLOBAL.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(data.length)
                .body(new ByteArrayResource(data));
    }

    // Anciens liens conservés : sans filière = global, avec filière = filière.
    @GetMapping("/import-export/export/assignments/pdf")
    public ResponseEntity<ByteArrayResource> exportAssignmentsPdf(@RequestParam(required = false) String filiere) throws Exception {
        if (filiere == null || filiere.isBlank()) {
            return exportAssignmentsGlobalPdf();
        }
        return exportAssignmentsByFilierePdf(filiere);
    }

    @GetMapping("/import-export/export/planning/pdf")
    public ResponseEntity<ByteArrayResource> exportPlanningPdf(@RequestParam(required = false) String filiere) throws Exception {
        if (filiere == null || filiere.isBlank()) {
            return exportPlanningGlobalPdf();
        }
        return exportPlanningByFilierePdf(filiere);
    }

    private String safeFilePart(String value) {
        if (value == null || value.isBlank()) {
            return "GLOBAL";
        }
        return value.trim().replaceAll("[^A-Za-z0-9_-]", "_");
    }
}
