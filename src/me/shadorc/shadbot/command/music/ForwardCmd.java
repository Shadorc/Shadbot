package me.shadorc.shadbot.command.music;

import java.util.concurrent.TimeUnit;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.MUSIC, names = { "forward" })
public class ForwardCmd extends AbstractCommand {

	@Override
	public void execute(Context context) {
		GuildMusic guildMusic = context.requireGuildMusic();
		String arg = context.requireArg();

		Long num = NumberUtils.asPositiveLong(arg);
		if(num == null) {
			try {
				num = TimeUtils.parseTime(arg);
			} catch (IllegalArgumentException err) {
				throw new IllegalCmdArgumentException(String.format("`%s` is not a valid number / time.", arg));
			}
		}

		long newPosition = guildMusic.getScheduler().changePosition(TimeUnit.SECONDS.toMillis(num));
		BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " New position: **%s**", FormatUtils.formatShortDuration(newPosition)),
				context.getChannel());
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Fast forward current song a specified amount of time.")
				.addArg("time", "can be seconds or time (e.g. 1m12s)", false)
				.build();
	}
}
