package me.shadorc.shadbot.command.utils.poll;

import java.util.List;

public class PollCreateSpec {

	private int duration;
	private String question;
	private List<String> choices;

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public void setChoices(List<String> choices) {
		this.choices = choices;
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

}
