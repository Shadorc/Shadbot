package com.shadorc.shadbot.core.command;

import com.shadorc.shadbot.object.Emoji;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.legacy.LegacyEmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public interface InteractionContext {

    Mono<Void> replyEphemeral(Emoji emoji, String message);

    Mono<Message> createFollowupMessage(String message);

    Mono<Message> createFollowupMessage(Emoji emoji, String message);

    @Deprecated
    Mono<Message> createFollowupMessage(Consumer<LegacyEmbedCreateSpec> embed);

    Mono<Message> createFollowupMessage(EmbedCreateSpec embed);

    Mono<Message> editFollowupMessage(String message);

    Mono<Message> editFollowupMessage(Emoji emoji, String message);

    @Deprecated
    Mono<Message> editFollowupMessage(Consumer<LegacyEmbedCreateSpec> embed);

    Mono<Message> editFollowupMessage(EmbedCreateSpec embed);

    Mono<Message> editInitialFollowupMessage(EmbedCreateSpec embed);

}
