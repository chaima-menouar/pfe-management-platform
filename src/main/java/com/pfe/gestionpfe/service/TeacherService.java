package com.pfe.gestionpfe.service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.pfe.gestionpfe.model.Role;
import com.pfe.gestionpfe.model.Teacher;
import com.pfe.gestionpfe.model.User;
import com.pfe.gestionpfe.repository.DefenseRepository;
import com.pfe.gestionpfe.repository.StudentRepository;
import com.pfe.gestionpfe.repository.TeacherRepository;
import com.pfe.gestionpfe.repository.UserRepository;

@Service
public class TeacherService {

    private static final String DEPARTEMENT_INFORMATIQUE = "Informatique";

    private final TeacherRepository teacherRepository;
    private final DefenseRepository defenseRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TeacherService(TeacherRepository teacherRepository,
                          DefenseRepository defenseRepository,
                          StudentRepository studentRepository,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.teacherRepository = teacherRepository;
        this.defenseRepository = defenseRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Teacher> getAllTeachers() {
        return teacherRepository.findAll().stream()
                .filter(t -> t.getDepartement() != null && t.getDepartement().equalsIgnoreCase(DEPARTEMENT_INFORMATIQUE))
                .toList();
    }

    public List<Teacher> getAvailableTeachersForEncadrement() {
        return teacherRepository.findByActifTrueAndDisponibleEncadrementTrue().stream()
                .filter(t -> t.getDepartement() != null && t.getDepartement().equalsIgnoreCase(DEPARTEMENT_INFORMATIQUE))
                .toList();
    }

    public List<Teacher> getAvailableTeachersForJury() {
        return teacherRepository.findByActifTrueAndDisponibleJuryTrue().stream()
                .filter(t -> t.getDepartement() != null && t.getDepartement().equalsIgnoreCase(DEPARTEMENT_INFORMATIQUE))
                .toList();
    }

    @Transactional
    public Teacher saveTeacher(Teacher teacher) {
        if (teacher == null) {
            throw new IllegalArgumentException("L'enseignant est invalide.");
        }

        teacher.setDepartement(DEPARTEMENT_INFORMATIQUE);

        if (teacher.getMaxEncadrement() == null) {
            teacher.setMaxEncadrement(3);
        }

        if (teacher.getMaxJury() == null) {
            teacher.setMaxJury(5);
        }

        if (teacher.getActif() == null) {
            teacher.setActif(true);
        }

        if (teacher.getDisponibleEncadrement() == null) {
            teacher.setDisponibleEncadrement(true);
        }

        if (teacher.getDisponibleJury() == null) {
            teacher.setDisponibleJury(true);
        }

        if (teacher.getEstProfLangue() == null) {
            teacher.setEstProfLangue(false);
        }

        if (teacher.getPpr() != null) {
            teacherRepository.findByPpr(teacher.getPpr().trim())
                    .ifPresent(existing -> {
                        if (teacher.getId() == null || !existing.getId().equals(teacher.getId())) {
                            throw new IllegalArgumentException("Ce PPR existe déjà.");
                        }
                    });
        }

        if (teacher.getEmail() != null) {
            teacherRepository.findByEmailIgnoreCase(teacher.getEmail().trim())
                    .ifPresent(existing -> {
                        if (teacher.getId() == null || !existing.getId().equals(teacher.getId())) {
                            throw new IllegalArgumentException("Cet email professeur existe déjà.");
                        }
                    });
        }

        String requestedPassword = teacher.getAccountPassword();
        if (requestedPassword != null && !requestedPassword.isBlank() && requestedPassword.length() < 12) {
            throw new IllegalArgumentException("Le mot de passe du compte encadrant doit contenir au moins 12 caractères.");
        }
        if (requestedPassword != null
                && requestedPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Le mot de passe du compte encadrant ne peut pas dépasser 72 octets UTF-8.");
        }

        String username = teacher.getEmail() == null ? "" : teacher.getEmail().trim().toLowerCase();
        userRepository.findByUsername(username).ifPresent(existing -> {
            boolean sameTeacher = existing.getTeacher() != null
                    && teacher.getId() != null
                    && existing.getTeacher().getId().equals(teacher.getId());
            if (!sameTeacher) {
                throw new IllegalArgumentException("Un compte utilisateur utilise déjà cet email.");
            }
        });

        Teacher savedTeacher = teacherRepository.save(teacher);
        User account = userRepository.findByTeacherId(savedTeacher.getId()).orElse(null);
        if (account != null || requestedPassword != null && !requestedPassword.isBlank()) {
            if (account == null) {
                account = new User();
                account.setRole(Role.ENCADRANT);
                account.setTeacher(savedTeacher);
            }
            account.setUsername(username);
            account.setEnabled(Boolean.TRUE.equals(savedTeacher.getActif()));
            if (requestedPassword != null && !requestedPassword.isBlank()) {
                account.setPassword(passwordEncoder.encode(requestedPassword));
            }
            userRepository.save(account);
        }

        return savedTeacher;
    }

    public Teacher getTeacherById(Long id) {
        return teacherRepository.findById(id)
                .filter(t -> t.getDepartement() != null && t.getDepartement().equalsIgnoreCase(DEPARTEMENT_INFORMATIQUE))
                .orElse(null);
    }

    public boolean canDeleteTeacher(Long id) {
        return !defenseRepository.existsByEncadrantId(id)
                && !defenseRepository.existsByJury1Id(id)
                && !defenseRepository.existsByJury2Id(id)
                && studentRepository.countByEncadrantId(id) == 0;
    }

    @Transactional
    public void deleteTeacher(Long id) {
        userRepository.findByTeacherId(id).ifPresent(userRepository::delete);
        teacherRepository.deleteById(id);
    }

    @Transactional
    public void deleteAllTeachers() {
        userRepository.deleteAll(userRepository.findByRole(Role.ENCADRANT));
        teacherRepository.deleteAll();
    }
}
