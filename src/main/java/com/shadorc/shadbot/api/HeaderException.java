package com.shadorc.shadbot.api;

import io.netty.handler.codec.http.HttpHeaderNames;
import reactor.netty.http.client.HttpClientResponse;

import java.io.IOException;

public class HeaderException extends IOException {

    public HeaderException(HttpClientResponse response, String body) {
        super("%s %s wrong header (%s) %s"
                .formatted(response.method().asciiName(), response.resourceUrl(),
                        response.responseHeaders().get(HttpHeaderNames.CONTENT_TYPE), body));
    }

}
