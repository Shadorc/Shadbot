package com.shadorc.shadbot.object;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.shadorc.shadbot.api.HeaderException;
import com.shadorc.shadbot.api.ServerAccessException;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.utils.LogUtil;
import com.shadorc.shadbot.utils.NetUtil;
import io.netty.handler.codec.http.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.util.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

public class RequestHelper {

    private static final Logger LOGGER = LogUtil.getLogger(RequestHelper.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.create().followRedirect(true);

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

    private static <T> Mono<T> handleResponse(final HttpClientResponse resp, final ByteBufMono byteBufMono, final JavaType type) {
        final int statusCode = resp.status().code();
        if (statusCode / 100 != 2 && statusCode != HttpResponseStatus.NOT_FOUND.code()) {
            return byteBufMono.asString()
                    .defaultIfEmpty("Empty body")
                    .flatMap(body -> Mono.error(new ServerAccessException(resp, body)));
        }

        final String contentType = resp.responseHeaders().get(HttpHeaderNames.CONTENT_TYPE);
        final boolean isXml = contentType.contains("text/xml");
        final boolean isJson = contentType.contains(HttpHeaderValues.APPLICATION_JSON);
        if (!isXml && !isJson) {
            return byteBufMono.asString()
                    .defaultIfEmpty("Empty body")
                    .flatMap(body -> Mono.error(new HeaderException(resp, body)));
        }

        return byteBufMono.asString()
                .flatMap(body -> Mono
                        .<T>fromCallable(() -> {
                            final String json = isXml ? XML.toJSONObject(body).toString() : body;
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("JSON deserialized from {}: {}",
                                        resp.fullPath(), new JSONObject(json).toString(2));
                            }
                            return NetUtil.MAPPER.readValue(json, type);
                        })
                        .onErrorMap(err -> err instanceof JSONException || err instanceof JsonProcessingException,
                                err -> new IOException(err.getMessage(),
                                        new IOException(String.format("Invalid JSON received (response: %s): %s", resp, body)))))
                .retryWhen(ExceptionHandler.RETRY_ON_INTERNET_FAILURES
                        .apply(String.format("Retries exhausted while accessing %s", resp.fullPath())));
    }
}
