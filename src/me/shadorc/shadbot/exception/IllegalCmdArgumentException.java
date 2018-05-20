package me.shadorc.shadbot.exception;

public class IllegalCmdArgumentException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public IllegalCmdArgumentException(String message) {
		super(message);
	}
}
