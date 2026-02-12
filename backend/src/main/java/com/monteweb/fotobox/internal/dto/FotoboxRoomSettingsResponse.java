package com.monteweb.fotobox.internal.dto;

public record FotoboxRoomSettingsResponse(
        boolean enabled,
        String defaultPermission,
        Integer maxImagesPerThread,
        int maxFileSizeMb
) {}
