package com.monteweb.user.internal.repository;

import com.monteweb.user.UserRole;
import com.monteweb.user.internal.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query(value = "SELECT * FROM users WHERE is_active = true AND :role = ANY(special_roles)", nativeQuery = true)
    java.util.List<User> findBySpecialRoleContaining(String role);

    @Query("""
        SELECT u FROM User u
        WHERE (:role IS NULL OR u.role = :role)
        AND (:active IS NULL OR u.active = :active)
        AND (:search IS NULL OR :search = ''
            OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY u.lastName ASC, u.firstName ASC
        """)
    Page<User> findFiltered(
            @Param("role") UserRole role,
            @Param("active") Boolean active,
            @Param("search") String search,
            Pageable pageable
    );
}
