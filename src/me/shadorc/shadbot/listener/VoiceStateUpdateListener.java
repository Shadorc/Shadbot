package me.shadorc.shadbot.listener;

import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.MusicManager;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import reactor.core.publisher.Mono;

public class VoiceStateUpdateListener {

    public static Mono<Void> onVoiceStateUpdateEvent(VoiceStateUpdateEvent event) {
        return Mono.justOrEmpty(event.getClient().getSelfId())
                .filter(selfId -> !event.getCurrent().getUserId().equals(selfId))
                .flatMap(selfId -> VoiceStateUpdateListener.onUserEvent(event));
    }

    private static Mono<Void> onUserEvent(VoiceStateUpdateEvent event) {
        final Snowflake guildId = event.getCurrent().getGuildId();
        final Snowflake selfId = event.getClient().getSelfId().get();

        final GuildMusic guildMusic = MusicManager.getMusic(guildId);
        // The bot is not playing music, ignore the event
        if (guildMusic == null) {
            return Mono.empty();
        }

        return event.getClient().getMemberById(guildId, selfId)
                .flatMap(Member::getVoiceState)
                .flatMap(VoiceState::getChannel)
                .flatMapMany(VoiceChannel::getVoiceStates)
                .count()
                .flatMap(memberCount -> {
                    // The bot is now alone: pause, schedule leave and warn users
                    if (memberCount == 1 && !guildMusic.isLeavingScheduled()) {
                        guildMusic.getTrackScheduler().getAudioPlayer().setPaused(true);
                        guildMusic.scheduleLeave();
                        return Mono.just(Emoji.INFO + " Nobody is listening anymore, music paused. I will leave the voice channel in 1 minute.");
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
