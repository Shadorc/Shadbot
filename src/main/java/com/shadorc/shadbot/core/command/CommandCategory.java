package com.shadorc.shadbot.core.command;

public enum CommandCategory {

    DONATOR("Donator"),
    UTILS("Utility"),
    FUN("Fun"),
    IMAGE("Image"),
    GAME("Game"),
    CURRENCY("Currency"),
    MUSIC("Music"),
    GAMESTATS("Game Stats"),
    INFO("Info"),
    MODERATION("Moderation"),
    OWNER("Owner"),
    SETTING("Setting");

    private final String name;

    CommandCategory(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
