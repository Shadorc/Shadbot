package com.shadorc.shadbot.command;

public class MissingArgumentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MissingArgumentException() {
        super("Missing Argument Exception", null, false, false);
    }

}
