package com.monteweb.notification.internal.service;

import com.monteweb.feed.FeedCommentCreatedEvent;
import com.monteweb.feed.FeedPostCreatedEvent;
import com.monteweb.messaging.MessageSentEvent;
import com.monteweb.notification.NotificationType;
import com.monteweb.shared.util.MentionParser;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Listens for content creation events (posts, comments, messages) and sends
 * MENTION notifications to users who are mentioned via the @[userId:displayName] syntax.
 */
@Component
public class MentionNotificationListener {

    private final NotificationService notificationService;

    public MentionNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @ApplicationModuleListener
    public void onFeedPostCreated(FeedPostCreatedEvent event) {
        Set<UUID> mentionedIds = MentionParser.extractMentionedUserIds(event.content());
        if (mentionedIds.isEmpty()) return;

        String link = event.sourceId() != null ? "/rooms/" + event.sourceId() : "/feed";

        for (UUID mentionedUserId : mentionedIds) {
            if (mentionedUserId.equals(event.authorId())) continue;

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
        if (mentionedIds.isEmpty()) return;

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
    }

    @ApplicationModuleListener
    public void onMessageSent(MessageSentEvent event) {
        Set<UUID> mentionedIds = MentionParser.extractMentionedUserIds(event.fullContent());
        if (mentionedIds.isEmpty()) return;

        String link = "/messages/" + event.conversationId();

        for (UUID mentionedUserId : mentionedIds) {
            if (mentionedUserId.equals(event.senderId())) continue;

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
