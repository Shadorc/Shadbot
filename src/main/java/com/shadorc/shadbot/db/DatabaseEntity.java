package com.shadorc.shadbot.db;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.reactivestreams.client.Success;
import reactor.core.publisher.Mono;

public interface DatabaseEntity {

    Mono<Void> insert();

    Mono<Void> delete();

}
