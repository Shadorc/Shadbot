package com.shadorc.shadbot.api;

import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.netty.http.client.HttpClientResponse;

import java.util.function.Predicate;

public class ServerAccessException extends RuntimeException {

    private final HttpClientResponse response;
    private final String body;

    public ServerAccessException(HttpClientResponse response, String body) {
        super(String.format("%s %s failed (%d) %s",
                response.method().asciiName(), response.resourceUrl(), response.status().code(), body));
        this.response = response;
        this.body = body;
    }

    public HttpClientResponse getResponse() {
        return this.response;
    }

    public String getBody() {
        return this.body;
    }

    public static Predicate<Throwable> isStatus(HttpResponseStatus status) {
        return err -> err instanceof ServerAccessException
                && ((ServerAccessException) err).getResponse().status().equals(status);
    }

}
