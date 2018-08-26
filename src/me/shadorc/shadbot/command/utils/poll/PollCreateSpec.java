package me.shadorc.shadbot.command.utils.poll;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import discord4j.core.object.reaction.ReactionEmoji;

public class PollCreateSpec {

	private final int duration;
	private final String question;
	private final List<String> choices;
	private final List<ReactionEmoji> reactions;

	public PollCreateSpec(int duration, String question, Set<String> choices, List<ReactionEmoji> reactions) {
		this.duration = duration;
		this.question = question;
		this.choices = new ArrayList<>(choices);
		this.reactions = reactions;
	}

	public int getDuration() {
		return duration;
	}

	public String getQuestion() {
		return question;
	}

	public List<String> getChoices() {
		return choices;
	}

	public List<ReactionEmoji> getReactions() {
		return reactions;
	}

}
