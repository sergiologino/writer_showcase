package io.altacod.publisher.storage;

import io.altacod.publisher.config.PublisherStorageProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

@Component
public class LocalObjectStorage {

    private final Path root;

    public LocalObjectStorage(PublisherStorageProperties props) {
        try {
            this.root = Paths.get(props.getLocalRoot()).toAbsolutePath().normalize();
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create storage root", e);
        }
    }

    public String store(long workspaceId, MultipartFile file) throws IOException {
        String ext = extension(file.getOriginalFilename());
        String key = workspaceId + "/" + UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        Path dest = root.resolve(key).normalize();
        if (!dest.startsWith(root)) {
            throw new IllegalStateException("Invalid storage path");
        }
        Files.createDirectories(dest.getParent());
        file.transferTo(dest);
        return key;
    }

    public void deleteIfExists(String storageKey) throws IOException {
        Path p = resolveNormalized(storageKey);
        Files.deleteIfExists(p);
    }

    public Path resolveExisting(String storageKey) throws IOException {
        Path p = resolveNormalized(storageKey);
        if (!Files.isRegularFile(p)) {
            throw new NoSuchFileException(storageKey);
        }
        return p;
    }

    private Path resolveNormalized(String storageKey) {
        Path p = root.resolve(storageKey).normalize();
        if (!p.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return p;
    }

    private static String extension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
