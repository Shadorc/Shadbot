package me.shadorc.shadbot.command.music;

import java.util.function.Consumer;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.MUSIC, names = { "name", "current", "np" })
public class NameCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final AudioTrackInfo trackInfo = context.requireGuildMusic().getTrackScheduler().getAudioPlayer().getPlayingTrack().getInfo();
		return context.getChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.MUSICAL_NOTE + " (**%s**) Currently playing: **%s**",
						context.getUsername(), FormatUtils.trackName(trackInfo)), channel))
				.then();
	}

	@Override
	public Mono<Consumer<? super EmbedCreateSpec>> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show current music name.")
				.build();
	}
}