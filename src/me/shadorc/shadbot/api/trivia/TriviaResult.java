package me.shadorc.shadbot.api.trivia;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.List;
import java.util.stream.Collectors;

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

}
