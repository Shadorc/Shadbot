package me.shadorc.discordbot.command.info;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandManager;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import sx.blah.discord.util.EmbedBuilder;

public class HelpCmd extends AbstractCommand {

	public HelpCmd() {
		super(Role.USER, "help", "aide");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(context.hasArg() && CommandManager.getInstance().getCommand(context.getArg()) != null) {
			CommandManager.getInstance().getCommand(context.getArg()).showHelp(context);
			return;
		}

		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Shadbot Help")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.withDesc("Get more information by using " + context.getPrefix() + "help <command>.")
				.appendField("Utils Commands:",
						"`" + context.getPrefix() + "translate`"
								+ " `" + context.getPrefix() + "wiki`"
								+ " `" + context.getPrefix() + "calc`"
								+ " `" + context.getPrefix() + "weather`"
								+ " `" + context.getPrefix() + "urban`", false)
				.appendField("Fun Commands:",
						"`" + context.getPrefix() + "chat`", false)
				.appendField("Image Commands:",
						"`" + context.getPrefix() + "image`"
								+ " `" + context.getPrefix() + "gif`", false)
				.appendField("Games Commands:",
						"`" + context.getPrefix() + "dice`"
								+ " `" + context.getPrefix() + "slot_machine`"
								+ " `" + context.getPrefix() + "russian_roulette`"
								+ " `" + context.getPrefix() + "trivia`"
								+ " `" + context.getPrefix() + "rps`", false)
				.appendField("Currency Commands:",
						"`" + context.getPrefix() + "transfer`"
								+ " `" + context.getPrefix() + "leaderboard`"
								+ " `" + context.getPrefix() + "coins`", false)
				.appendField("Music Commands:",
						"`" + context.getPrefix() + "play`"
								+ " `" + context.getPrefix() + "volume`"
								+ " `" + context.getPrefix() + "pause`"
								+ " `" + context.getPrefix() + "resume`"
								+ " `" + context.getPrefix() + "repeat`"
								+ " `" + context.getPrefix() + "stop`"
								+ " `" + context.getPrefix() + "next`"
								+ " `" + context.getPrefix() + "name`"
								+ " `" + context.getPrefix() + "playlist`"
								+ " `" + context.getPrefix() + "clear`"
								+ " `" + context.getPrefix() + "shuffle`", false)
				.appendField("Games Stats Commands:",
						"`" + context.getPrefix() + "overwatch`"
								+ " `" + context.getPrefix() + "diablo`"
								+ " `" + context.getPrefix() + "cs`", false)
				.appendField("Info Commands:",
						"`" + context.getPrefix() + "info`"
								+ " `" + context.getPrefix() + "suggest`"
								+ " `" + context.getPrefix() + "ping`", false)
				.appendField("French Commands:",
						"`" + context.getPrefix() + "dtc`"
								+ " `" + context.getPrefix() + "blague`"
								+ " `" + context.getPrefix() + "vacs`", false);

		if(context.getAuthorRole().equals(Role.ADMIN)) {
			builder.appendField("Admin Commands:",
					"`" + context.getPrefix() + "settings`", false);
		}

		if(context.getAuthorRole().equals(Role.OWNER)) {
			builder.appendField("Owner Commands:",
					"`" + context.getPrefix() + "shutdown`", false);
		}

		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Show help for all the commands.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
