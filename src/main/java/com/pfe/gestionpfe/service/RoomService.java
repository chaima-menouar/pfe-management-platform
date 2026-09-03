package com.pfe.gestionpfe.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.pfe.gestionpfe.model.Room;
import com.pfe.gestionpfe.repository.DefenseRepository;
import com.pfe.gestionpfe.repository.RoomRepository;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final DefenseRepository defenseRepository;

    public RoomService(RoomRepository roomRepository, DefenseRepository defenseRepository) {
        this.roomRepository = roomRepository;
        this.defenseRepository = defenseRepository;
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public Room saveRoom(Room room) {
        if (room == null || room.getNomSalle() == null || room.getNomSalle().isBlank()) {
            throw new IllegalArgumentException("Le nom de la salle est obligatoire.");
        }

        String normalizedName = room.getNomSalle().trim();
        roomRepository.findByNomSalleIgnoreCase(normalizedName).ifPresent(existing -> {
            if (room.getId() == null || !existing.getId().equals(room.getId())) {
                throw new IllegalArgumentException("Une salle portant ce nom existe déjà.");
            }
        });

        room.setNomSalle(normalizedName);
        if (room.getActive() == null) {
            room.setActive(true);
        }
        if (room.getCapacite() == null || room.getCapacite() < 1) {
            throw new IllegalArgumentException("La capacité doit être supérieure à zéro.");
        }
        return roomRepository.save(room);
    }

    public Room getRoomById(Long id) {
        return roomRepository.findById(id).orElse(null);
    }

    public boolean canDeleteRoom(Long id) {
        return !defenseRepository.existsByRoomId(id);
    }

    public void deleteRoom(Long id) {
        roomRepository.deleteById(id);
    }
}
