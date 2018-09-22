package me.shadorc.shadbot.api.trivia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TriviaResult {

	@JsonProperty("category")
	private String category;
	@JsonProperty("type")
	private String type;
	@JsonProperty("difficulty")
	private String difficulty;
	@JsonProperty("question")
	private String question;
	@JsonProperty("correct_answer")
	private String correctAnswer;
	@JsonProperty("incorrect_answers")
	private List<String> incorrectAnswers;

	public String getCategory() {
		return this.category;
	}

	public String getType() {
		return this.type;
	}

	public String getDifficulty() {
		return this.difficulty;
	}

	public String getQuestion() {
		return Jsoup.parse(this.question).text();
	}

	public String getCorrectAnswer() {
		return Jsoup.parse(this.correctAnswer).text();
	}

	public List<String> getIncorrectAnswers() {
		return this.incorrectAnswers.stream()
				.map(Jsoup::parse)
				.map(Document::text)
				.collect(Collectors.toList());
	}

	public List<String> getAnswers() {
		final List<String> answers = new ArrayList<>();
		if(this.getType().equals("multiple")) {
			answers.addAll(this.getIncorrectAnswers());
			answers.add(this.getCorrectAnswer());
			Collections.shuffle(answers);
		} else {
			answers.addAll(List.of("True", "False"));
		}
		return answers;
	}

	@Override
	public String toString() {
		return String.format("TriviaResult [category=%s, type=%s, difficulty=%s, question=%s, correctAnswer=%s, incorrectAnswers=%s]",
				this.category, this.type, this.difficulty, this.question, this.correctAnswer, this.incorrectAnswers);
	}

}
