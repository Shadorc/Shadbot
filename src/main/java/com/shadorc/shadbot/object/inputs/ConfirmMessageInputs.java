package com.shadorc.shadbot.object.inputs;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConfirmMessageInputs extends MessageInputs {

    private final Snowflake userId;
    private final Mono<?> task;
    private final AtomicBoolean isCancelled;

    public ConfirmMessageInputs(GatewayDiscordClient gateway, Duration timeout, Snowflake channelId,
                                Snowflake userId, Mono<?> task) {
        super(gateway, timeout, channelId);
        this.userId = userId;
        this.task = task;
        this.isCancelled = new AtomicBoolean(false);
    }

    @Override
    public Mono<Boolean> isValidEvent(MessageCreateEvent event) {
        return Mono.justOrEmpty(event.getMessage().getAuthor())
                .map(User::getId)
                .filter(this.userId::equals)
                .map(__ -> event.getMessage().getContent())
                .map(content -> {
                    if ("no".equalsIgnoreCase(content)) {
                        this.isCancelled.set(true);
                    }
                    return "yes".equalsIgnoreCase(content);
                });
    }

    @Override
    public boolean takeEventWile(MessageCreateEvent event) {
        return !this.isCancelled.get();
    }

    @Override
    public Mono<?> processEvent(MessageCreateEvent event) {
        return this.task;
    }
}
