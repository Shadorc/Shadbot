package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.rest.util.Snowflake;
import io.prometheus.client.Gauge;
import reactor.core.publisher.Mono;

public class VoiceStateUpdateListener implements EventListener<VoiceStateUpdateEvent> {

    public static final Gauge VOICE_COUNT_GAUGE = Gauge.build()
            .namespace("shadbot")
            .name("voice_count")
            .help("Connected voice channel count")
            .register();

    @Override
    public Class<VoiceStateUpdateEvent> getEventType() {
        return VoiceStateUpdateEvent.class;
    }

    @Override
    public Mono<Void> execute(VoiceStateUpdateEvent event) {
        final Snowflake userId = event.getCurrent().getUserId();

        // If the voice state update comes from the bot...
        if (userId.equals(Shadbot.getSelfId())) {
            // If the voice state update comes from the bot disconnection...
            if (event.getCurrent().getChannelId().isEmpty()) {
                return Mono.fromRunnable(VOICE_COUNT_GAUGE::dec)
                        .and(MusicManager.getInstance().destroyConnection(event.getCurrent().getGuildId()));
            }
            // ... else, if the voice state update comes from the bot connection
            else {
                return Mono.fromRunnable(VOICE_COUNT_GAUGE::inc);
            }
        }
        // If the voice state update does not come from the bot...
        else {
            return VoiceStateUpdateListener.onUserEvent(event);
        }
    }

    private static Mono<Void> onUserEvent(VoiceStateUpdateEvent event) {
        final Snowflake guildId = event.getCurrent().getGuildId();

        final GuildMusic guildMusic = MusicManager.getInstance().getGuildMusic(guildId).orElse(null);
        // The bot is not playing music, ignore the event
        if (guildMusic == null) {
            return Mono.empty();
        }

        return event.getClient().getMemberById(guildId, Shadbot.getSelfId())
                .flatMap(Member::getVoiceState)
                .flatMap(VoiceState::getChannel)
                .flatMapMany(VoiceChannel::getVoiceStates)
                .count()
                .flatMap(memberCount -> {
                    // The bot is now alone: pause, schedule leave and warn users
                    if (memberCount == 1 && !guildMusic.isLeavingScheduled()) {
                        guildMusic.getTrackScheduler().getAudioPlayer().setPaused(true);
                        guildMusic.scheduleLeave();
                        return Mono.just(Emoji.INFO + " Nobody is listening anymore, music paused. I will leave the " +
                                "voice channel in 1 minute.");
                    }
                    // The bot is no more alone: unpause, cancel leave and warn users
                    else if (memberCount != 1 && guildMusic.isLeavingScheduled()) {
                        guildMusic.getTrackScheduler().getAudioPlayer().setPaused(false);
                        guildMusic.cancelLeave();
                        return Mono.just(Emoji.INFO + " Somebody joined me, music resumed.");
                    }
                    // Ignore the event
                    return Mono.empty();
                })
                .flatMap(content -> guildMusic.getMessageChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(content, channel)))
                .then();
    }

}
