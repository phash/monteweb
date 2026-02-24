package com.monteweb.feed.internal.service;

import com.monteweb.feed.*;
import com.monteweb.feed.internal.dto.CreatePollRequest;
import com.monteweb.feed.internal.model.*;
import com.monteweb.feed.internal.repository.*;
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

    private static final Set<String> ALLOWED_EMOJIS = Set.of("👍", "👎", "❤️", "😂", "😢");

    private final FeedPostRepository postRepository;
    private final FeedPostCommentRepository commentRepository;
    private final FeedReactionRepository reactionRepository;
    private final FeedPollRepository pollRepository;
    private final FeedPollVoteRepository pollVoteRepository;
    private final UserModuleApi userModuleApi;
    private final RoomModuleApi roomModuleApi;
    private final ApplicationEventPublisher eventPublisher;

    public FeedService(FeedPostRepository postRepository,
                       FeedPostCommentRepository commentRepository,
                       FeedReactionRepository reactionRepository,
                       FeedPollRepository pollRepository,
                       FeedPollVoteRepository pollVoteRepository,
                       UserModuleApi userModuleApi,
                       RoomModuleApi roomModuleApi,
                       ApplicationEventPublisher eventPublisher) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.reactionRepository = reactionRepository;
        this.pollRepository = pollRepository;
        this.pollVoteRepository = pollVoteRepository;
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

    @Override
    public List<FeedPostInfo> searchPosts(String query, int limit, UUID userId) {
        return postRepository.searchPosts(query, userId, limit).stream()
                .map(this::toPostInfo)
                .toList();
    }

    // --- Internal service methods ---

    @Transactional
    public FeedPostInfo createPost(UUID authorId, String title, String content,
                                   SourceType sourceType, UUID sourceId, boolean parentOnly) {
        return createPost(authorId, title, content, sourceType, sourceId, parentOnly, null);
    }

    @Transactional
    public FeedPostInfo createPost(UUID authorId, String title, String content,
                                   SourceType sourceType, UUID sourceId, boolean parentOnly,
                                   CreatePollRequest pollRequest) {
        // Validate: must have content or poll
        if ((content == null || content.isBlank()) && pollRequest == null) {
            throw new BusinessException("Post must have content or a poll");
        }

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
        post.setContent(content != null && !content.isBlank() ? content : null);
        post.setSourceType(sourceType);
        post.setSourceId(sourceId);
        post.setParentOnly(parentOnly);

        post = postRepository.save(post);

        // Create poll if requested
        if (pollRequest != null) {
            createFeedPoll(post.getId(), pollRequest);
        }

        String authorName = userModuleApi.findById(authorId).map(UserInfo::displayName).orElse("Unknown");
        eventPublisher.publishEvent(new FeedPostCreatedEvent(
                post.getId(), authorId, authorName, title,
                content != null ? content : pollRequest.question(),
                sourceType, sourceId
        ));

        return toPostInfo(post, authorId);
    }

    private void createFeedPoll(UUID postId, CreatePollRequest request) {
        var poll = new FeedPoll();
        poll.setPostId(postId);
        poll.setQuestion(request.question());
        poll.setMultiple(request.multiple());
        poll.setClosesAt(request.closesAt());
        poll = pollRepository.save(poll);

        for (int i = 0; i < request.options().size(); i++) {
            var option = new FeedPollOption();
            option.setPoll(poll);
            option.setLabel(request.options().get(i));
            option.setPosition(i);
            poll.getOptions().add(option);
        }
        pollRepository.save(poll);
    }

    // --- Poll voting ---

    @Transactional
    public PollInfo voteFeedPoll(UUID postId, UUID userId, List<UUID> optionIds) {
        var poll = pollRepository.findByPostId(postId)
                .orElseThrow(() -> new ResourceNotFoundException("FeedPoll", postId));

        if (poll.getClosesAt() != null && Instant.now().isAfter(poll.getClosesAt())) {
            throw new BusinessException("Poll is closed");
        }

        if (!poll.isMultiple() && optionIds.size() > 1) {
            throw new BusinessException("Only one option can be selected");
        }

        // Validate options belong to this poll
        var validOptionIds = poll.getOptions().stream().map(FeedPollOption::getId).collect(Collectors.toSet());
        for (UUID optionId : optionIds) {
            if (!validOptionIds.contains(optionId)) {
                throw new BusinessException("Invalid option");
            }
        }

        // Remove existing votes
        pollVoteRepository.deleteByOptionIdInAndUserId(validOptionIds, userId);
        pollVoteRepository.flush();

        // Add new votes
        for (UUID optionId : optionIds) {
            var vote = new FeedPollVote();
            vote.setOptionId(optionId);
            vote.setUserId(userId);
            pollVoteRepository.save(vote);
        }

        return toPollInfo(poll, userId);
    }

    @Transactional
    public PollInfo closeFeedPoll(UUID postId, UUID userId) {
        var post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("FeedPost", postId));

        // Only author or admin can close
        var user = userModuleApi.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!post.getAuthorId().equals(userId) && user.role() != UserRole.SUPERADMIN) {
            throw new ForbiddenException("Only the author or admin can close this poll");
        }

        var poll = pollRepository.findByPostId(postId)
                .orElseThrow(() -> new ResourceNotFoundException("FeedPoll", postId));

        poll.setClosesAt(Instant.now());
        pollRepository.save(poll);

        return toPollInfo(poll, userId);
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

        String authorName = userModuleApi.findById(authorId).map(UserInfo::displayName).orElse("Unknown");
        eventPublisher.publishEvent(new FeedCommentCreatedEvent(
                comment.getId(), postId, authorId, authorName, content
        ));

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

    // --- Reactions ---

    @Transactional
    public void togglePostReaction(UUID postId, UUID userId, String emoji) {
        validateEmoji(emoji);
        postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("FeedPost", postId));
        var existing = reactionRepository.findByPostIdAndUserIdAndEmoji(postId, userId, emoji);
        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
        } else {
            var reaction = new FeedReaction();
            reaction.setPostId(postId);
            reaction.setUserId(userId);
            reaction.setEmoji(emoji);
            reactionRepository.save(reaction);
        }
    }

    @Transactional
    public void toggleCommentReaction(UUID commentId, UUID userId, String emoji) {
        validateEmoji(emoji);
        commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("FeedPostComment", commentId));
        var existing = reactionRepository.findByCommentIdAndUserIdAndEmoji(commentId, userId, emoji);
        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
        } else {
            var reaction = new FeedReaction();
            reaction.setCommentId(commentId);
            reaction.setUserId(userId);
            reaction.setEmoji(emoji);
            reactionRepository.save(reaction);
        }
    }

    public List<FeedPostInfo.ReactionSummary> getPostReactions(UUID postId, UUID currentUserId) {
        return buildReactionSummaries(reactionRepository.findByPostId(postId), currentUserId);
    }

    public List<com.monteweb.feed.internal.dto.CommentResponse.ReactionSummary> getCommentReactions(UUID commentId, UUID currentUserId) {
        var reactions = reactionRepository.findByCommentId(commentId);
        return reactions.stream()
                .collect(Collectors.groupingBy(FeedReaction::getEmoji))
                .entrySet().stream()
                .map(e -> new com.monteweb.feed.internal.dto.CommentResponse.ReactionSummary(
                        e.getKey(),
                        e.getValue().size(),
                        e.getValue().stream().anyMatch(r -> r.getUserId().equals(currentUserId))
                ))
                .sorted(Comparator.comparingLong(com.monteweb.feed.internal.dto.CommentResponse.ReactionSummary::count).reversed())
                .toList();
    }

    private List<FeedPostInfo.ReactionSummary> buildReactionSummaries(List<FeedReaction> reactions, UUID currentUserId) {
        return reactions.stream()
                .collect(Collectors.groupingBy(FeedReaction::getEmoji))
                .entrySet().stream()
                .map(e -> new FeedPostInfo.ReactionSummary(
                        e.getKey(),
                        e.getValue().size(),
                        currentUserId != null && e.getValue().stream().anyMatch(r -> r.getUserId().equals(currentUserId))
                ))
                .sorted(Comparator.comparingLong(FeedPostInfo.ReactionSummary::count).reversed())
                .toList();
    }

    private void validateEmoji(String emoji) {
        if (!ALLOWED_EMOJIS.contains(emoji)) {
            throw new BusinessException("Invalid emoji. Allowed: " + ALLOWED_EMOJIS);
        }
    }

    private FeedPostInfo toPostInfo(FeedPost post) {
        return toPostInfo(post, null);
    }

    private FeedPostInfo toPostInfo(FeedPost post, UUID currentUserId) {
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

        PollInfo pollInfo = pollRepository.findByPostId(post.getId())
                .map(poll -> toPollInfo(poll, currentUserId))
                .orElse(null);

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
                List.of(),
                pollInfo,
                post.getPublishedAt(),
                post.getCreatedAt()
        );
    }

    private PollInfo toPollInfo(FeedPoll poll, UUID currentUserId) {
        var allOptionIds = poll.getOptions().stream().map(FeedPollOption::getId).toList();
        var allVotes = pollVoteRepository.findByOptionIdIn(allOptionIds);
        var userVotedOptionIds = currentUserId != null
                ? allVotes.stream()
                    .filter(v -> v.getUserId().equals(currentUserId))
                    .map(FeedPollVote::getOptionId)
                    .collect(Collectors.toSet())
                : Set.<UUID>of();

        var voteCounts = allVotes.stream()
                .collect(Collectors.groupingBy(FeedPollVote::getOptionId, Collectors.counting()));

        boolean closed = poll.getClosesAt() != null && Instant.now().isAfter(poll.getClosesAt());

        var options = poll.getOptions().stream()
                .map(o -> new PollInfo.OptionInfo(
                        o.getId(),
                        o.getLabel(),
                        voteCounts.getOrDefault(o.getId(), 0L).intValue(),
                        userVotedOptionIds.contains(o.getId())
                ))
                .toList();

        return new PollInfo(
                poll.getId(),
                poll.getQuestion(),
                poll.isMultiple(),
                closed,
                allVotes.size(),
                options,
                poll.getClosesAt()
        );
    }

    private com.monteweb.feed.internal.dto.CommentResponse toCommentResponse(FeedPostComment c) {
        String authorName = userModuleApi.findById(c.getAuthorId())
                .map(UserInfo::displayName)
                .orElse("Unknown");
        return new com.monteweb.feed.internal.dto.CommentResponse(
                c.getId(), c.getPost().getId(), c.getAuthorId(), authorName, c.getContent(), List.of(), c.getCreatedAt()
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
