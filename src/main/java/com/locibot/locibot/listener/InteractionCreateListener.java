package com.locibot.locibot.listener;

import com.locibot.locibot.core.command.*;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.core.i18n.I18nManager;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.ReactorUtil;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.object.command.ApplicationCommandInteraction;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;

public class InteractionCreateListener implements EventListener<InteractionCreateEvent> {

    @Override
    public Class<InteractionCreateEvent> getEventType() {
        return InteractionCreateEvent.class;
    }

    @Override
    public Mono<?> execute(InteractionCreateEvent event) {
        Telemetry.INTERACTING_USERS.add(event.getInteraction().getUser().getId().asLong());

        // TODO Feature: Interactions from DM
        if (event.getInteraction().getGuildId().isEmpty()) {
            //return event.getInteraction().getChannel().ofType(PrivateChannel.class).flatMap(privateChannel -> CommandProcessor.processCommand(new PrivateContext(event))); //TODO: Parse with CommandProcessor
            final BaseCmd command = CommandManager.getCommand(event.getInteraction().getCommandInteraction().flatMap(ApplicationCommandInteraction::getName).orElseThrow());
            return event.acknowledge().then(command.execute(new PrivateContext(event)));
        }

        return event.getInteraction().getChannel()
                .ofType(TextChannel.class)
                .flatMap(channel -> channel.getEffectivePermissions(event.getClient().getSelfId()))
                .filterWhen(ReactorUtil.filterOrExecute(
                        permissions -> permissions.contains(Permission.SEND_MESSAGES)
                                && permissions.contains(Permission.VIEW_CHANNEL),
                        event.replyEphemeral(Emoji.RED_CROSS
                                + I18nManager.localize(Config.DEFAULT_LOCALE, "interaction.missing.permissions"))))
                .flatMap(__ -> Mono.justOrEmpty(event.getInteraction().getGuildId()))
                .flatMap(guildId -> DatabaseManager.getGuilds().getDBGuild(guildId))
                .map(dbGuild -> new Context(event, dbGuild))
                .flatMap(CommandProcessor::processCommand);
    }

}
