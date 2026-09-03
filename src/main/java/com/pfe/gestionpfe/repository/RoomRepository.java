package com.pfe.gestionpfe.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pfe.gestionpfe.model.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByActiveTrue();
    Optional<Room> findByNomSalleIgnoreCase(String nomSalle);
}
