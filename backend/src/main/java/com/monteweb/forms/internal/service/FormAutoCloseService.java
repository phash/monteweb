package com.monteweb.forms.internal.service;

import com.monteweb.forms.*;
import com.monteweb.forms.internal.repository.FormRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@EnableScheduling
@ConditionalOnProperty(prefix = "monteweb.modules", name = "forms.enabled", havingValue = "true")
public class FormAutoCloseService {

    private static final Logger log = LoggerFactory.getLogger(FormAutoCloseService.class);

    private final FormRepository formRepository;
    private final ApplicationEventPublisher eventPublisher;

    public FormAutoCloseService(FormRepository formRepository, ApplicationEventPublisher eventPublisher) {
        this.formRepository = formRepository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(cron = "0 15 0 * * *")
    @Transactional
    public void autoCloseExpiredForms() {
        var expiredForms = formRepository.findByStatusAndDeadlineBefore(FormStatus.PUBLISHED, LocalDate.now());
        for (var form : expiredForms) {
            form.setStatus(FormStatus.CLOSED);
            form.setClosedAt(Instant.now());
            formRepository.save(form);

            eventPublisher.publishEvent(new FormClosedEvent(
                form.getId(), form.getTitle(), form.getScope(), form.getScopeId(),
                form.getSectionIds() != null ? List.of(form.getSectionIds()) : List.of()
            ));
        }
        if (!expiredForms.isEmpty()) {
            log.info("Auto-closed {} expired forms", expiredForms.size());
        }
    }
}
