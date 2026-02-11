package com.monteweb.room.internal.service;

import com.monteweb.room.DiscussionThreadCreatedEvent;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.room.RoomRole;
import com.monteweb.room.internal.model.DiscussionReply;
import com.monteweb.room.internal.model.DiscussionThread;
import com.monteweb.room.internal.model.ThreadStatus;
import com.monteweb.room.internal.repository.DiscussionReplyRepository;
import com.monteweb.room.internal.repository.DiscussionThreadRepository;
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
    private final UserModuleApi userModuleApi;
    private final ApplicationEventPublisher eventPublisher;

    public DiscussionThreadService(DiscussionThreadRepository threadRepository,
                                   DiscussionReplyRepository replyRepository,
                                   RoomModuleApi roomModuleApi,
                                   UserModuleApi userModuleApi,
                                   ApplicationEventPublisher eventPublisher) {
        this.threadRepository = threadRepository;
        this.replyRepository = replyRepository;
        this.roomModuleApi = roomModuleApi;
        this.userModuleApi = userModuleApi;
        this.eventPublisher = eventPublisher;
    }

    public Page<ThreadInfo> getThreads(UUID roomId, UUID userId, String statusFilter, Pageable pageable) {
        requireRoomMember(roomId, userId);
        Page<DiscussionThread> threads;
        if (statusFilter != null && !statusFilter.isBlank()) {
            threads = threadRepository.findByRoomIdAndStatusOrderByCreatedAtDesc(
                    roomId, ThreadStatus.valueOf(statusFilter.toUpperCase()), pageable);
        } else {
            threads = threadRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable);
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
    public ThreadInfo createThread(UUID roomId, UUID userId, String title, String content) {
        requireLeader(roomId, userId);

        var thread = new DiscussionThread();
        thread.setRoomId(roomId);
        thread.setCreatedBy(userId);
        thread.setTitle(title);
        thread.setContent(content);
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
        requireLeader(roomId, userId);
        var thread = findThread(threadId);
        if (!thread.getRoomId().equals(roomId)) {
            throw new ResourceNotFoundException("Thread", threadId);
        }
        thread.setStatus(ThreadStatus.ARCHIVED);
        return toThreadInfo(threadRepository.save(thread));
    }

    @Transactional
    public void deleteThread(UUID roomId, UUID threadId, UUID userId) {
        requireLeader(roomId, userId);
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

    private void requireLeader(UUID roomId, UUID userId) {
        var role = roomModuleApi.getUserRoleInRoom(userId, roomId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this room"));
        if (role != RoomRole.LEADER) {
            throw new ForbiddenException("Only room leaders can perform this action");
        }
    }

    private ThreadInfo toThreadInfo(DiscussionThread thread) {
        String authorName = userModuleApi.findById(thread.getCreatedBy())
                .map(u -> u.firstName() + " " + u.lastName())
                .orElse("Unknown");
        long replyCount = replyRepository.countByThreadId(thread.getId());

        return new ThreadInfo(
                thread.getId(), thread.getRoomId(), thread.getCreatedBy(), authorName,
                thread.getTitle(), thread.getContent(), thread.getStatus().name(),
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
            String title, String content, String status,
            long replyCount, java.time.Instant createdAt, java.time.Instant updatedAt
    ) {}

    public record ReplyInfo(
            UUID id, UUID threadId, UUID authorId, String authorName,
            String content, java.time.Instant createdAt
    ) {}
}
