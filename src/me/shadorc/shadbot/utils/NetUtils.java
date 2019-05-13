package me.shadorc.shadbot.utils;

import com.fasterxml.jackson.databind.JavaType;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import me.shadorc.shadbot.Config;
import org.apache.http.HttpStatus;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import reactor.core.Exceptions;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class NetUtils {

    private static final HttpClient HTTP_CLIENT = HttpClient.create();

    /**
     * @param html - The HTML to convert to text
     * @return html converted to text with new lines preserved
     */
    public static String br2nl(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        final Document document = Jsoup.parse(html);
        document.outputSettings(new Document.OutputSettings().prettyPrint(false));// makes html() preserve linebreak and spacing
        document.select("br").append("\\n");
        document.select("p").prepend("\\n\\n");
        final String str = document.html().replaceAll("\\\\n", "\n");
        return Jsoup.clean(str, "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false));
    }

    /**
     * @param str - the string to encode as UTF-8
     * @return The string encoded as UTF-8
     */
    public static String encode(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return URLEncoder.encode(str, StandardCharsets.UTF_8);
    }

    /**
     * @param url - a string representing an URL to check
     * @return true if the string is a valid and reachable URL, false otherwise
     */
    public static boolean isValidUrl(String url) {
        try {
            HTTP_CLIENT.get()
                    .uri(url)
                    .response()
                    .timeout(Config.DEFAULT_TIMEOUT)
                    .block();
            return true;
        } catch (final Exception err) {
            return false;
        }
    }

    public static Tuple2<HttpClientResponse, String> getResponseSingle(String url, Consumer<HttpHeaders> headerBuilder) {
        return HTTP_CLIENT
                .headers(headerBuilder
                        .andThen(header -> header.add(HttpHeaderNames.USER_AGENT, Config.USER_AGENT)))
                .get()
                .uri(url)
                .responseSingle((response, content) -> content.asString(StandardCharsets.UTF_8)
                        .map(body -> Tuples.of(response, body)))
                .timeout(Config.DEFAULT_TIMEOUT)
                .block();
    }

    public static Tuple2<HttpClientResponse, String> getResponseSingle(String url) {
        return NetUtils.getResponseSingle(url, header -> {
        });
    }

    public static HttpClientResponse getResponse(String url) {
        return NetUtils.getResponseSingle(url).getT1();
    }

    public static String getBody(String url) {
        return NetUtils.getResponseSingle(url).getT2();
    }

    public static Document getDocument(String url) {
        return Jsoup.parse(NetUtils.getBody(url));
    }

    public static <T> T readValue(String url, Class<T> type) {
        try {
            return Utils.MAPPER.readValue(NetUtils.getJSON(url), type);
        } catch (final IOException err) {
            throw Exceptions.propagate(err);
        }
    }

    public static <T> T readValue(String url, JavaType type) {
        try {
            return Utils.MAPPER.readValue(NetUtils.getJSON(url), type);
        } catch (final IOException err) {
            throw Exceptions.propagate(err);
        }
    }

    public static String getJSON(String url) throws HttpStatusException {
        final Tuple2<HttpClientResponse, String> responseSingle = NetUtils.getResponseSingle(url);
        final HttpClientResponse response = responseSingle.getT1();
        final String content = responseSingle.getT2();
        if (!response.responseHeaders().get(HttpHeaderNames.CONTENT_TYPE).startsWith(HttpHeaderValues.APPLICATION_JSON.toString())
                || !response.status().equals(HttpResponseStatus.OK)) {
            throw new HttpStatusException(String.format("Invalid JSON:%nURL: %s%nStatus: %s%nHeaders: %s%nContent: %s",
                    url, response.status(), response.responseHeaders(), content),
                    HttpStatus.SC_SERVICE_UNAVAILABLE, url);
        }
        return content;
    }
}
