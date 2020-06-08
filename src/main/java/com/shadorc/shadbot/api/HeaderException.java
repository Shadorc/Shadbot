package com.shadorc.shadbot.api;

import io.netty.handler.codec.http.HttpHeaderNames;
import reactor.netty.http.client.HttpClientResponse;

import java.io.IOException;

public class HeaderException extends IOException {

    private final HttpClientResponse response;
    private final String body;

    public HeaderException(HttpClientResponse response, String body) {
        super(String.format("%s %s wrong header (%s) %s",
                response.method().asciiName(), response.resourceUrl(),
                response.responseHeaders().get(HttpHeaderNames.CONTENT_TYPE), body));
        this.response = response;
        this.body = body;
    }

    public HttpClientResponse getResponse() {
        return this.response;
    }

    public String getBody() {
        return this.body;
    }

}
