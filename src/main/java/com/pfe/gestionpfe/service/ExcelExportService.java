package com.pfe.gestionpfe.service;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.pfe.gestionpfe.model.Defense;
import com.pfe.gestionpfe.model.Student;

@Service
public class ExcelExportService {

    private final StudentService studentService;
    private final DefenseService defenseService;

    public ExcelExportService(StudentService studentService, DefenseService defenseService) {
        this.studentService = studentService;
        this.defenseService = defenseService;
    }

    public byte[] exportAssignmentsExcel(String departement) throws Exception {
        Workbook workbook = new XSSFWorkbook();

        List<Student> students = getStudents(departement);
        Map<String, Short> colors = buildColors();

        Sheet summary = workbook.createSheet("Résumé");
        createTitle(summary, workbook, "Affectation des encadrants - " + safeDepartment(departement));
        fillAssignmentSummary(summary, students);

        Map<String, Integer> sheetIndexes = new LinkedHashMap<>();
        for (Student s : students) {
            if (s.getFiliere() == null || s.getFiliere().isBlank()) continue;
            sheetIndexes.putIfAbsent(s.getFiliere(), sheetIndexes.size() + 1);
        }

        for (String filiere : sheetIndexes.keySet()) {
            Sheet sh = workbook.createSheet("Affect_" + filiere);
            Row h = sh.createRow(0);
            createHeaderCell(h, 0, "Encadrant", workbook);
            createHeaderCell(h, 1, "Étudiant", workbook);
            createHeaderCell(h, 2, "Filière", workbook);

            int rowIdx = 1;
            for (Student s : students) {
                if (s.getEncadrant() == null) continue;
                if (!filiere.equalsIgnoreCase(s.getFiliere())) continue;

                Row row = sh.createRow(rowIdx++);
                createCell(row, 0, s.getEncadrant().getNom() + " " + s.getEncadrant().getPrenom());
                createCellColored(row, 1, s.getNom() + " " + s.getPrenom(), colors.getOrDefault(s.getFiliere(), IndexedColors.WHITE.getIndex()), workbook);
                createCellColored(row, 2, s.getFiliere(), colors.getOrDefault(s.getFiliere(), IndexedColors.WHITE.getIndex()), workbook);
            }

            autoSize(sh, 3);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    public byte[] exportPlanningExcel(String departement) throws Exception {
        Workbook workbook = new XSSFWorkbook();

        List<Defense> defenses = getDefenses(departement);
        Map<String, Short> colors = buildColors();

        Sheet summary = workbook.createSheet("Résumé");
        createTitle(summary, workbook, "Planning des soutenances - " + safeDepartment(departement));
        fillPlanningSummary(summary, defenses);

        Sheet planning = workbook.createSheet("Planning global");
        Row header = planning.createRow(0);

        String[] cols = {"ID", "Encadrant", "Jury1", "Jury2", "Date", "Heure", "Salle", "Nom", "Prénom", "Filière"};
        for (int i = 0; i < cols.length; i++) {
            createHeaderCell(header, i, cols[i], workbook);
        }

        int rowIdx = 1;
        for (Defense d : defenses) {
            Row row = planning.createRow(rowIdx++);
            String filiere = d.getStudent() != null ? d.getStudent().getFiliere() : "";
            short color = colors.getOrDefault(filiere, IndexedColors.WHITE.getIndex());

            createCell(row, 0, String.valueOf(rowIdx - 1));
            createCellColored(row, 1, fullName(d.getEncadrant()), color, workbook);
            createCellColored(row, 2, fullName(d.getJury1()), color, workbook);
            createCellColored(row, 3, fullName(d.getJury2()), color, workbook);
            createCell(row, 4, d.getDateSoutenance() != null ? d.getDateSoutenance().toString() : "");
            createCell(row, 5, d.getHeureDebut() != null ? d.getHeureDebut().toString() : "");
            createCell(row, 6, d.getRoom() != null ? d.getRoom().getNomSalle() : "");
            createCell(row, 7, d.getStudent() != null ? d.getStudent().getNom() : "");
            createCell(row, 8, d.getStudent() != null ? d.getStudent().getPrenom() : "");
            createCellColored(row, 9, filiere, color, workbook);
        }
        autoSize(planning, cols.length);

        Map<String, Integer> byDateSheet = new LinkedHashMap<>();
        for (Defense d : defenses) {
            if (d.getDateSoutenance() == null) continue;
            byDateSheet.putIfAbsent(d.getDateSoutenance().toString(), byDateSheet.size() + 1);
        }

        for (String date : byDateSheet.keySet()) {
            Sheet dateSheet = workbook.createSheet("Date_" + date.replace("-", "_"));
            Row h = dateSheet.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                createHeaderCell(h, i, cols[i], workbook);
            }

            int r = 1;
            for (Defense d : defenses) {
                if (d.getDateSoutenance() == null || !date.equals(d.getDateSoutenance().toString())) continue;
                Row row = dateSheet.createRow(r++);
                String filiere = d.getStudent() != null ? d.getStudent().getFiliere() : "";
                short color = colors.getOrDefault(filiere, IndexedColors.WHITE.getIndex());

                createCell(row, 0, String.valueOf(r - 1));
                createCellColored(row, 1, fullName(d.getEncadrant()), color, workbook);
                createCellColored(row, 2, fullName(d.getJury1()), color, workbook);
                createCellColored(row, 3, fullName(d.getJury2()), color, workbook);
                createCell(row, 4, d.getDateSoutenance() != null ? d.getDateSoutenance().toString() : "");
                createCell(row, 5, d.getHeureDebut() != null ? d.getHeureDebut().toString() : "");
                createCell(row, 6, d.getRoom() != null ? d.getRoom().getNomSalle() : "");
                createCell(row, 7, d.getStudent() != null ? d.getStudent().getNom() : "");
                createCell(row, 8, d.getStudent() != null ? d.getStudent().getPrenom() : "");
                createCellColored(row, 9, filiere, color, workbook);
            }
            autoSize(dateSheet, cols.length);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    private void createTitle(Sheet sheet, Workbook wb, String title) {
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);

        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        cell.setCellStyle(style);

        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));
    }

    private void fillAssignmentSummary(Sheet sheet, List<Student> students) {
        Row h = sheet.createRow(2);
        h.createCell(0).setCellValue("Indicateur");
        h.createCell(1).setCellValue("Valeur");

        long total = students.size();
        long affected = students.stream().filter(s -> s.getEncadrant() != null).count();

        Row r1 = sheet.createRow(3);
        r1.createCell(0).setCellValue("Total étudiants");
        r1.createCell(1).setCellValue(total);

        Row r2 = sheet.createRow(4);
        r2.createCell(0).setCellValue("Affectés");
        r2.createCell(1).setCellValue(affected);

        Row r3 = sheet.createRow(5);
        r3.createCell(0).setCellValue("Non affectés");
        r3.createCell(1).setCellValue(total - affected);
    }

    private void fillPlanningSummary(Sheet sheet, List<Defense> defenses) {
        Row h = sheet.createRow(2);
        h.createCell(0).setCellValue("Indicateur");
        h.createCell(1).setCellValue("Valeur");

        Row r1 = sheet.createRow(3);
        r1.createCell(0).setCellValue("Total soutenances");
        r1.createCell(1).setCellValue(defenses.size());

        long withRoom = defenses.stream().filter(d -> d.getRoom() != null).count();
        Row r2 = sheet.createRow(4);
        r2.createCell(0).setCellValue("Soutenances avec salle");
        r2.createCell(1).setCellValue(withRoom);
    }

    private Map<String, Short> buildColors() {
        return Map.of(
                "GI", IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex(),
                "ID", IndexedColors.LIGHT_ORANGE.getIndex(),
                "TDIA", IndexedColors.LIGHT_YELLOW.getIndex(),
                "GC", IndexedColors.LIGHT_GREEN.getIndex(),
                "GM", IndexedColors.LIGHT_TURQUOISE.getIndex(),
                "GE", IndexedColors.ROSE.getIndex(),
                "G2E", IndexedColors.LAVENDER.getIndex()
        );
    }

    private void createCell(Row row, int col, String value) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value);
    }

    private void createCellColored(Row row, int col, String value, short color, Workbook wb) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value);

        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cell.setCellStyle(style);
    }

    private void createHeaderCell(Row row, int col, String value, Workbook wb) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);

        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        cell.setCellStyle(style);
    }

    private void autoSize(Sheet sheet, int n) {
        for (int i = 0; i < n; i++) sheet.autoSizeColumn(i);
    }

    private List<Student> getStudents(String dep) {
        if (dep == null || dep.isBlank()) return studentService.getAllStudents();
        return studentService.getStudentsByDepartement(dep);
    }

    private List<Defense> getDefenses(String dep) {
        if (dep == null || dep.isBlank()) return defenseService.getAllDefenses();
        return defenseService.getDefensesByDepartement(dep);
    }

    private String fullName(Object t) {
        if (t == null) return "";
        try {
            return (String) t.getClass().getMethod("getNom").invoke(t) + " " +
                   (String) t.getClass().getMethod("getPrenom").invoke(t);
        } catch (Exception e) {
            return "";
        }
    }

    private String safeDepartment(String dep) {
        return dep == null || dep.isBlank() ? "Tous les départements" : dep;
    }
}