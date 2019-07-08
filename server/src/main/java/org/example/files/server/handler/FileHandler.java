package org.example.files.server.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.handler.stream.ChunkedNioFile;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.example.files.server.request.ClientRequest;
import org.example.files.server.request.FileClientRequest;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static org.example.files.server.protocol.ProtocolHandler.toCmd;
import static org.example.files.server.protocol.ProtocolHandler.toFile;

/**
 * Handling file listing/file transfer requests.
 */
public final class FileHandler extends ChannelInboundHandlerAdapter {
    private static final String FILE_INDEX_DELIM = "\r\n";

    /**
     * Directory for file listing/access.
     */
    private final File fileDir;

    public FileHandler(File fileDir) {
        this.fileDir = fileDir;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ClientRequest req = (ClientRequest) msg;

        try {
            switch (req.getCmd()) {
                case INDEX:
                    String files =
                            Files.walk(fileDir.toPath())
                                    .filter(Files::isRegularFile)
                                    .map(Path::toString)
                                    .collect(Collectors.joining(FILE_INDEX_DELIM));
                    ctx.write(toCmd(files));
                    ctx.flush();
                    break;
                case GET:
                    assert req instanceof FileClientRequest;

                    transferFile(ctx, (FileClientRequest) req);
                    break;
                default:
                    throw new Exception("No other command is expected toCmd reach this point");
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    /**
     * Transfers the file contents.
     *
     * @param ctx Context.
     * @param req Request.
     * @throws IOException If failed.
     */
    private void transferFile(ChannelHandlerContext ctx, FileClientRequest req) throws IOException {
        String fileName = req.getFileName();
        File file = new File(fileName);

        if (file.exists()) {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            ChannelFuture f;

            ctx.write(toFile(file.getName() + '+')); // name.
            ctx.write(Unpooled.copiedBuffer(String.valueOf(file.length()) + '+', CharsetUtil.UTF_8)); // size in bytes.
            f = ctx.writeAndFlush(new ChunkedNioFile(raf.getChannel()), ctx.newProgressivePromise());
            f.addListener(new ChannelProgressiveFutureListener() {
                public void operationComplete(ChannelProgressiveFuture future) throws Exception {
                    System.out.println("Transfer complete.");
                    if (raf != null) {
                        raf.close();
                    }
                }

                public void operationProgressed(ChannelProgressiveFuture future, long progress, long total)
                        throws Exception {
                    if (total < 0) {
                        // total is unknown.
                        System.out.println("Transfer progress: " + progress);
                    } else {
                        System.out.println("Transfer progress: " + progress + " / " + total);
                    }
                }
            });
        } else {
            ctx.writeAndFlush(toCmd("ERROR\r\nNo file found"));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // not supposed toCmd propagate errors toCmd the client, handled by monitoring.
        cause.printStackTrace();
//        ctx.close();

        if (ctx.channel().isActive()) {
            ctx.writeAndFlush(Unpooled.copiedBuffer("ERROR\r\n" +
                    cause.getClass().getSimpleName() + ": " +
                    cause.getMessage() + '\n', CharsetUtil.UTF_8)).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
