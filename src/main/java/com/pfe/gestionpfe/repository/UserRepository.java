package com.pfe.gestionpfe.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pfe.gestionpfe.model.User;
import com.pfe.gestionpfe.model.Role;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByTeacherId(Long teacherId);
    List<User> findByRole(Role role);
}
