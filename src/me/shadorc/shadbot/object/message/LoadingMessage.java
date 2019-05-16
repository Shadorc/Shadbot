package me.shadorc.shadbot.object.message;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.ExceptionHandler;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class LoadingMessage implements Publisher<Void> {

    private static final Duration TYPING_TIMEOUT = Duration.ofMinutes(1);

    private final DiscordClient client;
    private final Snowflake channelId;
    private final List<Subscriber<? super Void>> subscribers;

    @Nullable
    private String content;
    @Nullable
    private Consumer<EmbedCreateSpec> embed;

    /**
     * Start typing until a message is send or 30 seconds have passed
     *
     * @param client    - the Discord client
     * @param channelId - the Channel ID in which send the message
     */
    public LoadingMessage(DiscordClient client, Snowflake channelId) {
        this.client = client;
        this.channelId = channelId;
        this.subscribers = new ArrayList<>();

        this.startTyping().subscribe(null, err -> ExceptionHandler.handleUnknownError(client, err));
    }

    public LoadingMessage setContent(String content) {
        this.content = content;
        return this;
    }

    public LoadingMessage setEmbed(Consumer<EmbedCreateSpec> embed) {
        this.embed = embed;
        return this;
    }

    /**
     * Send a message and stop typing when the message has been sent or an error occurred
     */
    public Mono<Message> send() {
        final Consumer<MessageCreateSpec> consumer = spec -> {
            if (this.content != null) {
                spec.setContent(this.content);
            }
            if (this.embed != null) {
                spec.setEmbed(this.embed);
            }
        };
        return this.client.getChannelById(this.channelId)
                .cast(MessageChannel.class)
                .flatMap(channel -> DiscordUtils.sendMessage(consumer, channel, this.embed != null))
                .timeout(TYPING_TIMEOUT)
                .doAfterTerminate(this::stopTyping);
    }

    /**
     * Start typing in the channel until a message is send or the typing timeout have passed
     */
    private Flux<Long> startTyping() {
        return this.client.getChannelById(this.channelId)
                .cast(MessageChannel.class)
                .filterWhen(channel -> DiscordUtils.hasPermission(channel, this.client.getSelfId().get(), Permission.SEND_MESSAGES))
                .flatMapMany(channel -> channel.typeUntil(this))
                .take(TYPING_TIMEOUT);
    }

    /**
     * Stop typing
     */
    public void stopTyping() {
        this.subscribers.forEach(Subscriber::onComplete);
    }

    @Override
    public void subscribe(Subscriber<? super Void> subscriber) {
        this.subscribers.add(subscriber);
    }

}
