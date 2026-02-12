package com.monteweb.fotobox;

import java.util.List;
import java.util.UUID;

/**
 * Public API: Facade interface for the fotobox module.
 */
public interface FotoboxModuleApi {
    List<FotoboxThreadInfo> getThreadsByRoom(UUID roomId);
    boolean isFotoboxEnabledForRoom(UUID roomId);
}
