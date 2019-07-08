package org.example.files.client.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.CharsetUtil;
import io.netty.util.ResourceLeakDetector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;

import static io.netty.util.ResourceLeakDetector.Level.PARANOID;
import static org.junit.jupiter.api.Assertions.*;

class ClientHandlerTest {
    private static String FILE_DIR = "/tmp";

    private ClientHandler handler = new ClientHandler(FILE_DIR);

    EmbeddedChannel ch = null;

    @BeforeEach
    public void start() {
        ch = new EmbeddedChannel(handler);
    }

    @BeforeAll
    public static void setup() {
        ResourceLeakDetector.setLevel(PARANOID);
    }

    @Test
    void channelRead0() throws IOException {
        // output to console.
        ch.writeInbound(Unpooled.copiedBuffer("0+cmd", CharsetUtil.UTF_8));

        // output to file -> 0 size, empty string goes to console.
        ch.writeInbound(Unpooled.copiedBuffer("1+a.txt+0+", CharsetUtil.UTF_8));
        File file = new File("/tmp/a.txt");
        assertTrue(file.exists());

        // output to file -> 1 byte size
        ch.writeInbound(Unpooled.copiedBuffer("1+a.txt+1+a", CharsetUtil.UTF_8));
        FileReader reader = null;
        try {
            reader = new FileReader("/tmp/a.txt");
            assertEquals((int) 'a', reader.read());
        } finally {
            if (reader != null)
                reader.close();
        }
    }

    @Test
    void fileSize() throws Exception {
        ByteBuf buf = Unpooled.wrappedBuffer("10+".getBytes(CharsetUtil.UTF_8));

        assertEquals(10L, handler.fileSize(buf));
    }

    @Test
    void fileSizeBadFormat() throws Exception {
        ByteBuf buf = Unpooled.wrappedBuffer("+10+".getBytes(CharsetUtil.UTF_8));
        assertThrows(Exception.class, () -> handler.fileSize(buf));
    }

    @Test
    void filePath() throws Exception {
        ByteBuf buf = Unpooled.wrappedBuffer("a.txt+".getBytes(CharsetUtil.UTF_8));

        assertEquals(Paths.get("/tmp/a.txt"), handler.filePath(buf, "/tmp"));
    }

    @Test
    void filePathNoDir() throws Exception {
        ByteBuf buf = Unpooled.wrappedBuffer("a.txt+".getBytes(CharsetUtil.UTF_8));
        assertThrows(Exception.class, () -> handler.filePath(buf, ""));
        assertThrows(Exception.class, () -> handler.filePath(buf, null));
    }

    @Test
    void initStream() throws IOException {
        OutputStream out = null;
        try {
            out = handler.initStream(Paths.get("/tmp/a.txt"));
            // recreate without errors.
            out = handler.initStream(Paths.get("/tmp/a.txt"));
        } finally {
            assertNotNull(out);
            out.close();
        }
    }

    @Test
    void exceptionCaught() {
        // exceptions are not thrown, only logged.
        assertDoesNotThrow(() -> {
            ch.writeInbound(Unpooled.copiedBuffer("1+", CharsetUtil.UTF_8));
        });
    }
}