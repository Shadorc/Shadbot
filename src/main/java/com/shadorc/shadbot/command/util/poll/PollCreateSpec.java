package com.shadorc.shadbot.command.util.poll;

import discord4j.core.object.reaction.ReactionEmoji;

import java.time.Duration;
import java.util.Map;

public record PollCreateSpec(Duration duration,
                             String question,
                             Map<String, ReactionEmoji> choices) {

}
