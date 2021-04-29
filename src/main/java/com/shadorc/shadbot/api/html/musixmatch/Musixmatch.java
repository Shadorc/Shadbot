package com.shadorc.shadbot.api.html.musixmatch;

import com.shadorc.shadbot.utils.NetUtil;
import com.shadorc.shadbot.utils.StringUtil;
import org.jsoup.nodes.Document;

public record Musixmatch(Document document, String url) {

    public String getArtist() {
        return this.document.getElementsByClass("mxm-track-title__artist").text();
    }

    public String getTitle() {
        return StringUtil.remove(this.document.getElementsByClass("mxm-track-title__track ").text(), "Lyrics");
    }

    public String getImageUrl() {
        return "https:%s"
                .formatted(this.document.getElementsByClass("banner-album-image")
                        .select("img")
                        .first()
                        .attr("src"));
    }

    public String getLyrics() {
        return NetUtil.cleanWithLinebreaks(this.document.getElementsByClass("mxm-lyrics__content ").html());
    }

}
