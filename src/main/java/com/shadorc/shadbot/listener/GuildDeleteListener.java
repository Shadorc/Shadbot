package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.cache.CacheManager;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.rest.http.client.ClientException;
import reactor.core.publisher.Mono;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class GuildDeleteListener implements EventListener<GuildDeleteEvent> {

    private static final String TEXT = String.format("I'm not part of your server anymore, thanks for having tested me!" +
            "%nIf you encountered an issue or want to let me know what could be improved, " +
            "do not hesitate to join my support server and tell me!" +
            "%n%s", Config.SUPPORT_SERVER_URL);

    @Override
    public Class<GuildDeleteEvent> getEventType() {
        return GuildDeleteEvent.class;
    }

    @Override
    public Mono<Void> execute(GuildDeleteEvent event) {
        DEFAULT_LOGGER.info("{Guild ID: {}} Disconnected", event.getGuildId().asLong());

        final Mono<Void> deleteGuild = DatabaseManager.getGuilds()
                .getDBGuild(event.getGuildId())
                .flatMap(DBGuild::delete);

        final Snowflake guildOwnerId = CacheManager.getInstance().getGuildOwnersCache().delete(event.getGuildId());
        final Mono<Message> sendMessage = event.getClient()
                .getUserById(guildOwnerId)
                .flatMap(User::getPrivateChannel)
                .flatMap(channel -> DiscordUtils.sendMessage(TEXT, channel))
                .onErrorResume(ClientException.class, err -> Mono.empty());

        return MusicManager.getInstance().destroyConnection(event.getGuildId())
                .and(deleteGuild)
                .and(sendMessage);
    }

}
