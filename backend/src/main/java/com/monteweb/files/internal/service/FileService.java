package com.monteweb.files.internal.service;

import com.monteweb.files.FileInfo;
import com.monteweb.files.FilesModuleApi;
import com.monteweb.files.FolderInfo;
import com.monteweb.files.internal.model.RoomFile;
import com.monteweb.files.internal.model.RoomFolder;
import com.monteweb.files.internal.repository.RoomFileRepository;
import com.monteweb.files.internal.repository.RoomFolderRepository;
import com.monteweb.files.FileDeletedEvent;
import com.monteweb.files.FileUploadedEvent;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.room.RoomRole;
import com.monteweb.user.UserRole;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.exception.ForbiddenException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import com.monteweb.user.UserModuleApi;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.monteweb.shared.util.ClamAvService;
import com.monteweb.shared.util.FileValidationUtils;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
@ConditionalOnProperty(prefix = "monteweb.modules.files", name = "enabled", havingValue = "true")
public class FileService implements FilesModuleApi {

    private final RoomFileRepository fileRepository;
    private final RoomFolderRepository folderRepository;
    private final FileStorageService storageService;
    private final RoomModuleApi roomModuleApi;
    private final UserModuleApi userModuleApi;
    private final ApplicationEventPublisher eventPublisher;
    private final ClamAvService clamAvService;

    public FileService(RoomFileRepository fileRepository,
                       RoomFolderRepository folderRepository,
                       FileStorageService storageService,
                       RoomModuleApi roomModuleApi,
                       UserModuleApi userModuleApi,
                       ApplicationEventPublisher eventPublisher,
                       ClamAvService clamAvService) {
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
        this.storageService = storageService;
        this.roomModuleApi = roomModuleApi;
        this.userModuleApi = userModuleApi;
        this.eventPublisher = eventPublisher;
        this.clamAvService = clamAvService;
    }

    // ---- Public API (FilesModuleApi) ----

    @Override
    @Transactional(readOnly = true)
    public List<FileInfo> findByRoom(UUID roomId) {
        return fileRepository.findByRoomIdOrderByCreatedAtDesc(roomId).stream()
                .map(this::toFileInfo)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FolderInfo> findFoldersByRoom(UUID roomId) {
        return folderRepository.findByRoomIdOrderByNameAsc(roomId).stream()
                .map(this::toFolderInfo)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long getStorageUsedByRoom(UUID roomId) {
        return fileRepository.sumFileSizeByRoomId(roomId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileInfo> findAllFiles() {
        return fileRepository.findAll().stream()
                .map(this::toFileInfo)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public String getStoragePath(UUID fileId) {
        return fileRepository.findById(fileId)
                .map(RoomFile::getStoragePath)
                .orElse(null);
    }

    // ---- File operations ----

    @Transactional(readOnly = true)
    public List<FileInfo> listFiles(UUID roomId, UUID folderId, UUID userId) {
        requireRoomMembership(userId, roomId);

        List<RoomFile> files;
        String folderAudience = null;
        if (folderId == null) {
            files = fileRepository.findByRoomIdAndFolderIdIsNullOrderByCreatedAtDesc(roomId);
        } else {
            files = fileRepository.findByRoomIdAndFolderIdOrderByCreatedAtDesc(roomId, folderId);
            folderAudience = folderRepository.findById(folderId)
                    .filter(f -> f.getRoomId().equals(roomId))
                    .map(RoomFolder::getAudience)
                    .orElse(null);
        }

        // Filter files by their EFFECTIVE audience (own audience inheriting the folder's
        // visibility, US-143) so a broad file inside a restrictive folder is hidden from
        // users who may not see the folder.
        Set<String> allowedAudiences = getAllowedAudiences(userId, roomId);
        final String inheritedAudience = folderAudience;
        return files.stream()
                .filter(f -> allowedAudiences.contains(effectiveAudience(inheritedAudience, f.getAudience())))
                .map(this::toFileInfo)
                .toList();
    }

    private static final long MAX_FILE_SIZE = 100L * 1024 * 1024; // 100 MB

    public FileInfo uploadFile(UUID roomId, UUID folderId, UUID userId, MultipartFile file, String audience) {
        requireRoomMembership(userId, roomId);

        if (file.isEmpty()) {
            throw new BusinessException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("File too large. Maximum size is 100 MB.");
        }

        String folderAudience = null;
        if (folderId != null) {
            var parentFolder = folderRepository.findById(folderId)
                    .filter(f -> f.getRoomId().equals(roomId))
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", folderId));
            folderAudience = parentFolder.getAudience();
        }

        // Validate audience value
        String resolvedAudience = "ALL";
        if (audience != null && !audience.isBlank()) {
            if (!Set.of("ALL", "PARENTS_ONLY", "STUDENTS_ONLY").contains(audience.toUpperCase())) {
                throw new BusinessException("Invalid audience value: " + audience);
            }
            resolvedAudience = audience.toUpperCase();
        }

        // Files inherit the visibility of their containing folder (US-143): a file may
        // never be broader than the folder it lives in. A restrictive (non-ALL) folder
        // forces its audience onto the file, so e.g. an "ALL" file placed inside a
        // PARENTS_ONLY folder is stored — and later served — as PARENTS_ONLY.
        resolvedAudience = effectiveAudience(folderAudience, resolvedAudience);

        // Detect actual content type via magic bytes instead of trusting client header
        String detectedContentType = FileValidationUtils.detectContentType(file);

        // Virus scan before storing (no-op unless the 'clamav' module toggle is enabled).
        try {
            ClamAvService.ScanResult scan = clamAvService.scan(file.getBytes());
            if (!scan.isClean()) {
                throw new BusinessException("File rejected: malware detected (" + scan.virusName() + ")");
            }
        } catch (java.io.IOException e) {
            throw new BusinessException("Could not read the uploaded file for virus scanning");
        }

        String storedName = UUID.randomUUID() + "_" + sanitizeFileName(file.getOriginalFilename());
        String storagePath = storageService.upload(roomId, folderId, storedName, file, detectedContentType);

        var roomFile = new RoomFile();
        roomFile.setRoomId(roomId);
        roomFile.setFolderId(folderId);
        roomFile.setOriginalName(file.getOriginalFilename());
        roomFile.setStoredName(storedName);
        roomFile.setContentType(detectedContentType);
        roomFile.setFileSize(file.getSize());
        roomFile.setStoragePath(storagePath);
        roomFile.setUploadedBy(userId);
        roomFile.setAudience(resolvedAudience);

        var saved = fileRepository.save(roomFile);
        eventPublisher.publishEvent(new FileUploadedEvent(
                saved.getId(), roomId, saved.getOriginalName(),
                saved.getContentType(), saved.getFileSize(),
                saved.getStoragePath(), userId));
        return toFileInfo(saved);
    }

    @Transactional(readOnly = true)
    public InputStream downloadFile(UUID roomId, UUID fileId, UUID userId) {
        requireRoomMembership(userId, roomId);

        var roomFile = fileRepository.findById(fileId)
                .filter(f -> f.getRoomId().equals(roomId))
                .orElseThrow(() -> new ResourceNotFoundException("File", fileId));

        requireAudienceAccess(userId, roomId, effectiveAudienceForFile(roomFile));
        return storageService.download(roomFile.getStoragePath());
    }

    @Transactional(readOnly = true)
    public RoomFile getFileMetadata(UUID roomId, UUID fileId, UUID userId) {
        requireRoomMembership(userId, roomId);
        var roomFile = fileRepository.findById(fileId)
                .filter(f -> f.getRoomId().equals(roomId))
                .orElseThrow(() -> new ResourceNotFoundException("File", fileId));
        requireAudienceAccess(userId, roomId, effectiveAudienceForFile(roomFile));
        return roomFile;
    }

    public void deleteFile(UUID roomId, UUID fileId, UUID userId) {
        var roomFile = fileRepository.findById(fileId)
                .filter(f -> f.getRoomId().equals(roomId))
                .orElseThrow(() -> new ResourceNotFoundException("File", fileId));

        // Only uploader, room leaders, or admins (SUPERADMIN / section-scoped SECTION_ADMIN) can delete
        if (!roomFile.getUploadedBy().equals(userId) && !hasAdminRoleForRoom(userId, roomId)) {
            var role = roomModuleApi.getUserRoleInRoom(userId, roomId);
            if (role.isEmpty() || !"LEADER".equals(role.get().name())) {
                throw new ForbiddenException("Only the uploader or room leaders can delete files");
            }
        }

        storageService.delete(roomFile.getStoragePath());
        UUID deletedFileId = roomFile.getId();
        fileRepository.delete(roomFile);
        eventPublisher.publishEvent(new FileDeletedEvent(deletedFileId));
    }

    // ---- Folder operations ----

    @Transactional(readOnly = true)
    public List<FolderInfo> listFolders(UUID roomId, UUID parentId, UUID userId) {
        requireRoomMembership(userId, roomId);

        List<RoomFolder> folders;
        if (parentId == null) {
            folders = folderRepository.findByRoomIdAndParentIdIsNullOrderByNameAsc(roomId);
        } else {
            folders = folderRepository.findByRoomIdAndParentIdOrderByNameAsc(roomId, parentId);
        }

        // Filter folders by audience based on user's role
        Set<String> allowedAudiences = getAllowedAudiences(userId, roomId);
        return folders.stream()
                .filter(f -> allowedAudiences.contains(f.getAudience()))
                .map(this::toFolderInfo)
                .toList();
    }

    public FolderInfo createFolder(UUID roomId, UUID parentId, String name, String audience, UUID userId) {
        requireRoomMembership(userId, roomId);

        if (folderRepository.existsByRoomIdAndParentIdAndName(roomId, parentId, name)) {
            throw new BusinessException("A folder with this name already exists in this location");
        }

        if (parentId != null) {
            folderRepository.findById(parentId)
                    .filter(f -> f.getRoomId().equals(roomId))
                    .orElseThrow(() -> new ResourceNotFoundException("Parent folder", parentId));
        }

        // Resolve audience: Parents always get PARENTS_ONLY, others can choose
        String resolvedAudience = resolveAudience(audience, userId);

        var folder = new RoomFolder();
        folder.setRoomId(roomId);
        folder.setParentId(parentId);
        folder.setName(name);
        folder.setCreatedBy(userId);
        folder.setAudience(resolvedAudience);

        return toFolderInfo(folderRepository.save(folder));
    }

    public void deleteFolder(UUID roomId, UUID folderId, UUID userId) {
        requireRoomMembership(userId, roomId);

        var folder = folderRepository.findById(folderId)
                .filter(f -> f.getRoomId().equals(roomId))
                .orElseThrow(() -> new ResourceNotFoundException("Folder", folderId));

        // Only the folder creator, a room LEADER, or an admin (SUPERADMIN globally,
        // SECTION_ADMIN scoped to this room's section) may delete a folder. Deletion
        // cascades to all files and sub-folders, so this must not be open to any member.
        boolean isCreator = userId.equals(folder.getCreatedBy());
        boolean isLeader = roomModuleApi.getUserRoleInRoom(userId, roomId)
                .map(r -> "LEADER".equals(r.name()))
                .orElse(false);
        if (!isCreator && !isLeader && !hasAdminRoleForRoom(userId, roomId)) {
            throw new ForbiddenException("Only the folder creator, a room leader, or an admin can delete this folder");
        }

        deleteFolderRecursive(roomId, folder);
    }

    /**
     * Recursively deletes a folder, its files (from MinIO + DB), and its sub-folders.
     * Authorization is checked once by the public {@link #deleteFolder} entry point; the
     * authorized actor's deletion cascades over the entire sub-tree.
     */
    private void deleteFolderRecursive(UUID roomId, RoomFolder folder) {
        // Delete all files in this folder from storage
        var files = fileRepository.findByRoomIdAndFolderIdOrderByCreatedAtDesc(roomId, folder.getId());
        for (var file : files) {
            storageService.delete(file.getStoragePath());
        }
        fileRepository.deleteAll(files);

        // Delete sub-folders recursively
        var subFolders = folderRepository.findByRoomIdAndParentIdOrderByNameAsc(roomId, folder.getId());
        for (var sub : subFolders) {
            deleteFolderRecursive(roomId, sub);
        }

        folderRepository.delete(folder);
    }

    // ---- Helpers ----

    /**
     * Returns true if the user is a global SUPERADMIN, or a SECTION_ADMIN scoped to the
     * section the given room belongs to. SECTION_ADMINs are NOT global admins: a section
     * admin scoped to e.g. Krippe must not gain access to an Oberstufe room.
     * Mirrors {@code DiscussionThreadService.hasAdminRoleForRoom} / {@code RoomController.isAdminForSection}.
     */
    private boolean hasAdminRoleForRoom(UUID userId, UUID roomId) {
        var user = userModuleApi.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        if (user.role() == UserRole.SUPERADMIN) {
            return true;
        }
        if (user.role() == UserRole.SECTION_ADMIN) {
            var sectionId = roomModuleApi.findById(roomId).map(r -> r.sectionId()).orElse(null);
            return sectionId != null && user.specialRoles() != null
                    && user.specialRoles().contains("SECTION_ADMIN:" + sectionId);
        }
        return false;
    }

    private void requireRoomMembership(UUID userId, UUID roomId) {
        if (!hasAdminRoleForRoom(userId, roomId) && !roomModuleApi.isUserInRoom(userId, roomId)) {
            throw new ForbiddenException("You are not a member of this room");
        }
    }

    private void requireAudienceAccess(UUID userId, UUID roomId, String audience) {
        if ("ALL".equals(audience)) return;
        Set<String> allowed = getAllowedAudiences(userId, roomId);
        if (!allowed.contains(audience)) {
            throw new ForbiddenException("You do not have access to this file");
        }
    }

    /**
     * Resolves the EFFECTIVE audience of a file given its containing folder's audience
     * (US-143 inheritance). A restrictive (non-ALL) folder forces its audience onto the
     * file so the file can never be broader than the folder it lives in. When the folder
     * is ALL (or the file is at the room root with no folder) the file keeps its own audience.
     */
    private String effectiveAudience(String folderAudience, String fileAudience) {
        if (folderAudience != null && !"ALL".equals(folderAudience)) {
            return folderAudience;
        }
        return fileAudience != null ? fileAudience : "ALL";
    }

    /**
     * Looks up a file's containing folder (if any) and returns the file's effective,
     * folder-inherited audience. Used by single-file access checks (download / metadata)
     * so they cannot bypass the folder-level visibility restriction.
     */
    private String effectiveAudienceForFile(RoomFile roomFile) {
        String folderAudience = null;
        if (roomFile.getFolderId() != null) {
            folderAudience = folderRepository.findById(roomFile.getFolderId())
                    .map(RoomFolder::getAudience)
                    .orElse(null);
        }
        return effectiveAudience(folderAudience, roomFile.getAudience());
    }

    /**
     * Determines which audience values a user is allowed to see in a room.
     * LEADER, TEACHER, SUPERADMIN, SECTION_ADMIN see all audiences.
     * PARENT_MEMBER sees ALL + PARENTS_ONLY.
     * MEMBER (student) sees ALL + STUDENTS_ONLY.
     * GUEST sees ALL only.
     */
    private Set<String> getAllowedAudiences(UUID userId, UUID roomId) {
        var roomRole = roomModuleApi.getUserRoleInRoom(userId, roomId).orElse(null);
        var userInfo = userModuleApi.findById(userId).orElse(null);
        var userRole = userInfo != null ? userInfo.role() : null;

        // Leaders, teachers, and admins (SUPERADMIN globally, SECTION_ADMIN only for
        // rooms in their own section) see everything.
        if (roomRole == RoomRole.LEADER
                || userRole == UserRole.TEACHER
                || hasAdminRoleForRoom(userId, roomId)) {
            return Set.of("ALL", "PARENTS_ONLY", "STUDENTS_ONLY");
        }

        // Parent members see ALL + PARENTS_ONLY
        if (roomRole == RoomRole.PARENT_MEMBER || userRole == UserRole.PARENT) {
            return Set.of("ALL", "PARENTS_ONLY");
        }

        // Students (MEMBER role) see ALL + STUDENTS_ONLY
        if (userRole == UserRole.STUDENT) {
            return Set.of("ALL", "STUDENTS_ONLY");
        }

        // Guests and others see ALL only
        return Set.of("ALL");
    }

    private FileInfo toFileInfo(RoomFile f) {
        String uploaderName = userModuleApi.findById(f.getUploadedBy())
                .map(u -> u.firstName() + " " + u.lastName())
                .orElse("Unknown");

        return new FileInfo(
                f.getId(),
                f.getRoomId(),
                f.getFolderId(),
                f.getOriginalName(),
                f.getContentType(),
                f.getFileSize(),
                f.getUploadedBy(),
                uploaderName,
                f.getAudience(),
                f.getCreatedAt()
        );
    }

    /**
     * Resolves the audience for a new folder or thread.
     * Parents always get PARENTS_ONLY. Teachers/Leaders/Admins can choose.
     */
    private String resolveAudience(String audience, UUID userId) {
        var userInfo = userModuleApi.findById(userId).orElse(null);
        var userRole = userInfo != null ? userInfo.role() : null;

        // Parents always create with PARENTS_ONLY
        if (userRole == UserRole.PARENT) {
            return "PARENTS_ONLY";
        }

        // Teachers, leaders, admins can choose
        if (audience != null && !audience.isBlank()) {
            String upper = audience.toUpperCase();
            if (!Set.of("ALL", "PARENTS_ONLY", "STUDENTS_ONLY").contains(upper)) {
                throw new BusinessException("Invalid audience value: " + audience);
            }
            return upper;
        }

        return "ALL";
    }

    private FolderInfo toFolderInfo(RoomFolder f) {
        return new FolderInfo(
                f.getId(),
                f.getRoomId(),
                f.getParentId(),
                f.getName(),
                f.getAudience(),
                f.getCreatedAt()
        );
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "unnamed";
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * DSGVO: Clean up all files for a deleted user.
     */
    @Transactional
    public void cleanupUserData(UUID userId) {
        var files = fileRepository.findByUploadedBy(userId);
        for (var file : files) {
            storageService.delete(file.getStoragePath());
        }
        fileRepository.deleteAll(files);
    }

    /**
     * DSGVO: Export all files data for a user.
     */
    @Override
    public Map<String, Object> exportUserData(UUID userId) {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        var files = fileRepository.findByUploadedBy(userId);
        data.put("files", files.stream().map(f -> Map.of(
                "id", f.getId(),
                "originalFilename", f.getOriginalName(),
                "createdAt", f.getCreatedAt()
        )).toList());
        return data;
    }
}
