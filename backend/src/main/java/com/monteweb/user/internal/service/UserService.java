package com.monteweb.user.internal.service;

import com.monteweb.shared.exception.ResourceNotFoundException;
import com.monteweb.user.*;
import com.monteweb.user.internal.model.User;
import com.monteweb.user.internal.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;;

@Service
@Transactional(readOnly = true)
public class UserService implements UserModuleApi {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UserService(UserRepository userRepository, ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Optional<UserInfo> findById(UUID id) {
        return userRepository.findById(id).map(this::toUserInfo);
    }

    @Override
    public Optional<UserInfo> findByEmail(String email) {
        return userRepository.findByEmail(email).map(this::toUserInfo);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public UserInfo createUser(String email, String passwordHash, String firstName, String lastName, String phone, UserRole role) {
        var user = new User();
        user.setEmail(email.toLowerCase().trim());
        user.setPasswordHash(passwordHash);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        user.setRole(role);
        user.setDisplayName(firstName + " " + lastName);

        user = userRepository.save(user);

        eventPublisher.publishEvent(new UserRegisteredEvent(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole()
        ));

        return toUserInfo(user);
    }

    @Override
    public Optional<String> getPasswordHash(String email) {
        return userRepository.findByEmail(email).map(User::getPasswordHash);
    }

    @Override
    @Transactional
    public void updateLastLogin(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
        });
    }

    @Override
    public Page<UserInfo> searchUsers(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return userRepository.findByActiveTrue(pageable).map(this::toUserInfo);
        }
        return userRepository.searchByDisplayNameOrEmail(query.trim(), pageable).map(this::toUserInfo);
    }

    public Page<UserInfo> findAll(Pageable pageable) {
        return userRepository.findByActiveTrue(pageable).map(this::toUserInfo);
    }

    public Page<UserInfo> findFiltered(UserRole role, Boolean active, String search, Pageable pageable) {
        return userRepository.findFiltered(role, active, search, pageable).map(this::toUserInfo);
    }

    public User findEntityById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional
    public UserInfo updateAvatarUrl(UUID userId, String avatarUrl) {
        var user = findEntityById(userId);
        user.setAvatarUrl(avatarUrl);
        return toUserInfo(userRepository.save(user));
    }

    @Transactional
    public UserInfo updateProfile(UUID userId, String firstName, String lastName, String phone) {
        var user = findEntityById(userId);
        if (firstName != null) user.setFirstName(firstName);
        if (lastName != null) user.setLastName(lastName);
        if (phone != null) user.setPhone(phone);
        user.setDisplayName(user.getFirstName() + " " + user.getLastName());
        return toUserInfo(userRepository.save(user));
    }

    @Override
    @Transactional
    public void updatePasswordHash(UUID userId, String passwordHash) {
        var user = findEntityById(userId);
        user.setPasswordHash(passwordHash);
        userRepository.save(user);
    }

    @Override
    public Optional<UserInfo> findByOidcProviderAndSubject(String provider, String subject) {
        return userRepository.findByOidcProviderAndOidcSubject(provider, subject)
                .map(this::toUserInfo);
    }

    @Override
    @Transactional
    public UserInfo createOidcUser(String email, String firstName, String lastName,
                                    String oidcProvider, String oidcSubject, UserRole role) {
        var user = new User();
        user.setEmail(email.toLowerCase().trim());
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setDisplayName(firstName + " " + lastName);
        user.setRole(role);
        user.setOidcProvider(oidcProvider);
        user.setOidcSubject(oidcSubject);
        user.setEmailVerified(true); // OIDC provider already verified email
        user = userRepository.save(user);

        eventPublisher.publishEvent(new UserRegisteredEvent(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole()
        ));

        return toUserInfo(user);
    }

    @Override
    @Transactional
    public void linkOidcProvider(UUID userId, String oidcProvider, String oidcSubject) {
        var user = findEntityById(userId);
        user.setOidcProvider(oidcProvider);
        user.setOidcSubject(oidcSubject);
        userRepository.save(user);
    }

    @Transactional
    public UserInfo adminUpdateProfile(UUID userId, String email, String firstName, String lastName, String phone) {
        var user = findEntityById(userId);
        if (email != null && !email.equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(email.toLowerCase().trim())) {
                throw new com.monteweb.shared.exception.BusinessException("Email already in use");
            }
            user.setEmail(email.toLowerCase().trim());
        }
        if (firstName != null) user.setFirstName(firstName);
        if (lastName != null) user.setLastName(lastName);
        if (phone != null) user.setPhone(phone);
        user.setDisplayName(user.getFirstName() + " " + user.getLastName());
        return toUserInfo(userRepository.save(user));
    }

    @Transactional
    public UserInfo addSpecialRole(UUID userId, String role) {
        var user = findEntityById(userId);
        var roles = new java.util.ArrayList<>(java.util.Arrays.asList(user.getSpecialRoles() != null ? user.getSpecialRoles() : new String[0]));
        if (!roles.contains(role)) {
            roles.add(role);
            user.setSpecialRoles(roles.toArray(new String[0]));
            userRepository.save(user);
        }
        return toUserInfo(user);
    }

    @Transactional
    public UserInfo removeSpecialRole(UUID userId, String role) {
        var user = findEntityById(userId);
        var roles = new java.util.ArrayList<>(java.util.Arrays.asList(user.getSpecialRoles() != null ? user.getSpecialRoles() : new String[0]));
        roles.remove(role);
        user.setSpecialRoles(roles.toArray(new String[0]));
        return toUserInfo(userRepository.save(user));
    }

    public List<UserInfo> findBySpecialRoleContaining(String rolePrefix) {
        return userRepository.findBySpecialRoleContaining(rolePrefix).stream()
                .map(this::toUserInfo)
                .toList();
    }

    @Override
    public List<UserInfo> findByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return userRepository.findAllById(ids).stream()
                .map(this::toUserInfo)
                .toList();
    }

    @Transactional
    public UserInfo updateRole(UUID userId, UserRole role) {
        var user = findEntityById(userId);
        user.setRole(role);
        return toUserInfo(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserInfo setActive(UUID userId, boolean active) {
        var user = findEntityById(userId);
        user.setActive(active);
        return toUserInfo(userRepository.save(user));
    }

    /**
     * DSGVO: Export all personal data for a user.
     */
    public Map<String, Object> exportUserData(UUID userId) {
        var user = findEntityById(userId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("email", user.getEmail());
        data.put("firstName", user.getFirstName());
        data.put("lastName", user.getLastName());
        data.put("displayName", user.getDisplayName());
        data.put("phone", user.getPhone());
        data.put("role", user.getRole());
        data.put("createdAt", user.getCreatedAt());
        data.put("lastLoginAt", user.getLastLoginAt());
        return data;
    }

    /**
     * DSGVO: Anonymize a user account. Replaces personal data but keeps the record
     * for audit trail integrity.
     */
    @Transactional
    public void anonymizeUser(UUID userId, String reason) {
        var user = findEntityById(userId);
        user.setEmail(UUID.randomUUID() + "@deleted.local");
        user.setFirstName("Geloeschter");
        user.setLastName("Benutzer");
        user.setDisplayName("Geloeschter Benutzer");
        user.setPhone(null);
        user.setAvatarUrl(null);
        user.setPasswordHash("DELETED");
        user.setActive(false);
        user.setDeletedAt(Instant.now());
        user.setDeletionReason(reason);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserInfo switchActiveRole(UUID userId, UserRole newRole) {
        var user = findEntityById(userId);
        if (user.getRole() == UserRole.SUPERADMIN || user.getRole() == UserRole.STUDENT) {
            throw new com.monteweb.shared.exception.BusinessException("Fixed-role users cannot switch roles");
        }
        if (!user.getAssignedRolesAsSet().contains(newRole.name())) {
            throw new com.monteweb.shared.exception.BusinessException("Role not in assigned roles");
        }
        user.setRole(newRole);
        return toUserInfo(userRepository.save(user));
    }

    @Transactional
    public UserInfo updateAssignedRoles(UUID userId, Set<String> roles) {
        var user = findEntityById(userId);
        if (user.getRole() == UserRole.SUPERADMIN || user.getRole() == UserRole.STUDENT) {
            throw new com.monteweb.shared.exception.BusinessException("Cannot assign switchable roles to SUPERADMIN or STUDENT");
        }
        // Validate all roles are valid switchable roles
        var validRoles = Set.of("TEACHER", "PARENT", "SECTION_ADMIN");
        for (String r : roles) {
            if (!validRoles.contains(r)) {
                throw new com.monteweb.shared.exception.BusinessException("Invalid assignable role: " + r);
            }
        }
        user.setAssignedRoles(roles.toArray(new String[0]));
        // If active role is not in the new assigned roles, switch to first
        if (!roles.contains(user.getRole().name()) && !roles.isEmpty()) {
            user.setRole(UserRole.valueOf(roles.iterator().next()));
        }
        return toUserInfo(userRepository.save(user));
    }

    private UserInfo toUserInfo(User user) {
        return new UserInfo(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getDisplayName(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getSpecialRolesAsSet(),
                user.getAssignedRolesAsSet(),
                user.isActive()
        );
    }
}
