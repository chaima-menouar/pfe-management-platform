package com.pfe.gestionpfe.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pfe.gestionpfe.model.Defense;
import com.pfe.gestionpfe.model.Student;

@Service
public class PdfExportService {

    private final StudentService studentService;
    private final DefenseService defenseService;

    private static final Color HEADER_BLACK = new Color(0, 0, 0);
    private static final Color HEADER_BLUE = new Color(26, 61, 145);
    private static final Color SECTION_BLUE = new Color(20, 172, 229);
    private static final Color WHITE = Color.WHITE;
    private static final Color BORDER = Color.BLACK;

    // palette profs
    private static final Color[] PROFESSOR_COLORS = new Color[]{
            new Color(0, 176, 80),
            new Color(244, 177, 131),
            new Color(217, 151, 232),
            new Color(180, 198, 231),
            new Color(112, 48, 160),
            new Color(234, 209, 220),
            new Color(255, 217, 102),
            new Color(146, 208, 80),
            new Color(0, 176, 240),
            new Color(255, 0, 0),
            new Color(255, 192, 0),
            new Color(204, 255, 204),
            new Color(153, 204, 255),
            new Color(255, 102, 0),
            new Color(153, 153, 153),
            new Color(0, 0, 255),
            new Color(102, 204, 255),
            new Color(204, 0, 153),
            new Color(153, 153, 0),
            new Color(102, 255, 102)
    };

    // palette filières
    private static final Color[] FILIERE_COLORS = new Color[]{
            new Color(189, 215, 238), // bleu clair
            new Color(248, 203, 173), // orange clair
            new Color(255, 242, 204), // jaune clair
            new Color(198, 224, 180), // vert clair
            new Color(217, 210, 233), // mauve clair
            new Color(252, 228, 214), // saumon
            new Color(221, 235, 247), // bleu très clair
            new Color(226, 239, 218)  // vert très clair
    };

    private final Map<String, Color> professorColorMap = new LinkedHashMap<>();
    private final Map<String, Color> filiereColorMap = new LinkedHashMap<>();
    private int professorColorIndex = 0;
    private int filiereColorIndex = 0;

    public PdfExportService(StudentService studentService, DefenseService defenseService) {
        this.studentService = studentService;
        this.defenseService = defenseService;
    }

    // ========================= AFFECTATION =========================
    public byte[] exportAssignmentsPdf(String departement) throws Exception {
        resetColorMaps();

        List<Student> students = getStudents(departement);
        prepareColorsFromStudents(students);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 18, 18, 18, 18);
        PdfWriter.getInstance(document, out);
        document.open();

        addAssignmentHeader(document, departement, students);
        addLegend(document);

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8f);
        table.setWidths(new float[]{1.4f, 1.6f, 1.6f, 2.4f, 1.4f, 1.6f, 1.8f});

        addBlueHeaderCell(table, "CNE");
        addBlueHeaderCell(table, "Nom");
        addBlueHeaderCell(table, "Prénom");
        addBlueHeaderCell(table, "Email");
        addBlueHeaderCell(table, "Filière");
        addBlueHeaderCell(table, "Spécialité");
        addBlueHeaderCell(table, "Encadrant");

        for (Student s : students) {
            String filiere = value(s.getFiliere());
            String shortFiliere = shortFiliere(filiere);

            String encadrantName = s.getEncadrant() != null
                    ? (value(s.getEncadrant().getNom()) + " " + value(s.getEncadrant().getPrenom())).trim()
                    : "-";

            addBodyCell(table, value(s.getCne()), WHITE);
            addBodyCell(table, value(s.getNom()), WHITE);
            addBodyCell(table, value(s.getPrenom()), WHITE);
            addBodyCell(table, value(s.getEmail()), WHITE);
            addBodyCell(table, shortFiliere, getFiliereColor(filiere));
            addBodyCell(table, value(s.getSpecialite()), WHITE);
            addBodyCell(table, encadrantName, getProfessorColor(encadrantName));
        }

        document.add(table);
        addStamp(document);
        document.close();

        return out.toByteArray();
    }

    // ========================= REPARTITION PERSONNELLE =========================
    public byte[] exportPersonalRepartitionPdf(List<Student> students, String teacherName, String departement) throws Exception {
        resetColorMaps();
        prepareColorsFromStudents(students);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 18, 18, 18, 18);
        PdfWriter.getInstance(document, out);
        document.open();

        addRepartitionHeader(document, "Répartition personnelle - " + teacherName, departement, students);
        addLegend(document);

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8f);
        table.setWidths(new float[]{1.4f, 1.6f, 1.6f, 2.4f, 1.4f, 1.6f});

        addBlueHeaderCell(table, "CNE");
        addBlueHeaderCell(table, "Nom");
        addBlueHeaderCell(table, "Prénom");
        addBlueHeaderCell(table, "Email");
        addBlueHeaderCell(table, "Filière");
        addBlueHeaderCell(table, "Spécialité");

        for (Student s : students) {
            String filiere = value(s.getFiliere());
            Color filiereColor = getFiliereColor(filiere);

            addBodyCell(table, value(s.getCne()), WHITE);
            addBodyCell(table, value(s.getNom()), WHITE);
            addBodyCell(table, value(s.getPrenom()), WHITE);
            addBodyCell(table, value(s.getEmail()), WHITE);
            addBodyCell(table, shortFiliere(filiere), filiereColor);
            addBodyCell(table, value(s.getSpecialite()), WHITE);
        }

        document.add(table);
        addStamp(document);
        document.close();

        return out.toByteArray();
    }

    // ========================= REPARTITION DEPARTEMENT =========================
    public byte[] exportDepartmentRepartitionPdf(List<Student> students, String departement) throws Exception {
        resetColorMaps();
        prepareColorsFromStudents(students);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 18, 18, 18, 18);
        PdfWriter.getInstance(document, out);
        document.open();

        addRepartitionHeader(document, "Répartition département", departement, students);
        addLegend(document);

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8f);
        table.setWidths(new float[]{1.4f, 1.6f, 1.6f, 1.4f, 1.6f, 2.0f, 2.4f});

        addBlueHeaderCell(table, "CNE");
        addBlueHeaderCell(table, "Nom");
        addBlueHeaderCell(table, "Prénom");
        addBlueHeaderCell(table, "Filière");
        addBlueHeaderCell(table, "Spécialité");
        addBlueHeaderCell(table, "Encadrant");
        addBlueHeaderCell(table, "Email");

        for (Student s : students) {
            String filiere = value(s.getFiliere());
            String encadrantName = s.getEncadrant() != null
                    ? (value(s.getEncadrant().getNom()) + " " + value(s.getEncadrant().getPrenom())).trim()
                    : "-";

            addBodyCell(table, value(s.getCne()), WHITE);
            addBodyCell(table, value(s.getNom()), WHITE);
            addBodyCell(table, value(s.getPrenom()), WHITE);
            addBodyCell(table, shortFiliere(filiere), getFiliereColor(filiere));
            addBodyCell(table, value(s.getSpecialite()), WHITE);
            addBodyCell(table, encadrantName, getProfessorColor(encadrantName));
            addBodyCell(table, value(s.getEmail()), WHITE);
        }

        document.add(table);
        addStamp(document);
        document.close();

        return out.toByteArray();
    }

    // ========================= PLANNING =========================
    public byte[] exportPlanningPdf(String departement) throws Exception {
        List<Defense> defenses = getDefenses(departement);
        return exportPlanningPdfFromDefenses(defenses, departement);
    }

    public byte[] exportPlanningPdfFromDefenses(List<Defense> defenses, String departement) throws Exception {
        resetColorMaps();
        prepareColorsFromDefenses(defenses);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 18, 18, 18, 18);
        PdfWriter.getInstance(document, out);
        document.open();

        addPlanningHeader(document, departement, defenses);
        addLegend(document);

        PdfPTable table = new PdfPTable(10);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8f);
        table.setWidths(new float[]{0.6f, 1.9f, 1.9f, 1.9f, 1.9f, 1.0f, 0.8f, 0.8f, 0.8f, 0.8f});

        addBlackHeaderCell(table, "ID");
        addBlackHeaderCell(table, "Encadrant");
        addBlackHeaderCell(table, "Membre de jury 1");
        addBlackHeaderCell(table, "Membre de jury 2");
        addBlackHeaderCell(table, "Date");
        addBlackHeaderCell(table, "Heure");
        addBlackHeaderCell(table, "Salle");
        addBlackHeaderCell(table, "Nom d'étudiant");
        addBlackHeaderCell(table, "Prénom d'étudiant");
        addBlackHeaderCell(table, "Filière");

        int index = 1;
        for (Defense d : defenses) {
            String encadrantName = fullName(d.getEncadrant());
            String jury1Name     = fullName(d.getJury1());
            String jury2Name     = fullName(d.getJury2());

            String studentNom    = d.getStudent() != null ? value(d.getStudent().getNom())    : "-";
            String studentPrenom = d.getStudent() != null ? value(d.getStudent().getPrenom()) : "-";
            String filiere       = d.getStudent() != null ? value(d.getStudent().getFiliere()): "-";
            String shortFiliere  = shortFiliere(filiere);

            addBodyCell(table, String.valueOf(index++), WHITE);
            addBodyCell(table, encadrantName, getProfessorColor(encadrantName));
            addBodyCell(table, jury1Name,     getProfessorColor(jury1Name));
            addBodyCell(table, jury2Name,     getProfessorColor(jury2Name));
            addBodyCell(table, value(d.getDateSoutenance()),  new Color(255, 242, 102));
            addBodyCell(table, value(d.getHeureDebut()),      new Color(169, 208, 142));
            addBodyCell(table, d.getRoom() != null ? value(d.getRoom().getNomSalle()) : "-", WHITE);
            addBodyCell(table, studentNom,    getFiliereColor(filiere));
            addBodyCell(table, studentPrenom, getFiliereColor(filiere));
            addBodyCell(table, shortFiliere,  getFiliereColor(filiere));
        }

        document.add(table);
        addStamp(document);
        document.close();

        return out.toByteArray();
    }

    // ========================= HEADERS =========================
    private void addAssignmentHeader(Document document, String departement, List<Student> students) throws Exception {
        Font schoolFont = new Font(Font.HELVETICA, 13, Font.BOLD, Color.BLACK);
        Font depFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.BLACK);
        Font titleFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.BLACK);
        Font yearFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);

        PdfPTable box = new PdfPTable(1);
        box.setWidthPercentage(48);
        box.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell cell = new PdfPCell();
        cell.setBorderWidth(1.5f);
        cell.setPadding(8f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph p1 = new Paragraph("Ecole Nationale des Sciences Appliquées - Al Hoceima", schoolFont);
        p1.setAlignment(Element.ALIGN_CENTER);
        Paragraph p2 = new Paragraph("Département " + safeDepartmentHeader(departement), depFont);
        p2.setAlignment(Element.ALIGN_CENTER);
        Paragraph p3 = new Paragraph("Affectation des encadrants de Projet de Fin d'Etude", titleFont);
        p3.setAlignment(Element.ALIGN_CENTER);
        Paragraph p4 = new Paragraph("Année Universitaire " + getAcademicYearFromStudents(students), yearFont);
        p4.setAlignment(Element.ALIGN_CENTER);

        cell.addElement(p1);
        cell.addElement(p2);
        cell.addElement(p3);
        cell.addElement(p4);

        box.addCell(cell);
        document.add(box);
    }

    private void addRepartitionHeader(Document document, String title, String departement, List<Student> students) throws Exception {
        Font schoolFont = new Font(Font.HELVETICA, 13, Font.BOLD, Color.BLACK);
        Font depFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.BLACK);
        Font titleFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.BLACK);
        Font yearFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);

        PdfPTable box = new PdfPTable(1);
        box.setWidthPercentage(55);
        box.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell cell = new PdfPCell();
        cell.setBorderWidth(1.5f);
        cell.setPadding(8f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph p1 = new Paragraph("Ecole Nationale des Sciences Appliquées - Al Hoceima", schoolFont);
        p1.setAlignment(Element.ALIGN_CENTER);
        Paragraph p2 = new Paragraph("Département " + safeDepartmentHeader(departement), depFont);
        p2.setAlignment(Element.ALIGN_CENTER);
        Paragraph p3 = new Paragraph(title, titleFont);
        p3.setAlignment(Element.ALIGN_CENTER);
        Paragraph p4 = new Paragraph("Année Universitaire " + getAcademicYearFromStudents(students), yearFont);
        p4.setAlignment(Element.ALIGN_CENTER);

        cell.addElement(p1);
        cell.addElement(p2);
        cell.addElement(p3);
        cell.addElement(p4);

        box.addCell(cell);
        document.add(box);
    }

    private void addPlanningHeader(Document document, String departement, List<Defense> defenses) throws Exception {
        Font schoolFont = new Font(Font.HELVETICA, 16, Font.BOLD, Color.BLACK);
        Font depFont = new Font(Font.HELVETICA, 14, Font.BOLD, Color.BLACK);
        Font titleFont = new Font(Font.HELVETICA, 13, Font.NORMAL, Color.BLACK);
        Font sessionFont = new Font(Font.HELVETICA, 12, Font.ITALIC, Color.BLACK);
        Font yearFont = new Font(Font.HELVETICA, 12, Font.NORMAL, Color.BLACK);

        Paragraph school = new Paragraph("Ecole Nationale des Sciences Appliquées - Al Hoceima", schoolFont);
        school.setAlignment(Element.ALIGN_CENTER);

        Paragraph dep = new Paragraph("Département INFORMATIQUE ", depFont);
        dep.setAlignment(Element.ALIGN_CENTER);

        Paragraph title = new Paragraph("Planning des soutenances des Projets de Fin d'Etude", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);

        Paragraph session = new Paragraph("(Première Session)", sessionFont);
        session.setAlignment(Element.ALIGN_CENTER);

        Paragraph year = new Paragraph("Année Universitaire " + getAcademicYearFromDefenses(defenses), yearFont);
        year.setAlignment(Element.ALIGN_CENTER);
        year.setSpacingAfter(8f);

        document.add(school);
        document.add(dep);
        document.add(title);
        document.add(session);
        document.add(year);
    }

    // ========================= LEGEND =========================
    private void addLegend(Document document) throws Exception {
        if (filiereColorMap.isEmpty()) {
            return;
        }

        PdfPTable legendTable = new PdfPTable(2);
        legendTable.setWidthPercentage(20);
        legendTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        legendTable.setSpacingBefore(6f);
        legendTable.setSpacingAfter(8f);
        legendTable.setWidths(new float[]{1f, 2f});

        for (Map.Entry<String, Color> entry : filiereColorMap.entrySet()) {
            PdfPCell colorCell = new PdfPCell(new Phrase(""));
            colorCell.setBackgroundColor(entry.getValue());
            colorCell.setFixedHeight(14f);
            colorCell.setBorder(Rectangle.NO_BORDER);

            PdfPCell textCell = new PdfPCell(new Phrase("Filière " + shortFiliere(entry.getKey()),
                    new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK)));
            textCell.setBorder(Rectangle.NO_BORDER);
            textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            legendTable.addCell(colorCell);
            legendTable.addCell(textCell);
        }

        document.add(legendTable);
    }

    // ========================= CELLS =========================
    private void addBlueHeaderCell(PdfPTable table, String value) {
        Font font = new Font(Font.HELVETICA, 10, Font.BOLD, WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setBackgroundColor(HEADER_BLUE);
        styleHeaderCell(cell);
        table.addCell(cell);
    }

    private void addBlackHeaderCell(PdfPTable table, String value) {
        Font font = new Font(Font.HELVETICA, 9, Font.BOLD, WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setBackgroundColor(HEADER_BLACK);
        styleHeaderCell(cell);
        table.addCell(cell);
    }

    private void styleHeaderCell(PdfPCell cell) {
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5f);
        cell.setBorderColor(BORDER);
    }

    private void addBodyCell(PdfPTable table, String value, Color bg) {
        Font font = new Font(Font.HELVETICA, 9, Font.BOLD, Color.BLACK);
        PdfPCell cell = new PdfPCell(new Phrase(value == null || value.isBlank() ? "-" : value, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3f);
        cell.setBorderColor(BORDER);
        table.addCell(cell);
    }

    // ========================= COLORS =========================
    private void resetColorMaps() {
        professorColorMap.clear();
        filiereColorMap.clear();
        professorColorIndex = 0;
        filiereColorIndex = 0;
    }

    private void prepareColorsFromStudents(List<Student> students) {
        for (Student s : students) {
            registerFiliere(value(s.getFiliere()));
            if (s.getEncadrant() != null) {
                registerProfessor((value(s.getEncadrant().getNom()) + " " + value(s.getEncadrant().getPrenom())).trim());
            }
        }
    }

    private void prepareColorsFromDefenses(List<Defense> defenses) {
        for (Defense d : defenses) {
            if (d.getStudent() != null) {
                registerFiliere(value(d.getStudent().getFiliere()));
            }
            registerProfessor(fullName(d.getEncadrant()));
            registerProfessor(fullName(d.getJury1()));
            registerProfessor(fullName(d.getJury2()));
        }
    }

    private void registerProfessor(String professorName) {
        if (professorName == null || professorName.isBlank() || "-".equals(professorName.trim())) {
            return;
        }

        String key = professorName.trim().toUpperCase();
        if (!professorColorMap.containsKey(key)) {
            professorColorMap.put(key, PROFESSOR_COLORS[professorColorIndex % PROFESSOR_COLORS.length]);
            professorColorIndex++;
        }
    }

    private void registerFiliere(String filiere) {
        if (filiere == null || filiere.isBlank() || "-".equals(filiere.trim())) {
            return;
        }

        String key = normalizeFiliereKey(filiere);
        if (!filiereColorMap.containsKey(key)) {
            filiereColorMap.put(key, FILIERE_COLORS[filiereColorIndex % FILIERE_COLORS.length]);
            filiereColorIndex++;
        }
    }

    private Color getProfessorColor(String professorName) {
        if (professorName == null || professorName.isBlank() || "-".equals(professorName.trim())) {
            return WHITE;
        }
        String key = professorName.trim().toUpperCase();
        registerProfessor(professorName);
        return professorColorMap.getOrDefault(key, WHITE);
    }

    private Color getFiliereColor(String filiere) {
        if (filiere == null || filiere.isBlank() || "-".equals(filiere.trim())) {
            return WHITE;
        }
        String key = normalizeFiliereKey(filiere);
        registerFiliere(filiere);
        return filiereColorMap.getOrDefault(key, WHITE);
    }

    private String normalizeFiliereKey(String filiere) {
        String upper = filiere.trim().toUpperCase();

        if (upper.contains("(GI)") || upper.equals("GI")) return "GI";
        if (upper.contains("(ID)") || upper.equals("ID")) return "ID";
        if (upper.contains("TDIA")) return "TDIA";
        if (upper.contains("GTR")) return "GTR";
        if (upper.contains("GC")) return "GC";
        if (upper.contains("GM")) return "GM";
        if (upper.contains("GE")) return "GE";
        if (upper.contains("G2E")) return "G2E";

        return upper;
    }

    private String shortFiliere(String filiere) {
        if (filiere == null || filiere.isBlank()) {
            return "-";
        }
        return normalizeFiliereKey(filiere);
    }

    // ========================= CACHET =========================
    private void addStamp(Document document) {
        try {
            InputStream is = getClass().getResourceAsStream("/static/images/cachet.png");
            if (is == null) {
                return;
            }

            byte[] imageBytes = is.readAllBytes();
            Image stamp = Image.getInstance(imageBytes);
            stamp.scaleToFit(110, 110);
            stamp.setAlignment(Image.ALIGN_RIGHT);
            stamp.setSpacingBefore(18f);

            document.add(stamp);
        } catch (Exception e) {
            // ignore
        }
    }

    // ========================= HELPERS =========================
    private List<Student> getStudents(String departement) {
        if (departement == null || departement.isBlank()) {
            return studentService.getAllStudents();
        }
        return studentService.getStudentsByDepartement(departement);
    }

    private List<Defense> getDefenses(String departement) {
        if (departement == null || departement.isBlank()) {
            return defenseService.getAllDefenses();
        }
        return defenseService.getDefensesByDepartement(departement);
    }

    private String fullName(Object obj) {
        if (obj == null) {
            return "-";
        }
        try {
            String prenom = String.valueOf(obj.getClass().getMethod("getPrenom").invoke(obj));
            String nom = String.valueOf(obj.getClass().getMethod("getNom").invoke(obj));
            String full = (nom + " " + prenom).trim();
            return full.isBlank() ? "-" : full;
        } catch (Exception e) {
            return "-";
        }
    }

    private String value(Object object) {
        if (object == null) {
            return "-";
        }
        if (object instanceof java.time.LocalDate date) {
            return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        if (object instanceof java.time.LocalTime time) {
            return time.format(DateTimeFormatter.ofPattern("H'h'"));
        }
        String text = String.valueOf(object).trim();
        return text.isBlank() ? "-" : text;
    }

    private String safeDepartmentHeader(String departement) {
        if (departement == null || departement.isBlank()) {
            return "Mathématiques et Informatique";
        }
        return departement;
    }

    private String getAcademicYearFromStudents(List<Student> students) {
        LocalDate now = LocalDate.now();
        return buildAcademicYear(now);
    }

    private String getAcademicYearFromDefenses(List<Defense> defenses) {
        for (Defense d : defenses) {
            if (d.getDateSoutenance() != null) {
                return buildAcademicYear(d.getDateSoutenance());
            }
        }
        return buildAcademicYear(LocalDate.now());
    }

private String buildAcademicYear(LocalDate date) {
        int year = date.getYear();
        if (date.getMonthValue() >= 9) {
            return year + "/" + (year + 1);
        }
        return (year - 1) + "/" + year;
    }

    // ========================= FICHE EVALUATION PFE - v3 =========================
public byte[] exportFicheEvaluationPfe(Long studentId) throws Exception {
    Student student = studentService.getStudentById(studentId);
    if (student == null) {
        throw new IllegalArgumentException("Étudiant introuvable avec ID: " + studentId);
    }

    // Récupérer la soutenance
    Defense defense = null;
    List<Defense> defenses = defenseService.getAllDefenses();
    for (Defense d : defenses) {
        if (d.getStudent() != null && d.getStudent().getId().equals(studentId)) {
            defense = d;
            break;
        }
    }

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4, 45, 45, 30, 30);
    PdfWriter.getInstance(document, out);
    document.open();

    // ── Fonts ──
    Font f9n  = new Font(Font.HELVETICA,  9, Font.NORMAL, Color.BLACK);
    Font f9i  = new Font(Font.HELVETICA,  9, Font.ITALIC, Color.BLACK);
    Font f10n = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
    Font f10b = new Font(Font.HELVETICA, 10, Font.BOLD,   Color.BLACK);
    Font f10u = new Font(Font.HELVETICA, 10, Font.BOLD | Font.UNDERLINE, Color.BLACK);
    Font f11b = new Font(Font.HELVETICA, 11, Font.BOLD,   Color.BLACK);

    // ════════════════════════════════════════════════════
    // EN-TÊTE : Logo UAE | Texte | Logo ENSAH
    // ════════════════════════════════════════════════════
    PdfPTable headerTable = new PdfPTable(new float[]{1.3f, 4.5f, 1.3f});
    headerTable.setWidthPercentage(100);
    headerTable.setSpacingAfter(4f);

    // Logo gauche : UAE
    try {
        byte[] imgBytes = getClass().getResourceAsStream("/static/images/logo.png").readAllBytes();
        Image logo = Image.getInstance(imgBytes);
        logo.scaleToFit(65, 65);
        PdfPCell c = new PdfPCell(logo, false);
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(c);
    } catch (Exception e) {
        PdfPCell c = new PdfPCell(new Phrase(""));
        c.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(c);
    }

    // Texte central
    PdfPCell centerCell = new PdfPCell();
    centerCell.setBorder(Rectangle.NO_BORDER);
    centerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    Paragraph p1 = new Paragraph("UNIVERSITE ABDELMALEK ESSAADI", f11b);
    p1.setAlignment(Element.ALIGN_CENTER);
    centerCell.addElement(p1);
    Paragraph p2 = new Paragraph("Ecole Nationale des Sciences Appliquées d'Al-Hoceima - Maroc", f9n);
    p2.setAlignment(Element.ALIGN_CENTER);
    centerCell.addElement(p2);
    headerTable.addCell(centerCell);

    // Logo droit : ENSAH
    try {
        byte[] imgBytes = getClass().getResourceAsStream("/static/images/uae.jpg").readAllBytes();
        Image ensah = Image.getInstance(imgBytes);
        ensah.scaleToFit(150, 150);
        PdfPCell c = new PdfPCell(ensah, false);
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(c);
    } catch (Exception e) {
        PdfPCell c = new PdfPCell(new Phrase(""));
        c.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(c);
    }

    document.add(headerTable);

    // Ligne séparatrice
    PdfPTable line = new PdfPTable(1);
    line.setWidthPercentage(100);
    PdfPCell lc = new PdfPCell(new Phrase(""));
    lc.setBorder(Rectangle.BOTTOM);
    lc.setBorderWidthBottom(1f);
    lc.setPadding(0);
    line.addCell(lc);
    document.add(line);

    // ════════════════════════════════════════════════════
    // SOUS-TITRE
    // ════════════════════════════════════════════════════
    Paragraph dept = new Paragraph("Département de Mathématiques et Informatique", f10b);
    dept.setAlignment(Element.ALIGN_CENTER);
    dept.setSpacingBefore(6f);
    document.add(dept);

    Paragraph fiche = new Paragraph("Fiche d'évaluation du Projet de Fin d'Étude", f10b);
    fiche.setAlignment(Element.ALIGN_CENTER);
    document.add(fiche);

    Paragraph annee = new Paragraph("Année Universitaire : " + buildAcademicYear(LocalDate.now()), f10b);
    annee.setAlignment(Element.ALIGN_CENTER);
    document.add(annee);

    // ════════════════════════════════════════════════════
    // NOM - PRÉNOM
    // ════════════════════════════════════════════════════
    Paragraph nomLabel = new Paragraph();
    nomLabel.setSpacingBefore(8f);
    nomLabel.add(new Chunk("Nom - Prénom de l'élève ingénieur :", f10u));
    document.add(nomLabel);

    String studentName = value(student.getNom()) + " " + value(student.getPrenom());
    document.add(new Paragraph("   − " + studentName, f10n));

    // ════════════════════════════════════════════════════
    // FILIÈRE
    // ════════════════════════════════════════════════════
    Paragraph filLabel = new Paragraph();
    filLabel.setSpacingBefore(6f);
    filLabel.add(new Chunk("Filière :", f10u));
    document.add(filLabel);

    String filiere = value(student.getFiliere()).trim().toLowerCase();
    boolean isID   = filiere.contains("donn") || filiere.equals("id");
    boolean isGI   = filiere.contains("génie") || filiere.contains("genie") || filiere.contains("informatique") || filiere.equals("gi");
    boolean isTDIA = filiere.contains("digit") || filiere.contains("transform") || filiere.contains("tdia");

    PdfPTable filTable = new PdfPTable(new float[]{1f, 1f, 1f});
    filTable.setWidthPercentage(100);
    filTable.setSpacingBefore(3f);
    filTable.setSpacingAfter(3f);
    addFiliereCell(filTable, "Ingénierie des Données",      isID,   f10n);
    addFiliereCell(filTable, "Génie Informatique",           isGI,   f10n);
    addFiliereCell(filTable, "Transformation Digital & IA", isTDIA, f10n);
    document.add(filTable);

    // ════════════════════════════════════════════════════
    // INTITULÉ DU RAPPORT — champ vide à remplir à la main
    // ════════════════════════════════════════════════════
    Paragraph rapLabel = new Paragraph();
    rapLabel.setSpacingBefore(4f);
    rapLabel.add(new Chunk("Intitulé du rapport :", f10u));
    document.add(rapLabel);

    // Ligne pointillée vide
    document.add(new Paragraph("   − …………………………………………………………………………………………………….", f10n));

    // ════════════════════════════════════════════════════
    // ENCADRANT INTERNE
    // ════════════════════════════════════════════════════
    Paragraph encLabel = new Paragraph();
    encLabel.setSpacingBefore(4f);
    encLabel.add(new Chunk("L'encadrant(e) interne:", f10u));
    document.add(encLabel);

    // Nom de l'encadrant (= Président du jury)
    String encadrantNom = "Pr. ………………………………………………………………………….";
    if (student.getEncadrant() != null) {
        encadrantNom = "Pr. " + student.getEncadrant().getNom() + " " + student.getEncadrant().getPrenom();
    }
    document.add(new Paragraph("   − " + encadrantNom, f10n));

    // ════════════════════════════════════════════════════
    // MEMBRES DU JURY
    // Président   = encadrant interne
    // Rapporteur1 = jury1
    // Rapporteur2 = jury2
    // ════════════════════════════════════════════════════
    Paragraph juryLabel = new Paragraph();
    juryLabel.setSpacingBefore(4f);
    juryLabel.add(new Chunk("Membres du jury :", f10u));
    document.add(juryLabel);

    // Président = encadrant
    String president   = encadrantNom; // même valeur que l'encadrant interne

    // Rapporteurs = jury1 et jury2 de la soutenance
    String rapporteur1 = "Pr. …………………………………………………";
    String rapporteur2 = "Pr. …………………………………………………";

    if (defense != null) {
        if (defense.getJury1() != null)
            rapporteur1 = "Pr. " + defense.getJury1().getNom() + " " + defense.getJury1().getPrenom();
        if (defense.getJury2() != null)
            rapporteur2 = "Pr. " + defense.getJury2().getNom() + " " + defense.getJury2().getPrenom();
    }

    PdfPTable juryTable = new PdfPTable(new float[]{5f, 2f});
    juryTable.setWidthPercentage(90);
    juryTable.setHorizontalAlignment(Element.ALIGN_LEFT);
    juryTable.setSpacingBefore(3f);
    juryTable.setSpacingAfter(4f);

    addJuryRow(juryTable, "   − " + president,   "Président",  f10n);
    addJuryRow(juryTable, "   − " + rapporteur1, "Rapporteur", f10n);
    addJuryRow(juryTable, "   − " + rapporteur2, "Rapporteur", f10n);

    document.add(juryTable);

    // ════════════════════════════════════════════════════
    // NOTES : C, M, S
    // ════════════════════════════════════════════════════
    Paragraph nc = new Paragraph();
    nc.add(new Chunk("Note du Contenu ", f10u));
    nc.add(new Chunk("(En prenant en compte l'appréciation de l'entreprise)", f9i));
    document.add(nc);
    document.add(new Paragraph("   C =", f10n));

    Paragraph nm = new Paragraph("Note du Mémoire", f10u);
    nm.setSpacingBefore(4f);
    document.add(nm);
    document.add(new Paragraph("   M =", f10n));

    Paragraph ns = new Paragraph("Note de la Soutenance", f10u);
    ns.setSpacingBefore(4f);
    document.add(ns);
    document.add(new Paragraph("   S =", f10n));

    // ════════════════════════════════════════════════════
    // MOYENNE dans un encadré gris
    // ════════════════════════════════════════════════════
    PdfPTable moyTable = new PdfPTable(1);
    moyTable.setWidthPercentage(100);
    moyTable.setSpacingBefore(8f);
    moyTable.setSpacingAfter(8f);

    PdfPCell moyHeader = new PdfPCell(new Phrase("MOYENNE", f10b));
    moyHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
    moyHeader.setBackgroundColor(new Color(200, 200, 200));
    moyHeader.setPadding(4f);
    moyHeader.setBorder(Rectangle.BOX);
    moyTable.addCell(moyHeader);

    PdfPCell moyFormule = new PdfPCell(new Phrase("Moyenne   = C * 0,5+ M * 0,2 + S * 0,3  =", f10b));
    moyFormule.setPadding(6f);
    moyFormule.setBorder(Rectangle.BOX);
    moyTable.addCell(moyFormule);

    document.add(moyTable);

    // ════════════════════════════════════════════════════
    // DATE + SIGNATURES
    // ════════════════════════════════════════════════════
    document.add(new Paragraph("Le : ………………………", f10n));

    Paragraph sigTitle = new Paragraph("Signature des membres du jury :", f10b);
    sigTitle.setSpacingBefore(6f);
    document.add(sigTitle);

    PdfPTable sigTable = new PdfPTable(3);
    sigTable.setWidthPercentage(100);
    sigTable.setSpacingBefore(6f);

    for (int i = 0; i < 3; i++) {
        Paragraph sp = new Paragraph("Pr. ………… …………", f10n);
        sp.setAlignment(Element.ALIGN_CENTER);
        PdfPCell sc = new PdfPCell();
        sc.addElement(sp);
        sc.setBorder(Rectangle.NO_BORDER);
        sc.setHorizontalAlignment(Element.ALIGN_CENTER);
        sc.setPaddingTop(4f);
        sigTable.addCell(sc);
    }
    document.add(sigTable);

    document.close();
    return out.toByteArray();
}

// ── Helper : cellule filière avec case à cocher ──
private void addFiliereCell(PdfPTable table, String label, boolean checked, Font font) {
    String box = checked ? "[X] " : "[ ] ";
    Paragraph p = new Paragraph(box + label, font);
    p.setAlignment(Element.ALIGN_CENTER);
    PdfPCell c = new PdfPCell();
    c.addElement(p);
    c.setBorder(Rectangle.NO_BORDER);
    c.setHorizontalAlignment(Element.ALIGN_CENTER);
    table.addCell(c);
}

// ── Helper : ligne jury ──
private void addJuryRow(PdfPTable table, String name, String role, Font font) {
    PdfPCell nc = new PdfPCell(new Phrase(name, font));
    nc.setBorder(Rectangle.NO_BORDER);
    nc.setPaddingBottom(3f);
    table.addCell(nc);

    PdfPCell rc = new PdfPCell(new Phrase(role, font));
    rc.setBorder(Rectangle.NO_BORDER);
    rc.setPaddingBottom(3f);
    table.addCell(rc);
}

    private void addLabelValueRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, new Font(Font.HELVETICA, 11, Font.BOLD, Color.BLACK)));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, new Font(Font.HELVETICA, 11, Font.NORMAL, Color.BLACK)));
        valueCell.setBorder(Rectangle.BOX);
        valueCell.setPadding(5f);
        valueCell.setBackgroundColor(new Color(245, 245, 245));
        table.addCell(valueCell);
    }

    private void addCheckboxRow(PdfPTable table, String label, String checkedValue) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, new Font(Font.HELVETICA, 11, Font.BOLD, Color.BLACK)));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5f);
        table.addCell(labelCell);

        String gi = "GI".equalsIgnoreCase(checkedValue) ? "☑" : "☐";
        String id = "Ingénierie des Données".equalsIgnoreCase(checkedValue) ? "☑" : "☐";

        String checkboxText = gi + " Ingénierie des Données    " + id + " Génie Informatique";
        PdfPCell checkCell = new PdfPCell(new Phrase(checkboxText, new Font(Font.HELVETICA, 11, Font.NORMAL, Color.BLACK)));
        checkCell.setBorder(Rectangle.BOX);
        checkCell.setPadding(5f);
        table.addCell(checkCell);
    }

    private void addNoteRow(PdfPTable table, String label, String prefix) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, new Font(Font.HELVETICA, 11, Font.NORMAL, Color.BLACK)));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(8f);
        table.addCell(labelCell);

        PdfPCell noteCell = new PdfPCell(new Phrase(prefix, new Font(Font.HELVETICA, 11, Font.BOLD, Color.BLACK)));
        noteCell.setBorder(Rectangle.BOX);
        noteCell.setPadding(8f);
        table.addCell(noteCell);
    }
}
