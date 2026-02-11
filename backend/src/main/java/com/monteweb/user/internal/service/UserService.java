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

    public User findEntityById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
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
    public UserInfo updateRole(UUID userId, UserRole role) {
        var user = findEntityById(userId);
        user.setRole(role);
        return toUserInfo(userRepository.save(user));
    }

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
                user.isActive()
        );
    }
}
