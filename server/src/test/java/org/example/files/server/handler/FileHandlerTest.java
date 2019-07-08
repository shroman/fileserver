package org.example.files.server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.ResourceLeakDetector;
import org.example.files.server.request.ClientRequest;
import org.example.files.server.request.Command;
import org.example.files.server.request.FileClientRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static io.netty.util.ResourceLeakDetector.Level.PARANOID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileHandlerTest {
    EmbeddedChannel ch = null;

    @BeforeAll
    public static void setup() {
        ResourceLeakDetector.setLevel(PARANOID);
    }

    @BeforeEach
    public void start() throws URISyntaxException {
        ch = new EmbeddedChannel(new ChunkedWriteHandler(),
                new FileHandler(new File(this.getClass().getClassLoader().getResource(".").toURI())));
    }

    @Test
    public void testIndex() {
        ch.writeInbound(new ClientRequest(Command.INDEX));

        ByteBuf buf = ch.readOutbound();
        assertTrue(buf.toString(CharsetUtil.UTF_8).startsWith("0+"));
        assertTrue(!buf.toString(CharsetUtil.UTF_8).isEmpty());
        System.out.println(buf.toString(CharsetUtil.UTF_8));
    }

    @Test
    void testGet() throws URISyntaxException {
        String fName = Paths.get(Paths.get(this.getClass().getClassLoader().getResource(".").toURI()).toString(),
                "test.txt").toString();
        ch.writeInbound(new FileClientRequest(Command.GET, fName));

        ByteBuf buf = ch.readOutbound();
        assertTrue(buf.toString(CharsetUtil.UTF_8).startsWith("1+")); // file name.
        buf = ch.readOutbound(); // size.
        buf = ch.readOutbound(); // contents.
        assertTrue(buf.toString(CharsetUtil.UTF_8).startsWith("abc"));
//        System.out.println(buf.toString(CharsetUtil.UTF_8));
    }

    @Test
    void testGetUnknFile() throws URISyntaxException {
        String fName = Paths.get(Paths.get(this.getClass().getClassLoader().getResource(".").toURI()).toString(),
                "a.txt").toString();
        ch.writeInbound(new FileClientRequest(Command.GET, fName));

        ByteBuf buf = ch.readOutbound();
        assertTrue(buf.toString(CharsetUtil.UTF_8).startsWith("0+ERROR"));
//        System.out.println(buf.toString(CharsetUtil.UTF_8));
    }

    @Test
    void testGetUnallowedCmd() throws URISyntaxException {
        ch.writeInbound(new ClientRequest(Command.Q));
        ByteBuf buf = ch.readOutbound();
        assertTrue(buf.toString(CharsetUtil.UTF_8).startsWith("ERROR"));
    }

    @Test
    public void exception() {
        // exceptions are not thrown, but written to the channel
        assertDoesNotThrow(() -> {
            ch.pipeline().fireChannelRead("aaa");
        });
        ByteBuf buf = ch.readOutbound();
        assertTrue(buf.toString(CharsetUtil.UTF_8).startsWith("ERROR"));
    }
}
