package me.shadorc.shadbot.listener;

import java.util.Optional;

import org.apache.commons.lang3.BooleanUtils;

import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

public class VoiceStateUpdateListener {

	public static void onVoiceStateUpdateEvent(VoiceStateUpdateEvent event) {
		event.getClient().getSelfId().ifPresent(selfId -> {
			final Snowflake userId = event.getCurrent().getUserId();
			if(userId.equals(selfId)) {
				VoiceStateUpdateListener.onBotEvent(event);
			} else {
				VoiceStateUpdateListener.onUserEvent(event);
			}
		});
	}

	private static void onBotEvent(VoiceStateUpdateEvent event) {
		// If the bot is no more in a voice channel, it left
		if(!event.getCurrent().getChannelId().isPresent()) {
			final Snowflake guildId = event.getCurrent().getGuildId();
			final GuildMusic guildMusic = GuildMusicManager.GUILD_MUSIC_MAP.get(guildId);
			if(guildMusic != null) {
				guildMusic.destroy();
				LogUtils.infof("{Guild ID: %d} Voice channel left.", guildId.asLong());
			}
		}
	}

	private static void onUserEvent(VoiceStateUpdateEvent event) {
		final Snowflake guildId = event.getCurrent().getGuildId();

		final GuildMusic guildMusic = GuildMusicManager.GUILD_MUSIC_MAP.get(guildId);
		if(guildMusic == null) {
			return;
		}

		DiscordUtils.getVoiceChannelId(event.getClient().getSelf().flatMap(user -> user.asMember(guildId)))
				.flatMap(Mono::justOrEmpty)
				.flatMap(botVoiceChannel -> event.getCurrent().getGuild()
						.flatMapMany(DiscordUtils::getMembers)
						.filter(member -> !member.getId().equals(event.getClient().getSelfId().get()))
						.flatMap(DiscordUtils::getVoiceChannelId)
						.flatMap(Mono::justOrEmpty)
						.filter(botVoiceChannel::equals)
						.hasElements())
				.map(BooleanUtils::negate)
				.map(isAlone -> {
					if(isAlone && !guildMusic.isLeavingScheduled()) {
						guildMusic.getScheduler().getAudioPlayer().setPaused(true);
						guildMusic.scheduleLeave();
						return Optional.of(Emoji.INFO + " Nobody is listening anymore, music paused. I will leave the voice channel in 1 minute.");
					} else if(!isAlone && guildMusic.isLeavingScheduled()) {
						guildMusic.getScheduler().getAudioPlayer().setPaused(false);
						guildMusic.cancelLeave();
						return Optional.of(Emoji.INFO + " Somebody joined me, music resumed.");
					}
					return Optional.empty();
				})
				.flatMap(Mono::justOrEmpty)
				.flatMap(content -> BotUtils.sendMessage(content.toString(), guildMusic.getMessageChannel()))
				.subscribe();
	}

}
