package com.shadorc.shadbot.core.command;

import discord4j.rest.util.ApplicationCommandOptionType;

public abstract class SubCmd extends Cmd {

    private final GroupCmd groupCmd;

    protected SubCmd(GroupCmd groupCmd, CommandCategory category, String name, String description) {
        super(category, name, description);
        this.groupCmd = groupCmd;
    }

    protected SubCmd(GroupCmd groupCmd, CommandCategory category, CommandPermission permission, String name,
                     String description) {
        super(category, permission, name, description);
        this.groupCmd = groupCmd;
    }

    protected SubCmd(GroupCmd groupCmd, CommandCategory category, CommandPermission permission, String name,
                     String description, ApplicationCommandOptionType type) {
        super(category, permission, name, description, type);
        this.groupCmd = groupCmd;
    }

    public GroupCmd getGroupCmd() {
        return this.groupCmd;
    }

    public String getFullName() {
        return "%s %s".formatted(this.groupCmd.getName(), this.getName());
    }

}
