package me.shadorc.discordbot.command.admin;

import java.time.temporal.ChronoUnit;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;

public class ToggleNSFWCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public ToggleNSFWCmd() {
		super(Role.ADMIN, "toggle_nsfw");
		rateLimiter = new RateLimiter(RateLimiter.COMMON_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		if(!BotUtils.hasPermission(context.getChannel(), Permissions.MANAGE_CHANNELS)) {
			BotUtils.sendMessage(Emoji.ACCESS_DENIED + " I can't execute this command due to the lack of permission."
					+ "\nPlease, check my permissions and channel-specific ones to verify that **Manage channels** is checked.",
					context.getChannel());
			LogUtils.info("{Guild ID: " + context.getChannel().getGuild().getLongID() + "} "
					+ "Shadbot wasn't allowed to manage channel.");
			return;
		}

		boolean wasNSFW = context.getChannel().isNSFW();
		context.getChannel().changeNSFW(!wasNSFW);
		BotUtils.sendMessage(Emoji.CHECK_MARK + " This channel is now " + (wasNSFW ? "" : "N") + "SFW.", context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Toggle NSFW in the channel where the command is executed.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
