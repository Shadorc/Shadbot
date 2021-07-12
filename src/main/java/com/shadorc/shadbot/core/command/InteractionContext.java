package com.shadorc.shadbot.core.command;

import com.shadorc.shadbot.object.Emoji;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

public interface InteractionContext {

    Mono<Void> replyEphemeral(Emoji emoji, String message);

    Mono<Message> createFollowupMessage(String message);

    Mono<Message> createFollowupMessage(Emoji emoji, String message);

    Mono<Message> createFollowupMessage(EmbedCreateSpec embed);

    Mono<Message> editFollowupMessage(String message);

    Mono<Message> editFollowupMessage(Emoji emoji, String message);

    Mono<Message> editFollowupMessage(EmbedCreateSpec embed);

    Mono<Message> editInitialFollowupMessage(EmbedCreateSpec embed);

}
