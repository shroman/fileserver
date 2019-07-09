package org.example.files.server.request;

/**
 * File request.
 */
public class FileClientRequest extends ClientRequest {
    private String fileName;

    public FileClientRequest(Command cmd, String fileName) {
        super(cmd);
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
