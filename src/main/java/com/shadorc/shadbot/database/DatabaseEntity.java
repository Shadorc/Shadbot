package com.shadorc.shadbot.database;

import reactor.core.publisher.Mono;

public interface DatabaseEntity {

    Mono<Void> insert();

    Mono<Void> delete();

}
