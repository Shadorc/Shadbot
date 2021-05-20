package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.core.command.Cmd;
import com.shadorc.shadbot.core.command.Setting;
import com.shadorc.shadbot.database.DatabaseManager;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import discord4j.core.event.domain.channel.VoiceChannelDeleteEvent;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;

public class ChannelDeleteListener {

    public static class VoiceChannelDeleteListener implements EventListener<VoiceChannelDeleteEvent> {

        @Override
        public Class<VoiceChannelDeleteEvent> getEventType() {
            return VoiceChannelDeleteEvent.class;
        }

        @Override
        public Mono<?> execute(VoiceChannelDeleteEvent event) {
            return DatabaseManager.getGuilds()
                    .getDBGuild(event.getChannel().getGuildId())
                    .flatMap(dbGuild -> {
                        final Set<Snowflake> allowedVoiceChannelIds = dbGuild.getSettings().getAllowedVoiceChannelIds();
                        // If the channel was an allowed channel...
                        if (allowedVoiceChannelIds.remove(event.getChannel().getId())) {
                            // ...update settings to remove the deleted one
                            return dbGuild.updateSetting(Setting.ALLOWED_VOICE_CHANNELS, allowedVoiceChannelIds);
                        }
                        return Mono.empty();
                    });
        }
    }

    public static class TextChannelDeleteListener implements EventListener<TextChannelDeleteEvent> {

        @Override
        public Class<TextChannelDeleteEvent> getEventType() {
            return TextChannelDeleteEvent.class;
        }

        @Override
        public Mono<?> execute(TextChannelDeleteEvent event) {
            return DatabaseManager.getGuilds()
                    .getDBGuild(event.getChannel().getGuildId())
                    .flatMap(dbGuild -> {
                        final Snowflake channelId = event.getChannel().getId();
                        Mono<Void> request = Mono.empty();

                        final var restrictedChannels = dbGuild.getSettings().getRestrictedChannels();
                        // If the channel was a restricted channel...
                        if (restrictedChannels.containsKey(channelId)) {
                            // ...update settings to remove the deleted one
                            restrictedChannels.get(channelId).clear();

                            final var restrictedChannelsSeralized = restrictedChannels.entrySet().stream()
                                    .collect(Collectors.toUnmodifiableMap(
                                            entry -> entry.getKey().asString(),
                                            entry -> entry.getValue().stream()
                                                    .map(Cmd::getName)
                                                    .collect(Collectors.toUnmodifiableSet())));

                            request = request
                                    .and(dbGuild.updateSetting(Setting.RESTRICTED_CHANNELS, restrictedChannelsSeralized));
                        }

                        final Set<Snowflake> allowedTextChannelIds = dbGuild.getSettings().getAllowedTextChannelIds();
                        // If the channel was an allowed channel...
                        if (allowedTextChannelIds.remove(channelId)) {
                            // ...update settings to remove the deleted one
                            request = request
                                    .and(dbGuild.updateSetting(Setting.ALLOWED_TEXT_CHANNELS, allowedTextChannelIds));
                        }

                        return request;
                    });
        }
    }
}
