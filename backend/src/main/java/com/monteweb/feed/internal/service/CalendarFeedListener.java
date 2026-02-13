package com.monteweb.feed.internal.service;

import com.monteweb.calendar.EventCancelledEvent;
import com.monteweb.calendar.EventDeletedEvent;
import com.monteweb.feed.FeedModuleApi;
import com.monteweb.feed.SourceType;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens to calendar events and creates feed posts.
 * - Cancel: system post visible to ALL users
 * - Delete: targeted post visible only to ATTENDING users
 */
@Component
public class CalendarFeedListener {

    private final FeedModuleApi feedModule;

    public CalendarFeedListener(FeedModuleApi feedModule) {
        this.feedModule = feedModule;
    }

    @Async
    @EventListener
    public void onEventCancelled(EventCancelledEvent event) {
        String title = "Termin abgesagt: " + event.eventTitle();
        String content = event.cancellerName() + " hat den Termin \"" + event.eventTitle() + "\" abgesagt.";

        feedModule.createSystemPost(title, content, SourceType.SCHOOL, null);
    }

    @Async
    @EventListener
    public void onEventDeleted(EventDeletedEvent event) {
        if (event.attendingUserIds() == null || event.attendingUserIds().isEmpty()) {
            return;
        }

        String title = "Termin entfernt: " + event.eventTitle();
        String content = "Der Termin \"" + event.eventTitle() + "\", dem Sie zugesagt hatten, wurde entfernt.";

        feedModule.createTargetedSystemPost(title, content, SourceType.SYSTEM, null, event.attendingUserIds());
    }
}
