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
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.MUSIC, names = { "volume" }, alias = "vol")
public class VolumeCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final GuildMusic guildMusic = context.requireGuildMusic();

		return DiscordUtils.requireSameVoiceChannel(context)
				.flatMap(voiceChannelId -> {
					final TrackScheduler scheduler = guildMusic.getScheduler();
					if(!context.getArg().isPresent()) {
						return BotUtils.sendMessage(String.format(Emoji.SOUND + " (**%s**) Current volume level: **%d%%**",
								context.getUsername(), scheduler.getAudioPlayer().getVolume()), context.getChannel())
								.then();
					}

					final String arg = context.getArg().get();
					final Integer volume = NumberUtils.asPositiveInt(arg);
					if(volume == null) {
						throw new CommandException(String.format("`%s` is not a valid volume.", arg));
					}

					scheduler.setVolume(volume);
					return BotUtils.sendMessage(String.format(Emoji.SOUND + " Volume level set to **%s%%** by **%s**.",
							scheduler.getAudioPlayer().getVolume(), context.getUsername()),
							context.getChannel())
							.then();
				});
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show or change current volume level.")
				.addArg("volume", "must be between 0 and 100", true)
				.build();
	}
}
