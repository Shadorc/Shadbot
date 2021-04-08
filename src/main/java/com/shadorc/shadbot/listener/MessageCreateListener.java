package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.database.DatabaseManager;
import com.shadorc.shadbot.database.guilds.entity.Settings;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

@Deprecated
public class MessageCreateListener implements EventListener<MessageCreateEvent> {

    @Override
    public Class<MessageCreateEvent> getEventType() {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<?> execute(MessageCreateEvent event) {
        return Mono.justOrEmpty(event.getGuildId())
                .flatMap(guildId -> DatabaseManager.getGuilds().getSettings(guildId))
                .map(Settings::getPrefix)
                .flatMap(Mono::justOrEmpty)
                .filter(event.getMessage().getContent()::startsWith)
                .flatMap(__ -> event.getMessage().getChannel())
                .flatMap(channel -> DiscordUtil.sendMessage(Emoji.WARNING, "You should use Slash commands!", channel)); // TODO
    }

}
