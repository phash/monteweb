package com.monteweb.tasks.internal.service;

import com.monteweb.user.UserDeletionExecutedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DSGVO: Anonymizes task data when a user account is deleted.
 */
@Component
@ConditionalOnProperty(prefix = "monteweb.modules.tasks", name = "enabled", havingValue = "true")
public class TasksDeletionListener {

    private static final Logger log = LoggerFactory.getLogger(TasksDeletionListener.class);

    private final TaskService taskService;

    public TasksDeletionListener(TaskService taskService) {
        this.taskService = taskService;
    }

    @Async
    @EventListener
    @Transactional
    public void onUserDeletion(UserDeletionExecutedEvent event) {
        log.info("Anonymizing tasks for deleted user {}", event.userId());
        taskService.cleanupUserData(event.userId());
    }
}
