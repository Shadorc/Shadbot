package com.shadorc.shadbot.music;

import java.io.Serial;

public class NoMusicException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public NoMusicException() {
        super("No Music Exception", null, false, false);
    }

}
