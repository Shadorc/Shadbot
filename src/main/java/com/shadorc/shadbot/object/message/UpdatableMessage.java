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
        this.messageId = new AtomicLong();
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
     * Delete the previous message sent, if present, then send a message with the current content and embed set.
     */
    public Mono<Message> send() {
        return this.send(this.content, this.embed)
                .then(Mono.fromRunnable(() -> {
                    this.content = null;
                    this.embed = null;
                }));
    }

    /**
     * Delete the previous message sent, if present, then send a message with the provided content and embed.
     *
     * @param content - the content
     * @param embed - the embed
     */
    private Mono<Message> send(@Nullable String content, @Nullable Consumer<EmbedCreateSpec> embed) {
        final Consumer<MessageCreateSpec> consumer = spec -> {
            if (content != null) {
                spec.setContent(content);
            }
            if (embed != null) {
                spec.setEmbed(embed);
            }
        };

        return this.deleteMessage()
                .then(this.client.getChannelById(this.channelId))
                .cast(MessageChannel.class)
                .flatMap(channel -> DiscordUtils.sendMessage(consumer, channel, embed != null))
                .doOnNext(message -> this.messageId.set(message.getId().asLong()));
    }

    /**
     * Delete the previous message sent, if present.
     */
    public Mono<Void> deleteMessage() {
        return Mono.just(Snowflake.of(this.messageId.get()))
                .filter(messageId -> messageId.asLong() != 0)
                .flatMap(messageId -> this.client.getMessageById(this.channelId, messageId))
                .flatMap(Message::delete);
    }
}
