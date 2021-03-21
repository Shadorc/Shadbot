package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.core.command.CommandProcessor;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.db.DatabaseManager;
import discord4j.core.event.domain.InteractionCreateEvent;
import reactor.core.publisher.Mono;

public class InteractionCreateListener implements EventListener<InteractionCreateEvent> {

    @Override
    public Class<InteractionCreateEvent> getEventType() {
        return InteractionCreateEvent.class;
    }

    @Override
    public Mono<?> execute(InteractionCreateEvent event) {
        return event.acknowledge()
                // TODO: Interactions from DM
                .then(DatabaseManager.getGuilds().getDBGuild(event.getInteraction().getGuildId().get()))
                .map(dbGuild -> new Context(event, dbGuild))
                .flatMap(CommandProcessor::processCommand);
    }

}
