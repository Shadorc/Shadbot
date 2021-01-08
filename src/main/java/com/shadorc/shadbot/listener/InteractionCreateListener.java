package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.core.command.CommandProcessor;
import discord4j.core.event.domain.InteractionCreateEvent;
import reactor.core.publisher.Mono;

public class InteractionCreateListener implements EventListener<InteractionCreateEvent> {

    @Override
    public Class<InteractionCreateEvent> getEventType() {
        return InteractionCreateEvent.class;
    }

    @Override
    public Mono<?> execute(InteractionCreateEvent event) {
        return CommandProcessor.processEvent(event);
    }

}
