package com.pfe.gestionpfe.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
        return "login"; // Cette méthode affiche la page de connexion login.html lorsqu’un utilisateur accède à /login.
    }

    @GetMapping("/redirect")
    public String redirect(Authentication auth) {

        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ENCADRANT"))) {
            return "redirect:/encadrant";
        }

        return "redirect:/" ;// dashboard admin;
    }
}

//  est un objet Spring Security injecté automatiquement. Il contient toutes les informations sur l'utilisateur connecté : son nom, ses rôles, ses permissions. Tu n'as pas besoin de le créer ou de le chercher toi-même — Spring te le passe directement.