package me.shadorc.shadbot.exception;

public class MissingPermissionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public MissingPermissionException() {
		super("Missing Permission Exception", null, false, false);
	}

}
