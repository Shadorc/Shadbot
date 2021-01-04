package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.LogUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import reactor.core.publisher.Mono;
import reactor.util.Logger;

public class VoiceStateUpdateListener implements EventListener<VoiceStateUpdateEvent> {

    private static final Logger LOGGER = LogUtils.getLogger(VoiceStateUpdateListener.class, LogUtils.Category.MUSIC);

    @Override
    public Class<VoiceStateUpdateEvent> getEventType() {
        return VoiceStateUpdateEvent.class;
    }

    @Override
    public Mono<?> execute(VoiceStateUpdateEvent event) {
        final Snowflake userId = event.getCurrent().getUserId();
        final Snowflake guildId = event.getCurrent().getGuildId();

        // If the voice state update comes from the bot...
        if (userId.equals(event.getClient().getSelfId())) {
            LOGGER.trace("{Guild ID: {}} Voice state update event: {}", guildId.asLong(), event);
            if (event.isLeaveEvent()) {
                LOGGER.info("{Guild ID: {}} Voice channel left", guildId.asLong());
                return Mono.fromRunnable(Telemetry.VOICE_COUNT_GAUGE::dec)
                        .and(MusicManager.getInstance().destroyConnection(guildId));
            } else if (event.isJoinEvent()) {
                LOGGER.info("{Guild ID: {}} Voice channel joined", guildId.asLong());
                return Mono.fromRunnable(Telemetry.VOICE_COUNT_GAUGE::inc);
            } else if (event.isMoveEvent()) {
                LOGGER.info("{Guild ID: {}} Voice channel moved", guildId.asLong());
            }
        }
        // If the voice state update does not come from the bot...
        else {
            return VoiceStateUpdateListener.onUserEvent(event);
        }

        return Mono.empty();
    }

    private static Mono<?> onUserEvent(VoiceStateUpdateEvent event) {
        final Snowflake guildId = event.getCurrent().getGuildId();
        return Mono.defer(() -> Mono.justOrEmpty(MusicManager.getInstance().getGuildMusic(guildId)))
                .flatMap(guildMusic -> event.getClient()
                        .getMemberById(guildId, event.getClient().getSelfId())
                        .flatMap(Member::getVoiceState)
                        .flatMap(VoiceState::getChannel)
                        .flatMapMany(VoiceChannel::getVoiceStates)
                        .flatMap(VoiceState::getMember)
                        .filter(member -> !member.isBot())
                        .count()
                        // Everyone left or somebody joined
                        .filter(memberCount -> (memberCount == 0) != guildMusic.isLeavingScheduled())
                        .map(memberCount -> {
                            LOGGER.debug("{Guild ID: {}} On user event, memberCount: {}, leavingScheduled: {}",
                                    guildId.asLong(), memberCount, guildMusic.isLeavingScheduled());
                            final StringBuilder strBuilder = new StringBuilder(Emoji.INFO.toString());
                            // The bot is now alone: pause, schedule leave and warn users
                            if (memberCount == 0 && !guildMusic.isLeavingScheduled()) {
                                guildMusic.getTrackScheduler().getAudioPlayer().setPaused(true);
                                guildMusic.scheduleLeave();
                                strBuilder.append(" Nobody is listening anymore, music paused. I will leave the " +
                                        "voice channel in 1 minute.");
                                LOGGER.debug("{Guild ID: {}} Nobody is listening anymore, music paused, leave scheduled", guildId.asLong());
                            }
                            // The bot is no more alone: unpause, cancel leave and warn users
                            else if (memberCount != 0 && guildMusic.isLeavingScheduled()) {
                                guildMusic.getTrackScheduler().getAudioPlayer().setPaused(false);
                                guildMusic.cancelLeave();
                                strBuilder.append(" Somebody joined me, music resumed.");
                                LOGGER.debug("{Guild ID: {}} Somebody joined, music resumed", guildId.asLong());
                            }
                            return strBuilder.toString();
                        })
                        .flatMap(content -> guildMusic.getMessageChannel()
                                .flatMap(channel -> DiscordUtils.sendMessage(content, channel))));
    }

}
