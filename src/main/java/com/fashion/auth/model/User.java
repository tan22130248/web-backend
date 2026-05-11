//package com.fashion.auth.model;
//
//import jakarta.persistence.*;
//import lombok.*;
//import org.hibernate.annotations.GenericGenerator;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "users")
//@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
//public class User {
//
//    @Id
//    @GeneratedValue(generator = "uuid2")
//    @GenericGenerator(name = "uuid2", strategy = "uuid2")
//    @Column(length = 36, updatable = false, nullable = false)
//    private String id;
//
//    @Column(name = "full_name", nullable = false, length = 150)
//    private String fullName;
//
//    @Column(nullable = false, unique = true, length = 255)
//    private String email;
//
//    @Column(name = "password_hash", nullable = false, length = 255)
//    private String passwordHash;
//
//    @Column(length = 20)
//    private String phone;
//
//    @Column(name = "avatar_url", length = 500)
//    private String avatarUrl;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false, length = 10)
//    private Role role = Role.buyer;
//
//    @Column(name = "is_active", nullable = false)
//    private boolean isActive = true;
//
//    @Column(name = "created_at", updatable = false)
//    private LocalDateTime createdAt = LocalDateTime.now();
//
//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt = LocalDateTime.now();
//
//    @PreUpdate
//    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }
//
//    public enum Role { buyer, seller, admin }
//}
package com.fashion.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class User {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "char(36)", updatable = false, nullable = false)
    private String id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 20)
    private String phone;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Role role = Role.buyer;

    // Use boolean (primitive) + explicit column name to avoid Lombok/JPA mismatch
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }

    public enum Role { buyer, seller, admin }
}
