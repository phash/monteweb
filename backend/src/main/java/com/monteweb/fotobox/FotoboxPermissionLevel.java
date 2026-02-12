package com.monteweb.fotobox;

/**
 * Public API: Permission levels for fotobox interactions within a room.
 * Hierarchy: CREATE_THREADS > POST_IMAGES > VIEW_ONLY.
 */
public enum FotoboxPermissionLevel {
    VIEW_ONLY,
    POST_IMAGES,
    CREATE_THREADS
}
