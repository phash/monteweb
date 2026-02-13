package com.monteweb.files;

import java.time.Instant;
import java.util.UUID;

public record FolderInfo(
        UUID id,
        UUID roomId,
        UUID parentId,
        String name,
        String audience,
        Instant createdAt
) {
}
