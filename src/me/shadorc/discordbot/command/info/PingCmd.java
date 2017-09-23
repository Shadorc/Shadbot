package me.shadorc.discordbot.command.info;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class PingCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public PingCmd() {
		super(CommandCategory.INFO, Role.USER, "ping");
		this.rateLimiter = new RateLimiter(RateLimiter.COMMON_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		long ping = Math.abs(ChronoUnit.MILLIS.between(LocalDateTime.now(), context.getMessage().getCreationDate()));
		BotUtils.sendMessage(Emoji.GEAR + " Ping: " + ping + "ms", context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show Shadbot's ping.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
