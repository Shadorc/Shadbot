package me.shadorc.shadbot.api.image.deviantart;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Image {

	@Nullable
	@JsonProperty("content")
	private Content content;
	@JsonProperty("author")
	private Author author;
	@JsonProperty("url")
	private String url;
	@JsonProperty("title")
	private String title;
	@JsonProperty("category_path")
	private String categoryPath;

	public Content getContent() {
		return this.content;
	}

	public Author getAuthor() {
		return this.author;
	}

	public String getUrl() {
		return this.url;
	}

	public String getTitle() {
		return this.title;
	}

	public String getCategoryPath() {
		return this.categoryPath;
	}

}
