package com.pfe.gestionpfe.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pfe.gestionpfe.model.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByCneIgnoreCase(String cne);

    Optional<Student> findByEmailIgnoreCase(String email);

    List<Student> findByEncadrantIsNull();

    List<Student> findByAffectationManuelleFalse();

    List<Student> findByEncadrantIsNotNull();

    List<Student> findByFiliereIgnoreCase(String filiere);

    List<Student> findByFiliereIgnoreCaseAndEncadrantIsNotNull(String filiere);

    List<Student> findByFiliereIgnoreCaseAndEncadrantIsNull(String filiere);

    List<Student> findByFiliereIgnoreCaseAndAffectationManuelleFalse(String filiere);

    List<Student> findByDepartementIgnoreCaseAndFiliereIgnoreCase(String departement, String filiere);

    long countByEncadrantId(Long teacherId);
}