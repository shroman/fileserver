package org.example.files.server.request;

import java.io.Serializable;

/**
 * Client request.
 */
public class ClientRequest implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 0L;

    /**
     * Command type.
     */
    private final Command cmd;

    public Command getCmd() {
        return cmd;
    }

    public ClientRequest(Command cmd) {
        this.cmd = cmd;
    }
}
