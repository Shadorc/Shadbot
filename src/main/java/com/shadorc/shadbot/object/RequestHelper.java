package com.shadorc.shadbot.object;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.shadorc.shadbot.api.HeaderException;
import com.shadorc.shadbot.api.ServerAccessException;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.utils.NetUtils;
import io.netty.handler.codec.http.*;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.XML;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

public class RequestHelper {

    private static final HttpClient HTTP_CLIENT = HttpClient.create()
            .followRedirect(true);

    private final String url;

    private HttpMethod method = HttpMethod.GET;
    private Consumer<HttpHeaders> headers = headers -> headers.add(HttpHeaderNames.USER_AGENT, Config.USER_AGENT);

    private RequestHelper(final String url) {
        this.url = url;
    }

    public static RequestHelper fromUrl(final String url) {
        return new RequestHelper(url);
    }

    public static Mono<String> request(final String url) {
        return RequestHelper.fromUrl(url)
                .request()
                .responseSingle((resp, body) -> body.asString(StandardCharsets.UTF_8))
                .timeout(Config.TIMEOUT);
    }

    public RequestHelper setMethod(final HttpMethod method) {
        this.method = Objects.requireNonNull(method);
        return this;
    }

    public RequestHelper addHeaders(final CharSequence name, final Object value) {
        this.headers = this.headers.andThen(headers -> headers.add(name, value));
        return this;
    }

    public HttpClient.RequestSender request() {
        return HTTP_CLIENT
                .headers(this.headers)
                .request(this.method)
                .uri(this.url);
    }

    public <T> Mono<T> to(final JavaType type) {
        return this.request()
                .<T>responseSingle((resp, body) -> RequestHelper.handleResponse(resp, body, type))
                .timeout(Config.TIMEOUT);
    }

    public <T> Mono<T> to(final Class<? extends T> type) {
        return this.to(TypeFactory.defaultInstance().constructType(type));
    }

    private static <T> Mono<T> handleResponse(final HttpClientResponse resp, final ByteBufMono body, final JavaType type) {
        final int statusCode = resp.status().code();
        if (statusCode / 100 != 2 && statusCode != HttpResponseStatus.NOT_FOUND.code()) {
            return body.asString()
                    .defaultIfEmpty("Empty body")
                    .flatMap(err -> Mono.error(new ServerAccessException(resp, err)));
        }
        final String contentType = resp.responseHeaders().get(HttpHeaderNames.CONTENT_TYPE);
        final boolean isXml = contentType.contains("text/xml");
        final boolean isJson = contentType.contains(HttpHeaderValues.APPLICATION_JSON);
        if (!isXml && !isJson) {
            return body.asString()
                    .defaultIfEmpty("Empty body")
                    .flatMap(err -> Mono.error(new HeaderException(resp, err)));
        }
        return body.asInputStream()
                .flatMap(input -> Mono.fromCallable(() -> {
                    String content = "Unknown";
                    try (input) {
                        content = IOUtils.toString(input, StandardCharsets.UTF_8);
                        if (isXml) {
                            content = XML.toJSONObject(content).toString();
                        }
                        return NetUtils.MAPPER.readValue(content, type);
                    } catch (JSONException err) {
                        throw new JSONException(err.getMessage(),
                                new RuntimeException(String.format("Invalid JSON received (response: %s): %s", resp, content)));
                    }
                }));
    }

}
