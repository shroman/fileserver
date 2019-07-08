package org.example.files.server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.ResourceLeakDetector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static io.netty.util.ResourceLeakDetector.Level.PARANOID;
import static org.junit.jupiter.api.Assertions.*;

class CommandHandlerTest {
    EmbeddedChannel ch = null;

    @BeforeAll
    public static void setup() {
        ResourceLeakDetector.setLevel(PARANOID);
    }

    @BeforeEach
    public void start() {
        ch = new EmbeddedChannel(new CommandHandler(), new ChunkedWriteHandler(), new FileHandler(new File(".")));
    }

    @Test
    public void testQuitUpper() {
        ch.writeInbound(Unpooled.copiedBuffer("QUIT", CharsetUtil.UTF_8));
        assertEquals("0+Good bye\r\n\r\n", ((ByteBuf) ch.readOutbound()).toString(CharsetUtil.UTF_8));
    }

    @Test
    public void testQuitLower() {
        ch.writeInbound(Unpooled.copiedBuffer("quit", CharsetUtil.UTF_8));
        assertEquals("0+Good bye\r\n\r\n", ((ByteBuf) ch.readOutbound()).toString(CharsetUtil.UTF_8));
    }

    @Test
    public void testQuitSpaces() {
        ch.writeInbound(Unpooled.copiedBuffer(" quit ", CharsetUtil.UTF_8));
        assertEquals("0+Good bye\r\n\r\n", ((ByteBuf) ch.readOutbound()).toString(CharsetUtil.UTF_8));
    }

    @Test
    public void testQ() {
        ch.writeInbound(Unpooled.copiedBuffer("Q", CharsetUtil.UTF_8));
        assertEquals("0+Good bye\r\n\r\n", ((ByteBuf) ch.readOutbound()).toString(CharsetUtil.UTF_8));
    }

    @Test
    public void testUnknown() {
        ch.writeInbound(Unpooled.copiedBuffer("aaa", CharsetUtil.UTF_8));
        assertEquals("0+Unknown command\r\n\r\n", ((ByteBuf) ch.readOutbound()).toString(CharsetUtil.UTF_8));
    }

    @Test
    public void testEmpty() {
        ch.writeInbound(Unpooled.copiedBuffer("", CharsetUtil.UTF_8));
        assertEquals("0+Unknown command\r\n\r\n", ((ByteBuf) ch.readOutbound()).toString(CharsetUtil.UTF_8));
    }

    @Test
    public void testIndex() {
        ch.writeInbound(Unpooled.copiedBuffer(" index", CharsetUtil.UTF_8));

        ByteBuf buf = ch.readOutbound();
        assertTrue(buf.toString(CharsetUtil.UTF_8).startsWith("0+"));
        assertTrue(!buf.toString(CharsetUtil.UTF_8).isEmpty());
    }

    @Test
    public void exception() {
        // exceptions are not thrown, but written toCmd the channel
        assertDoesNotThrow(() -> {
            ch.pipeline().fireChannelRead("aaa");
        });
        ByteBuf buf = ch.readOutbound();
        assertTrue(buf.toString(CharsetUtil.UTF_8).startsWith("0+ERROR"));
    }
}
