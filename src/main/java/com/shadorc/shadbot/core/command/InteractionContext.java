package com.shadorc.shadbot.core.command;

import com.shadorc.shadbot.object.Emoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.MessageData;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public interface InteractionContext {

    Mono<MessageData> reply(String message);

    Mono<MessageData> reply(Emoji emoji, String message);

    Mono<MessageData> reply(Consumer<EmbedCreateSpec> embed);

    Mono<MessageData> editReply(Emoji emoji, String message);

    Mono<MessageData> editReply(Consumer<EmbedCreateSpec> embed);

}
