package com.pfe.gestionpfe.service;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.pfe.gestionpfe.model.Room;
import com.pfe.gestionpfe.model.Student;
import com.pfe.gestionpfe.model.Teacher;
import com.pfe.gestionpfe.repository.RoomRepository;
import com.pfe.gestionpfe.repository.StudentRepository;
import com.pfe.gestionpfe.repository.TeacherRepository;

@Service
public class ExcelImportService {

    private static final String DEPARTEMENT_INFORMATIQUE = "Informatique";
    private static final long MAX_EXCEL_SIZE = 5L * 1024L * 1024L;

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final RoomRepository roomRepository;

    public ExcelImportService(StudentRepository studentRepository,
                              TeacherRepository teacherRepository,
                              RoomRepository roomRepository) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.roomRepository = roomRepository;
    }

    /**
     * Format accepté pour les étudiants :
     * CNE | NOM | PRENOM | EMAIL PERSONNEL | EMAIL ACADEMIQUE
     *
     * La filière est déduite du nom du fichier : GI / ID / TDIA.
     */
    public int importStudentsFromExcel(MultipartFile file) throws IOException {
        validateExcelFile(file);
        List<Student> studentsToSave = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        String filiere = detectFiliereFromFilename(file.getOriginalFilename());

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            ensureWorkbookHasSheet(workbook);
            var sheet = workbook.getSheetAt(0);
            validateStudentHeader(sheet.getRow(0), formatter);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row, 5)) {
                    continue;
                }

                String cne = getCellValue(row.getCell(0), formatter).toUpperCase();
                String nom = getCellValue(row.getCell(1), formatter);
                String prenom = getCellValue(row.getCell(2), formatter);
                String emailAcademique = getCellValue(row.getCell(4), formatter).toLowerCase();

                if (cne.isBlank() || nom.isBlank() || prenom.isBlank() || emailAcademique.isBlank()) {
                    continue;
                }

                Student student = studentRepository.findByCneIgnoreCase(cne)
                        .or(() -> studentRepository.findByEmailIgnoreCase(emailAcademique))
                        .orElse(new Student());

                student.setCne(cne);
                student.setNom(toTitleCase(nom));
                student.setPrenom(toTitleCase(prenom));
                student.setEmail(emailAcademique);
                student.setDepartement(DEPARTEMENT_INFORMATIQUE);
                student.setFiliere(filiere);
                student.setSpecialite(filiere);
                student.setLangueSoutenance("FR");
                if (student.getThemePfe() == null) {
                    student.setThemePfe("");
                }
                if (student.getAffectationManuelle() == null) {
                    student.setAffectationManuelle(false);
                }

                studentsToSave.add(student);
            }
        }

        studentRepository.saveAll(studentsToSave);
        return studentsToSave.size();
    }

    /**
     * Format accepté pour les enseignants :
     * ligne 1 : Encadrant | vide | Discipline
     * ligne 2 : Nom | Prénom | vide
     * lignes suivantes : Nom | Prénom | Discipline
     */
    public int importTeachersFromExcel(MultipartFile file) throws IOException {
        validateExcelFile(file);
        List<Teacher> teachersToSave = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            ensureWorkbookHasSheet(workbook);
            var sheet = workbook.getSheetAt(0);
            validateTeacherHeader(sheet.getRow(0), sheet.getRow(1), formatter);

            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row, 3)) {
                    continue;
                }

                String nom = getCellValue(row.getCell(0), formatter);
                String prenom = getCellValue(row.getCell(1), formatter);
                String discipline = getCellValue(row.getCell(2), formatter);

                if (nom.isBlank() || prenom.isBlank()) {
                    continue;
                }

                String email = buildTeacherEmail(prenom, nom);
                String ppr = buildStablePpr(nom, prenom);

                Teacher teacher = teacherRepository.findByEmailIgnoreCase(email)
                        .or(() -> teacherRepository.findByPpr(ppr))
                        .orElse(new Teacher());

                teacher.setPpr(ppr);
                teacher.setNom(toTitleCase(nom));
                teacher.setPrenom(toTitleCase(prenom));
                teacher.setEmail(email);
                teacher.setDepartement(DEPARTEMENT_INFORMATIQUE);
                teacher.setSpecialite(discipline.isBlank() ? "Informatique" : discipline.trim());
                teacher.setLangue("FR");
                teacher.setEstProfLangue(false);
                teacher.setMaxEncadrement(5);
                teacher.setMaxJury(5);
                teacher.setActif(true);
                teacher.setDisponibleEncadrement(true);
                teacher.setDisponibleJury(true);

                teachersToSave.add(teacher);
            }
        }

        teacherRepository.saveAll(teachersToSave);
        return teachersToSave.size();
    }

    public int importRoomsFromExcel(MultipartFile file) throws IOException {
        validateExcelFile(file);
        List<Room> rooms = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            ensureWorkbookHasSheet(workbook);
            var sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row, 3)) {
                    continue;
                }

                String nomSalle = getCellValue(row.getCell(0), formatter);
                if (nomSalle.isBlank()) {
                    continue;
                }

                Room room = new Room();
                room.setNomSalle(nomSalle);
                room.setCapacite(parseInteger(getCellValue(row.getCell(1), formatter), 30));
                room.setActive(parseBooleanOrDefault(getCellValue(row.getCell(2), formatter), true));

                rooms.add(room);
            }
        }

        roomRepository.saveAll(rooms);
        return rooms.size();
    }

    private void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier Excel est vide.");
        }
        if (file.getSize() > MAX_EXCEL_SIZE) {
            throw new IllegalArgumentException("Le fichier Excel dépasse la taille maximale de 5 Mo.");
        }

        String filename = file.getOriginalFilename() == null
                ? ""
                : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")) {
            throw new IllegalArgumentException("Le fichier doit être au format .xlsx ou .xls.");
        }
    }

    private void ensureWorkbookHasSheet(Workbook workbook) {
        if (workbook.getNumberOfSheets() == 0) {
            throw new IllegalArgumentException("Le classeur Excel ne contient aucune feuille.");
        }
    }

    private void validateStudentHeader(Row header, DataFormatter formatter) {
        if (header == null) {
            throw new IllegalArgumentException("Fichier étudiants invalide : ligne d'en-tête introuvable.");
        }
        String h0 = normalize(getCellValue(header.getCell(0), formatter));
        String h1 = normalize(getCellValue(header.getCell(1), formatter));
        String h2 = normalize(getCellValue(header.getCell(2), formatter));
        String h4 = normalize(getCellValue(header.getCell(4), formatter));

        if (!h0.equals("cne") || !h1.equals("nom") || !h2.equals("prenom") || !h4.contains("email academique")) {
            throw new IllegalArgumentException("Format étudiants invalide. Format attendu : CNE | NOM | PRENOM | EMAIL PERSONNEL | EMAIL ACADEMIQUE.");
        }
    }

    private void validateTeacherHeader(Row firstHeader, Row secondHeader, DataFormatter formatter) {
        if (firstHeader == null || secondHeader == null) {
            throw new IllegalArgumentException("Fichier enseignants invalide : en-têtes introuvables.");
        }
        String h0 = normalize(getCellValue(firstHeader.getCell(0), formatter));
        String h2 = normalize(getCellValue(firstHeader.getCell(2), formatter));
        String hNom = normalize(getCellValue(secondHeader.getCell(0), formatter));
        String hPrenom = normalize(getCellValue(secondHeader.getCell(1), formatter));

        if (!h0.contains("encadrant") || !h2.contains("discpline") && !h2.contains("discipline")
                || !hNom.equals("nom") || !hPrenom.equals("prenom")) {
            throw new IllegalArgumentException("Format enseignants invalide. Format attendu : Nom | Prénom | Discipline, avec les deux lignes d'en-tête du fichier fourni.");
        }
    }

    private String detectFiliereFromFilename(String filename) {
        String name = normalize(filename == null ? "" : filename);
        String tokens = " " + name.replaceAll("[^a-z0-9]+", " ") + " ";
        if (name.contains("transformation") || name.contains("intelligence artificielle") || name.contains("tdia")) {
            return "TDIA";
        }
        if (name.contains("donnees") || name.contains("ingenierie des donnees") || tokens.contains(" id ")) {
            return "ID";
        }
        if (name.contains("genie informatique") || tokens.contains(" gi ")) {
            return "GI";
        }
        throw new IllegalArgumentException(
                "Filière indétectable dans le nom du fichier. Ajoutez GI, ID ou TDIA au nom.");
    }

    private String buildTeacherEmail(String prenom, String nom) {
        String base = removeAccents((prenom + "." + nom).toLowerCase())
                .replaceAll("[^a-z0-9.]+", "")
                .replaceAll("\\.+", ".")
                .replaceAll("^\\.|\\.$", "");
        if (base.isBlank()) {
            base = "enseignant";
        }
        return base + "@uae.ac.ma";
    }

    private String buildStablePpr(String nom, String prenom) {
        int hash = (normalize(nom) + "|" + normalize(prenom)).hashCode();
        return String.valueOf(100000 + Math.floorMod(hash, 900000));
    }

    private String getCellValue(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell).trim();
    }

    private boolean isRowEmpty(Row row, int maxColumns) {
        DataFormatter formatter = new DataFormatter();
        for (int i = 0; i < maxColumns; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && !formatter.formatCellValue(cell).trim().isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String normalize(String value) {
        return removeAccents(value == null ? "" : value)
                .trim()
                .toLowerCase()
                .replaceAll("\\s+", " ");
    }

    private String removeAccents(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    private String toTitleCase(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        if (normalized.isBlank()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (String part : normalized.split(" ")) {
            if (part.isBlank()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        return result.toString();
    }

    private Boolean parseBoolean(String value) {
        if (value == null) {
            return false;
        }
        String v = value.trim().toLowerCase();
        return v.equals("true") || v.equals("1") || v.equals("oui") || v.equals("yes");
    }

    private Boolean parseBooleanOrDefault(String value, boolean defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return parseBoolean(value);
    }

    private Integer parseInteger(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
