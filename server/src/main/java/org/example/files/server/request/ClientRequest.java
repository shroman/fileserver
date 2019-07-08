package org.example.files.server.request;

import java.io.Serializable;

public class ClientRequest implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 0L;

    private final Command cmd;

    public Command getCmd() {
        return cmd;
    }

    public ClientRequest(Command cmd) {
        this.cmd = cmd;
    }
}
