package cl.duoc.eventos.tickets.service;

import cl.duoc.eventos.tickets.exception.NotFoundException;
import cl.duoc.eventos.tickets.exception.StorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Almacenamiento temporal de tickets en el sistema de archivos EFS.
 * En el contenedor, el EFS se monta en la ruta app.storage.efs-path (/app/efs).
 */
@Service
public class EfsStorageService {

    private final Path basePath;

    public EfsStorageService(@Value("${app.storage.efs-path}") String efsPath) {
        this.basePath = Paths.get(efsPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.basePath);
        } catch (IOException e) {
            throw new StorageException("No se pudo acceder al directorio EFS: " + this.basePath, e);
        }
    }

    public Path getBasePath() {
        return basePath;
    }

    public void write(String relativePath, byte[] content) {
        Path target = resolveSafe(relativePath);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new StorageException("No se pudo escribir en EFS: " + relativePath, e);
        }
    }

    public byte[] read(String relativePath) {
        Path target = resolveSafe(relativePath);
        if (!Files.exists(target)) {
            throw new NotFoundException("El archivo no existe en EFS: " + relativePath);
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new StorageException("No se pudo leer desde EFS: " + relativePath, e);
        }
    }

    public boolean delete(String relativePath) {
        Path target = resolveSafe(relativePath);
        try {
            boolean deleted = Files.deleteIfExists(target);
            removeEmptyParents(target.getParent());
            return deleted;
        } catch (IOException e) {
            throw new StorageException("No se pudo eliminar de EFS: " + relativePath, e);
        }
    }

    public boolean exists(String relativePath) {
        return Files.exists(resolveSafe(relativePath));
    }

    /** Evita path traversal: la ruta resultante debe permanecer dentro del directorio base. */
    private Path resolveSafe(String relativePath) {
        Path resolved = basePath.resolve(relativePath).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new IllegalArgumentException("Ruta inválida: " + relativePath);
        }
        return resolved;
    }

    /** Limpia carpetas vacías de usuario/evento tras eliminar un ticket. */
    private void removeEmptyParents(Path dir) throws IOException {
        Path current = dir;
        while (current != null && !current.equals(basePath) && current.startsWith(basePath)
                && Files.isDirectory(current)) {
            try (var entries = Files.list(current)) {
                if (entries.findAny().isPresent()) {
                    return;
                }
            }
            Files.delete(current);
            current = current.getParent();
        }
    }
}
