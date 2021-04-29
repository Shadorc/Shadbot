package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.core.command.CommandManager;
import com.shadorc.shadbot.core.i18n.I18nManager;
import com.shadorc.shadbot.database.DatabaseManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Deprecated
public class MessageCreateListener implements EventListener<MessageCreateEvent> {

    @Override
    public Class<MessageCreateEvent> getEventType() {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<?> execute(MessageCreateEvent event) {
        return Mono.justOrEmpty(event.getGuildId())
                .flatMap(guildId -> DatabaseManager.getGuilds().getDBGuild(guildId))
                .filter(dbGuild -> {
                    final Optional<String> prefixOpt = dbGuild.getSettings().getPrefix();
                    final String content = event.getMessage().getContent();
                    if (prefixOpt.isPresent() && MessageCreateListener.isCommand(prefixOpt.orElseThrow(), content)) {
                        return true;
                    }
                    return MessageCreateListener.isCommand("/", content);
                })
                .flatMap(dbGuild -> event.getMessage().getChannel()
                        .flatMap(channel -> DiscordUtil.sendMessage(Emoji.WARNING,
                                I18nManager.localize(dbGuild.getLocale(), "warning.slash.commands"), channel)));
    }

    private static boolean isCommand(String prefix, String content) {
        return content.startsWith(prefix)
                && CommandManager.getCommand(content.replaceFirst(prefix, "").split(" ")[0]) != null;
    }

}
