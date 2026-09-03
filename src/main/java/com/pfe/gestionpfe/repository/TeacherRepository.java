package com.pfe.gestionpfe.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pfe.gestionpfe.model.Teacher;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByPpr(String ppr);

    Optional<Teacher> findByEmailIgnoreCase(String email);

    List<Teacher> findByActifTrueAndDisponibleEncadrementTrue();

    List<Teacher> findByActifTrueAndDisponibleJuryTrue();

    List<Teacher> findBySpecialiteIgnoreCase(String specialite);

    List<Teacher> findByActifTrue();

    List<Teacher> findByDisponibleJuryTrueAndActifTrue();

    List<Teacher> findByDisponibleEncadrementTrueAndActifTrue();

    List<Teacher> findBySpecialiteIgnoreCaseAndActifTrue(String specialite);

    List<Teacher> findByLangueIgnoreCase(String langue);

    List<Teacher> findByEstProfLangueTrueAndLangueIgnoreCaseAndDisponibleJuryTrueAndActifTrue(String langue);

}