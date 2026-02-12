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
        requireRoomMember(roomId, userId);

        var room = roomRepository.findById(roomId).orElse(null);
        var settings = room != null ? room.getSettings() : RoomSettings.defaults();

        // If discussions are disabled, return empty
        if (settings.effectiveDiscussionMode() == DiscussionMode.DISABLED) {
            return Page.empty(pageable);
        }

        // Determine which audiences the user can see
        var user = userModuleApi.findById(userId);
        var userRole = user.map(u -> u.role()).orElse(null);
        var roomRole = roomModuleApi.getUserRoleInRoom(userId, roomId).orElse(null);

        boolean canSeeAll = userRole == UserRole.TEACHER || userRole == UserRole.SUPERADMIN
                || userRole == UserRole.SECTION_ADMIN || roomRole == RoomRole.LEADER;

        Page<DiscussionThread> threads;
        if (canSeeAll) {
            if (statusFilter != null && !statusFilter.isBlank()) {
                threads = threadRepository.findByRoomIdAndStatusOrderByCreatedAtDesc(
                        roomId, ThreadStatus.valueOf(statusFilter.toUpperCase()), pageable);
            } else {
                threads = threadRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable);
            }
        } else {
            boolean isParent = roomRole == RoomRole.PARENT_MEMBER || userRole == UserRole.PARENT;
            boolean isStudent = userRole == UserRole.STUDENT;

            java.util.List<ThreadAudience> allowedAudiences;
            if (isParent) {
                allowedAudiences = java.util.List.of(ThreadAudience.ALLE, ThreadAudience.ELTERN);
            } else if (isStudent && settings.childDiscussionEnabled()) {
                allowedAudiences = java.util.List.of(ThreadAudience.ALLE, ThreadAudience.KINDER);
            } else if (isStudent) {
                // Child discussions not enabled: only see ALLE
                allowedAudiences = java.util.List.of(ThreadAudience.ALLE);
            } else {
                allowedAudiences = java.util.List.of(ThreadAudience.ALLE);
            }

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

    private void requireRoomMember(UUID roomId, UUID userId) {
        if (!roomModuleApi.isUserInRoom(userId, roomId)) {
            throw new ForbiddenException("You are not a member of this room");
        }
    }

    /**
     * Check if user can create threads: LEADER, TEACHER (in any room they're member of),
     * or PARENT if allowMemberThreadCreation is enabled.
     */
    private void requireThreadCreator(UUID roomId, UUID userId) {
        var roomRole = roomModuleApi.getUserRoleInRoom(userId, roomId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this room"));
        var userRole = userModuleApi.findById(userId).map(u -> u.role()).orElse(null);

        // LEADER can always create
        if (roomRole == RoomRole.LEADER) return;
        // TEACHER can create discussions in rooms they're a member of
        if (userRole == UserRole.TEACHER) return;
        // SUPERADMIN can always create
        if (userRole == UserRole.SUPERADMIN) return;

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
     * Check if user can manage (archive/delete) threads: LEADER, TEACHER, SUPERADMIN.
     */
    private void requireThreadManager(UUID roomId, UUID userId) {
        var roomRole = roomModuleApi.getUserRoleInRoom(userId, roomId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this room"));
        var userRole = userModuleApi.findById(userId).map(u -> u.role()).orElse(null);

        if (roomRole == RoomRole.LEADER) return;
        if (userRole == UserRole.TEACHER) return;
        if (userRole == UserRole.SUPERADMIN) return;

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
