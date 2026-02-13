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

import com.monteweb.user.UserRole;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
            try {
                FotoboxPermissionLevel.valueOf(request.defaultPermission());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid permission level: " + request.defaultPermission()
                        + ". Allowed values: VIEW_ONLY, POST_IMAGES, CREATE_THREADS");
            }
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

        // Filter threads by audience based on user's role
        Set<String> allowedAudiences = getAllowedAudiences(userId, roomId);
        return threads.stream()
                .filter(t -> allowedAudiences.contains(t.getAudience()))
                .map(this::toThreadInfo)
                .toList();
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
        thread.setAudience(resolveAudience(request.audience(), userId));
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

    private static final int MAX_FILES_PER_UPLOAD = 20;

    @Transactional
    public List<FotoboxImageInfo> uploadImages(UUID userId, UUID roomId, UUID threadId,
                                                List<MultipartFile> files, String caption) {
        if (files.size() > MAX_FILES_PER_UPLOAD) {
            throw new BadRequestException("Too many files in a single upload. Maximum: " + MAX_FILES_PER_UPLOAD);
        }
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
                thread.getAudience(),
                thread.getCreatedBy(),
                createdByName,
                thread.getCreatedAt()
        );
    }

    /**
     * Resolves the audience for a new thread.
     * Parents always get PARENTS_ONLY. Teachers/Leaders/Admins can choose.
     */
    private String resolveAudience(String audience, UUID userId) {
        var userInfo = userModule.findById(userId).orElse(null);
        var userRole = userInfo != null ? userInfo.role() : null;

        // Parents always create with PARENTS_ONLY
        if (userRole == UserRole.PARENT) {
            return "PARENTS_ONLY";
        }

        // Teachers, leaders, admins can choose
        if (audience != null && !audience.isBlank()) {
            String upper = audience.toUpperCase();
            if (!Set.of("ALL", "PARENTS_ONLY", "STUDENTS_ONLY").contains(upper)) {
                throw new BadRequestException("Invalid audience value: " + audience);
            }
            return upper;
        }

        return "ALL";
    }

    /**
     * Determines which audience values a user is allowed to see in a room.
     * Same logic as FileService.getAllowedAudiences.
     */
    private Set<String> getAllowedAudiences(UUID userId, UUID roomId) {
        var roomRole = roomModule.getUserRoleInRoom(userId, roomId).orElse(null);
        var userInfo = userModule.findById(userId).orElse(null);
        var userRole = userInfo != null ? userInfo.role() : null;

        // Leaders, teachers, superadmins, section admins see everything
        if (roomRole == RoomRole.LEADER
                || userRole == UserRole.TEACHER
                || userRole == UserRole.SUPERADMIN
                || userRole == UserRole.SECTION_ADMIN) {
            return Set.of("ALL", "PARENTS_ONLY", "STUDENTS_ONLY");
        }

        // Parents see ALL + PARENTS_ONLY
        if (userRole == UserRole.PARENT) {
            return Set.of("ALL", "PARENTS_ONLY");
        }

        // Students see ALL + STUDENTS_ONLY
        if (userRole == UserRole.STUDENT) {
            return Set.of("ALL", "STUDENTS_ONLY");
        }

        // Others see ALL only
        return Set.of("ALL");
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
