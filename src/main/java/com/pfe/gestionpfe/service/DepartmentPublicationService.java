package com.pfe.gestionpfe.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.pfe.gestionpfe.model.DepartmentPublication;
import com.pfe.gestionpfe.model.PublicationType;
import com.pfe.gestionpfe.repository.DepartmentPublicationRepository;

@Service
public class DepartmentPublicationService {

    private final DepartmentPublicationRepository repository;

    public DepartmentPublicationService(DepartmentPublicationRepository repository) {
        this.repository = repository;
    }

    public DepartmentPublication publish(String departement, PublicationType type, String generatedBy) {
        DepartmentPublication publication = new DepartmentPublication(
                departement,
                type,
                LocalDateTime.now(),
                generatedBy
        );
        return repository.save(publication);
    }

    public Optional<DepartmentPublication> getLatestPlanning(String departement) {
        return repository.findTopByDepartementAndTypeOrderByGeneratedAtDesc(departement, PublicationType.PLANNING);
    }

    public Optional<DepartmentPublication> getLatestRepartition(String departement) {
        return repository.findTopByDepartementAndTypeOrderByGeneratedAtDesc(departement, PublicationType.REPARTITION);
    }
}