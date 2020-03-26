package com.shadorc.shadbot.api.json.image.deviantart;

import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.util.annotation.Nullable;

import java.util.Optional;

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

    public Optional<Content> getContent() {
        return Optional.ofNullable(this.content);
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

    @Override
    public String toString() {
        return "Image{" +
                "content=" + this.content +
                ", author=" + this.author +
                ", url='" + this.url + '\'' +
                ", title='" + this.title + '\'' +
                ", categoryPath='" + this.categoryPath + '\'' +
                '}';
    }
}
