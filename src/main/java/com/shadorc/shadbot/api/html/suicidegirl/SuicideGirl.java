package com.shadorc.shadbot.api.html.suicidegirl;

import com.shadorc.shadbot.utils.RandUtil;
import com.shadorc.shadbot.utils.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class SuicideGirl {

    private final Element element;

    public SuicideGirl(Document document) {
        this.element = RandUtil.randValue(document.getElementsByTag("article"));
    }

    public String getName() {
        return StringUtil.capitalize(this.element.getElementsByTag("a")
                .attr("href")
                .split("/")[2]
                .trim());
    }

    public String getImageUrl() {
        return this.element.select("noscript")
                .attr("data-retina");
    }

    public String getUrl() {
        return this.element.getElementsByClass("facebook-share")
                .attr("href");
    }
}
