package com.shadorc.shadbot.object.help;

public class Argument {

    private final String name;
    private final String description;
    private final boolean isOptional;

    protected Argument(String name, String description, boolean isOptional) {
        this.name = name;
        this.description = description;
        this.isOptional = isOptional;
    }

    protected String getName() {
        return this.name;
    }

    protected String getDescription() {
        return this.description;
    }

    protected boolean isOptional() {
        return this.isOptional;
    }

}
