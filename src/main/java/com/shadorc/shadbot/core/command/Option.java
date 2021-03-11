package com.shadorc.shadbot.core.command;

import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.rest.util.ApplicationCommandOptionType;

import java.util.Collections;
import java.util.List;

public class Option {

    private final String name;
    private final String description;
    private final boolean isRequired;
    private final int type;
    private final List<ApplicationCommandOptionChoiceData> choices;

    public Option(String name, String description, boolean isRequired, ApplicationCommandOptionType type) {
        this(name, description, isRequired, type, Collections.emptyList());
    }

    public Option(String name, String description, boolean isRequired, ApplicationCommandOptionType type,
                  List<ApplicationCommandOptionChoiceData> choices) {
        this.name = name;
        this.description = description;
        this.isRequired = isRequired;
        this.type = type.getValue();
        this.choices = choices;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public int getType() {
        return this.type;
    }

    public boolean isRequired() {
        return this.isRequired;
    }

    public List<ApplicationCommandOptionChoiceData> getChoices() {
        return this.choices;
    }
}
