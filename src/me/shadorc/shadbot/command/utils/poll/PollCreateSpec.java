package me.shadorc.shadbot.command.utils.poll;

import java.util.Map;

import discord4j.core.object.reaction.ReactionEmoji;

public class PollCreateSpec {

	private final int duration;
	private final String question;
	private final Map<String, ReactionEmoji> choices;

	public PollCreateSpec(int duration, String question, Map<String, ReactionEmoji> choices) {
		this.duration = duration;
		this.question = question;
		this.choices = choices;
	}

	public int getDuration() {
		return duration;
	}

	public String getQuestion() {
		return question;
	}

	public Map<String, ReactionEmoji> getChoices() {
		return choices;
	}

}
