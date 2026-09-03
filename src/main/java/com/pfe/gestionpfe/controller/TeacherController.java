package com.pfe.gestionpfe.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.transaction.annotation.Transactional;

import com.pfe.gestionpfe.model.Teacher;
import com.pfe.gestionpfe.service.ParametrageData;
import com.pfe.gestionpfe.service.TeacherService;
import com.pfe.gestionpfe.service.StudentService;
import com.pfe.gestionpfe.service.DefenseService;

@Controller
public class TeacherController {

    private final TeacherService teacherService;
    private final StudentService studentService;
    private final DefenseService defenseService;
    private final ParametrageData parametrageData;

    public TeacherController(TeacherService teacherService,
                             StudentService studentService,
                             DefenseService defenseService,
                             ParametrageData parametrageData) {
        this.teacherService = teacherService;
        this.studentService = studentService;
        this.defenseService = defenseService;
        this.parametrageData = parametrageData;
    }

    @GetMapping("/teachers")
    public String listTeachers(Model model) {
        model.addAttribute("teachers", teacherService.getAllTeachers());
        return "teachers";
    }

    @GetMapping("/teachers/new")
    public String showAddTeacherForm(Model model) {
        Teacher teacher = new Teacher();
        teacher.setDepartement("Informatique");
        model.addAttribute("teacher", teacher);
        loadTeacherFormData(model);
        return "add-teacher";
    }

    @PostMapping("/teachers/save")
    public String saveTeacher(@Valid @ModelAttribute("teacher") Teacher teacher,
                              BindingResult bindingResult,
                              Model model) {

        if (bindingResult.hasErrors()) {
            teacher.setAccountPassword(null);
            loadTeacherFormData(model);
            if (teacher.getId() != null) {
                return "edit-teacher";
            }
            return "add-teacher";
        }

        try {
            teacherService.saveTeacher(teacher);
        } catch (IllegalArgumentException e) {
            teacher.setAccountPassword(null);
            model.addAttribute("error", e.getMessage());
            loadTeacherFormData(model);
            if (teacher.getId() != null) {
                return "edit-teacher";
            }
            return "add-teacher";
        }

        return "redirect:/teachers";
    }

    @GetMapping("/teachers/edit/{id}")
    public String showEditTeacherForm(@PathVariable Long id, Model model) {
        Teacher teacher = teacherService.getTeacherById(id);

        if (teacher == null) {
            model.addAttribute("teachers", teacherService.getAllTeachers());
            model.addAttribute("error", "Enseignant introuvable.");
            return "teachers";
        }

        model.addAttribute("teacher", teacher);
        loadTeacherFormData(model);
        return "edit-teacher";
    }

    @PostMapping("/teachers/delete-all")
    @Transactional
    public String deleteAllTeachers() {
        defenseService.deleteAllDefenses();
        studentService.clearAllEncadrants();
        teacherService.deleteAllTeachers();
        return "redirect:/teachers?success=deleteAll";
    }

    @PostMapping("/teachers/delete/{id}")
    public String deleteTeacher(@PathVariable Long id, Model model) {
        if (!teacherService.canDeleteTeacher(id)) {
            model.addAttribute("teachers", teacherService.getAllTeachers());
            model.addAttribute("error", "Impossible de supprimer cet enseignant car il est lié à un étudiant ou à une soutenance.");
            return "teachers";
        }

        teacherService.deleteTeacher(id);
        return "redirect:/teachers";
    }

    private void loadTeacherFormData(Model model) {
        model.addAttribute("filieres", parametrageData.getFilieresInformatique());
        model.addAttribute("specialites", parametrageData.getAllSpecialitesInformatique());
        model.addAttribute("specialitesByFiliere", parametrageData.getSpecialitesByFiliere());
    }
} 
// 
//La méthode loadTeacherFormData prépare les données du formulaire enseignant :
// specialitesByFiliere est un Map<String, List<String>> — il permet au formulaire HTML de changer dynamiquement la liste des spécialités en fonction de la filière choisie (via JavaScript dans le template).
