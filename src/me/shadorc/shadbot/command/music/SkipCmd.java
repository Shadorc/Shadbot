package me.shadorc.shadbot.command.music;

import java.util.function.Consumer;

import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

@RateLimited(max = 1, cooldown = 1)
@Command(category = CommandCategory.MUSIC, names = { "skip", "next" })
public class SkipCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final GuildMusic guildMusic = context.requireGuildMusic();

		final Mono<Message> messageMono = context.getChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.TRACK_NEXT + " Music skipped by **%s**.",
						context.getUsername()), channel));

		if(context.getArg().isPresent()) {
			final int playlistSize = guildMusic.getTrackScheduler().getPlaylist().size();
			final Integer num = NumberUtils.asIntBetween(context.getArg().get(), 1, playlistSize);
			if(num == null) {
				throw new CommandException(String.format("Number must be between 1 and %d.", playlistSize));
			}
			guildMusic.getTrackScheduler().skipTo(num);
		} else {
			if(guildMusic.getTrackScheduler().nextTrack()) {
				// If the music has been started correctly, we resume it in case the previous music was on pause
				guildMusic.getTrackScheduler().getAudioPlayer().setPaused(false);
			} else {
				// There is no more music, this is the end
				return messageMono.then(guildMusic.end());
			}
		}

		return messageMono.then();
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Skip current music and play the next one if it exists."
						+ "\nYou can also directly skip to a music in the playlist by specifying its number.")
				.addArg("num", "the number of the music in the playlist to play", true)
				.build();
	}
}