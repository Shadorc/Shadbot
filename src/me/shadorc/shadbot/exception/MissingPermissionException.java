package me.shadorc.shadbot.exception;

import discord4j.core.object.util.Permission;

public class MissingPermissionException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public enum Type {
		USER, BOT;
	}
	
	private final Type type;
	private final Permission permission;

	public MissingPermissionException(Type type, Permission permission) {
		super("Missing Permission Exception", null, false, false);
		this.type = type;
		this.permission = permission;
	}
	
	public Type getType() {
		return this.type;
	}
	
	public Permission getPermission() {
		return this.permission;
	}

}
