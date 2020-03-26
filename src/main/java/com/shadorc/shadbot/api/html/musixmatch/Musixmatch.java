package com.shadorc.shadbot.api.html.musixmatch;

import com.shadorc.shadbot.utils.NetUtils;
import discord4j.core.object.Embed;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;

public class Musixmatch {

    private static final int MAX_LYRICS_LENGTH = Embed.MAX_DESCRIPTION_LENGTH / 3;

    private final Document document;

    public Musixmatch(Document document) {
        this.document = document;
    }

    public String getArtist() {
        return this.document.getElementsByClass("mxm-track-title__artist").text();
    }

    public String getTitle() {
        return StringUtils.remove(this.document.getElementsByClass("mxm-track-title__track ").text(), "Lyrics");
    }

    public String getImageUrl() {
        return String.format("https:%s",
                this.document.getElementsByClass("banner-album-image").select("img").first().attr("src"));
    }

    public String getLyrics() {
        return StringUtils.abbreviate(
                NetUtils.cleanWithLinebreaks(this.document.getElementsByClass("mxm-lyrics__content ").html()), MAX_LYRICS_LENGTH);
    }

}
