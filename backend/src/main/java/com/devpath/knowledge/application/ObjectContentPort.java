package com.devpath.knowledge.application;

import java.util.UUID;

public interface ObjectContentPort {
    String put(UUID ownerId, UUID documentId, UUID versionId, String name, String content);
    String read(UUID ownerId, String objectReference);
    void deleteVersion(UUID ownerId, UUID documentId, UUID versionId);
}
