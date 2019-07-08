package org.example.files.server.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.example.files.server.request.ClientRequest;
import org.example.files.server.request.Command;
import org.example.files.server.request.FileClientRequest;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Simple protocol handler util.
 * <p>
 * Text protocol is as follows (simple just enough for the functionality):
 * <pre>
 *     Request is one-liner: INDEX, GET <filename>, QUIT, Q.
 *
 *     Response can be of two kinds:
 *     0+response for console output
 *     1+filename+size+bytestream
 * </pre>
 */
public class ProtocolHandler {
    /**
     * Logger.
     */
    private static final Logger logger = Logger.getLogger(ProtocolHandler.class.getName());

    /**
     * Cmd response mark.
     */
    protected static final String CMDLINE = "0+";

    /**
     * File writing operation mark.
     */
    static final String FILE = "1+";

    public static ClientRequest from(ByteBuf buf) throws Exception {
        assert buf != null;

        if (buf.readableBytes() > 1024)
            throw new Exception("Super-long command. Won't handle that");

        String req = buf.toString(CharsetUtil.UTF_8);
        String[] splitReq = Arrays.stream(req.trim().split("\\s+"))
                .map(String::trim).toArray(String[]::new);

        if (splitReq[0].equalsIgnoreCase(Command.INDEX.name())) {
            return new ClientRequest(Command.INDEX);
        } else if (splitReq[0].equalsIgnoreCase(Command.QUIT.name()) || splitReq[0].equalsIgnoreCase(Command.Q.name())) {
            return new ClientRequest(Command.QUIT);
        } else if (splitReq[0].equalsIgnoreCase(Command.GET.name())) {
            // limitation: file name cannot be space-delimited.
            if (splitReq.length < 2) {
                logger.severe("No file name specified");

                return new FileClientRequest(Command.GET, "nonexistingfile");
            }

            return new FileClientRequest(Command.GET, splitReq[1].trim());
        } else {
            logger.warning("Unknown command: " + splitReq[0]);
            return new ClientRequest(Command.NOOP);
        }
    }

    /**
     * Prepares a console(command line) response.
     */
    public static ByteBuf toCmd(String rsp) {
        // with /r/n for decoder
        return Unpooled.copiedBuffer(CMDLINE + rsp + "\r\n\r\n", CharsetUtil.UTF_8);
    }

    /**
     * Prepares a file response.
     */
    public static ByteBuf toFile(String rsp) {
        return Unpooled.copiedBuffer(FILE + rsp, CharsetUtil.UTF_8);
    }
}
