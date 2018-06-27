package me.shadorc.shadbot.exception;

public class NoMusicException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public NoMusicException() {
		super("No Music Exception", null, false, false);
	}

}
