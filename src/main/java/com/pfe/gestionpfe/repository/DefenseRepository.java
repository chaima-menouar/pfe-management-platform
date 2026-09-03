package com.pfe.gestionpfe.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pfe.gestionpfe.model.Defense;

public interface DefenseRepository extends JpaRepository<Defense, Long> {

    boolean existsByEncadrantId(Long id);
    boolean existsByJury1Id(Long id);
    boolean existsByJury2Id(Long id);
    boolean existsByStudentId(Long id);
    boolean existsByRoomId(Long id);

    boolean existsByStudentIdAndStatut(Long studentId, String statut);

    List<Defense> findByDateSoutenance(LocalDate dateSoutenance);

    List<Defense> findByDateSoutenanceAndRoomId(LocalDate dateSoutenance, Long roomId);

    @Query("""
        select d from Defense d
        where d.room is not null
          and d.room.id = :roomId
          and d.dateSoutenance = :date
          and d.heureDebut = :heure
    """)
    List<Defense> findRoomDefensesByDateAndHeure(@Param("roomId") Long roomId,
                                                 @Param("date") LocalDate date,
                                                 @Param("heure") LocalTime heure);

    @Query("""
        select d from Defense d
        where d.room is not null
          and d.room.id = :roomId
          and d.dateSoutenance = :date
        order by d.heureDebut asc
    """)
    List<Defense> findRoomDefensesByDate(@Param("roomId") Long roomId,
                                         @Param("date") LocalDate date);

    @Query("""
        select d from Defense d
        where d.dateSoutenance = :date
          and d.heureDebut = :heure
          and (
                (d.encadrant is not null and d.encadrant.id = :teacherId)
             or (d.jury1 is not null and d.jury1.id = :teacherId)
             or (d.jury2 is not null and d.jury2.id = :teacherId)
          )
    """)
    List<Defense> findTeacherDefensesByDateAndHeure(@Param("teacherId") Long teacherId,
                                                    @Param("date") LocalDate date,
                                                    @Param("heure") LocalTime heure);

    @Query("""
        select d from Defense d
        where d.dateSoutenance = :date
          and (
                (d.encadrant is not null and d.encadrant.id = :teacherId)
             or (d.jury1 is not null and d.jury1.id = :teacherId)
             or (d.jury2 is not null and d.jury2.id = :teacherId)
          )
        order by d.heureDebut asc
    """)
    List<Defense> findTeacherDefensesByDate(@Param("teacherId") Long teacherId,
                                            @Param("date") LocalDate date);

    @Query("""
        select d from Defense d
        where d.student is not null
          and lower(d.student.filiere) = lower(:filiere)
        order by d.dateSoutenance asc, d.heureDebut asc
    """)
    List<Defense> findByStudentFiliereOrderByDateSoutenanceAscHeureDebutAsc(
            @Param("filiere") String filiere
    );

    @Query("""
        select d from Defense d
        where d.student is not null
          and lower(d.student.departement) = 'informatique'
        order by d.dateSoutenance asc, d.heureDebut asc
    """)
    List<Defense> findAllInformatiqueOrdered();

    @Query("""
        select d from Defense d
        order by d.dateSoutenance asc, d.heureDebut asc
    """)
    List<Defense> findAllOrdered();

    long countByStatut(String statut);

    @Query("""
        select count(d) from Defense d
        where d.student is not null
          and lower(d.student.filiere) = lower(:filiere)
    """)
    long countByStudentFiliere(@Param("filiere") String filiere);

    @Query("""
        select count(d) from Defense d
        where d.student is not null
          and lower(d.student.departement) = 'informatique'
    """)
    long countInformatiqueDefenses();
}