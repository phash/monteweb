package com.monteweb.feed.internal.service;

import com.monteweb.feed.*;
import com.monteweb.feed.internal.model.FeedPost;
import com.monteweb.feed.internal.model.FeedPostComment;
import com.monteweb.feed.internal.repository.FeedPostCommentRepository;
import com.monteweb.feed.internal.repository.FeedPostRepository;
import com.monteweb.room.RoomInfo;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.exception.ForbiddenException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class FeedService implements FeedModuleApi {

    private final FeedPostRepository postRepository;
    private final FeedPostCommentRepository commentRepository;
    private final UserModuleApi userModuleApi;
    private final RoomModuleApi roomModuleApi;
    private final ApplicationEventPublisher eventPublisher;

    public FeedService(FeedPostRepository postRepository,
                       FeedPostCommentRepository commentRepository,
                       UserModuleApi userModuleApi,
                       RoomModuleApi roomModuleApi,
                       ApplicationEventPublisher eventPublisher) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userModuleApi = userModuleApi;
        this.roomModuleApi = roomModuleApi;
        this.eventPublisher = eventPublisher;
    }

    // --- Public API (FeedModuleApi) ---

    @Override
    @Transactional
    public FeedPostInfo createSystemPost(String title, String content, SourceType sourceType, UUID sourceId) {
        var post = new FeedPost();
        post.setAuthorId(UUID.fromString("00000000-0000-0000-0000-000000000000")); // system user
        post.setTitle(title);
        post.setContent(content);
        post.setSourceType(sourceType);
        post.setSourceId(sourceId);
        post.setPinned(true);
        return toPostInfo(postRepository.save(post));
    }

    @Override
    @Transactional
    public FeedPostInfo createTargetedSystemPost(String title, String content, SourceType sourceType, UUID sourceId, List<UUID> targetUserIds) {
        var post = new FeedPost();
        post.setAuthorId(UUID.fromString("00000000-0000-0000-0000-000000000000")); // system user
        post.setTitle(title);
        post.setContent(content);
        post.setSourceType(sourceType);
        post.setSourceId(sourceId);
        post.setPinned(true);
        if (targetUserIds != null && !targetUserIds.isEmpty()) {
            post.setTargetUserIds(targetUserIds.toArray(new UUID[0]));
        }
        return toPostInfo(postRepository.save(post));
    }

    @Override
    public Optional<FeedPostInfo> findPostById(UUID postId) {
        return postRepository.findById(postId).map(this::toPostInfo);
    }

    @Override
    public Page<FeedPostInfo> getPersonalFeed(UUID userId, Pageable pageable) {
        var userInfo = userModuleApi.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Gather the user's room IDs and section IDs
        var rooms = roomModuleApi.findByUserId(userId);
        var roomIds = rooms.stream().map(RoomInfo::id).collect(Collectors.toSet());
        var sectionIds = rooms.stream()
                .map(RoomInfo::sectionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Remove muted rooms from the feed
        var mutedRoomIds = roomModuleApi.getMutedRoomIds(userId);
        if (mutedRoomIds != null && !mutedRoomIds.isEmpty()) {
            roomIds.removeAll(mutedRoomIds);
        }

        // Ensure non-empty collections for the IN clause
        if (roomIds.isEmpty()) roomIds.add(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        if (sectionIds.isEmpty()) sectionIds.add(UUID.fromString("00000000-0000-0000-0000-000000000000"));

        boolean isParent = userInfo.role() == UserRole.PARENT;

        return postRepository.findPersonalFeed(roomIds, sectionIds, isParent, userId, pageable)
                .map(this::toPostInfo);
    }

    // --- Internal service methods ---

    @Transactional
    public FeedPostInfo createPost(UUID authorId, String title, String content,
                                   SourceType sourceType, UUID sourceId, boolean parentOnly) {
        // Validate author can post to this source
        if (sourceType == SourceType.ROOM && sourceId != null) {
            if (!roomModuleApi.isUserInRoom(authorId, sourceId)) {
                throw new ForbiddenException("Not a member of this room");
            }
        }
        if (sourceType == SourceType.SCHOOL || sourceType == SourceType.SECTION) {
            var user = userModuleApi.findById(authorId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", authorId));
            boolean isStaff = user.role() == UserRole.SUPERADMIN || user.role() == UserRole.SECTION_ADMIN || user.role() == UserRole.TEACHER;
            boolean isElternbeirat = user.specialRoles() != null && (user.specialRoles().contains("ELTERNBEIRAT")
                    || (sourceId != null && user.specialRoles().contains("ELTERNBEIRAT:" + sourceId)));
            if (!isStaff && !isElternbeirat) {
                throw new ForbiddenException("Only staff or Elternbeirat can create school-wide or section posts");
            }
        }

        var post = new FeedPost();
        post.setAuthorId(authorId);
        post.setTitle(title);
        post.setContent(content);
        post.setSourceType(sourceType);
        post.setSourceId(sourceId);
        post.setParentOnly(parentOnly);

        post = postRepository.save(post);

        String authorName = userModuleApi.findById(authorId).map(UserInfo::displayName).orElse("Unknown");
        eventPublisher.publishEvent(new FeedPostCreatedEvent(
                post.getId(), authorId, authorName, title, sourceType, sourceId
        ));

        return toPostInfo(post);
    }

    @Transactional
    public FeedPostInfo updatePost(UUID postId, UUID userId, String title, String content, Boolean parentOnly) {
        var post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("FeedPost", postId));
        if (!post.getAuthorId().equals(userId)) {
            throw new ForbiddenException("Only the author can edit this post");
        }
        if (title != null) post.setTitle(title);
        if (content != null) post.setContent(content);
        if (parentOnly != null) post.setParentOnly(parentOnly);
        return toPostInfo(postRepository.save(post));
    }

    @Transactional
    public void deletePost(UUID postId, UUID userId) {
        var post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("FeedPost", postId));
        var user = userModuleApi.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!post.getAuthorId().equals(userId) && user.role() != UserRole.SUPERADMIN) {
            throw new ForbiddenException("Only the author or admin can delete this post");
        }
        postRepository.delete(post);
    }

    @Transactional
    public FeedPostInfo togglePin(UUID postId, UUID userId) {
        var post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("FeedPost", postId));
        // Only room leaders or admins can pin
        post.setPinned(!post.isPinned());
        return toPostInfo(postRepository.save(post));
    }

    public Page<com.monteweb.feed.internal.dto.CommentResponse> getComments(UUID postId, Pageable pageable) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId, pageable)
                .map(this::toCommentResponse);
    }

    @Transactional
    public com.monteweb.feed.internal.dto.CommentResponse addComment(UUID postId, UUID authorId, String content) {
        var post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("FeedPost", postId));
        var comment = new FeedPostComment();
        comment.setPost(post);
        comment.setAuthorId(authorId);
        comment.setContent(content);
        comment = commentRepository.save(comment);
        return toCommentResponse(comment);
    }

    public Page<FeedPostInfo> getPostsBySource(SourceType sourceType, UUID sourceId, Pageable pageable) {
        return postRepository
                .findBySourceTypeAndSourceIdOrderByPinnedDescPublishedAtDesc(sourceType, sourceId, pageable)
                .map(this::toPostInfo);
    }

    public List<FeedPostInfo> getActiveSystemBanners() {
        return postRepository.findActiveSystemBanners().stream()
                .map(this::toPostInfo)
                .toList();
    }

    private FeedPostInfo toPostInfo(FeedPost post) {
        String authorName = userModuleApi.findById(post.getAuthorId())
                .map(UserInfo::displayName)
                .orElse("System");

        String sourceName = resolveSourceName(post.getSourceType(), post.getSourceId());

        var attachments = post.getAttachments().stream()
                .map(a -> new FeedPostInfo.AttachmentInfo(
                        a.getId(), a.getFileName(), a.getFileUrl(), a.getFileType(),
                        a.getFileSize() != null ? a.getFileSize() : 0
                ))
                .toList();

        return new FeedPostInfo(
                post.getId(),
                post.getAuthorId(),
                authorName,
                post.getTitle(),
                post.getContent(),
                post.getSourceType(),
                post.getSourceId(),
                sourceName,
                post.isPinned(),
                post.isParentOnly(),
                post.getComments().size(),
                attachments,
                post.getPublishedAt(),
                post.getCreatedAt()
        );
    }

    private com.monteweb.feed.internal.dto.CommentResponse toCommentResponse(FeedPostComment c) {
        String authorName = userModuleApi.findById(c.getAuthorId())
                .map(UserInfo::displayName)
                .orElse("Unknown");
        return new com.monteweb.feed.internal.dto.CommentResponse(
                c.getId(), c.getPost().getId(), c.getAuthorId(), authorName, c.getContent(), c.getCreatedAt()
        );
    }

    private String resolveSourceName(SourceType type, UUID sourceId) {
        if (sourceId == null) return null;
        return switch (type) {
            case ROOM -> roomModuleApi.findById(sourceId).map(RoomInfo::name).orElse(null);
            case SECTION, SCHOOL, BOARD, SYSTEM -> null;
        };
    }

    /**
     * DSGVO: Clean up all feed data for a deleted user.
     */
    @Transactional
    public void cleanupUserData(UUID userId) {
        commentRepository.deleteAll(commentRepository.findByAuthorId(userId));
        postRepository.deleteAll(postRepository.findByAuthorId(userId));
    }

    /**
     * DSGVO: Export all feed data for a user.
     */
    @Override
    public Map<String, Object> exportUserData(UUID userId) {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        var posts = postRepository.findByAuthorId(userId);
        data.put("posts", posts.stream().map(p -> Map.of(
                "id", p.getId(),
                "title", p.getTitle() != null ? p.getTitle() : "",
                "content", p.getContent() != null ? p.getContent() : "",
                "createdAt", p.getCreatedAt()
        )).toList());
        var comments = commentRepository.findByAuthorId(userId);
        data.put("comments", comments.stream().map(c -> Map.of(
                "id", c.getId(),
                "content", c.getContent(),
                "createdAt", c.getCreatedAt()
        )).toList());
        return data;
    }
}
