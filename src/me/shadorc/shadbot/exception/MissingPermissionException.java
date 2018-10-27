package me.shadorc.shadbot.exception;

import discord4j.core.object.util.Permission;

public class MissingPermissionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public enum UserType {
		NORMAL, BOT;
	}

	private final UserType type;
	private final Permission permission;

	public MissingPermissionException(UserType type, Permission permission) {
		super("Missing Permission Exception", null, false, false);
		this.type = type;
		this.permission = permission;
	}

	public UserType getType() {
		return this.type;
	}

	public Permission getPermission() {
		return this.permission;
	}

}
