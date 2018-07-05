package me.shadorc.shadbot.listener;

import java.util.Optional;

import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

public class VoiceStateUpdateListener {

	public static void onVoiceStateUpdateEvent(VoiceStateUpdateEvent event) {
		final Snowflake selfId = event.getClient().getSelfId().get();
		// TODO: Improve this in Discord4J
		final Snowflake userId = event.getOld()
				.map(VoiceState::getUserId)
				.orElse(event.getCurrent().getUserId());

		if(userId.equals(selfId)) {
			onBotEvent(event);
		} else {
			onUserEvent(event);
		}
	}

	private static void onBotEvent(VoiceStateUpdateEvent event) {
		// TODO: Change to !event.getCurrent.isPresent()
		// The bot has left a voice channel
		if(event.getCurrent() == null) {
			final Snowflake guildId = event.getCurrent().getGuildId();

			GuildMusic guildMusic = GuildMusicManager.GUILD_MUSIC_MAP.get(guildId);
			if(guildMusic != null) {
				guildMusic.destroy();
				LogUtils.infof("{Guild ID: %d} Voice channel left.", guildId.asLong());
			}
		}
	}

	private static void onUserEvent(VoiceStateUpdateEvent event) {
		final Snowflake guildId = event.getOld()
				.map(VoiceState::getGuildId)
				.orElse(event.getCurrent().getGuildId());
		final Snowflake selfId = event.getClient().getSelfId().get();

		check(event.getClient().getMemberById(guildId, selfId));
	}

	private static synchronized void check(Mono<Member> monoMember) {
		Mono.zip(monoMember, monoMember.flatMap(Member::getVoiceState))
				.subscribe(memberAndVoiceState -> {

					final Member member = memberAndVoiceState.getT1();
					final VoiceState voiceState = memberAndVoiceState.getT2();

					// The bot is not in a voice channel
					if(!voiceState.getChannelId().isPresent()) {
						return;
					}

					final Snowflake voiceChannelId = voiceState.getChannelId().get();

					GuildMusic guildMusic = GuildMusicManager.GUILD_MUSIC_MAP.get(member.getGuildId());
					if(guildMusic == null) {
						return;
					}

					isAlone(member.getGuild(), voiceChannelId).subscribe(isAlone -> {
						if(isAlone && !guildMusic.isLeavingScheduled()) {
							BotUtils.sendMessage(Emoji.INFO + " Nobody is listening anymore, music paused. "
									+ "I will leave the voice channel in 1 minute.",
									guildMusic.getMessageChannel()).subscribe();
							guildMusic.getScheduler().getAudioPlayer().setPaused(true);
							guildMusic.scheduleLeave();

						} else if(!isAlone && guildMusic.isLeavingScheduled()) {
							BotUtils.sendMessage(Emoji.INFO + " Somebody joined me, music resumed.",
									guildMusic.getMessageChannel()).subscribe();
							guildMusic.getScheduler().getAudioPlayer().setPaused(false);
							guildMusic.cancelLeave();
						}
					});
				});
	}

	private static Mono<Boolean> isAlone(Mono<Guild> guild, Snowflake voiceChannelId) {
		return guild.flatMapMany(Guild::getMembers)
				.filter(member -> !member.isBot())
				.flatMap(Member::getVoiceState)
				.map(VoiceState::getChannelId)
				.defaultIfEmpty(Optional.empty())
				.filter(Optional::isPresent)
				.map(Optional::get)
				.filter(voiceChannelId::equals)
				.hasElements();
	}

}
