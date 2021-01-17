package com.shadorc.shadbot.command;

import java.io.Serial;

public class MissingArgumentException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public MissingArgumentException() {
        super("Missing Argument Exception", null, false, false);
    }

}
