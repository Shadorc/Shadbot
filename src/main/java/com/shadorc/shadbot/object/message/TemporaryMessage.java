package com.shadorc.shadbot.object.message;

import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

public class TemporaryMessage {

    private final GatewayDiscordClient gateway;
    private final Snowflake channelId;
    private final Duration duration;

    /**
     * @param gateway   The Discord gateway.
     * @param channelId The Channel ID in which to send the message.
     * @param duration  The delay to wait before deleting the message.
     */
    public TemporaryMessage(GatewayDiscordClient gateway, Snowflake channelId, Duration duration) {
        this.gateway = gateway;
        this.channelId = channelId;
        this.duration = duration;
    }

    /**
     * Send a message and then wait {@code delay} {@code unit} to delete it.
     *
     * @param content The message's content.
     * @return A {@link Mono} representing the message sent.
     */
    public Mono<Void> send(String content) {
        return this.gateway.getChannelById(this.channelId)
                .cast(MessageChannel.class)
                .flatMap(channel -> DiscordUtils.sendMessage(content, channel))
                .flatMap(message -> Mono.delay(this.duration, Schedulers.boundedElastic())
                        .then(message.delete()));
    }

}
