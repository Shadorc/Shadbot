package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.core.command.CommandProcessor;
import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

public class MessageCreateListener implements EventListener<MessageCreateEvent> {

    @Override
    public Class<MessageCreateEvent> getEventType() {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Void> execute(MessageCreateEvent event) {
        return CommandProcessor.getInstance().processMessageEvent(event);
    }

}