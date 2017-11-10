package me.shadorc.discordbot.command.hidden;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import sx.blah.discord.util.EmbedBuilder;

public class ReportCmd extends AbstractCommand {

	public ReportCmd() {
		super(CommandCategory.HIDDEN, Role.USER, 30, "report", "suggest");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		BotUtils.sendMessage("{Guild ID: " + context.getGuild().getLongID() + " | User ID "
				+ context.getAuthor().getLongID() + "} " + context.getArg(),
				Shadbot.getClient().getChannelByID(Config.SUGGEST_CHANNEL_ID));
		BotUtils.sendMessage(Emoji.CHECK_MARK + " Report sent, thank you !", context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Send a message to my owner, this can be a suggestion, a bug report, anything.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " <message>`", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

}