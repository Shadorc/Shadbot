package me.shadorc.shadbot.api.image.deviantart;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResultResponse {

	@JsonProperty("content")
	private ContentResponse content;
	@JsonProperty("author")
	private AuthorResponse author;
	@JsonProperty("url")
	private String url;
	@JsonProperty("title")
	private String title;
	@JsonProperty("category_path")
	private String categoryPath;

	public ContentResponse getContent() {
		return content;
	}

	public AuthorResponse getAuthor() {
		return author;
	}

	public String getUrl() {
		return url;
	}

	public String getTitle() {
		return title;
	}

	public String getCategoryPath() {
		return categoryPath;
	}

}
