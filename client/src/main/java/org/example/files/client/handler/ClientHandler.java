package org.example.files.client.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;

public class ClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
    /**
     * Logger.
     */
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

    protected static final String CMDLINE = "0+";
    protected static final String FILE = "1+";

    private OutputStream outputStream;
    private final String fileDir;
    private Path destFilePath;
    private long fileSizeTotal;
    private long fileSizeTransferred;
    private byte[] buffer = new byte[0];

    /**
     * Constructor.
     *
     * @param fileDir Directory to write files to.
     */
    public ClientHandler(String fileDir) {
        this.fileDir = fileDir;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        // check head.
        if (msg.readableBytes() >= 2) {
            ByteBuf sliceBuf = msg.readSlice(2);

            if (sliceBuf.toString(CharsetUtil.UTF_8).equals(FILE)) {
                destFilePath = filePath(msg, fileDir);
                fileSizeTotal = fileSize(msg);
            } else if (sliceBuf.toString(CharsetUtil.UTF_8).equals(CMDLINE)) {
                // NOOP, just to skip the header.
            } else {
                // next chunk
                msg.resetReaderIndex();
//                throw new Exception("Bad protocol");
                // TODO; no protection from server sending bad protocol responses.
            }
        }

        if (fileSizeTotal == 0) {
            String cmdMsg = msg.toString(CharsetUtil.UTF_8);

            if (cmdMsg.isEmpty())
                System.out.print(">> ");
            else
                System.out.println(cmdMsg);
            return;
        }

        if (this.outputStream == null)
            this.outputStream = initStream(destFilePath);

        int size = msg.readableBytes();
        if (size > this.buffer.length)
            this.buffer = new byte[size + 2];

        msg.readBytes(this.buffer, 0, size);
        this.outputStream.write(this.buffer, 0, size);

        // since we go with LineBasedFrameDecoder, that removes /r/n and read line by line
        this.outputStream.write("\r\n".getBytes(), 0, 2);
        fileSizeTransferred += size + 2; // 2 byte for /r/n

//        System.out.println("Transferred: " + fileSizeTransferred + "/" + fileSizeTotal);

        if (fileSizeTransferred == fileSizeTotal) {
            fileSizeTotal = fileSizeTransferred = 0;
            this.outputStream = null;
            System.out.print(">> ");
        }
    }

    /**
     * Extracts file size.
     */
    long fileSize(ByteBuf msg) throws Exception {
        int idx = msg.indexOf(msg.readerIndex(), msg.writerIndex(), (byte) '+');

        if (idx <= 0)
            throw new Exception("Illegal protocol format");

        String fSize = msg.readSlice(idx - msg.readerIndex() + 1).toString(CharsetUtil.UTF_8); // with +
        return Long.valueOf(fSize.substring(0, fSize.length() - 1));
    }

    /**
     * Extracts file name and forms the path to store.
     */
    Path filePath(ByteBuf msg, String dir) throws Exception {
        int idx = msg.indexOf(msg.readerIndex(), msg.writerIndex(), (byte) '+');

        if (idx <= 0)
            throw new Exception("Illegal protocol format");

        if (dir == null || dir.isEmpty())
            throw new Exception("No directory");

        String fName = msg.readSlice(idx - msg.readerIndex() + 1).toString(CharsetUtil.UTF_8); // with +
        return Paths.get(dir, fName.substring(0, fName.length() - 1));
    }

    /**
     * Initializes an output stream.
     *
     * @throws IOException If failed.
     */
    OutputStream initStream(Path filePath) throws IOException {
        try {
            Files.createDirectories(filePath.getParent());
        } catch (FileAlreadyExistsException faee) {
            // only for IDE.
            logger.info("Directory exists: " + filePath.getParent().toString());
        }
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        System.out.println("OK: writing to " + filePath);

        return Files.newOutputStream(filePath, StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        logger.warning(cause.getLocalizedMessage());
        ctx.close();
    }
}
