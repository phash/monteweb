package com.monteweb.parentletter.internal.service;

import com.monteweb.family.FamilyInfo;
import com.monteweb.family.FamilyModuleApi;
import com.monteweb.notification.NotificationModuleApi;
import com.monteweb.notification.NotificationType;
import com.monteweb.parentletter.*;
import com.monteweb.parentletter.internal.model.ParentLetter;
import com.monteweb.parentletter.internal.model.ParentLetterConfig;
import com.monteweb.parentletter.internal.model.ParentLetterRecipient;
import com.monteweb.parentletter.internal.repository.ParentLetterConfigRepository;
import com.monteweb.parentletter.internal.repository.ParentLetterRecipientRepository;
import com.monteweb.parentletter.internal.repository.ParentLetterRepository;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.room.RoomRole;
import com.monteweb.school.SchoolModuleApi;
import com.monteweb.shared.exception.ForbiddenException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@Transactional
@ConditionalOnProperty(prefix = "monteweb.modules", name = "parentletter.enabled", havingValue = "true")
public class ParentLetterService implements ParentLetterModuleApi {

    private static final Logger log = LoggerFactory.getLogger(ParentLetterService.class);

    private final ParentLetterRepository letterRepository;
    private final ParentLetterRecipientRepository recipientRepository;
    private final ParentLetterConfigRepository configRepository;
    private final RoomModuleApi roomModuleApi;
    private final UserModuleApi userModuleApi;
    private final FamilyModuleApi familyModuleApi;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired(required = false)
    private NotificationModuleApi notificationModuleApi;

    @Autowired(required = false)
    private SchoolModuleApi schoolModuleApi;

    public ParentLetterService(ParentLetterRepository letterRepository,
                               ParentLetterRecipientRepository recipientRepository,
                               ParentLetterConfigRepository configRepository,
                               RoomModuleApi roomModuleApi,
                               UserModuleApi userModuleApi,
                               FamilyModuleApi familyModuleApi,
                               ApplicationEventPublisher eventPublisher) {
        this.letterRepository = letterRepository;
        this.recipientRepository = recipientRepository;
        this.configRepository = configRepository;
        this.roomModuleApi = roomModuleApi;
        this.userModuleApi = userModuleApi;
        this.familyModuleApi = familyModuleApi;
        this.eventPublisher = eventPublisher;
    }

    // ---- Public API (ParentLetterModuleApi) ----

    @Override
    @Transactional(readOnly = true)
    public List<ParentLetterInfo> getLettersForRoom(UUID roomId) {
        return letterRepository.findByRoomIdOrderByCreatedAtDesc(roomId).stream()
                .map(this::toLetterInfo)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ParentLetterInfo> findById(UUID letterId) {
        return letterRepository.findById(letterId).map(this::toLetterInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingForParent(UUID parentUserId) {
        // Count recipient rows for this parent that are OPEN (not yet confirmed)
        return recipientRepository.countByParentIdAndStatusNot(parentUserId, RecipientStatus.CONFIRMED);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> exportUserData(UUID userId) {
        Map<String, Object> data = new LinkedHashMap<>();

        // Letters created by the user
        var createdLetters = letterRepository.findByCreatedByOrderByCreatedAtDesc(userId, Pageable.unpaged())
                .getContent().stream()
                .map(l -> Map.of(
                        "id", l.getId(),
                        "title", l.getTitle(),
                        "status", l.getStatus().name(),
                        "roomId", l.getRoomId(),
                        "createdAt", l.getCreatedAt()
                ))
                .toList();
        data.put("lettersCreated", createdLetters);

        // Recipient entries (as parent)
        var recipientEntries = recipientRepository.findByParentId(userId).stream()
                .map(r -> Map.of(
                        "letterId", r.getLetter().getId(),
                        "studentId", r.getStudentId(),
                        "status", r.getStatus().name(),
                        "readAt", r.getReadAt() != null ? r.getReadAt() : "",
                        "confirmedAt", r.getConfirmedAt() != null ? r.getConfirmedAt() : ""
                ))
                .toList();
        data.put("recipientEntries", recipientEntries);

        return data;
    }

    // ---- Letter CRUD ----

    public ParentLetterDetailInfo createLetter(CreateParentLetterRequest request, UUID userId) {
        var room = roomModuleApi.findById(request.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", request.roomId()));

        // Only KLASSE rooms are allowed
        if (!"KLASSE".equals(room.type())) {
            throw new ForbiddenException("Parent letters can only be created for KLASSE rooms");
        }

        // Only LEADER or SUPERADMIN may create letters
        requireLeaderOrSuperadmin(request.roomId(), userId);

        var letter = new ParentLetter();
        letter.setRoomId(request.roomId());
        letter.setCreatedBy(userId);
        letter.setTitle(request.title());
        letter.setContent(request.content());
        letter.setStatus(ParentLetterStatus.DRAFT);
        letter.setSendDate(request.sendDate());
        letter.setDeadline(request.deadline());
        letter.setReminderDays(request.reminderDays() != null ? request.reminderDays() : 3);
        letter.setReminderSent(false);
        letter = letterRepository.save(letter);

        buildRecipients(letter, request.studentIds());

        return toLetterDetailInfo(letter);
    }

    public ParentLetterDetailInfo updateLetter(UUID id, UpdateParentLetterRequest request, UUID userId) {
        var letter = requireExistingDraft(id, userId);

        letter.setTitle(request.title());
        letter.setContent(request.content());
        letter.setSendDate(request.sendDate());
        letter.setDeadline(request.deadline());
        if (request.reminderDays() != null) {
            letter.setReminderDays(request.reminderDays());
        }
        letter = letterRepository.save(letter);

        // Rebuild recipients if studentIds changed
        recipientRepository.deleteByLetterId(letter.getId());
        buildRecipients(letter, request.studentIds());

        return toLetterDetailInfo(letter);
    }

    public ParentLetterDetailInfo sendLetter(UUID id, UUID userId) {
        var letter = letterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ParentLetter", id));

        requireLeaderOrSuperadmin(letter.getRoomId(), userId);

        if (letter.getStatus() != ParentLetterStatus.DRAFT) {
            throw new ForbiddenException("Only DRAFT letters can be sent");
        }

        Instant now = Instant.now();
        boolean scheduled = letter.getSendDate() != null && letter.getSendDate().isAfter(now);
        letter.setStatus(scheduled ? ParentLetterStatus.SCHEDULED : ParentLetterStatus.SENT);
        letter = letterRepository.save(letter);

        if (!scheduled) {
            sendNotificationsToRecipients(letter, userId);
            publishSentEvent(letter, userId);
        }

        return toLetterDetailInfo(letter);
    }

    public ParentLetterDetailInfo closeLetter(UUID id, UUID userId) {
        var letter = letterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ParentLetter", id));

        requireLeaderOrSuperadmin(letter.getRoomId(), userId);

        if (letter.getStatus() == ParentLetterStatus.DRAFT || letter.getStatus() == ParentLetterStatus.CLOSED) {
            throw new ForbiddenException("Only SENT or SCHEDULED letters can be closed");
        }

        letter.setStatus(ParentLetterStatus.CLOSED);
        letter = letterRepository.save(letter);
        return toLetterDetailInfo(letter);
    }

    public void deleteLetter(UUID id, UUID userId) {
        var letter = requireExistingDraft(id, userId);
        recipientRepository.deleteByLetterId(id);
        letterRepository.delete(letter);
    }

    // ---- Querying ----

    @Transactional(readOnly = true)
    public ParentLetterDetailInfo getLetterDetail(UUID id, UUID userId) {
        var letter = letterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ParentLetter", id));

        var user = userModuleApi.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        boolean isSuperadmin = user.role() == UserRole.SUPERADMIN;
        boolean isCreator = userId.equals(letter.getCreatedBy());
        boolean isLeader = roomModuleApi.getUserRoleInRoom(userId, letter.getRoomId())
                .map(role -> role == RoomRole.LEADER)
                .orElse(false);

        if (!isSuperadmin && !isCreator && !isLeader) {
            throw new ForbiddenException("You are not authorized to view this letter's details");
        }

        return toLetterDetailInfo(letter);
    }

    @Transactional(readOnly = true)
    public Page<ParentLetterInfo> getMyLetters(UUID userId, Pageable pageable) {
        return letterRepository.findByCreatedByOrderByCreatedAtDesc(userId, pageable)
                .map(this::toLetterInfo);
    }

    @Transactional(readOnly = true)
    public Page<ParentLetterDetailInfo> getLettersForParent(UUID parentId, Pageable pageable) {
        // Letters are fetched from the room's sent letters where this parent is a recipient
        // We use recipient entries to find relevant letter IDs
        var recipientEntries = recipientRepository.findByParentId(parentId);
        var letterIds = recipientEntries.stream()
                .map(r -> r.getLetter().getId())
                .distinct()
                .toList();

        if (letterIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // Filter to only SENT letters whose sendDate is in the past (or null)
        Instant now = Instant.now();
        var letters = letterRepository.findAllById(letterIds).stream()
                .filter(l -> l.getStatus() == ParentLetterStatus.SENT || l.getStatus() == ParentLetterStatus.CLOSED)
                .filter(l -> l.getSendDate() == null || !l.getSendDate().isAfter(now))
                .sorted(Comparator.comparing(ParentLetter::getCreatedAt).reversed())
                .toList();

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), letters.size());
        List<ParentLetter> pageContent = start >= letters.size() ? List.of() : letters.subList(start, end);

        return new org.springframework.data.domain.PageImpl<>(
                pageContent.stream().map(this::toLetterDetailInfo).toList(),
                pageable,
                letters.size()
        );
    }

    @Transactional
    public ParentLetterDetailInfo getLetterForParent(UUID letterId, UUID parentId) {
        var letter = letterRepository.findById(letterId)
                .orElseThrow(() -> new ResourceNotFoundException("ParentLetter", letterId));

        if (letter.getStatus() != ParentLetterStatus.SENT && letter.getStatus() != ParentLetterStatus.CLOSED) {
            throw new ForbiddenException("This letter is not available");
        }

        Instant now = Instant.now();
        if (letter.getSendDate() != null && letter.getSendDate().isAfter(now)) {
            throw new ForbiddenException("This letter has not been released yet");
        }

        // Mark all OPEN recipients for this parent as READ
        var recipients = recipientRepository.findByLetterIdAndStatus(letterId, RecipientStatus.OPEN)
                .stream()
                .filter(r -> r.getParentId().equals(parentId))
                .toList();

        for (var recipient : recipients) {
            recipient.setStatus(RecipientStatus.READ);
            recipient.setReadAt(now);
            recipientRepository.save(recipient);
        }

        return toLetterDetailInfo(letter);
    }

    @Transactional
    public ParentLetterRecipientInfo confirmLetter(UUID letterId, UUID parentId, UUID studentId) {
        var recipient = recipientRepository.findByLetterIdAndParentIdAndStudentId(letterId, parentId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ParentLetterRecipient for letter " + letterId + " parent " + parentId));

        if (!recipient.getParentId().equals(parentId)) {
            throw new ForbiddenException("You are not authorized to confirm this recipient");
        }

        if (recipient.getStatus() == RecipientStatus.CONFIRMED) {
            return toRecipientInfo(recipient);
        }

        recipient.setStatus(RecipientStatus.CONFIRMED);
        recipient.setConfirmedAt(Instant.now());
        recipient.setConfirmedBy(parentId);
        recipient = recipientRepository.save(recipient);

        return toRecipientInfo(recipient);
    }

    @Transactional
    public void markAsRead(UUID letterId, UUID parentId) {
        var recipients = recipientRepository.findByLetterIdAndStatus(letterId, RecipientStatus.OPEN)
                .stream()
                .filter(r -> r.getParentId().equals(parentId))
                .toList();

        Instant now = Instant.now();
        for (var r : recipients) {
            r.setStatus(RecipientStatus.READ);
            r.setReadAt(now);
            recipientRepository.save(r);
        }
    }

    // ---- Config ----

    @Transactional(readOnly = true)
    public ParentLetterConfigInfo getConfig(UUID sectionId) {
        Optional<ParentLetterConfig> config = sectionId != null
                ? configRepository.findBySectionId(sectionId)
                : configRepository.findBySectionIdIsNull();

        // Fall back to global config if no section-specific one exists
        if (config.isEmpty() && sectionId != null) {
            config = configRepository.findBySectionIdIsNull();
        }

        return config.map(this::toConfigInfo).orElseGet(() -> {
            // Return a default config if nothing persisted yet
            return new ParentLetterConfigInfo(null, sectionId, null, null, null, 3);
        });
    }

    public ParentLetterConfigInfo updateConfig(UpdateParentLetterConfigRequest request, UUID sectionId) {
        ParentLetterConfig config = sectionId != null
                ? configRepository.findBySectionId(sectionId).orElseGet(() -> {
                    var c = new ParentLetterConfig();
                    c.setSectionId(sectionId);
                    return c;
                })
                : configRepository.findBySectionIdIsNull().orElseGet(ParentLetterConfig::new);

        if (request.signatureTemplate() != null) {
            config.setSignatureTemplate(request.signatureTemplate());
        }
        if (request.reminderDays() != null) {
            config.setReminderDays(request.reminderDays());
        }

        config = configRepository.save(config);
        return toConfigInfo(config);
    }

    public ParentLetterConfigInfo updateLetterheadPath(UUID sectionId, String storagePath) {
        ParentLetterConfig config = sectionId != null
                ? configRepository.findBySectionId(sectionId).orElseGet(() -> {
                    var c = new ParentLetterConfig();
                    c.setSectionId(sectionId);
                    return c;
                })
                : configRepository.findBySectionIdIsNull().orElseGet(ParentLetterConfig::new);

        config.setLetterheadPath(storagePath);
        config = configRepository.save(config);
        return toConfigInfo(config);
    }

    public ParentLetterConfigInfo removeLetterhead(UUID sectionId) {
        Optional<ParentLetterConfig> configOpt = sectionId != null
                ? configRepository.findBySectionId(sectionId)
                : configRepository.findBySectionIdIsNull();

        if (configOpt.isPresent()) {
            var config = configOpt.get();
            config.setLetterheadPath(null);
            config = configRepository.save(config);
            return toConfigInfo(config);
        }
        return new ParentLetterConfigInfo(null, sectionId, null, null, null, 3);
    }

    // ---- DSGVO ----

    @Transactional
    public void cleanupUserData(UUID userId) {
        // Anonymize letters created by the deleted user — keep structure, nullify creator
        var createdLetters = letterRepository.findByCreatedByOrderByCreatedAtDesc(userId, Pageable.unpaged())
                .getContent();
        for (var letter : createdLetters) {
            letter.setCreatedBy(null);
        }
        letterRepository.saveAll(createdLetters);

        // Remove all recipient entries where this user was the parent
        recipientRepository.deleteByParentId(userId);

        // Remove recipient entries where this user was a student
        recipientRepository.deleteByStudentId(userId);
    }

    // ---- Reminder / Scheduled dispatch (called by ReminderService) ----

    @Transactional
    public void sendRemindersForLetter(ParentLetter letter) {
        var openRecipients = recipientRepository.findByLetterIdAndStatus(
                letter.getId(), RecipientStatus.OPEN);

        Instant now = Instant.now();
        for (var recipient : openRecipients) {
            if (notificationModuleApi != null) {
                try {
                    notificationModuleApi.sendNotification(
                            recipient.getParentId(),
                            NotificationType.PARENT_LETTER_REMINDER,
                            "Elternbrief: Bitte bestätigen",
                            "Der Elternbrief \"" + letter.getTitle() + "\" wartet noch auf Ihre Bestätigung.",
                            "/parent-letters/" + letter.getId(),
                            "PARENT_LETTER",
                            letter.getId()
                    );
                    recipient.setReminderSentAt(now);
                    recipientRepository.save(recipient);
                } catch (Exception e) {
                    log.warn("Failed to send reminder notification to parent {}: {}",
                            recipient.getParentId(), e.getMessage());
                }
            }
        }

        letter.setReminderSent(true);
        letterRepository.save(letter);
    }

    @Transactional
    public void dispatchScheduledLetter(ParentLetter letter, UUID dispatchedBy) {
        letter.setStatus(ParentLetterStatus.SENT);
        letter = letterRepository.save(letter);
        sendNotificationsToRecipients(letter, dispatchedBy);
        publishSentEvent(letter, dispatchedBy);
    }

    // ---- Private helpers ----

    private void requireLeaderOrSuperadmin(UUID roomId, UUID userId) {
        var user = userModuleApi.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.role() == UserRole.SUPERADMIN) {
            return;
        }

        var role = roomModuleApi.getUserRoleInRoom(userId, roomId);
        if (role.isEmpty() || role.get() != RoomRole.LEADER) {
            throw new ForbiddenException("Only LEADER or SUPERADMIN can manage parent letters");
        }
    }

    private ParentLetter requireExistingDraft(UUID id, UUID userId) {
        var letter = letterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ParentLetter", id));

        if (letter.getStatus() != ParentLetterStatus.DRAFT) {
            throw new ForbiddenException("Only DRAFT letters can be modified or deleted");
        }

        requireLeaderOrSuperadmin(letter.getRoomId(), userId);
        return letter;
    }

    /**
     * Build recipient rows for a letter.
     * If studentIds is non-null and non-empty, restrict to those students.
     * Otherwise, include all students in the room.
     */
    private void buildRecipients(ParentLetter letter, List<UUID> studentIds) {
        List<UUID> targetStudentIds;

        if (studentIds != null && !studentIds.isEmpty()) {
            targetStudentIds = studentIds;
        } else {
            // Get all room members and filter to students
            var memberIds = roomModuleApi.getMemberUserIds(letter.getRoomId());
            var members = userModuleApi.findByIds(memberIds);
            targetStudentIds = members.stream()
                    .filter(u -> u.role() == UserRole.STUDENT)
                    .map(UserInfo::id)
                    .toList();
        }

        for (UUID studentId : targetStudentIds) {
            var families = familyModuleApi.findByUserId(studentId);
            for (var family : families) {
                var parentMembers = family.members().stream()
                        .filter(m -> "PARENT".equals(m.role()))
                        .toList();
                for (var parentMember : parentMembers) {
                    // Avoid duplicates (unique constraint on letter_id + parent_id + student_id)
                    var existing = recipientRepository.findByLetterIdAndParentIdAndStudentId(
                            letter.getId(), parentMember.userId(), studentId);
                    if (existing.isEmpty()) {
                        var recipient = new ParentLetterRecipient();
                        recipient.setLetter(letter);
                        recipient.setStudentId(studentId);
                        recipient.setParentId(parentMember.userId());
                        recipient.setFamilyId(family.id());
                        recipient.setStatus(RecipientStatus.OPEN);
                        recipientRepository.save(recipient);
                    }
                }
            }
        }
    }

    private void sendNotificationsToRecipients(ParentLetter letter, UUID senderId) {
        if (notificationModuleApi == null) {
            return;
        }
        var recipients = recipientRepository.findByLetterIdOrderByCreatedAt(letter.getId());
        Set<UUID> notifiedParents = new HashSet<>();
        for (var recipient : recipients) {
            if (notifiedParents.add(recipient.getParentId())) {
                try {
                    notificationModuleApi.sendNotification(
                            recipient.getParentId(),
                            NotificationType.PARENT_LETTER,
                            "Neuer Elternbrief",
                            "Ein neuer Elternbrief wurde versandt: \"" + letter.getTitle() + "\"",
                            "/parent-letters/" + letter.getId(),
                            "PARENT_LETTER",
                            letter.getId()
                    );
                } catch (Exception e) {
                    log.warn("Failed to send notification to parent {}: {}",
                            recipient.getParentId(), e.getMessage());
                }
            }
        }
    }

    private void publishSentEvent(ParentLetter letter, UUID sentBy) {
        var recipients = recipientRepository.findByLetterIdOrderByCreatedAt(letter.getId());
        var parentIds = recipients.stream()
                .map(ParentLetterRecipient::getParentId)
                .distinct()
                .toList();

        String senderName = userModuleApi.findById(sentBy)
                .map(u -> u.firstName() + " " + u.lastName())
                .orElse("Unknown");

        eventPublisher.publishEvent(new ParentLetterSentEvent(
                letter.getId(),
                letter.getTitle(),
                letter.getRoomId(),
                sentBy,
                senderName,
                parentIds
        ));
    }

    /**
     * Resolve template variables in letter content.
     * Replaces: {Familie}, {NameKind}, {Anrede}, {LehrerName}
     */
    public String resolveVariables(String content, UserInfo student, FamilyInfo family,
                                   UserInfo parent, UserInfo teacher) {
        if (content == null) return "";
        return content
                .replace("{Familie}", family != null ? family.name() : "")
                .replace("{NameKind}", student != null ? student.firstName() : "")
                .replace("{Anrede}", parent != null
                        ? "Sehr geehrte/r " + parent.displayName()
                        : "Sehr geehrte Eltern")
                .replace("{LehrerName}", teacher != null ? teacher.displayName() : "");
    }

    // ---- Mappers ----

    private ParentLetterInfo toLetterInfo(ParentLetter letter) {
        long total = recipientRepository.countByLetterId(letter.getId());
        long confirmed = recipientRepository.countByLetterIdAndStatus(letter.getId(), RecipientStatus.CONFIRMED);

        String creatorName = letter.getCreatedBy() != null
                ? userModuleApi.findById(letter.getCreatedBy())
                        .map(u -> u.firstName() + " " + u.lastName())
                        .orElse("Unknown")
                : "Unknown";

        String roomName = roomModuleApi.findById(letter.getRoomId())
                .map(r -> r.name())
                .orElse("Unknown");

        return new ParentLetterInfo(
                letter.getId(),
                letter.getTitle(),
                letter.getStatus(),
                letter.getRoomId(),
                roomName,
                letter.getCreatedBy(),
                creatorName,
                letter.getSendDate(),
                letter.getDeadline(),
                (int) total,
                (int) confirmed,
                letter.getCreatedAt(),
                letter.getUpdatedAt()
        );
    }

    private ParentLetterDetailInfo toLetterDetailInfo(ParentLetter letter) {
        long total = recipientRepository.countByLetterId(letter.getId());
        long confirmed = recipientRepository.countByLetterIdAndStatus(letter.getId(), RecipientStatus.CONFIRMED);

        String creatorName = letter.getCreatedBy() != null
                ? userModuleApi.findById(letter.getCreatedBy())
                        .map(u -> u.firstName() + " " + u.lastName())
                        .orElse("Unknown")
                : "Unknown";

        String roomName = roomModuleApi.findById(letter.getRoomId())
                .map(r -> r.name())
                .orElse("Unknown");

        var recipients = recipientRepository.findByLetterIdOrderByCreatedAt(letter.getId())
                .stream()
                .map(this::toRecipientInfo)
                .toList();

        return new ParentLetterDetailInfo(
                letter.getId(),
                letter.getTitle(),
                letter.getContent(),
                letter.getStatus(),
                letter.getRoomId(),
                roomName,
                letter.getCreatedBy(),
                creatorName,
                letter.getSendDate(),
                letter.getDeadline(),
                letter.getReminderDays(),
                letter.isReminderSent(),
                (int) total,
                (int) confirmed,
                recipients,
                letter.getCreatedAt(),
                letter.getUpdatedAt()
        );
    }

    private ParentLetterRecipientInfo toRecipientInfo(ParentLetterRecipient recipient) {
        String studentName = userModuleApi.findById(recipient.getStudentId())
                .map(u -> u.firstName() + " " + u.lastName())
                .orElse("Unknown");

        String parentName = userModuleApi.findById(recipient.getParentId())
                .map(UserInfo::displayName)
                .orElse("Unknown");

        String familyName = familyModuleApi.findByUserId(recipient.getStudentId()).stream()
                .filter(f -> f.id().equals(recipient.getFamilyId()))
                .findFirst()
                .map(FamilyInfo::name)
                .orElse("Unknown");

        String confirmedByName = recipient.getConfirmedBy() != null
                ? userModuleApi.findById(recipient.getConfirmedBy())
                        .map(UserInfo::displayName)
                        .orElse("Unknown")
                : null;

        return new ParentLetterRecipientInfo(
                recipient.getId(),
                recipient.getStudentId(),
                studentName,
                recipient.getParentId(),
                parentName,
                familyName,
                recipient.getStatus(),
                recipient.getReadAt(),
                recipient.getConfirmedAt(),
                confirmedByName,
                recipient.getReminderSentAt()
        );
    }

    private ParentLetterConfigInfo toConfigInfo(ParentLetterConfig config) {
        String sectionName = null;
        if (config.getSectionId() != null && schoolModuleApi != null) {
            sectionName = schoolModuleApi.findById(config.getSectionId())
                    .map(s -> s.name())
                    .orElse(null);
        }

        return new ParentLetterConfigInfo(
                config.getId(),
                config.getSectionId(),
                sectionName,
                config.getLetterheadPath(),
                config.getSignatureTemplate(),
                config.getReminderDays()
        );
    }
}
