package com.shadorc.shadbot.api.json.xkcd;

import com.fasterxml.jackson.annotation.JsonProperty;

public class XkcdResponse {

    @JsonProperty("num")
    private int num;
    @JsonProperty("title")
    private String title;
    @JsonProperty("img")
    private String img;

    public int getNum() {
        return this.num;
    }

    public String getTitle() {
        return this.title;
    }

    public String getImg() {
        return this.img;
    }

    @Override
    public String toString() {
        return "XkcdResponse{" +
                "num=" + this.num +
                ", title='" + this.title + '\'' +
                ", img='" + this.img + '\'' +
                '}';
    }
}
