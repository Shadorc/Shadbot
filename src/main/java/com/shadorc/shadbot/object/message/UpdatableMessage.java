package com.shadorc.shadbot.object.message;

import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class UpdatableMessage {

    private final DiscordClient client;
    private final Snowflake channelId;
    private final AtomicLong messageId;

    @Nullable
    private String content;
    @Nullable
    private Consumer<EmbedCreateSpec> embed;

    /**
     * Sends a message that will be deleted each time the {@code send} method is called
     *
     * @param client    - the Discord client
     * @param channelId - the Channel ID in which to send the message
     */
    public UpdatableMessage(DiscordClient client, Snowflake channelId) {
        this.client = client;
        this.channelId = channelId;
        this.messageId = new AtomicLong(-1);
    }

    public UpdatableMessage setContent(String content) {
        this.content = content;
        return this;
    }

    public UpdatableMessage setEmbed(Consumer<EmbedCreateSpec> embed) {
        this.embed = embed;
        return this;
    }

    /**
     * Send a message and delete the previous one
     *
     * @param embed - the embed to send
     */
    public Mono<Message> send(Consumer<EmbedCreateSpec> embed) {
        return Mono.just(Snowflake.of(this.messageId.get()))
                .filter(messageId -> messageId.asLong() != -1)
                .flatMap(messageId -> this.client.getMessageById(this.channelId, messageId))
                .flatMap(Message::delete)
                .then(this.client.getChannelById(this.channelId))
                .cast(MessageChannel.class)
                .flatMap(channel -> DiscordUtils.sendMessage(embed, channel))
                .doOnNext(message -> this.messageId.set(message.getId().asLong()));
    }

    public Mono<Message> send() {
        final Consumer<MessageCreateSpec> consumer = spec -> {
            if (this.content != null) {
                spec.setContent(this.content);
            }
            if (this.embed != null) {
                spec.setEmbed(this.embed);
            }
        };

        return Mono.just(Snowflake.of(this.messageId.get()))
                .filter(messageId -> messageId.asLong() != -1)
                .flatMap(messageId -> this.client.getMessageById(this.channelId, messageId))
                .flatMap(Message::delete)
                .then(this.client.getChannelById(this.channelId))
                .cast(MessageChannel.class)
                .flatMap(channel -> DiscordUtils.sendMessage(consumer, channel, this.embed != null))
                .doOnNext(message -> this.messageId.set(message.getId().asLong()));
    }


}
