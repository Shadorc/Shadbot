package me.shadorc.shadbot.command.music;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.TrackScheduler;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.MUSIC, names = { "volume" }, alias = "vol")
public class VolumeCmd extends AbstractCommand {

	@Override
	public void execute(Context context) {
		GuildMusic guildMusic = context.requireGuildMusic();

		TrackScheduler scheduler = guildMusic.getScheduler();
		if(!context.getArg().isPresent()) {
			BotUtils.sendMessage(String.format(Emoji.SOUND + " Current volume level: **%d%%**",
					scheduler.getAudioPlayer().getVolume()),
					context.getChannel());
			return;
		}

		final String arg = context.getArg().get();
		Integer volume = NumberUtils.asPositiveInt(arg);
		if(volume == null) {
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid volume.", arg));
		}

		scheduler.setVolume(volume);
		BotUtils.sendMessage(String.format(Emoji.SOUND + " Volume level set to **%s%%**",
				scheduler.getAudioPlayer().getVolume()),
				context.getChannel());
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show or change current volume level.")
				.addArg("volume", "must be between 0 and 100", true)
				.build();
	}
}
