package com.monteweb.room.internal.service;

import com.monteweb.room.DiscussionThreadCreatedEvent;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.room.RoomRole;
import com.monteweb.room.internal.model.*;
import com.monteweb.user.UserRole;
import com.monteweb.room.internal.repository.DiscussionReplyRepository;
import com.monteweb.room.internal.repository.DiscussionThreadRepository;
import com.monteweb.room.internal.repository.RoomRepository;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.exception.ForbiddenException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import com.monteweb.user.UserModuleApi;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DiscussionThreadService {

    private final DiscussionThreadRepository threadRepository;
    private final DiscussionReplyRepository replyRepository;
    private final RoomModuleApi roomModuleApi;
    private final RoomRepository roomRepository;
    private final UserModuleApi userModuleApi;
    private final ApplicationEventPublisher eventPublisher;

    public DiscussionThreadService(DiscussionThreadRepository threadRepository,
                                   DiscussionReplyRepository replyRepository,
                                   RoomModuleApi roomModuleApi,
                                   RoomRepository roomRepository,
                                   UserModuleApi userModuleApi,
                                   ApplicationEventPublisher eventPublisher) {
        this.threadRepository = threadRepository;
        this.replyRepository = replyRepository;
        this.roomModuleApi = roomModuleApi;
        this.roomRepository = roomRepository;
        this.userModuleApi = userModuleApi;
        this.eventPublisher = eventPublisher;
    }

    public Page<ThreadInfo> getThreads(UUID roomId, UUID userId, String statusFilter, Pageable pageable) {
        // Membership gate: global admins and section admins scoped to this room's section
        // may always list; otherwise the user must be a member.
        if (!hasAdminRoleForRoom(userId, roomId) && !roomModuleApi.isUserInRoom(userId, roomId)) {
            throw new ForbiddenException("You are not a member of this room");
        }

        var room = roomRepository.findById(roomId).orElse(null);
        var settings = room != null ? room.getSettings() : RoomSettings.defaults();

        // If discussions are disabled, return empty
        if (settings.effectiveDiscussionMode() == DiscussionMode.DISABLED) {
            return Page.empty(pageable);
        }

        // Determine which audiences the user can see (null => may see all audiences)
        var allowedAudiences = allowedAudiencesFor(roomId, userId, settings);

        Page<DiscussionThread> threads;
        if (allowedAudiences == null) {
            if (statusFilter != null && !statusFilter.isBlank()) {
                threads = threadRepository.findByRoomIdAndStatusOrderByCreatedAtDesc(
                        roomId, ThreadStatus.valueOf(statusFilter.toUpperCase()), pageable);
            } else {
                threads = threadRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable);
            }
        } else {
            if (statusFilter != null && !statusFilter.isBlank()) {
                threads = threadRepository.findByRoomIdAndStatusAndAudienceInOrderByCreatedAtDesc(
                        roomId, ThreadStatus.valueOf(statusFilter.toUpperCase()), allowedAudiences, pageable);
            } else {
                threads = threadRepository.findByRoomIdAndAudienceInOrderByCreatedAtDesc(
                        roomId, allowedAudiences, pageable);
            }
        }

        return threads.map(this::toThreadInfo);
    }

    public ThreadInfo getThread(UUID roomId, UUID threadId, UUID userId) {
        requireRoomMember(roomId, userId);
        var thread = findThread(threadId);
        if (!thread.getRoomId().equals(roomId)) {
            throw new ResourceNotFoundException("Thread", threadId);
        }
        requireCanViewThread(thread, userId);
        return toThreadInfo(thread);
    }

    @Transactional
    public ThreadInfo createThread(UUID roomId, UUID userId, String title, String content, String audience) {
        requireThreadCreator(roomId, userId);

        var thread = new DiscussionThread();
        thread.setRoomId(roomId);
        thread.setCreatedBy(userId);
        thread.setTitle(title);
        thread.setContent(content);
        if (audience != null && !audience.isBlank()) {
            thread.setAudience(ThreadAudience.valueOf(audience.toUpperCase()));
        }
        thread = threadRepository.save(thread);

        String creatorName = userModuleApi.findById(userId)
                .map(u -> u.firstName() + " " + u.lastName())
                .orElse("Unknown");

        eventPublisher.publishEvent(new DiscussionThreadCreatedEvent(
                thread.getId(), roomId, userId, creatorName, title
        ));

        return toThreadInfo(thread);
    }

    @Transactional
    public ThreadInfo archiveThread(UUID roomId, UUID threadId, UUID userId) {
        requireThreadManager(roomId, userId);
        var thread = findThread(threadId);
        if (!thread.getRoomId().equals(roomId)) {
            throw new ResourceNotFoundException("Thread", threadId);
        }
        thread.setStatus(ThreadStatus.ARCHIVED);
        return toThreadInfo(threadRepository.save(thread));
    }

    @Transactional
    public void deleteThread(UUID roomId, UUID threadId, UUID userId) {
        requireThreadManager(roomId, userId);
        var thread = findThread(threadId);
        if (!thread.getRoomId().equals(roomId)) {
            throw new ResourceNotFoundException("Thread", threadId);
        }
        threadRepository.delete(thread);
    }

    public Page<ReplyInfo> getReplies(UUID roomId, UUID threadId, UUID userId, Pageable pageable) {
        requireRoomMember(roomId, userId);
        var thread = findThread(threadId);
        if (!thread.getRoomId().equals(roomId)) {
            throw new ResourceNotFoundException("Thread", threadId);
        }
        requireCanViewThread(thread, userId);
        return replyRepository.findByThreadIdOrderByCreatedAtAsc(threadId, pageable)
                .map(this::toReplyInfo);
    }

    @Transactional
    public ReplyInfo addReply(UUID roomId, UUID threadId, UUID userId, String content) {
        requireRoomMember(roomId, userId);

        // Check discussion mode - ANNOUNCEMENTS_ONLY means no replies
        var room = roomRepository.findById(roomId).orElse(null);
        var settings = room != null ? room.getSettings() : RoomSettings.defaults();
        if (settings.effectiveDiscussionMode() == DiscussionMode.ANNOUNCEMENTS_ONLY) {
            throw new BusinessException("Replies are not allowed in announcement-only mode");
        }
        if (settings.effectiveDiscussionMode() == DiscussionMode.DISABLED) {
            throw new BusinessException("Discussions are disabled in this room");
        }

        var thread = findThread(threadId);
        if (!thread.getRoomId().equals(roomId)) {
            throw new ResourceNotFoundException("Thread", threadId);
        }
        requireCanViewThread(thread, userId);
        if (thread.getStatus() != ThreadStatus.ACTIVE) {
            throw new BusinessException("Cannot reply to an archived thread");
        }

        var reply = new DiscussionReply();
        reply.setThreadId(threadId);
        reply.setAuthorId(userId);
        reply.setContent(content);
        return toReplyInfo(replyRepository.save(reply));
    }

    // ---- Helpers ----

    private DiscussionThread findThread(UUID threadId) {
        return threadRepository.findById(threadId)
                .orElseThrow(() -> new ResourceNotFoundException("Discussion thread", threadId));
    }

    /**
     * Computes which thread audiences a user may see in a room.
     * Returns {@code null} when the user may see ALL audiences (TEACHER, SUPERADMIN,
     * SECTION_ADMIN, or room LEADER). Otherwise returns the explicit list of allowed audiences.
     * Mirrors the audience logic used by {@link #getThreads}.
     */
    private java.util.List<ThreadAudience> allowedAudiencesFor(UUID roomId, UUID userId, RoomSettings settings) {
        var userRole = userModuleApi.findById(userId).map(u -> u.role()).orElse(null);
        var roomRole = roomModuleApi.getUserRoleInRoom(userId, roomId).orElse(null);

        // SUPERADMIN is handled (global) inside hasAdminRoleForRoom, which also section-scopes
        // SECTION_ADMIN to the room's section. A bare SECTION_ADMIN of another section must NOT
        // be treated as "see all audiences" in a foreign-section room they merely belong to.
        boolean canSeeAll = userRole == UserRole.TEACHER
                || hasAdminRoleForRoom(userId, roomId) || roomRole == RoomRole.LEADER;
        if (canSeeAll) {
            return null;
        }

        boolean isParent = roomRole == RoomRole.PARENT_MEMBER || userRole == UserRole.PARENT;
        boolean isStudent = userRole == UserRole.STUDENT;

        if (isParent) {
            return java.util.List.of(ThreadAudience.ALLE, ThreadAudience.ELTERN);
        } else if (isStudent && settings.childDiscussionEnabled()) {
            return java.util.List.of(ThreadAudience.ALLE, ThreadAudience.KINDER);
        } else {
            // Students without child discussions enabled, or any other member: only ALLE
            return java.util.List.of(ThreadAudience.ALLE);
        }
    }

    /**
     * Ensures the caller is allowed to view a thread based on its audience.
     * Applies the SAME audience filter as {@link #getThreads} so that single-thread reads
     * (and replies) cannot bypass the list-level audience restriction. Throws
     * {@link ResourceNotFoundException} to avoid revealing the existence of hidden threads.
     */
    private void requireCanViewThread(DiscussionThread thread, UUID userId) {
        var room = roomRepository.findById(thread.getRoomId()).orElse(null);
        var settings = room != null ? room.getSettings() : RoomSettings.defaults();
        var allowed = allowedAudiencesFor(thread.getRoomId(), userId, settings);
        if (allowed != null && !allowed.contains(thread.getAudience())) {
            throw new ResourceNotFoundException("Discussion thread", thread.getId());
        }
    }

    /**
     * Returns true if the user is a global SUPERADMIN, or a SECTION_ADMIN scoped to the
     * section the given room belongs to. SECTION_ADMINs are NOT global admins: a section
     * admin scoped to e.g. Krippe must not gain access to an Oberstufe room.
     * Mirrors {@code RoomController.requireLeaderOrAdmin} / {@code isAdminForSection}.
     */
    private boolean hasAdminRoleForRoom(UUID userId, UUID roomId) {
        var user = userModuleApi.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        if (user.role() == UserRole.SUPERADMIN) {
            return true;
        }
        if (user.role() == UserRole.SECTION_ADMIN) {
            var sectionId = roomModuleApi.findById(roomId).map(r -> r.sectionId()).orElse(null);
            return sectionId != null && user.specialRoles() != null
                    && user.specialRoles().contains("SECTION_ADMIN:" + sectionId);
        }
        return false;
    }

    private void requireRoomMember(UUID roomId, UUID userId) {
        if (!hasAdminRoleForRoom(userId, roomId) && !roomModuleApi.isUserInRoom(userId, roomId)) {
            throw new ForbiddenException("You are not a member of this room");
        }
    }

    /**
     * Check if user can create threads: LEADER, TEACHER (in any room they're member of),
     * or PARENT if allowMemberThreadCreation is enabled.
     */
    private void requireThreadCreator(UUID roomId, UUID userId) {
        var userRole = userModuleApi.findById(userId).map(u -> u.role()).orElse(null);
        // SUPERADMIN (global) and SECTION_ADMIN scoped to this room's section can always create
        if (hasAdminRoleForRoom(userId, roomId)) return;

        var roomRole = roomModuleApi.getUserRoleInRoom(userId, roomId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this room"));

        // LEADER can always create
        if (roomRole == RoomRole.LEADER) return;
        // TEACHER can create discussions in rooms they're a member of
        if (userRole == UserRole.TEACHER) return;

        // PARENT can create if room setting allows
        if (userRole == UserRole.PARENT || roomRole == RoomRole.PARENT_MEMBER) {
            var room = roomRepository.findById(roomId).orElse(null);
            var settings = room != null ? room.getSettings() : RoomSettings.defaults();
            if (settings.allowMemberThreadCreation()) {
                return;
            }
        }

        throw new ForbiddenException("You do not have permission to create threads in this room");
    }

    /**
     * Check if user can manage (archive/delete) threads: LEADER, TEACHER, SUPERADMIN, SECTION_ADMIN.
     */
    private void requireThreadManager(UUID roomId, UUID userId) {
        var userRole = userModuleApi.findById(userId).map(u -> u.role()).orElse(null);
        if (hasAdminRoleForRoom(userId, roomId)) return;

        var roomRole = roomModuleApi.getUserRoleInRoom(userId, roomId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this room"));

        if (roomRole == RoomRole.LEADER) return;
        if (userRole == UserRole.TEACHER) return;

        throw new ForbiddenException("Only room leaders or teachers can manage threads");
    }

    private ThreadInfo toThreadInfo(DiscussionThread thread) {
        String authorName = userModuleApi.findById(thread.getCreatedBy())
                .map(u -> u.firstName() + " " + u.lastName())
                .orElse("Unknown");
        long replyCount = replyRepository.countByThreadId(thread.getId());

        return new ThreadInfo(
                thread.getId(), thread.getRoomId(), thread.getCreatedBy(), authorName,
                thread.getTitle(), thread.getContent(), thread.getStatus().name(),
                thread.getAudience().name(),
                replyCount, thread.getCreatedAt(), thread.getUpdatedAt()
        );
    }

    private ReplyInfo toReplyInfo(DiscussionReply reply) {
        String authorName = userModuleApi.findById(reply.getAuthorId())
                .map(u -> u.firstName() + " " + u.lastName())
                .orElse("Unknown");
        return new ReplyInfo(
                reply.getId(), reply.getThreadId(), reply.getAuthorId(), authorName,
                reply.getContent(), reply.getCreatedAt()
        );
    }

    // ---- DTOs ----

    public record ThreadInfo(
            UUID id, UUID roomId, UUID createdBy, String creatorName,
            String title, String content, String status, String audience,
            long replyCount, java.time.Instant createdAt, java.time.Instant updatedAt
    ) {}

    public record ReplyInfo(
            UUID id, UUID threadId, UUID authorId, String authorName,
            String content, java.time.Instant createdAt
    ) {}
}
