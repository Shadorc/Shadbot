package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.core.command.CommandProcessor;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.i18n.I18nManager;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.database.DatabaseManager;
import discord4j.core.event.domain.InteractionCreateEvent;
import reactor.core.publisher.Mono;

public class InteractionCreateListener implements EventListener<InteractionCreateEvent> {

    @Override
    public Class<InteractionCreateEvent> getEventType() {
        return InteractionCreateEvent.class;
    }

    @Override
    public Mono<?> execute(InteractionCreateEvent event) {
        // TODO Feature: Interactions from DM
        if (event.getInteraction().getGuildId().isEmpty()) {
            return event.reply(I18nManager.localize(Config.DEFAULT_LOCALE, "interaction.dm"));
        }
        return event.acknowledge()
                .then(Mono.justOrEmpty(event.getInteraction().getGuildId())
                        .flatMap(guildId -> DatabaseManager.getGuilds().getDBGuild(guildId)))
                .map(dbGuild -> new Context(event, dbGuild))
                .flatMap(CommandProcessor::processCommand);
    }

}
