package com.fashion.auth.repository;

import com.fashion.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByFullName(String fullName);   // used as "username" lookup
    boolean existsByEmail(String email);
    boolean existsByFullName(String fullName);
}
