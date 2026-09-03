package com.pfe.gestionpfe;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.pfe.gestionpfe.model.Role;
import com.pfe.gestionpfe.model.Room;
import com.pfe.gestionpfe.model.User;
import com.pfe.gestionpfe.repository.RoomRepository;
import com.pfe.gestionpfe.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;
    private final boolean bootstrapRooms;

    public DataInitializer(RoomRepository roomRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.bootstrap-admin.username}") String adminUsername,
                           @Value("${app.bootstrap-admin.password}") String adminPassword,
                           @Value("${app.bootstrap-rooms:true}") boolean bootstrapRooms) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.bootstrapRooms = bootstrapRooms;
    }

    @Override
    public void run(String... args) {
        if (bootstrapRooms) {
            initRooms();
        }
        initAdmin();
    }

    private void initRooms() {
        if (roomRepository.count() > 0) {
            return;
        }

        roomRepository.save(createRoom("S4A", 30));
        roomRepository.save(createRoom("S5A", 35));
        roomRepository.save(createRoom("Labo Info 1", 25));
    }

    private Room createRoom(String name, int capacity) {
        Room room = new Room();
        room.setNomSalle(name);
        room.setCapacite(capacity);
        room.setActive(true);
        return room;
    }

    private void initAdmin() {
        if (!userRepository.findByRole(Role.ADMIN).isEmpty()) {
            return;
        }

        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "APP_ADMIN_PASSWORD is required to create the first administrator account.");
        }
        if (adminPassword.length() < 12) {
            throw new IllegalStateException(
                    "APP_ADMIN_PASSWORD must contain at least 12 characters.");
        }
        if (adminPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(
                    "APP_ADMIN_PASSWORD cannot exceed 72 UTF-8 bytes when BCrypt is used.");
        }
        if (adminUsername == null || adminUsername.isBlank()) {
            throw new IllegalStateException("APP_ADMIN_USERNAME cannot be blank.");
        }
        if (userRepository.findByUsername(adminUsername.trim()).isPresent()) {
            throw new IllegalStateException(
                    "APP_ADMIN_USERNAME is already used by a non-administrator account.");
        }

        User admin = new User();
        admin.setUsername(adminUsername.trim());
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        userRepository.save(admin);
    }
}
