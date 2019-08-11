package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.Config;
import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.object.Inputs;
import com.shadorc.shadbot.utils.ExitCode;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Consumer;

public class ConfirmInputs extends Inputs {

    private final Mono<Void> task;

    public ConfirmInputs(DiscordClient client, Duration timeout, Mono<Void> task) {
        super(client, timeout);
        this.task = task;
    }

    @Override
    public Mono<Boolean> isValidEvent(MessageCreateEvent event) {
        return Mono.justOrEmpty(event.getMessage().getAuthor())
                .map(User::getId)
                .map(authorId -> authorId.equals(Shadbot.getOwnerId()) || Config.ADDITIONAL_OWNERS.contains(authorId));
    }

    @Override
    public boolean takeEventWile(MessageCreateEvent event) {
        return true;
    }

    @Override
    public Mono<Void> processEvent(MessageCreateEvent event) {
        return task;
    }
}
