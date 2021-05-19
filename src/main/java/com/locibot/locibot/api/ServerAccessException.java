package com.locibot.locibot.api;

import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.netty.http.client.HttpClientResponse;

import java.util.function.Predicate;

public class ServerAccessException extends RuntimeException {

    private final HttpClientResponse response;

    public ServerAccessException(HttpClientResponse response, String body) {
        super("%s %s failed (%d) %s"
                .formatted(response.method().asciiName(), response.resourceUrl(), response.status().code(), body));
        this.response = response;
    }

    public HttpClientResponse getResponse() {
        return this.response;
    }

    public static Predicate<Throwable> isStatus(HttpResponseStatus status) {
        return thr -> thr instanceof ServerAccessException err
                && err.getResponse().status().equals(status);
    }

}
