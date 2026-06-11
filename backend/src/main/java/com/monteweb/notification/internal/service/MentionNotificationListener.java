package com.monteweb.notification.internal.service;

import com.monteweb.feed.FeedCommentCreatedEvent;
import com.monteweb.feed.FeedModuleApi;
import com.monteweb.feed.FeedPostCreatedEvent;
import com.monteweb.feed.FeedPostInfo;
import com.monteweb.feed.SourceType;
import com.monteweb.messaging.MessageSentEvent;
import com.monteweb.notification.NotificationType;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.shared.util.MentionParser;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Listens for content creation events (posts, comments, messages) and sends
 * MENTION notifications to users who are mentioned via the @[userId:displayName] syntax.
 *
 * <p>Additionally, when a comment is added to a post, the post's author receives a
 * {@link NotificationType#COMMENT} notification (US-330), unless the commenter is the
 * author themselves or the author was already notified via a mention in the same comment.
 */
@Component
public class MentionNotificationListener {

    private final NotificationService notificationService;
    private final FeedModuleApi feedModuleApi;
    private final RoomModuleApi roomModuleApi;

    public MentionNotificationListener(NotificationService notificationService,
                                       FeedModuleApi feedModuleApi,
                                       RoomModuleApi roomModuleApi) {
        this.notificationService = notificationService;
        this.feedModuleApi = feedModuleApi;
        this.roomModuleApi = roomModuleApi;
    }

    @ApplicationModuleListener
    public void onFeedPostCreated(FeedPostCreatedEvent event) {
        Set<UUID> mentionedIds = MentionParser.extractMentionedUserIds(event.content());
        if (mentionedIds.isEmpty()) return;

        // Security: for ROOM-scoped posts, only members can see the post, so MENTION
        // notifications (which carry the post title/preview) must not be delivered to
        // non-members. Otherwise a member could embed @[arbitraryUserId:...] to leak a
        // private room post to an outsider and spam them. Broader scopes (SECTION/SCHOOL/
        // BOARD/SYSTEM) are visible to the relevant audience, so mentions stay allowed.
        boolean roomScoped = event.sourceType() == SourceType.ROOM && event.sourceId() != null;

        String link = event.sourceId() != null ? "/rooms/" + event.sourceId() : "/feed";

        for (UUID mentionedUserId : mentionedIds) {
            if (mentionedUserId.equals(event.authorId())) continue;
            if (roomScoped && !roomModuleApi.isUserInRoom(mentionedUserId, event.sourceId())) continue;

            notificationService.sendNotification(
                    mentionedUserId,
                    NotificationType.MENTION,
                    event.authorName() + " hat dich in einem Beitrag erwaehnt",
                    event.title() != null ? event.title() : "Neuer Beitrag",
                    link,
                    "FEED_POST",
                    event.postId()
            );
        }
    }

    @ApplicationModuleListener
    public void onFeedCommentCreated(FeedCommentCreatedEvent event) {
        Set<UUID> mentionedIds = MentionParser.extractMentionedUserIds(event.content());

        String link = "/feed";

        for (UUID mentionedUserId : mentionedIds) {
            if (mentionedUserId.equals(event.authorId())) continue;

            notificationService.sendNotification(
                    mentionedUserId,
                    NotificationType.MENTION,
                    event.authorName() + " hat dich in einem Kommentar erwaehnt",
                    truncate(event.content(), 100),
                    link,
                    "FEED_COMMENT",
                    event.commentId()
            );
        }

        // Notify the post author about the new comment (US-330: COMMENT notification),
        // unless they wrote the comment themselves or were already notified via a mention.
        FeedPostInfo post = feedModuleApi.findPostById(event.postId()).orElse(null);
        if (post == null) return;

        UUID postAuthorId = post.authorId();
        if (postAuthorId == null) return;
        if (postAuthorId.equals(event.authorId())) return;
        if (mentionedIds.contains(postAuthorId)) return;

        notificationService.sendNotification(
                postAuthorId,
                NotificationType.COMMENT,
                event.authorName() + " hat deinen Beitrag kommentiert",
                truncate(event.content(), 100),
                link,
                "FEED_COMMENT",
                event.commentId()
        );
    }

    @ApplicationModuleListener
    public void onMessageSent(MessageSentEvent event) {
        Set<UUID> mentionedIds = MentionParser.extractMentionedUserIds(event.fullContent());
        if (mentionedIds.isEmpty()) return;

        // Security: only deliver MENTION notifications (which carry a preview of the private
        // message body) to actual participants of the conversation. Otherwise a participant
        // could embed @[arbitraryUserId:...] to leak private message content to outsiders and
        // spam unsolicited notifications to any user by UUID.
        Set<UUID> participants = new HashSet<>();
        if (event.recipientIds() != null) {
            participants.addAll(event.recipientIds());
        }
        participants.add(event.senderId());

        String link = "/messages/" + event.conversationId();

        for (UUID mentionedUserId : mentionedIds) {
            if (mentionedUserId.equals(event.senderId())) continue;
            if (!participants.contains(mentionedUserId)) continue;

            notificationService.sendNotification(
                    mentionedUserId,
                    NotificationType.MENTION,
                    event.senderName() + " hat dich in einer Nachricht erwaehnt",
                    event.contentPreview(),
                    link,
                    "MESSAGE",
                    event.messageId()
            );
        }
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
