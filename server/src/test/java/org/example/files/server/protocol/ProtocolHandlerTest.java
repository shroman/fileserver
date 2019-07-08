package org.example.files.server.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.example.files.server.request.ClientRequest;
import org.example.files.server.request.Command;
import org.example.files.server.request.FileClientRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolHandlerTest {
    @Test
    void fromOverLongCommand() throws Exception {
        byte[] bytes = new byte[1025];

        ByteBuf largeBuf = Unpooled.wrappedBuffer(bytes);
        assertThrows(Exception.class, () -> ProtocolHandler.from(largeBuf));
    }

    @Test
    void fromEmptyCommand() throws Exception {
        ByteBuf buf = Unpooled.wrappedBuffer("".getBytes());
        ClientRequest req = ProtocolHandler.from(buf);

        assertTrue(req.getCmd() == Command.NOOP);
    }

    @Test
    void fromToQuit() throws Exception {
        ByteBuf buf = Unpooled.wrappedBuffer("quit ".getBytes());
        ClientRequest req = ProtocolHandler.from(buf);

        assertTrue(req.getCmd() == Command.QUIT);

        buf = Unpooled.wrappedBuffer(" q".getBytes());
        req = ProtocolHandler.from(buf);

        assertTrue(req.getCmd() == Command.QUIT);
    }

    @Test
    void fromToIndex() throws Exception {
        ByteBuf buf = Unpooled.wrappedBuffer(" index ".getBytes());
        ClientRequest req = ProtocolHandler.from(buf);

        assertTrue(req.getCmd() == Command.INDEX);
    }

    @Test
    void fromToGetEmptyFile() throws Exception {
        ByteBuf buf = Unpooled.wrappedBuffer("get ".getBytes());
        FileClientRequest req = (FileClientRequest) ProtocolHandler.from(buf);

        assertTrue(req.getCmd() == Command.GET);
        assertEquals("nonexistingfile", req.getFileName());
    }

    @Test
    void fromToGet() throws Exception {
        ByteBuf buf = Unpooled.wrappedBuffer("get a.txt ".getBytes());
        FileClientRequest req = (FileClientRequest) ProtocolHandler.from(buf);

        assertTrue(req.getCmd() == Command.GET);
        assertEquals("a.txt", req.getFileName());
    }

    @Test
    void fromToGetMultiFile() throws Exception {
        ByteBuf buf = Unpooled.wrappedBuffer("get a.txt b.txt".getBytes());
        FileClientRequest req = (FileClientRequest) ProtocolHandler.from(buf);

        assertTrue(req.getCmd() == Command.GET);
        // b.txt is ignored
        assertEquals("a.txt", req.getFileName());
    }
}