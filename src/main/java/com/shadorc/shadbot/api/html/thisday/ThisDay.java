package com.shadorc.shadbot.api.html.thisday;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ThisDay {

    private static final Pattern TAG_PATTERN = Pattern.compile("<a href=\"/events/date/[0-9]+\" class=\"date\">");

    private final Document document;

    public ThisDay(Document document) {
        this.document = document;
    }

    public String getDate() {
        return this.document.getElementsByClass("date-large")
                .first()
                .attr("datetime");
    }

    public String getEvents() {
        return this.document.getElementsByClass("event-list event-list--with-advert")
                .stream()
                .map(element -> element.getElementsByClass("event"))
                .flatMap(Elements::stream)
                .map(Element::html)
                .map(html -> TAG_PATTERN.matcher(html).replaceFirst("**"))
                .map(html -> html.replaceFirst(Pattern.quote("</a>"), "**"))
                .map(Jsoup::parse)
                .map(Document::text)
                .collect(Collectors.joining("\n\n"));
    }

    @Override
    public String toString() {
        return "ThisDay{" +
                "date='" + this.getDate() + '\'' +
                ", events='" + this.getEvents() + '\'' +
                '}';
    }
}
