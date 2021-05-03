package com.shadorc.shadbot.api.html.Genius;

import com.shadorc.shadbot.utils.NetUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Lyrics {

    private final Document document;

    public Lyrics(Document document) {
        this.document = document;
    }

    public String getText() {
        return NetUtil.cleanWithLinebreaks(this.document.html());
    }

}
