package com.devpath.shared.infrastructure;

import com.devpath.shared.application.ObjectContentPort;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FilesystemObjectContentAdapter implements ObjectContentPort {
    private static final String PREFIX = "object://";
    private final Path root;

    public FilesystemObjectContentAdapter(@Value("${devpath.knowledge.object-storage-root}") String configuredRoot) {
        root = Path.of(configuredRoot).toAbsolutePath().normalize();
    }

    @Override
    public String put(UUID ownerId, UUID resourceId, UUID versionId, String name, String content) {
        Path target = resolve(ownerId, resourceId, versionId, name);
        try {
            Files.createDirectories(target.getParent());
            Path temporary = Files.createTempFile(target.getParent(), ".pending-", ".tmp");
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return PREFIX + ownerId + "/" + resourceId + "/" + versionId + "/" + normalizeName(name);
        } catch (IOException exception) {
            throw new IllegalStateException("Object content could not be stored", exception);
        }
    }

    @Override
    public String read(UUID ownerId, String objectReference) {
        Path target = safe(root.resolve(reference(ownerId, objectReference)));
        try {
            return Files.readString(target, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Object content is unavailable", exception);
        }
    }

    @Override
    public void deleteVersion(UUID ownerId, UUID resourceId, UUID versionId) {
        Path directory = resolve(ownerId, resourceId, versionId, "placeholder").getParent();
        if (!Files.exists(directory)) return;
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Object content could not be deleted", exception);
        }
    }

    private Path resolve(UUID ownerId, UUID resourceId, UUID versionId, String name) {
        return safe(root.resolve(ownerId.toString()).resolve(resourceId.toString()).resolve(versionId.toString())
            .resolve(normalizeName(name)));
    }

    private String reference(UUID ownerId, String value) {
        if (value == null || !value.startsWith(PREFIX + ownerId + "/")) {
            throw new IllegalArgumentException("Object reference is outside the owner scope");
        }
        return value.substring(PREFIX.length());
    }

    private Path safe(Path candidate) {
        Path normalized = candidate.toAbsolutePath().normalize();
        if (!normalized.startsWith(root)) throw new IllegalArgumentException("Object path is outside the configured root");
        return normalized;
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank() || name.startsWith("/") || name.contains("..") || name.contains("\\")) {
            throw new IllegalArgumentException("Object name is invalid");
        }
        return name;
    }
}
