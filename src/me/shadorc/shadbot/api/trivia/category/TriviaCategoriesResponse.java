
package me.shadorc.shadbot.api.trivia.category;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TriviaCategoriesResponse {

	@JsonProperty("trivia_categories")
	private List<TriviaCategory> categories;

	public List<TriviaCategory> getCategories() {
		return categories;
	}

	public List<Integer> getIds() {
		return this.getCategories().stream().map(TriviaCategory::getId).collect(Collectors.toList());
	}

	public List<String> getNames() {
		return this.getCategories().stream().map(TriviaCategory::getName).collect(Collectors.toList());
	}

	@Override
	public String toString() {
		return String.format("TriviaCategoriesResponse [categories=%s]", categories);
	}

}
