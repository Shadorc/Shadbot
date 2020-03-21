package com.shadorc.shadbot.exception;

import discord4j.rest.util.Permission;

public class MissingPermissionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Permission permission;

    public MissingPermissionException(Permission permission) {
        super("Missing Permission Exception", null, false, false);
        this.permission = permission;
    }

    public Permission getPermission() {
        return this.permission;
    }

}
