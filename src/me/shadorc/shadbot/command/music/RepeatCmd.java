package me.shadorc.shadbot.command.music;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.TrackScheduler;
import me.shadorc.shadbot.music.TrackScheduler.RepeatMode;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.MUSIC, names = { "repeat", "loop" })
public class RepeatCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final GuildMusic guildMusic = context.requireGuildMusic();

		return DiscordUtils.requireSameVoiceChannel(context)
				.flatMap(voiceChannelId -> {

					RepeatMode mode;
					if(context.getArg().isPresent()) {
						mode = Utils.getEnum(RepeatMode.class, context.getArg().get());
						if(mode == null) {
							throw new CommandException(String.format("`%s` is not a valid mode.", context.getArg().get()));
						}
					}
					// By default, modifications are made on song repeat mode
					else {
						mode = RepeatMode.SONG;
					}

					final TrackScheduler scheduler = guildMusic.getScheduler();

					scheduler.setRepeatMode(scheduler.getRepeatMode().equals(mode) ? RepeatMode.NONE : mode);

					final Emoji emoji = scheduler.getRepeatMode().equals(RepeatMode.NONE) ? Emoji.PLAY : Emoji.REPEAT;
					final String playlistRepetition = RepeatMode.PLAYLIST.equals(mode) ? "Playlist " : "";
					final String modeStr = scheduler.getRepeatMode().equals(RepeatMode.NONE) ? "disabled" : "enabled";

					return BotUtils.sendMessage(String.format("%s %sRepetition %s by **%s**.",
							emoji, playlistRepetition, modeStr, context.getUsername()), context.getChannel())
							.then();
				});
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Toggle song/playlist repetition.")
				.setUsage("[song/playlist]")
				.addArg("song/playlist", "repeat the current song/playlist", true)
				.build();
	}

}
