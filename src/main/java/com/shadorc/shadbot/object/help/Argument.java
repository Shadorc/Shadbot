package com.shadorc.shadbot.object.help;

public class Argument {

    private final String name;
    private final String description;
    private final boolean isFacultative;

    protected Argument(String name, String description, boolean isFacultative) {
        this.name = name;
        this.description = description;
        this.isFacultative = isFacultative;
    }

    protected String getName() {
        return this.name;
    }

    protected String getDescription() {
        return this.description;
    }

    protected boolean isFacultative() {
        return this.isFacultative;
    }

}
