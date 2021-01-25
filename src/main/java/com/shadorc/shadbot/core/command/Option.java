package com.shadorc.shadbot.core.command;

import discord4j.rest.util.ApplicationCommandOptionType;

public class Option {

    private final String name;
    private final String description;
    private final boolean isRequired;
    private final int type;

    public Option(String name, String description, boolean isRequired, ApplicationCommandOptionType type) {
        this.name = name;
        this.description = description;
        this.isRequired = isRequired;
        this.type = type.getValue();
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public boolean isRequired() {
        return this.isRequired;
    }

    public int getType() {
        return this.type;
    }

}
