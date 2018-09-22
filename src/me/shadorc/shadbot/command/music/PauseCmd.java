package me.shadorc.shadbot.command.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.MUSIC, names = { "pause", "unpause", "resume" })
public class PauseCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final GuildMusic guildMusic = context.requireGuildMusic();
		final AudioPlayer audioPlayer = guildMusic.getScheduler().getAudioPlayer();

		return DiscordUtils.requireSameVoiceChannel(context)
				.flatMap(voiceChannelId -> {
					audioPlayer.setPaused(!audioPlayer.isPaused());

					String message;
					if(audioPlayer.isPaused()) {
						message = String.format(Emoji.PAUSE + " Music paused by **%s**.", context.getUsername());
					} else {
						message = String.format(Emoji.PLAY + " Music resumed by **%s**.", context.getUsername());
					}

					return BotUtils.sendMessage(message, context.getChannel()).then();
				});
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Pause current music. Use this command again to resume.")
				.build();
	}
}