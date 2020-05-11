package com.shadorc.shadbot.object.message;

import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.util.Snowflake;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class UpdatableMessage {

    private final GatewayDiscordClient client;
    private final Snowflake channelId;
    private final AtomicLong messageId;

    @Nullable
    private String content;
    @Nullable
    private Consumer<EmbedCreateSpec> embed;

    /**
     * Sends a message that will be deleted each time the {@code send} method is called.
     *
     * @param client The Discord client.
     * @param channelId The Channel ID in which to send the message.
     */
    public UpdatableMessage(GatewayDiscordClient client, Snowflake channelId) {
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
    public Mono<Void> send() {
        return this.send(this.content, this.embed)
                .then(Mono.fromRunnable(() -> {
                    this.content = null;
                    this.embed = null;
                }));
    }

    /**
     * Delete the previous message sent, if present, then send a message with the provided content and embed.
     *
     * @param content The content to send, may be null.
     * @param embed The embed to send, may be null.
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
        return Mono.just(this.messageId.get())
                .filter(messageId -> messageId != 0)
                .map(Snowflake::of)
                .flatMap(messageId -> this.client.getMessageById(this.channelId, messageId))
                .onErrorResume(ClientException.isStatusCode(HttpResponseStatus.FORBIDDEN.code()), err -> Mono.empty())
                .flatMap(Message::delete);
    }
}
