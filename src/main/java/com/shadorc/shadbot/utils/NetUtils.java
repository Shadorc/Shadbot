package com.shadorc.shadbot.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.shadorc.shadbot.api.HeaderException;
import com.shadorc.shadbot.api.ServerAccessException;
import com.shadorc.shadbot.data.Config;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClient.RequestSender;
import reactor.netty.http.client.HttpClientResponse;
import reactor.util.annotation.Nullable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class NetUtils {

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    private static final HttpClient HTTP_CLIENT = HttpClient.create()
            .followRedirect(true);

    // Source: https://urlregex.com/
    private static final Pattern URL_MATCH = Pattern.compile(
            "((https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])");

    /**
     * @param str The string to check.
     * @return {@code true} if the string is a valid URL, {@code false} otherwise.
     */
    public static boolean isUrl(String str) {
        return URL_MATCH.matcher(str).matches();
    }

    /**
     * @param html The HTML to convert to text with new lines preserved, may be {@code null}.
     * @return The provided HTML converted to text with new lines preserved or {@code null} if null string input.
     */
    @Nullable
    public static String cleanWithLinebreaks(@Nullable String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        final Document document = Jsoup.parse(html);
        // Makes html() preserve linebreak and spacing
        document.outputSettings(new Document.OutputSettings().prettyPrint(false));
        document.select("br").append("\\n");
        document.select("p").prepend("\\n\\n");
        final String str = document.html().replace("\\\\n", "\n");
        return Jsoup.clean(str, "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false));
    }

    /**
     * @param str The string to encode as UTF-8, may be {@code null}.
     * @return The string encoded as UTF-8 or {@code null} if null string input.
     */
    @Nullable
    public static String encode(@Nullable String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return URLEncoder.encode(str, StandardCharsets.UTF_8);
    }

    private static <T> Mono<T> handleResponse(HttpClientResponse resp, ByteBufMono body, JavaType type) {
        final int statusCode = resp.status().code();
        if (statusCode / 100 != 2 && statusCode != 404) {
            return body.asString()
                    .defaultIfEmpty("Empty body")
                    .flatMap(err -> Mono.error(new ServerAccessException(resp, err)));
        }
        if (!resp.responseHeaders().get(HttpHeaderNames.CONTENT_TYPE).startsWith(HttpHeaderValues.APPLICATION_JSON.toString())) {
            return body.asString()
                    .defaultIfEmpty("Empty body")
                    .flatMap(err -> Mono.error(new HeaderException(resp, err)));
        }

        return body.asInputStream()
                .flatMap(input -> Mono.fromCallable(() -> NetUtils.MAPPER.readValue(input, type)));
    }

    public static RequestSender request(Consumer<HttpHeaders> headerBuilder, HttpMethod method, String url) {
        return HTTP_CLIENT
                .headers(headerBuilder.andThen(header -> header.add(HttpHeaderNames.USER_AGENT, Config.USER_AGENT)))
                .request(method)
                .uri(url);
    }

    public static RequestSender request(HttpMethod method, String url) {
        return NetUtils.request(spec -> {}, method, url);
    }

    public static Mono<String> post(Consumer<HttpHeaders> headerBuilder, String url, String content) {
        return NetUtils.request(headerBuilder, HttpMethod.POST, url)
                .send((req, res) -> res.sendString(Mono.just(content), StandardCharsets.UTF_8))
                .responseSingle((res, con) -> con.asString(StandardCharsets.UTF_8))
                .timeout(Config.TIMEOUT);
    }

    public static <T> Mono<T> get(Consumer<HttpHeaders> headerBuilder, String url, JavaType type) {
        return NetUtils.request(headerBuilder, HttpMethod.GET, url)
                .<T>responseSingle((resp, body) -> NetUtils.handleResponse(resp, body, type))
                .timeout(Config.TIMEOUT);
    }

    public static <T> Mono<T> get(Consumer<HttpHeaders> headerBuilder, String url, Class<? extends T> type) {
        return NetUtils.get(headerBuilder, url, TypeFactory.defaultInstance().constructType(type));
    }

    public static <T> Mono<T> get(String url, JavaType type) {
        return NetUtils.get(spec -> {}, url, type);
    }

    public static <T> Mono<T> get(String url, Class<? extends T> type) {
        return NetUtils.get(url, TypeFactory.defaultInstance().constructType(type));
    }

    public static Mono<String> get(Consumer<HttpHeaders> headerBuilder, String url) {
        return NetUtils.request(headerBuilder, HttpMethod.GET, url)
                .responseSingle((resp, body) -> body.asString(StandardCharsets.UTF_8))
                .timeout(Config.TIMEOUT);
    }

    public static Mono<String> get(String url) {
        return NetUtils.get(spec -> {}, url);
    }

}
