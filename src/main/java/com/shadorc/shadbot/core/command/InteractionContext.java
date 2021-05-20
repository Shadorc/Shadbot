package com.shadorc.shadbot.core.command;

import com.shadorc.shadbot.object.Emoji;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public interface InteractionContext {

    Mono<Void> replyEphemeral(Emoji emoji, String message);

    Mono<Message> createFollowupMessage(String message);

    Mono<Message> createFollowupMessage(Emoji emoji, String message);

    Mono<Message> createFollowupMessage(Consumer<EmbedCreateSpec> embed);

    Mono<Message> editFollowupMessage(String message);

    Mono<Message> editFollowupMessage(Emoji emoji, String message);

    Mono<Message> editFollowupMessage(Consumer<EmbedCreateSpec> embed);

    Mono<Message> editInitialFollowupMessage(Consumer<EmbedCreateSpec> embed);

}
