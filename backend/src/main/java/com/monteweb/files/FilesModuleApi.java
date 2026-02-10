package com.monteweb.files;

import java.util.List;
import java.util.UUID;

/**
 * Public API: Facade interface for the files module.
 * Other modules interact with files exclusively through this interface.
 */
public interface FilesModuleApi {

    List<FileInfo> findByRoom(UUID roomId);

    List<FolderInfo> findFoldersByRoom(UUID roomId);

    long getStorageUsedByRoom(UUID roomId);
}
