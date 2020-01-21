package com.shadorc.shadbot.db;

import reactor.core.publisher.Mono;

public interface DatabaseEntity {

    Mono<Void> insert();

    Mono<Void> delete();

}
