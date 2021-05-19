package com.locibot.locibot.api.json.trivia;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.List;

public record TriviaResult(String category,
                           String type,
                           String difficulty,
                           String question,
                           @JsonProperty("correct_answer") String correctAnswer,
                           @JsonProperty("incorrect_answers") List<String> incorrectAnswers) {

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
                .toList();
    }

}
