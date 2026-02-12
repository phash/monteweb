package com.monteweb.fotobox.internal.service;

import com.monteweb.fotobox.*;
import com.monteweb.fotobox.internal.dto.*;
import com.monteweb.fotobox.internal.model.FotoboxImage;
import com.monteweb.fotobox.internal.model.FotoboxRoomSettings;
import com.monteweb.fotobox.internal.model.FotoboxThread;
import com.monteweb.fotobox.internal.repository.FotoboxImageRepository;
import com.monteweb.fotobox.internal.repository.FotoboxRoomSettingsRepository;
import com.monteweb.fotobox.internal.repository.FotoboxThreadRepository;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.room.RoomRole;
import com.monteweb.shared.exception.BadRequestException;
import com.monteweb.shared.exception.ForbiddenException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import com.monteweb.user.UserModuleApi;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "monteweb.modules", name = "fotobox.enabled", havingValue = "true")
@RequiredArgsConstructor
public class FotoboxService implements FotoboxModuleApi {

    private final FotoboxThreadRepository threadRepo;
    private final FotoboxImageRepository imageRepo;
    private final FotoboxRoomSettingsRepository settingsRepo;
    private final FotoboxPermissionService permissionService;
    private final FotoboxStorageService storageService;
    private final RoomModuleApi roomModule;
    private final UserModuleApi userModule;
    private final ApplicationEventPublisher eventPublisher;

    // --- Settings ---

    public FotoboxRoomSettingsResponse getSettings(UUID userId, UUID roomId) {
        permissionService.requireRoomMember(userId, roomId);
        var settings = permissionService.getSettingsOrDefault(roomId);
        return new FotoboxRoomSettingsResponse(
                settings.isEnabled(),
                settings.getDefaultPermission(),
                settings.getMaxImagesPerThread(),
                settings.getMaxFileSizeMb()
        );
    }

    @Transactional
    public FotoboxRoomSettingsResponse updateSettings(UUID userId, UUID roomId, UpdateSettingsRequest request) {
        if (!permissionService.isLeaderOrAdmin(userId, roomId)) {
            throw new ForbiddenException("Only room leaders can modify fotobox settings");
        }
        var settings = settingsRepo.findByRoomId(roomId).orElseGet(() -> {
            var s = new FotoboxRoomSettings();
            s.setRoomId(roomId);
            return s;
        });
        if (request.enabled() != null) settings.setEnabled(request.enabled());
        if (request.defaultPermission() != null) {
            // Validate permission value
            FotoboxPermissionLevel.valueOf(request.defaultPermission());
            settings.setDefaultPermission(request.defaultPermission());
        }
        if (request.maxImagesPerThread() != null) {
            settings.setMaxImagesPerThread(request.maxImagesPerThread() <= 0 ? null : request.maxImagesPerThread());
        }
        if (request.maxFileSizeMb() != null) settings.setMaxFileSizeMb(request.maxFileSizeMb());
        settingsRepo.save(settings);
        return new FotoboxRoomSettingsResponse(
                settings.isEnabled(),
                settings.getDefaultPermission(),
                settings.getMaxImagesPerThread(),
                settings.getMaxFileSizeMb()
        );
    }

    // --- Threads ---

    public List<FotoboxThreadInfo> getThreads(UUID userId, UUID roomId) {
        permissionService.requirePermission(userId, roomId, FotoboxPermissionLevel.VIEW_ONLY);
        var threads = threadRepo.findByRoomIdOrderByCreatedAtDesc(roomId);
        return threads.stream().map(this::toThreadInfo).toList();
    }

    public FotoboxThreadInfo getThread(UUID userId, UUID roomId, UUID threadId) {
        permissionService.requirePermission(userId, roomId, FotoboxPermissionLevel.VIEW_ONLY);
        var thread = threadRepo.findById(threadId)
                .orElseThrow(() -> new ResourceNotFoundException("FotoboxThread", threadId));
        if (!thread.getRoomId().equals(roomId)) {
            throw new ResourceNotFoundException("FotoboxThread", threadId);
        }
        return toThreadInfo(thread);
    }

    public List<FotoboxImageInfo> getThreadImages(UUID userId, UUID roomId, UUID threadId) {
        permissionService.requirePermission(userId, roomId, FotoboxPermissionLevel.VIEW_ONLY);
        var thread = threadRepo.findById(threadId)
                .orElseThrow(() -> new ResourceNotFoundException("FotoboxThread", threadId));
        if (!thread.getRoomId().equals(roomId)) {
            throw new ResourceNotFoundException("FotoboxThread", threadId);
        }
        var images = imageRepo.findByThreadIdOrderBySortOrderAscCreatedAtAsc(threadId);
        return images.stream().map(this::toImageInfo).toList();
    }

    @Transactional
    public FotoboxThreadInfo createThread(UUID userId, UUID roomId, CreateThreadRequest request) {
        permissionService.requirePermission(userId, roomId, FotoboxPermissionLevel.CREATE_THREADS);
        var thread = new FotoboxThread();
        thread.setRoomId(roomId);
        thread.setTitle(request.title());
        thread.setDescription(request.description());
        thread.setCreatedBy(userId);
        threadRepo.save(thread);

        eventPublisher.publishEvent(new FotoboxThreadCreatedEvent(
                thread.getId(), roomId, userId, thread.getTitle()
        ));

        return toThreadInfo(thread);
    }

    @Transactional
    public FotoboxThreadInfo updateThread(UUID userId, UUID roomId, UUID threadId, UpdateThreadRequest request) {
        var thread = threadRepo.findById(threadId)
                .orElseThrow(() -> new ResourceNotFoundException("FotoboxThread", threadId));
        if (!thread.getRoomId().equals(roomId)) {
            throw new ResourceNotFoundException("FotoboxThread", threadId);
        }
        if (!permissionService.isThreadOwnerOrLeader(userId, threadId)) {
            throw new ForbiddenException("Only thread creator or room leader can edit this thread");
        }
        if (request.title() != null) thread.setTitle(request.title());
        if (request.description() != null) thread.setDescription(request.description());
        threadRepo.save(thread);
        return toThreadInfo(thread);
    }

    @Transactional
    public void deleteThread(UUID userId, UUID roomId, UUID threadId) {
        var thread = threadRepo.findById(threadId)
                .orElseThrow(() -> new ResourceNotFoundException("FotoboxThread", threadId));
        if (!thread.getRoomId().equals(roomId)) {
            throw new ResourceNotFoundException("FotoboxThread", threadId);
        }
        if (!permissionService.isThreadOwnerOrLeader(userId, threadId)) {
            throw new ForbiddenException("Only thread creator or room leader can delete this thread");
        }
        // Delete all images from storage
        var images = imageRepo.findByThreadIdOrderBySortOrderAscCreatedAtAsc(threadId);
        for (var image : images) {
            storageService.delete(image.getStoragePath());
            storageService.delete(image.getThumbnailPath());
        }
        imageRepo.deleteAllByThreadId(threadId);
        threadRepo.delete(thread);
    }

    // --- Images ---

    @Transactional
    public List<FotoboxImageInfo> uploadImages(UUID userId, UUID roomId, UUID threadId,
                                                List<MultipartFile> files, String caption) {
        permissionService.requirePermission(userId, roomId, FotoboxPermissionLevel.POST_IMAGES);
        var thread = threadRepo.findById(threadId)
                .orElseThrow(() -> new ResourceNotFoundException("FotoboxThread", threadId));
        if (!thread.getRoomId().equals(roomId)) {
            throw new ResourceNotFoundException("FotoboxThread", threadId);
        }

        var settings = permissionService.getSettingsOrDefault(roomId);
        int currentCount = imageRepo.countByThreadId(threadId);
        if (settings.getMaxImagesPerThread() != null
                && currentCount + files.size() > settings.getMaxImagesPerThread()) {
            throw new BadRequestException("Maximum images per thread exceeded (max: "
                    + settings.getMaxImagesPerThread() + ")");
        }

        long maxSizeBytes = (long) settings.getMaxFileSizeMb() * 1024 * 1024;
        List<FotoboxImageInfo> result = new ArrayList<>();
        int sortOrder = currentCount;

        for (MultipartFile file : files) {
            if (file.getSize() > maxSizeBytes) {
                throw new BadRequestException("File too large: " + file.getOriginalFilename()
                        + " (max: " + settings.getMaxFileSizeMb() + " MB)");
            }

            String contentType = storageService.validateAndDetectContentType(file);
            String extension = FotoboxStorageService.extensionFromContentType(contentType);
            UUID imageId = UUID.randomUUID();

            String storagePath = storageService.uploadOriginal(
                    roomId, threadId, imageId, extension, file, contentType);
            String thumbnailPath = storageService.uploadThumbnail(
                    roomId, threadId, imageId, extension, file);

            var image = new FotoboxImage();
            image.setId(imageId);
            image.setThreadId(threadId);
            image.setUploadedBy(userId);
            image.setOriginalFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "image");
            image.setStoragePath(storagePath);
            image.setThumbnailPath(thumbnailPath);
            image.setFileSize(file.getSize());
            image.setContentType(contentType);
            image.setCaption(caption);
            image.setSortOrder(sortOrder++);
            imageRepo.save(image);

            // Set first image as cover if none set
            if (thread.getCoverImageId() == null) {
                thread.setCoverImageId(image.getId());
                threadRepo.save(thread);
            }

            result.add(toImageInfo(image));
        }

        return result;
    }

    @Transactional
    public FotoboxImageInfo updateImage(UUID userId, UUID imageId, UpdateImageRequest request) {
        var image = imageRepo.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("FotoboxImage", imageId));
        if (!permissionService.isImageOwnerOrLeader(userId, imageId)) {
            throw new ForbiddenException("Only image uploader or room leader can edit this image");
        }
        if (request.caption() != null) image.setCaption(request.caption());
        if (request.sortOrder() != null) image.setSortOrder(request.sortOrder());
        imageRepo.save(image);
        return toImageInfo(image);
    }

    @Transactional
    public void deleteImage(UUID userId, UUID imageId) {
        var image = imageRepo.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("FotoboxImage", imageId));
        if (!permissionService.isImageOwnerOrLeader(userId, imageId)) {
            throw new ForbiddenException("Only image uploader or room leader can delete this image");
        }
        storageService.delete(image.getStoragePath());
        storageService.delete(image.getThumbnailPath());

        // Update cover image if this was the cover
        var thread = threadRepo.findById(image.getThreadId()).orElse(null);
        if (thread != null && imageId.equals(thread.getCoverImageId())) {
            thread.setCoverImageId(null);
            threadRepo.save(thread);
        }

        imageRepo.delete(image);
    }

    /**
     * Returns the image entity for streaming, with permission check.
     */
    public FotoboxImage getImageForDownload(UUID userId, UUID imageId) {
        var image = imageRepo.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("FotoboxImage", imageId));
        var thread = threadRepo.findById(image.getThreadId())
                .orElseThrow(() -> new ResourceNotFoundException("FotoboxThread", image.getThreadId()));
        permissionService.requirePermission(userId, thread.getRoomId(), FotoboxPermissionLevel.VIEW_ONLY);
        return image;
    }

    // --- ModuleApi implementation ---

    @Override
    public List<FotoboxThreadInfo> getThreadsByRoom(UUID roomId) {
        var threads = threadRepo.findByRoomIdOrderByCreatedAtDesc(roomId);
        return threads.stream().map(this::toThreadInfo).toList();
    }

    @Override
    public boolean isFotoboxEnabledForRoom(UUID roomId) {
        return settingsRepo.findByRoomId(roomId)
                .map(FotoboxRoomSettings::isEnabled)
                .orElse(false);
    }

    // --- Mapping ---

    private FotoboxThreadInfo toThreadInfo(FotoboxThread thread) {
        int imageCount = imageRepo.countByThreadId(thread.getId());
        String createdByName = userModule.findById(thread.getCreatedBy())
                .map(u -> u.firstName() + " " + u.lastName())
                .orElse("Unknown");
        String coverThumbnailUrl = null;
        if (thread.getCoverImageId() != null) {
            coverThumbnailUrl = "/api/v1/fotobox/images/" + thread.getCoverImageId() + "/thumbnail";
        }
        return new FotoboxThreadInfo(
                thread.getId(),
                thread.getRoomId(),
                thread.getTitle(),
                thread.getDescription(),
                thread.getCoverImageId(),
                coverThumbnailUrl,
                imageCount,
                thread.getCreatedBy(),
                createdByName,
                thread.getCreatedAt()
        );
    }

    private FotoboxImageInfo toImageInfo(FotoboxImage image) {
        String uploadedByName = userModule.findById(image.getUploadedBy())
                .map(u -> u.firstName() + " " + u.lastName())
                .orElse("Unknown");
        return new FotoboxImageInfo(
                image.getId(),
                image.getThreadId(),
                image.getUploadedBy(),
                uploadedByName,
                image.getOriginalFilename(),
                "/api/v1/fotobox/images/" + image.getId(),
                "/api/v1/fotobox/images/" + image.getId() + "/thumbnail",
                image.getFileSize(),
                image.getContentType(),
                image.getWidth(),
                image.getHeight(),
                image.getCaption(),
                image.getSortOrder(),
                image.getCreatedAt()
        );
    }
}
