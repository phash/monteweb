package com.monteweb.parentletter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ParentLetterModuleApi {
    List<ParentLetterInfo> getLettersForRoom(UUID roomId);
    Optional<ParentLetterInfo> findById(UUID letterId);
    long countPendingForParent(UUID parentUserId);
    Map<String, Object> exportUserData(UUID userId);
}
