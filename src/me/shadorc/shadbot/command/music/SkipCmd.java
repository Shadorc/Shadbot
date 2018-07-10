package me.shadorc.shadbot.command.music;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import reactor.core.publisher.Mono;

@RateLimited(max = 1, cooldown = 1)
@Command(category = CommandCategory.MUSIC, names = { "skip", "next" })
public class SkipCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final GuildMusic guildMusic = context.requireGuildMusic();

		if(context.getArg().isPresent()) {
			final int playlistSize = guildMusic.getScheduler().getPlaylist().size();
			Integer num = NumberUtils.asIntBetween(context.getArg().get(), 1, playlistSize);
			if(num == null) {
				throw new CommandException(String.format("Number must be between 1 and %d.", playlistSize));
			}
			guildMusic.getScheduler().skipTo(num);
		}

		else {
			if(guildMusic.getScheduler().nextTrack()) {
				// If the music has been started correctly, we resume it in case the previous music was on pause
				guildMusic.getScheduler().getAudioPlayer().setPaused(false);
			} else {
				// There is no more music, this is the end
				guildMusic.end();
			}
		}

		return context.getAuthorName()
				.flatMap(username -> BotUtils.sendMessage(String.format(Emoji.TRACK_NEXT + " Music skipped by **%s**.", username), context.getChannel()))
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Skip current music and play the next one if it exists."
						+ "\nYou can also directly skip to a music in the playlist by specifying its number.")
				.addArg("num", "the number of the music in the playlist to play", true)
				.build();
	}
}