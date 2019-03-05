package me.shadorc.shadbot.command.utils.poll;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;

import discord4j.core.object.reaction.ReactionEmoji;

public class PollCreateSpec {

	private final Duration duration;
	private final String question;
	private final Map<String, ReactionEmoji> choices;

	public PollCreateSpec(Duration duration, String question, Map<String, ReactionEmoji> choices) {
		this.duration = duration;
		this.question = question;
		this.choices = choices;
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
		return this.getChoices().values();
	}

}
