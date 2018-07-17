package me.shadorc.shadbot.api.trivia;

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
		return category;
	}

	public String getType() {
		return type;
	}

	public String getDifficulty() {
		return difficulty;
	}

	public String getQuestion() {
		return Jsoup.parse(question).text();
	}

	public String getCorrectAnswer() {
		return Jsoup.parse(correctAnswer).text();
	}

	public List<String> getIncorrectAnswers() {
		return incorrectAnswers.stream()
				.map(Jsoup::parse)
				.map(Document::text)
				.collect(Collectors.toList());
	}

}
