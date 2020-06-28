package com.shadorc.shadbot.api.html.musixmatch;

import com.shadorc.shadbot.utils.NetUtils;
import com.shadorc.shadbot.utils.StringUtils;
import org.jsoup.nodes.Document;

public class Musixmatch {

    private final Document document;
    private final String url;

    public Musixmatch(Document document, String url) {
        this.document = document;
        this.url = url;
    }

    public String getUrl() {
        return this.url;
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
        return NetUtils.cleanWithLinebreaks(this.document.getElementsByClass("mxm-lyrics__content ").html());
    }

}
