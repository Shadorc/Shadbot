package me.shadorc.shadbot.listener;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.utils.TimeUtils;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

public class MessageUpdateListener {

    public static Mono<Void> onMessageUpdateEvent(MessageUpdateEvent event) {
        if (!event.isContentChanged() || event.getOld().isEmpty() || event.getGuildId().isEmpty()) {
            return Mono.empty();
        }

        // If the message has been sent more than 30 seconds ago, ignore it
        final Message oldMessage = event.getOld().get();
        if (TimeUtils.getMillisUntil(oldMessage.getTimestamp()) > TimeUnit.SECONDS.toMillis(30)) {
            return Mono.empty();
        }

        final Snowflake guildId = event.getGuildId().get();
        return Mono.zip(event.getMessage(), event.getMessage().flatMap(Message::getAuthorAsMember))
                .doOnNext(tuple -> MessageCreateListener.onMessageCreate(
                        new MessageCreateEvent(event.getClient(), tuple.getT1(), guildId.asLong(), tuple.getT2())))
                .then();
    }

}
