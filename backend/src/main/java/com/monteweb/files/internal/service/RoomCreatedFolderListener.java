package com.monteweb.files.internal.service;

import com.monteweb.files.internal.model.RoomFolder;
import com.monteweb.files.internal.repository.RoomFolderRepository;
import com.monteweb.room.RoomCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "monteweb.modules.files", name = "enabled", havingValue = "true")
public class RoomCreatedFolderListener {

    private static final Logger log = LoggerFactory.getLogger(RoomCreatedFolderListener.class);

    private final RoomFolderRepository folderRepository;

    public RoomCreatedFolderListener(RoomFolderRepository folderRepository) {
        this.folderRepository = folderRepository;
    }

    @EventListener
    @Transactional
    public void onRoomCreated(RoomCreatedEvent event) {
        if (!"KLASSE".equals(event.roomType())) {
            return;
        }

        log.info("Creating default folders for KLASSE room {} ({})", event.roomName(), event.roomId());

        createFolder(event, "Eltern & Lehrer", "PARENTS_ONLY");
        createFolder(event, "Schüler & Lehrer", "STUDENTS_ONLY");
        createFolder(event, "Alle", "ALL");
    }

    private void createFolder(RoomCreatedEvent event, String name, String audience) {
        var folder = new RoomFolder();
        folder.setRoomId(event.roomId());
        folder.setName(name);
        folder.setAudience(audience);
        folder.setCreatedBy(event.createdBy());
        folderRepository.save(folder);
    }
}
