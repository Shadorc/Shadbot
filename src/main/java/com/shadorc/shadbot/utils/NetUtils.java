package com.shadorc.shadbot.utils;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.shadorc.shadbot.Config;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClient.RequestSender;
import reactor.netty.http.client.HttpClientResponse;

import java.io.IOException;
import java.net.URL;
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
     * @param urlString - a string representing an URL to check
     * @return true if the string is a valid and reachable URL, false otherwise
     */
    public static Mono<Boolean> isValidUrl(String urlString) {
        try {
            new URL(urlString).toURI();
        } catch (final Exception ignored) {
            return Mono.just(false);
        }

        return NetUtils.request(HttpMethod.GET, urlString)
                .response()
                .timeout(Config.DEFAULT_TIMEOUT)
                .map(HttpClientResponse::status)
                .map(HttpResponseStatus::code)
                .map(statusCode -> statusCode >= 100 && statusCode < 400)
                .onErrorResume(ignored -> Mono.just(false));
    }

    private static <T> Mono<T> handleResponse(HttpClientResponse resp, ByteBufMono body, JavaType type) {
        final int statusCode = resp.status().code();
        if (statusCode / 100 != 2 && statusCode != 404) {
            return body.asString()
                    .flatMap(err -> Mono.error(new IOException(String.format("%s %s failed (%d) %s",
                            resp.method().asciiName(), resp.uri(), statusCode, err))));
        }
        if (!resp.responseHeaders().get(HttpHeaderNames.CONTENT_TYPE).startsWith(HttpHeaderValues.APPLICATION_JSON.toString())) {
            return body.asString()
                    .flatMap(err -> Mono.error(new IOException(String.format("%s %s wrong header (%s) %s",
                            resp.method().asciiName(), resp.uri(), resp.responseHeaders(), err))));
        }

        return body.asInputStream()
                .flatMap(input -> Mono.fromCallable(() -> Utils.MAPPER.readValue(input, type)));
    }

    public static RequestSender request(Consumer<HttpHeaders> headerBuilder, HttpMethod method, String url) {
        return HTTP_CLIENT
                .headers(headerBuilder.andThen(header -> header.add(HttpHeaderNames.USER_AGENT, Config.USER_AGENT)))
                .request(method)
                .uri(url);
    }

    public static RequestSender request(HttpMethod method, String url) {
        return NetUtils.request(ignored -> {
        }, method, url);
    }

    public static <T> Mono<T> get(Consumer<HttpHeaders> headerBuilder, String url, JavaType type) {
        return NetUtils.request(headerBuilder, HttpMethod.GET, url)
                .<T>responseSingle((resp, body) -> NetUtils.handleResponse(resp, body, type))
                .timeout(Config.DEFAULT_TIMEOUT);
    }

    public static <T> Mono<T> get(Consumer<HttpHeaders> headerBuilder, String url, Class<? extends T> type) {
        return NetUtils.get(headerBuilder, url, TypeFactory.defaultInstance().constructType(type));
    }

    public static <T> Mono<T> get(String url, JavaType type) {
        return NetUtils.get(ignored -> {
        }, url, type);
    }

    public static <T> Mono<T> get(String url, Class<? extends T> type) {
        return NetUtils.get(url, TypeFactory.defaultInstance().constructType(type));
    }

    public static Mono<String> get(String url) {
        return NetUtils.request(HttpMethod.GET, url)
                .responseSingle((resp, body) -> body.asString(StandardCharsets.UTF_8))
                .timeout(Config.DEFAULT_TIMEOUT);
    }

    public static Mono<HttpClientResponse> post(String url, String authorization, Object payload) {
        final Consumer<HttpHeaders> headerBuilder = header -> header.add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                .add(HttpHeaderNames.AUTHORIZATION, authorization);

        return NetUtils.request(headerBuilder, HttpMethod.POST, url)
                .send(Mono.just(Unpooled.wrappedBuffer(payload.toString().getBytes(StandardCharsets.UTF_8))))
                .response()
                .timeout(Config.DEFAULT_TIMEOUT);
    }

}