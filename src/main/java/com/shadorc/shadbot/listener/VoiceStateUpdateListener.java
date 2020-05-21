package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import io.prometheus.client.Gauge;
import reactor.core.publisher.Mono;

import static com.shadorc.shadbot.music.MusicManager.LOGGER;

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
        final Snowflake guildId = event.getCurrent().getGuildId();

        // If the voice state update comes from the bot...
        if (userId.equals(Shadbot.getSelfId())) {
            LOGGER.trace("{Guild ID: {}} Voice state update event: {}", guildId.asLong(), event);
            if (event.getCurrent().getChannelId().isEmpty() && event.getOld().isPresent()) {
                LOGGER.info("{Guild ID: {}} Voice channel left", guildId.asLong());
                return Mono.fromRunnable(VOICE_COUNT_GAUGE::dec)
                        .and(MusicManager.getInstance().destroyConnection(guildId));
            } else if (event.getCurrent().getChannelId().isPresent() && event.getOld().isEmpty()) {
                LOGGER.info("{Guild ID: {}} Voice channel joined", guildId.asLong());
                return Mono.fromRunnable(VOICE_COUNT_GAUGE::inc);
            } else {
                LOGGER.info("{Guild ID: {}} Voice channel moved", guildId.asLong());
            }
        }
        // If the voice state update does not come from the bot...
        else {
            return VoiceStateUpdateListener.onUserEvent(event);
        }

        return Mono.empty();
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
