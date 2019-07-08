package org.example.files.server;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileServerTest {

    @Test
    void checkCreateFileDirNoArgs() {
        assertThrows(Exception.class, () -> FileServer.checkCreateFileDir(new String[]{}));
    }

    @Test
    void checkCreateFileDirNonexisting() {
        assertThrows(Exception.class, () -> FileServer.checkCreateFileDir(new String[]{"/a/b/c"}));
    }

    @Test
    void checkCreateFileDirExisting() throws Exception {
        File dir = FileServer.checkCreateFileDir(new String[]{"/tmp"});
        assertEquals("tmp", dir.getName());
    }
}