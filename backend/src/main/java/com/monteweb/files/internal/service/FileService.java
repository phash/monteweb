package com.monteweb.files.internal.service;

import com.monteweb.files.FileInfo;
import com.monteweb.files.FilesModuleApi;
import com.monteweb.files.FolderInfo;
import com.monteweb.files.internal.model.RoomFile;
import com.monteweb.files.internal.model.RoomFolder;
import com.monteweb.files.internal.repository.RoomFileRepository;
import com.monteweb.files.internal.repository.RoomFolderRepository;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.exception.ForbiddenException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import com.monteweb.user.UserModuleApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FileService implements FilesModuleApi {

    private final RoomFileRepository fileRepository;
    private final RoomFolderRepository folderRepository;
    private final FileStorageService storageService;
    private final RoomModuleApi roomModuleApi;
    private final UserModuleApi userModuleApi;

    public FileService(RoomFileRepository fileRepository,
                       RoomFolderRepository folderRepository,
                       FileStorageService storageService,
                       RoomModuleApi roomModuleApi,
                       UserModuleApi userModuleApi) {
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
        this.storageService = storageService;
        this.roomModuleApi = roomModuleApi;
        this.userModuleApi = userModuleApi;
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

    // ---- File operations ----

    @Transactional(readOnly = true)
    public List<FileInfo> listFiles(UUID roomId, UUID folderId, UUID userId) {
        requireRoomMembership(userId, roomId);

        List<RoomFile> files;
        if (folderId == null) {
            files = fileRepository.findByRoomIdAndFolderIdIsNullOrderByCreatedAtDesc(roomId);
        } else {
            files = fileRepository.findByRoomIdAndFolderIdOrderByCreatedAtDesc(roomId, folderId);
        }
        return files.stream().map(this::toFileInfo).toList();
    }

    public FileInfo uploadFile(UUID roomId, UUID folderId, UUID userId, MultipartFile file) {
        requireRoomMembership(userId, roomId);

        if (folderId != null) {
            folderRepository.findById(folderId)
                    .filter(f -> f.getRoomId().equals(roomId))
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", folderId));
        }

        String storedName = UUID.randomUUID() + "_" + sanitizeFileName(file.getOriginalFilename());
        String storagePath = storageService.upload(roomId, folderId, storedName, file);

        var roomFile = new RoomFile();
        roomFile.setRoomId(roomId);
        roomFile.setFolderId(folderId);
        roomFile.setOriginalName(file.getOriginalFilename());
        roomFile.setStoredName(storedName);
        roomFile.setContentType(file.getContentType());
        roomFile.setFileSize(file.getSize());
        roomFile.setStoragePath(storagePath);
        roomFile.setUploadedBy(userId);

        return toFileInfo(fileRepository.save(roomFile));
    }

    @Transactional(readOnly = true)
    public InputStream downloadFile(UUID roomId, UUID fileId, UUID userId) {
        requireRoomMembership(userId, roomId);

        var roomFile = fileRepository.findById(fileId)
                .filter(f -> f.getRoomId().equals(roomId))
                .orElseThrow(() -> new ResourceNotFoundException("File", fileId));

        return storageService.download(roomFile.getStoragePath());
    }

    @Transactional(readOnly = true)
    public RoomFile getFileMetadata(UUID roomId, UUID fileId, UUID userId) {
        requireRoomMembership(userId, roomId);
        return fileRepository.findById(fileId)
                .filter(f -> f.getRoomId().equals(roomId))
                .orElseThrow(() -> new ResourceNotFoundException("File", fileId));
    }

    public void deleteFile(UUID roomId, UUID fileId, UUID userId) {
        var roomFile = fileRepository.findById(fileId)
                .filter(f -> f.getRoomId().equals(roomId))
                .orElseThrow(() -> new ResourceNotFoundException("File", fileId));

        // Only uploader or room leaders can delete
        if (!roomFile.getUploadedBy().equals(userId)) {
            var role = roomModuleApi.getUserRoleInRoom(userId, roomId);
            if (role.isEmpty() || !"LEADER".equals(role.get().name())) {
                throw new ForbiddenException("Only the uploader or room leaders can delete files");
            }
        }

        storageService.delete(roomFile.getStoragePath());
        fileRepository.delete(roomFile);
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
        return folders.stream().map(this::toFolderInfo).toList();
    }

    public FolderInfo createFolder(UUID roomId, UUID parentId, String name, UUID userId) {
        requireRoomMembership(userId, roomId);

        if (folderRepository.existsByRoomIdAndParentIdAndName(roomId, parentId, name)) {
            throw new BusinessException("A folder with this name already exists in this location");
        }

        if (parentId != null) {
            folderRepository.findById(parentId)
                    .filter(f -> f.getRoomId().equals(roomId))
                    .orElseThrow(() -> new ResourceNotFoundException("Parent folder", parentId));
        }

        var folder = new RoomFolder();
        folder.setRoomId(roomId);
        folder.setParentId(parentId);
        folder.setName(name);
        folder.setCreatedBy(userId);

        return toFolderInfo(folderRepository.save(folder));
    }

    public void deleteFolder(UUID roomId, UUID folderId, UUID userId) {
        requireRoomMembership(userId, roomId);

        var folder = folderRepository.findById(folderId)
                .filter(f -> f.getRoomId().equals(roomId))
                .orElseThrow(() -> new ResourceNotFoundException("Folder", folderId));

        // Delete all files in this folder from storage
        var files = fileRepository.findByRoomIdAndFolderIdOrderByCreatedAtDesc(roomId, folderId);
        for (var file : files) {
            storageService.delete(file.getStoragePath());
        }
        fileRepository.deleteAll(files);

        // Delete sub-folders recursively
        var subFolders = folderRepository.findByRoomIdAndParentIdOrderByNameAsc(roomId, folderId);
        for (var sub : subFolders) {
            deleteFolder(roomId, sub.getId(), userId);
        }

        folderRepository.delete(folder);
    }

    // ---- Helpers ----

    private void requireRoomMembership(UUID userId, UUID roomId) {
        if (!roomModuleApi.isUserInRoom(userId, roomId)) {
            throw new ForbiddenException("You are not a member of this room");
        }
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
                f.getCreatedAt()
        );
    }

    private FolderInfo toFolderInfo(RoomFolder f) {
        return new FolderInfo(
                f.getId(),
                f.getRoomId(),
                f.getParentId(),
                f.getName(),
                f.getCreatedAt()
        );
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "unnamed";
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
