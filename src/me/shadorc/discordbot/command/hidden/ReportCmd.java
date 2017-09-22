package me.shadorc.discordbot.command.hidden;

import java.time.temporal.ChronoUnit;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class ReportCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public ReportCmd() {
		super(CommandCategory.HIDDEN, Role.USER, "report", "suggest");
		this.rateLimiter = new RateLimiter(30, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		BotUtils.sendMessage("{Guild ID: " + context.getGuild().getLongID() + "} "
				+ context.getAuthorName() + " (ID: " + context.getAuthor().getLongID() + ") say: " + context.getArg(),
				Shadbot.getClient().getChannelByID(Config.SUGGEST_CHANNEL_ID));
		BotUtils.sendMessage(Emoji.CHECK_MARK + " Report sent, thank you !", context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Send a message to my author, this can be a suggestion, a bug report, anything.**")
				.appendField("Usage", "`" + context.getPrefix() + "report <message>`", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}