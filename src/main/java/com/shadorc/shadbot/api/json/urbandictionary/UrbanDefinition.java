package com.shadorc.shadbot.api.json.urbandictionary;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shadorc.shadbot.utils.StringUtil;

public class UrbanDefinition {

    @JsonProperty("definition")
    private String definition;
    @JsonProperty("example")
    private String example;
    @JsonProperty("word")
    private String word;
    @JsonProperty("permalink")
    private String permalink;

    public String getDefinition() {
        return StringUtil.remove(this.definition, "[", "]");
    }

    public String getExample() {
        return StringUtil.remove(this.example, "[", "]");
    }

    public String getWord() {
        return this.word;
    }

    public String getPermalink() {
        return this.permalink;
    }

    @Override
    public String toString() {
        return "UrbanDefinition{" +
                "definition='" + this.definition + '\'' +
                ", example='" + this.example + '\'' +
                ", word='" + this.word + '\'' +
                ", permalink='" + this.permalink + '\'' +
                '}';
    }
}
