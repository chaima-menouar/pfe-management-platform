package com.pfe.gestionpfe.controller;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import com.pfe.gestionpfe.model.Defense;
import com.pfe.gestionpfe.model.DepartmentPublication;
import com.pfe.gestionpfe.model.Student;
import com.pfe.gestionpfe.model.Teacher;
import com.pfe.gestionpfe.model.User;
import com.pfe.gestionpfe.repository.UserRepository;
import com.pfe.gestionpfe.service.DefenseService;
import com.pfe.gestionpfe.service.DepartmentPublicationService;
import com.pfe.gestionpfe.service.PdfExportService;
import com.pfe.gestionpfe.service.StudentService;

@Controller
public class EncadrantController {

    private final UserRepository userRepo;
    private final StudentService studentService;
    private final DefenseService defenseService;
    private final DepartmentPublicationService publicationService;
    private final PdfExportService pdfExportService;

    public EncadrantController(UserRepository userRepo,
                               StudentService studentService,
                               DefenseService defenseService,
                               DepartmentPublicationService publicationService,
                               PdfExportService pdfExportService) {
        this.userRepo = userRepo;
        this.studentService = studentService;
        this.defenseService = defenseService;
        this.publicationService = publicationService;
        this.pdfExportService = pdfExportService;
    }

    private Teacher getTeacher(Authentication auth) {
        User user = userRepo.findByUsername(auth.getName()).orElseThrow();
        if (user.getTeacher() == null) {
            throw new IllegalStateException("Aucun enseignant lié à cet utilisateur : " + auth.getName());
        }
        return user.getTeacher();
    }

    private List<Student> getStudentsOfEncadrant(Teacher teacher) {
        return studentService.getAllStudents().stream()
                .filter(s -> s.getEncadrant() != null
                        && s.getEncadrant().getId().equals(teacher.getId())) // Garder étudiants dont encadrant = enseignant connecté.
                .sorted(Comparator.comparing(Student::getNom, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private List<Defense> getDefensesOfTeacher(Teacher teacher) {
        return defenseService.getAllDefenses().stream()
                .filter(d ->
                        (d.getEncadrant() != null && d.getEncadrant().getId().equals(teacher.getId()))
                        || (d.getJury1() != null && d.getJury1().getId().equals(teacher.getId()))
                        || (d.getJury2() != null && d.getJury2().getId().equals(teacher.getId()))
                )
                .sorted(Comparator
                        .comparing(Defense::getDateSoutenance, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Defense::getHeureDebut, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private List<Student> getStudentsOfDepartment(String departement) {
        return studentService.getAllStudents().stream()
                .filter(s -> s.getDepartement() != null && s.getDepartement().equalsIgnoreCase(departement))
                .sorted(Comparator.comparing(Student::getNom, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private List<Defense> getDefensesOfDepartment(String departement) {
        return defenseService.getAllDefenses().stream()
                .filter(d -> d.getStudent() != null
                        && d.getStudent().getDepartement() != null
                        && d.getStudent().getDepartement().equalsIgnoreCase(departement))
                .sorted(Comparator
                        .comparing(Defense::getDateSoutenance, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Defense::getHeureDebut, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @GetMapping("/encadrant")
    public String dashboard(Authentication auth, Model model) {
        Teacher teacher = getTeacher(auth);
        List<Student> students = getStudentsOfEncadrant(teacher);
        List<Defense> defenses = getDefensesOfTeacher(teacher);

        DepartmentPublication latestPlanning = publicationService
                .getLatestPlanning(teacher.getDepartement())
                .orElse(null);

        DepartmentPublication latestRepartition = publicationService
                .getLatestRepartition(teacher.getDepartement())
                .orElse(null);

        model.addAttribute("teacher", teacher);
        model.addAttribute("students", students);
        model.addAttribute("defenses", defenses);
        model.addAttribute("studentsCount", students.size());
        model.addAttribute("defensesCount", defenses.size());
        model.addAttribute("latestPlanning", latestPlanning);
        model.addAttribute("latestRepartition", latestRepartition);

        return "encadrant/dashboard";
    }

    @GetMapping("/encadrant/etudiants")
    public String etudiants(Authentication auth, Model model) {
        Teacher teacher = getTeacher(auth);
        List<Student> students = getStudentsOfEncadrant(teacher);

        model.addAttribute("teacher", teacher);
        model.addAttribute("students", students);
        model.addAttribute("studentsCount", students.size());

        return "encadrant/etudiants";
    }

    @GetMapping("/encadrant/soutenances")
    public String soutenances(Authentication auth, Model model) {
        Teacher teacher = getTeacher(auth);
        List<Defense> defenses = getDefensesOfTeacher(teacher);

        model.addAttribute("teacher", teacher);
        model.addAttribute("defenses", defenses);
        model.addAttribute("defensesCount", defenses.size());

        return "encadrant/soutenances";
    }

    @GetMapping("/encadrant/planning")
    public String planning(Authentication auth, Model model) {
        Teacher teacher = getTeacher(auth);
        List<Defense> defenses = getDefensesOfTeacher(teacher);

        model.addAttribute("teacher", teacher);
        model.addAttribute("defenses", defenses);

        return "encadrant/planning";
    }

    @GetMapping("/encadrant/repartition")
    public String repartition(Authentication auth, Model model) {
        Teacher teacher = getTeacher(auth);
        List<Student> students = getStudentsOfEncadrant(teacher);

        model.addAttribute("teacher", teacher);
        model.addAttribute("students", students);

        return "encadrant/repartition";
    }

    @GetMapping("/encadrant/departement/planning")
    public String departmentPlanning(Authentication auth, Model model) {
        Teacher teacher = getTeacher(auth);
        List<Defense> defenses = getDefensesOfDepartment(teacher.getDepartement());

        model.addAttribute("teacher", teacher);
        model.addAttribute("defenses", defenses);

        return "encadrant/departement-planning";
    }

    @GetMapping("/encadrant/departement/repartition")
    public String departmentRepartition(Authentication auth, Model model) {
        Teacher teacher = getTeacher(auth);
        List<Student> students = getStudentsOfDepartment(teacher.getDepartement());

        model.addAttribute("teacher", teacher);
        model.addAttribute("students", students);

        return "encadrant/departement-repartition";
    }

    @GetMapping("/encadrant/download/planning/pdf")
    public ResponseEntity<byte[]> downloadPersonalPlanningPdf(Authentication auth) throws Exception {
        Teacher teacher = getTeacher(auth);
        List<Defense> defenses = getDefensesOfTeacher(teacher);

        byte[] data = pdfExportService.exportPlanningPdfFromDefenses(defenses, teacher.getDepartement());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=planning_personnel.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @GetMapping("/encadrant/download/planning/excel")
    public ResponseEntity<byte[]> downloadPersonalPlanningExcel(Authentication auth) throws Exception {
        Teacher teacher = getTeacher(auth);
        List<Defense> defenses = getDefensesOfTeacher(teacher);

        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Planning personnel");

        int rowIdx = 0;
        Row header = sheet.createRow(rowIdx++);
        header.createCell(0).setCellValue("Date");
        header.createCell(1).setCellValue("Heure debut");
        header.createCell(2).setCellValue("Heure fin");
        header.createCell(3).setCellValue("Etudiant");
        header.createCell(4).setCellValue("Salle");
        header.createCell(5).setCellValue("Role");
        header.createCell(6).setCellValue("Statut");

        for (Defense d : defenses) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(value(d.getDateSoutenance()));
            row.createCell(1).setCellValue(value(d.getHeureDebut()));
            row.createCell(2).setCellValue(value(d.getHeureFin()));
            row.createCell(3).setCellValue(d.getStudent() != null ? d.getStudent().getPrenom() + " " + d.getStudent().getNom() : "-");
            row.createCell(4).setCellValue(d.getRoom() != null ? d.getRoom().getNomSalle() : "-");
            row.createCell(5).setCellValue(getRoleLabel(d, teacher));
            row.createCell(6).setCellValue(value(d.getStatut()));
        }

        for (int i = 0; i < 7; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=planning_personnel.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(out.toByteArray());
    }

    @GetMapping("/encadrant/download/repartition/pdf")
    public ResponseEntity<byte[]> downloadPersonalRepartitionPdf(Authentication auth) throws Exception {
        Teacher teacher = getTeacher(auth);
        List<Student> students = getStudentsOfEncadrant(teacher);
        String teacherName = teacher.getPrenom() + " " + teacher.getNom();

        byte[] data = pdfExportService.exportPersonalRepartitionPdf(students, teacherName, teacher.getDepartement());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=repartition_personnelle.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @GetMapping("/encadrant/download/repartition/excel")
    public ResponseEntity<byte[]> downloadPersonalRepartitionExcel(Authentication auth) throws Exception {
        Teacher teacher = getTeacher(auth);
        List<Student> students = getStudentsOfEncadrant(teacher);

        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Repartition personnelle");

        int rowIdx = 0;
        Row header = sheet.createRow(rowIdx++);
        header.createCell(0).setCellValue("CNE");
        header.createCell(1).setCellValue("Nom");
        header.createCell(2).setCellValue("Prenom");
        header.createCell(3).setCellValue("Email");
        header.createCell(4).setCellValue("Filiere");
        header.createCell(5).setCellValue("Specialite");

        for (Student s : students) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(value(s.getCne()));
            row.createCell(1).setCellValue(value(s.getNom()));
            row.createCell(2).setCellValue(value(s.getPrenom()));
            row.createCell(3).setCellValue(value(s.getEmail()));
            row.createCell(4).setCellValue(value(s.getFiliere()));
            row.createCell(5).setCellValue(value(s.getSpecialite()));
        }

        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=repartition_personnelle.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(out.toByteArray());
    }

    @GetMapping("/encadrant/download/departement/planning/pdf")
    public ResponseEntity<byte[]> downloadDepartmentPlanningPdf(Authentication auth) throws Exception {
        Teacher teacher = getTeacher(auth);
        List<Defense> defenses = getDefensesOfDepartment(teacher.getDepartement());

        byte[] data = pdfExportService.exportPlanningPdfFromDefenses(defenses, teacher.getDepartement());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=planning_departement.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @GetMapping("/encadrant/download/departement/planning/excel")
    public ResponseEntity<byte[]> downloadDepartmentPlanningExcel(Authentication auth) throws Exception {
        Teacher teacher = getTeacher(auth);
        List<Defense> defenses = getDefensesOfDepartment(teacher.getDepartement());

        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Planning departement");

        int rowIdx = 0;
        Row header = sheet.createRow(rowIdx++);
        header.createCell(0).setCellValue("Date");
        header.createCell(1).setCellValue("Heure debut");
        header.createCell(2).setCellValue("Heure fin");
        header.createCell(3).setCellValue("Etudiant");
        header.createCell(4).setCellValue("Encadrant");
        header.createCell(5).setCellValue("Jury 1");
        header.createCell(6).setCellValue("Jury 2");
        header.createCell(7).setCellValue("Salle");

        for (Defense d : defenses) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(value(d.getDateSoutenance()));
            row.createCell(1).setCellValue(value(d.getHeureDebut()));
            row.createCell(2).setCellValue(value(d.getHeureFin()));
            row.createCell(3).setCellValue(d.getStudent() != null ? d.getStudent().getPrenom() + " " + d.getStudent().getNom() : "-");
            row.createCell(4).setCellValue(d.getEncadrant() != null ? d.getEncadrant().getPrenom() + " " + d.getEncadrant().getNom() : "-");
            row.createCell(5).setCellValue(d.getJury1() != null ? d.getJury1().getPrenom() + " " + d.getJury1().getNom() : "-");
            row.createCell(6).setCellValue(d.getJury2() != null ? d.getJury2().getPrenom() + " " + d.getJury2().getNom() : "-");
            row.createCell(7).setCellValue(d.getRoom() != null ? d.getRoom().getNomSalle() : "-");
        }

        for (int i = 0; i < 8; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=planning_departement.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(out.toByteArray());
    }

    @GetMapping("/encadrant/download/departement/repartition/pdf")
    public ResponseEntity<byte[]> downloadDepartmentRepartitionPdf(Authentication auth) throws Exception {
        Teacher teacher = getTeacher(auth);
        List<Student> students = getStudentsOfDepartment(teacher.getDepartement());

        byte[] data = pdfExportService.exportDepartmentRepartitionPdf(students, teacher.getDepartement());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=repartition_departement.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @GetMapping("/encadrant/download/departement/repartition/excel")
    public ResponseEntity<byte[]> downloadDepartmentRepartitionExcel(Authentication auth) throws Exception {
        Teacher teacher = getTeacher(auth);
        List<Student> students = getStudentsOfDepartment(teacher.getDepartement());

        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Repartition departement");

        int rowIdx = 0;
        Row header = sheet.createRow(rowIdx++);
        header.createCell(0).setCellValue("CNE");
        header.createCell(1).setCellValue("Nom");
        header.createCell(2).setCellValue("Prenom");
        header.createCell(3).setCellValue("Filiere");
        header.createCell(4).setCellValue("Specialite");
        header.createCell(5).setCellValue("Encadrant");
        header.createCell(6).setCellValue("Email");

        for (Student s : students) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(value(s.getCne()));
            row.createCell(1).setCellValue(value(s.getNom()));
            row.createCell(2).setCellValue(value(s.getPrenom()));
            row.createCell(3).setCellValue(value(s.getFiliere()));
            row.createCell(4).setCellValue(value(s.getSpecialite()));
            row.createCell(5).setCellValue(s.getEncadrant() != null ? s.getEncadrant().getPrenom() + " " + s.getEncadrant().getNom() : "-");
            row.createCell(6).setCellValue(value(s.getEmail()));
        }

        for (int i = 0; i < 7; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=repartition_departement.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(out.toByteArray());
    }

    private void addCell(PdfPTable table, String value) {
        table.addCell(new PdfPCell(new Phrase(value))); //Méthode utilitaire PDF.
    }

    private String value(Object object) {
        if (object == null) return "-";
        if (object instanceof java.time.LocalDate date) {
            return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        if (object instanceof java.time.LocalTime time) {
            return time.format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        return String.valueOf(object);
    }

    private String getRoleLabel(Defense d, Teacher teacher) { //Déterminer rôle enseignant dans soutenance.
        if (d.getEncadrant() != null && d.getEncadrant().getId().equals(teacher.getId())) {
            return "Encadrant";
        }
        if (d.getJury1() != null && d.getJury1().getId().equals(teacher.getId())) {
            return "Jury 1";
        }
        if (d.getJury2() != null && d.getJury2().getId().equals(teacher.getId())) {
            return "Jury 2";
        }
        return "-";
    }
}