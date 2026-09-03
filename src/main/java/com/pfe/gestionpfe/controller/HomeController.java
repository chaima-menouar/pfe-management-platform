package com.pfe.gestionpfe.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.pfe.gestionpfe.service.DashboardService;

@Controller
public class HomeController {

    private final DashboardService dashboardService;

    public HomeController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    } // C'est l'injection de dépendances par constructeur.  Tu n'écris jamais new DashboardService(). C'est le principe fondamental de Spring : c'est lui qui crée et connecte les objets. 

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("dashboard", dashboardService.buildDashboard());
        return "index";
    }
}
// Quand l'admin arrive sur http://localhost:8080/, cette méthode est appelée. Model est un objet Spring qui sert de "sac" pour transporter les données vers le template Thymeleaf. dashboardService.buildDashboard() retourne un objet avec toutes les statistiques (nombre d'étudiants, de profs, de soutenances planifiées...). Cet objet est mis dans le model sous le nom "dashboard". Dans index.html, Thymeleaf peut alors écrire ${dashboard.totalStudents} pour afficher ce chiffre. Enfin, return "index" dit à Spring d'afficher /templates/index.html.