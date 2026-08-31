package com.devpath.shared.application;

import java.util.UUID;

public interface ObjectContentPort {
    String put(UUID ownerId, UUID resourceId, UUID versionId, String name, String content);
    String read(UUID ownerId, String objectReference);
    void deleteVersion(UUID ownerId, UUID resourceId, UUID versionId);
}
