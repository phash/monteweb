package com.monteweb.user.internal.service;

import com.monteweb.calendar.CalendarModuleApi;
import com.monteweb.cleaning.CleaningModuleApi;
import com.monteweb.family.FamilyModuleApi;
import com.monteweb.feed.FeedModuleApi;
import com.monteweb.files.FilesModuleApi;
import com.monteweb.forms.FormsModuleApi;
import com.monteweb.fotobox.FotoboxModuleApi;
import com.monteweb.fundgrube.FundgrubeModuleApi;
import com.monteweb.jobboard.JobboardModuleApi;
import com.monteweb.messaging.MessagingModuleApi;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.shared.exception.ResourceNotFoundException;
import com.monteweb.user.*;
import com.monteweb.user.internal.model.DataAccessLog;
import com.monteweb.user.internal.model.User;
import com.monteweb.user.internal.repository.DataAccessLogRepository;
import com.monteweb.user.internal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class UserService implements UserModuleApi {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final DataAccessLogRepository dataAccessLogRepository;

    // Always-present module APIs
    private final FeedModuleApi feedModuleApi;
    private final RoomModuleApi roomModuleApi;
    private final FamilyModuleApi familyModuleApi;

    // Optional module APIs (conditional)
    private final MessagingModuleApi messagingModuleApi;
    private final CalendarModuleApi calendarModuleApi;
    private final JobboardModuleApi jobboardModuleApi;
    private final CleaningModuleApi cleaningModuleApi;
    private final FormsModuleApi formsModuleApi;
    private final FotoboxModuleApi fotoboxModuleApi;
    private final FundgrubeModuleApi fundgrubeModuleApi;
    private final FilesModuleApi filesModuleApi;

    public UserService(UserRepository userRepository,
                       ApplicationEventPublisher eventPublisher,
                       DataAccessLogRepository dataAccessLogRepository,
                       @Lazy FeedModuleApi feedModuleApi,
                       @Lazy RoomModuleApi roomModuleApi,
                       @Lazy FamilyModuleApi familyModuleApi,
                       @Lazy @Autowired(required = false) MessagingModuleApi messagingModuleApi,
                       @Lazy @Autowired(required = false) CalendarModuleApi calendarModuleApi,
                       @Lazy @Autowired(required = false) JobboardModuleApi jobboardModuleApi,
                       @Lazy @Autowired(required = false) CleaningModuleApi cleaningModuleApi,
                       @Lazy @Autowired(required = false) FormsModuleApi formsModuleApi,
                       @Lazy @Autowired(required = false) FotoboxModuleApi fotoboxModuleApi,
                       @Lazy @Autowired(required = false) FundgrubeModuleApi fundgrubeModuleApi,
                       @Lazy @Autowired(required = false) FilesModuleApi filesModuleApi) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.dataAccessLogRepository = dataAccessLogRepository;
        this.feedModuleApi = feedModuleApi;
        this.roomModuleApi = roomModuleApi;
        this.familyModuleApi = familyModuleApi;
        this.messagingModuleApi = messagingModuleApi;
        this.calendarModuleApi = calendarModuleApi;
        this.jobboardModuleApi = jobboardModuleApi;
        this.cleaningModuleApi = cleaningModuleApi;
        this.formsModuleApi = formsModuleApi;
        this.fotoboxModuleApi = fotoboxModuleApi;
        this.fundgrubeModuleApi = fundgrubeModuleApi;
        this.filesModuleApi = filesModuleApi;
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

    public Page<UserInfo> findDirectory(UserRole role, UUID sectionId, UUID roomId, String search, Pageable pageable) {
        List<UUID> userIds = null;

        if (roomId != null) {
            userIds = roomModuleApi.getMemberUserIds(roomId);
            if (userIds.isEmpty()) {
                return Page.empty(pageable);
            }
        } else if (sectionId != null) {
            var rooms = roomModuleApi.findBySectionId(sectionId);
            Set<UUID> memberIds = new LinkedHashSet<>();
            for (var room : rooms) {
                memberIds.addAll(roomModuleApi.getMemberUserIds(room.id()));
            }
            if (memberIds.isEmpty()) {
                return Page.empty(pageable);
            }
            userIds = new ArrayList<>(memberIds);
        }

        String searchTerm = (search == null || search.isBlank()) ? null : search.trim();
        return userRepository.findForDirectory(role, userIds, searchTerm, pageable).map(this::toUserInfo);
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
     * DSGVO: Export all personal data for a user, aggregated from all modules.
     */
    public Map<String, Object> exportUserData(UUID userId) {
        var user = findEntityById(userId);
        Map<String, Object> data = new LinkedHashMap<>();

        // User profile data
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("email", user.getEmail());
        profile.put("firstName", user.getFirstName());
        profile.put("lastName", user.getLastName());
        profile.put("displayName", user.getDisplayName());
        profile.put("phone", user.getPhone());
        profile.put("role", user.getRole());
        profile.put("createdAt", user.getCreatedAt());
        profile.put("lastLoginAt", user.getLastLoginAt());
        data.put("profile", profile);

        // Always-present modules
        data.put("feed", feedModuleApi.exportUserData(userId));
        data.put("rooms", roomModuleApi.exportUserData(userId));
        data.put("families", familyModuleApi.exportUserData(userId));

        // Optional modules
        if (messagingModuleApi != null) {
            data.put("messaging", messagingModuleApi.exportUserData(userId));
        }
        if (calendarModuleApi != null) {
            data.put("calendar", calendarModuleApi.exportUserData(userId));
        }
        if (jobboardModuleApi != null) {
            data.put("jobboard", jobboardModuleApi.exportUserData(userId));
        }
        if (cleaningModuleApi != null) {
            data.put("cleaning", cleaningModuleApi.exportUserData(userId));
        }
        if (formsModuleApi != null) {
            data.put("forms", formsModuleApi.exportUserData(userId));
        }
        if (fotoboxModuleApi != null) {
            data.put("fotobox", fotoboxModuleApi.exportUserData(userId));
        }
        if (fundgrubeModuleApi != null) {
            data.put("fundgrube", fundgrubeModuleApi.exportUserData(userId));
        }
        if (filesModuleApi != null) {
            data.put("files", filesModuleApi.exportUserData(userId));
        }

        data.put("exportedAt", Instant.now());

        // Log data access for DSGVO audit
        logDataAccess(userId, userId, "DATA_EXPORT", "User exported their personal data");

        return data;
    }

    /**
     * DSGVO: Log data access for audit trail (Art. 15 DSGVO).
     */
    @Transactional
    public void logDataAccess(UUID accessedBy, UUID targetUserId, String action, String details) {
        var log = new DataAccessLog();
        log.setAccessedBy(accessedBy);
        log.setTargetUserId(targetUserId);
        log.setAction(action);
        log.setDetails(details);
        dataAccessLogRepository.save(log);
    }

    /**
     * DSGVO: Request account deletion with 14-day grace period.
     */
    @Transactional
    public void requestDeletion(UUID userId) {
        var user = findEntityById(userId);
        if (user.getDeletionRequestedAt() != null) {
            throw new com.monteweb.shared.exception.BusinessException("Deletion already requested");
        }
        var now = Instant.now();
        var scheduledAt = now.plus(14, ChronoUnit.DAYS);
        user.setDeletionRequestedAt(now);
        user.setScheduledDeletionAt(scheduledAt);
        userRepository.save(user);
        eventPublisher.publishEvent(new UserDeletionRequestedEvent(userId, scheduledAt));
    }

    /**
     * DSGVO: Cancel a pending account deletion.
     */
    @Transactional
    public void cancelDeletion(UUID userId) {
        var user = findEntityById(userId);
        if (user.getDeletionRequestedAt() == null) {
            throw new com.monteweb.shared.exception.BusinessException("No deletion pending");
        }
        user.setDeletionRequestedAt(null);
        user.setScheduledDeletionAt(null);
        userRepository.save(user);
        eventPublisher.publishEvent(new UserDeletionCancelledEvent(userId));
    }

    /**
     * DSGVO: Anonymize a user account and publish deletion event for cross-module cleanup.
     */
    @Transactional
    public void anonymizeAndDelete(UUID userId, String reason) {
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
        user.setDeletionRequestedAt(null);
        user.setScheduledDeletionAt(null);
        user.setOidcProvider(null);
        user.setOidcSubject(null);
        userRepository.save(user);
        eventPublisher.publishEvent(new UserDeletionExecutedEvent(userId));
    }

    /**
     * Returns users whose scheduled deletion date has passed.
     */
    public List<User> findUsersScheduledForDeletion() {
        return userRepository.findByScheduledDeletionAtBeforeAndDeletionRequestedAtIsNotNull(Instant.now());
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

    // --- TOTP / 2FA ---

    @Override
    public boolean isTotpEnabled(UUID userId) {
        return userRepository.findById(userId)
                .map(User::isTotpEnabled)
                .orElse(false);
    }

    @Override
    public Optional<String> getTotpSecret(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getTotpSecret)
                .filter(s -> s != null && !s.isBlank());
    }

    @Override
    @Transactional
    public void setTotpSecret(UUID userId, String secret) {
        var user = findEntityById(userId);
        user.setTotpSecret(secret);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void enableTotp(UUID userId, String[] recoveryCodes) {
        var user = findEntityById(userId);
        user.setTotpEnabled(true);
        user.setTotpRecoveryCodes(recoveryCodes);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void disableTotp(UUID userId) {
        var user = findEntityById(userId);
        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        user.setTotpRecoveryCodes(null);
        userRepository.save(user);
    }

    @Override
    public String[] getTotpRecoveryCodes(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getTotpRecoveryCodes)
                .orElse(new String[0]);
    }

    @Override
    @Transactional
    public void setTotpRecoveryCodes(UUID userId, String[] codes) {
        var user = findEntityById(userId);
        user.setTotpRecoveryCodes(codes);
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
                user.getAssignedRolesAsSet(),
                user.isActive()
        );
    }
}
