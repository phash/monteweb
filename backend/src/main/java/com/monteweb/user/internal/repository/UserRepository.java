package com.monteweb.user.internal.repository;

import com.monteweb.user.UserRole;
import com.monteweb.user.internal.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findByRole(UserRole role, Pageable pageable);

    Page<User> findByActiveTrue(Pageable pageable);

    @Query("""
        SELECT u FROM User u
        WHERE u.active = true
        AND (LOWER(u.displayName) LIKE LOWER(CONCAT('%', :query, '%'))
             OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY u.displayName ASC
        """)
    Page<User> searchByDisplayNameOrEmail(String query, Pageable pageable);

    Optional<User> findByOidcProviderAndOidcSubject(String oidcProvider, String oidcSubject);

    @Query("SELECT u FROM User u WHERE u.active = true AND :role = ANY(u.specialRoles)")
    java.util.List<User> findBySpecialRoleContaining(String role);
}
