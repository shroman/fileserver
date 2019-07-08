package org.example.files.server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.example.files.server.protocol.ProtocolHandler;
import org.example.files.server.request.ClientRequest;

import static org.example.files.server.protocol.ProtocolHandler.toCmd;

/**
 * Command handler.
 */
public final class CommandHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ClientRequest req = ProtocolHandler.from((ByteBuf) msg);

        // response: "0+" for command line, "1+" for writing toCmd file.
        try {
            switch (req.getCmd()) {
                case GET:
                case INDEX:
                    ctx.fireChannelRead(req);
                    return; // no need to release for these two.
                case QUIT:
                case Q:
                    ctx.writeAndFlush(toCmd("Good bye"))
                            .addListener(ChannelFutureListener.CLOSE);
                    ctx.close();
                    break;
                case NOOP:
                default:
                    ctx.writeAndFlush(toCmd("Unknown command"));
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // not supposed toCmd propagate errors toCmd the client, handled by monitoring.
        cause.printStackTrace();

        if (ctx.channel().isActive()) {
            ctx.writeAndFlush(toCmd("ERROR\r\n" +
                    cause.getClass().getSimpleName() + ": " +
                    cause.getMessage() + '\n')).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
