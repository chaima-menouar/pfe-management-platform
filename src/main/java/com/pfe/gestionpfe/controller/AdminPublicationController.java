package com.pfe.gestionpfe.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.pfe.gestionpfe.model.PublicationType;
import com.pfe.gestionpfe.service.DepartmentPublicationService;

@Controller
public class AdminPublicationController {

    private final DepartmentPublicationService publicationService;

    public AdminPublicationController(DepartmentPublicationService publicationService) {
        this.publicationService = publicationService;
    }

    @PostMapping("/admin/publications/publish/planning/{departement}")
    public String publishPlanning(@PathVariable String departement,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        publicationService.publish(departement, PublicationType.PLANNING, authentication.getName());// publier le planning du département par l’utilisateur connecté.
        redirectAttributes.addFlashAttribute("successMessage",
                "Planning publié pour le département : " + departement);// Message temporaire conservé après redirection.
        return "redirect:/";
    }

    @PostMapping("/admin/publications/publish/repartition/{departement}")
    public String publishRepartition(@PathVariable String departement,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        publicationService.publish(departement, PublicationType.REPARTITION, authentication.getName());
        redirectAttributes.addFlashAttribute("successMessage",
                "Répartition publiée pour le département : " + departement);
        return "redirect:/";
    }
}
