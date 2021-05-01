package com.shadorc.shadbot.core.command;

import com.shadorc.shadbot.object.Emoji;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public interface InteractionContext {

    Mono<Message> reply(String message);

    Mono<Message> reply(Emoji emoji, String message);

    Mono<Message> reply(Consumer<EmbedCreateSpec> embed);

    Mono<Message> editReply(String message);

    Mono<Message> editReply(Emoji emoji, String message);

    Mono<Message> editReply(Consumer<EmbedCreateSpec> embed);

    Mono<Message> editInitialReply(Consumer<EmbedCreateSpec> embed);

}
