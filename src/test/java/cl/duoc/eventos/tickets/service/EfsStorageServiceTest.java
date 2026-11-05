package cl.duoc.eventos.tickets.service;

import cl.duoc.eventos.tickets.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EfsStorageServiceTest {

    @TempDir
    Path tmp;

    @Test
    void escribeLeeYEliminaArchivos() {
        EfsStorageService efs = new EfsStorageService(tmp.toString());
        byte[] data = "contenido".getBytes();

        efs.write("evento123/usuarioX/ticket00001.pdf", data);
        assertTrue(efs.exists("evento123/usuarioX/ticket00001.pdf"));
        assertArrayEquals(data, efs.read("evento123/usuarioX/ticket00001.pdf"));

        assertTrue(efs.delete("evento123/usuarioX/ticket00001.pdf"));
        assertFalse(efs.exists("evento123/usuarioX/ticket00001.pdf"));
        assertThrows(NotFoundException.class, () -> efs.read("evento123/usuarioX/ticket00001.pdf"));
    }

    @Test
    void bloqueaPathTraversal() {
        EfsStorageService efs = new EfsStorageService(tmp.toString());
        assertThrows(IllegalArgumentException.class, () -> efs.write("../fuera.pdf", new byte[]{1}));
    }
}
