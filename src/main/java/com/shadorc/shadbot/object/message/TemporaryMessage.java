package com.shadorc.shadbot.object.message;

import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

public class TemporaryMessage {

    private final GatewayDiscordClient client;
    private final Snowflake channelId;
    private final Duration duration;

    /**
     * @param client    - the Discord client
     * @param channelId - the Channel ID in which to send the message
     * @param duration  - the delay to wait before deleting the message
     */
    public TemporaryMessage(GatewayDiscordClient client, Snowflake channelId, Duration duration) {
        this.client = client;
        this.channelId = channelId;
        this.duration = duration;
    }

    /**
     * Send a message and then wait {@code delay} {@code unit} to delete it
     *
     * @param content - the message's content
     * @return A Mono representing the message sent
     */
    public Mono<Void> send(String content) {
        return this.client.getChannelById(this.channelId)
                .cast(MessageChannel.class)
                .flatMap(channel -> DiscordUtils.sendMessage(content, channel))
                .flatMap(message -> Mono.delay(this.duration, Schedulers.elastic())
                        .then(message.delete()));
    }

}
