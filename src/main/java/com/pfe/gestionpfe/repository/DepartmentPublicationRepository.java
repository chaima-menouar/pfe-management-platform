package com.pfe.gestionpfe.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pfe.gestionpfe.model.DepartmentPublication;
import com.pfe.gestionpfe.model.PublicationType;

public interface DepartmentPublicationRepository extends JpaRepository<DepartmentPublication, Long> {

    Optional<DepartmentPublication> findTopByDepartementAndTypeOrderByGeneratedAtDesc(
            String departement,
            PublicationType type
    );
}