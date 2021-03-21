package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.core.i18n.I18nManager;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import com.shadorc.shadbot.utils.LogUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.Logger;

public class VoiceStateUpdateListener implements EventListener<VoiceStateUpdateEvent> {

    private static final Logger LOGGER = LogUtil.getLogger(VoiceStateUpdateListener.class, LogUtil.Category.MUSIC);

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
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("{Guild ID: {}} Voice state update event: {}", guildId.asString(), event);
            }
            if (event.isLeaveEvent()) {
                LOGGER.info("{Guild ID: {}} Voice channel left", guildId.asString());
                Telemetry.VOICE_COUNT_GAUGE.dec();
                return MusicManager.destroyConnection(guildId);
            } else if (event.isJoinEvent()) {
                LOGGER.info("{Guild ID: {}} Voice channel joined", guildId.asString());
                Telemetry.VOICE_COUNT_GAUGE.inc();
            } else if (event.isMoveEvent()) {
                LOGGER.info("{Guild ID: {}} Voice channel moved", guildId.asString());
            }
        }
        // If the voice state update does not come from the bot...
        else {
            return VoiceStateUpdateListener.onUserEvent(event);
        }

        LOGGER.error("{Guild ID: {}} Unknown event: {}", guildId.asString(), event);
        return Mono.empty();
    }

    private static Mono<?> onUserEvent(VoiceStateUpdateEvent event) {
        final Snowflake guildId = event.getCurrent().getGuildId();
        return Mono.defer(() -> Mono.justOrEmpty(MusicManager.getGuildMusic(guildId)))
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
                        .zipWith(DatabaseManager.getGuilds().getDBGuild(guildId).map(DBGuild::getLocale))
                        .map(TupleUtils.function((memberCount, locale) -> {
                            LOGGER.debug("{Guild ID: {}} On user event, memberCount: {}, leavingScheduled: {}",
                                    guildId.asString(), memberCount, guildMusic.isLeavingScheduled());
                            // The bot is now alone: pause, schedule leave and warn users
                            if (memberCount == 0 && !guildMusic.isLeavingScheduled()) {
                                LOGGER.debug("{Guild ID: {}} Nobody is listening, music paused, leaving scheduled",
                                        guildId.asString());
                                guildMusic.getTrackScheduler().getAudioPlayer().setPaused(true);
                                guildMusic.scheduleLeave();
                                return I18nManager.localize(locale, "voicestateupdate.nobody.listening");
                            }
                            // The bot is no more alone: unpause, cancel leave and warn users
                            else if (memberCount != 0 && guildMusic.isLeavingScheduled()) {
                                LOGGER.debug("{Guild ID: {}} Somebody joined, music resumed, leaving cancelled",
                                        guildId.asString());
                                guildMusic.getTrackScheduler().getAudioPlayer().setPaused(false);
                                guildMusic.cancelLeave();
                                return I18nManager.localize(locale, "voicestateupdate.somebody.joined");
                            } else {
                                LOGGER.error("{Guild ID: {}} Illegal state detected! Member count: {}, leaving scheduled: {}",
                                        guildId.asString(), memberCount, guildMusic.isLeavingScheduled());
                                return "";
                            }
                        }))
                        .flatMap(content -> guildMusic.getMessageChannel()
                                .flatMap(channel -> DiscordUtil.sendMessage(Emoji.INFO + " " + content, channel))));
    }

}
