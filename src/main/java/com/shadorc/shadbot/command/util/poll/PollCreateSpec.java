package com.shadorc.shadbot.command.util.poll;

import discord4j.core.object.reaction.ReactionEmoji;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class PollCreateSpec {

    private final Duration duration;
    private final String question;
    private final Map<String, ReactionEmoji> choices;

    public PollCreateSpec(Duration duration, String question, Map<String, ReactionEmoji> choices) {
        this.duration = duration;
        this.question = question;
        this.choices = Collections.unmodifiableMap(choices);
    }

    public Duration getDuration() {
        return this.duration;
    }

    public String getQuestion() {
        return this.question;
    }

    public Map<String, ReactionEmoji> getChoices() {
        return this.choices;
    }

    public Collection<ReactionEmoji> getReactions() {
        return this.choices.values();
    }

}
