package com.locibot.locibot.database;

import reactor.core.publisher.Mono;

public interface DatabaseEntity {

    Mono<Void> insert();

    Mono<Void> delete();

}
