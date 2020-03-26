package com.shadorc.shadbot.listener;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.rest.util.Snowflake;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class MessageUpdateListener implements EventListener<MessageUpdateEvent> {

    @Override
    public Class<MessageUpdateEvent> getEventType() {
        return MessageUpdateEvent.class;
    }

    @Override
    public Mono<Void> execute(MessageUpdateEvent event) {
        if (!event.isContentChanged()) {
            return Mono.empty();
        }

        // The member can be empty if the message has been edited in a private channel
        final Mono<Optional<Member>> getMember = event.getMessage()
                .flatMap(Message::getAuthorAsMember)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty());

        // The guild ID can be null if the message has been edited in a private channel
        final Long guildId = event.getGuildId()
                .map(Snowflake::asLong)
                .orElse(null);

        return Mono.zip(event.getMessage(), getMember)
                .doOnNext(tuple -> event.getClient()
                        .getEventDispatcher()
                        .publish(new MessageCreateEvent(event.getClient(), event.getShardInfo(), tuple.getT1(),
                                guildId, tuple.getT2().orElse(null))))
                .then();
    }

}
