package me.shadorc.shadbot.command.music;

import java.util.function.Consumer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.MUSIC, names = { "pause", "unpause", "resume" })
public class PauseCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final AudioPlayer audioPlayer = context.requireGuildMusic().getTrackScheduler().getAudioPlayer();

		return DiscordUtils.requireSameVoiceChannel(context)
				.map(voiceChannelId -> {
					audioPlayer.setPaused(!audioPlayer.isPaused());
					if(audioPlayer.isPaused()) {
						return String.format(Emoji.PAUSE + " Music paused by **%s**.", context.getUsername());
					} else {
						return String.format(Emoji.PLAY + " Music resumed by **%s**.", context.getUsername());
					}
				})
				.flatMap(message -> context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(message, channel)))
				.then();
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Pause current music. Use this command again to resume.")
				.build();
	}
}