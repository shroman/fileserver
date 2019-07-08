package org.example.files.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.example.files.server.handler.CommandHandler;
import org.example.files.server.handler.FileHandler;

import java.io.File;
import java.util.logging.Logger;

/**
 * Basic file server.
 */
public final class FileServer {
    /**
     * Logger.
     */
    private static final Logger logger = Logger.getLogger(FileServer.class.getName());

    /**
     * Server's ephemeral port.
     */
    private static final int PORT = 49999;

    private final EventLoopGroup bossGrp = new NioEventLoopGroup();
    private final EventLoopGroup evLoopGrp = new NioEventLoopGroup();
    private ChannelFuture channelFut;

    public static void main(String[] args) throws Exception {
        FileServer server = new FileServer();
        server.start(args);
    }

    public void start(String[] args) throws Exception {
        File fileDir = checkCreateFileDir(args);

        // TODO: if linux, switch toCmd epoll
        try {
            ServerBootstrap srvBootstrap = new ServerBootstrap();
            srvBootstrap.group(bossGrp, evLoopGrp)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        // on a new connection accept.
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast("client_req_splitter", new LineBasedFrameDecoder(1024));
                            socketChannel.pipeline().addLast("cmd_handler", new CommandHandler());
                            socketChannel.pipeline().addLast(new ChunkedWriteHandler());
                            socketChannel.pipeline().addLast("file_handler", new FileHandler(fileDir));
                        }
                    });

            logger.info("Starting the file server on port: " + PORT);
            channelFut = srvBootstrap.bind(PORT).syncUninterruptibly();
            channelFut.channel().closeFuture().syncUninterruptibly();
        } catch (Exception ie) {
            logger.severe(ie.getLocalizedMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        try {
            channelFut.addListener(ChannelFutureListener.CLOSE);
        } finally {
            logger.info("Finalizing...");
            bossGrp.shutdownGracefully();
            evLoopGrp.shutdownGracefully();
        }
    }

    /**
     * Performs basic check for arguments and file directory.
     *
     * @param args Command line arguments.
     * @return Directory as a {@link File}.
     * @throws Exception If failed.
     */
    static File checkCreateFileDir(String[] args) throws Exception {
        if (args.length == 0)
            throw new Exception("Provide the file directory as an argument");

        File fileDir = new File(args[0]); // ignores other args.
        if (!fileDir.exists())
            throw new Exception("No such a directory: " + args[0]);

        return fileDir;
    }
}
