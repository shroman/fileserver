package org.example.files.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.util.CharsetUtil;
import org.example.files.client.handler.ClientHandler;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

/**
 * Basic client for the file server.
 */
public class FileClient implements Runnable {
    /**
     * Logger.
     */
    private static final Logger logger = Logger.getLogger(FileClient.class.getName());

    /**
     * Server's host.
     */
    private final String host;

    /**
     * Server's port.
     */
    private final int port;

    private ClientHandler msgHandler;

    private final EventLoopGroup evLoopGrp = new NioEventLoopGroup();

    private ChannelFuture channelF;

    /**
     * Constructor.
     *
     * @param fileDir Directory to write files to.
     */
    public FileClient(String host, int port, String fileDir) {
        this.host = host;
        this.port = port;
        msgHandler = new ClientHandler(fileDir);
    }

    @Override
    public void run() {
        try {
            Bootstrap clientBootstrap = new Bootstrap();
            clientBootstrap.group(evLoopGrp);
            clientBootstrap.channel(NioSocketChannel.class);
            clientBootstrap.option(ChannelOption.TCP_NODELAY, true);
            clientBootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            clientBootstrap.handler(
                    new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
//                            socketChannel.pipeline().addLast(new LoggingHandler(LogLevel.WARN));
                            socketChannel.pipeline().addLast("client_req_splitter", new LineBasedFrameDecoder(1024));
                            socketChannel.pipeline().addLast(msgHandler);
                        }
                    });

            channelF = clientBootstrap.connect(new InetSocketAddress(host, port))
                    .syncUninterruptibly();
            channelF.channel().closeFuture().addListener(
                    (ChannelFutureListener) future -> {
                        // on termination from the server (QUIT command)
                        stop();
                    }
            ).syncUninterruptibly();
        } catch (Exception e) {
            throw e;
        }
    }

    public void stop() {
        try {
            channelF.addListener(ChannelFutureListener.CLOSE);
        } finally {
            logger.info("Shutting down");
            evLoopGrp.shutdownGracefully();
        }
    }

    public void writeMessage(String msg) throws Exception {
        channelF.channel().writeAndFlush(Unpooled.copiedBuffer(msg + "\r\n", CharsetUtil.UTF_8));

        if (!channelF.sync().isSuccess())
            logger.severe("Failed to send: " + channelF.cause());
    }
}
