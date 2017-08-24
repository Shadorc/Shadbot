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

		String prefix = context.getPrefix();

		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Shadbot Help")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.withDesc("Get more information by using " + prefix + "help <command>.")
				.appendField("Utils Commands:",
						"`" + prefix + "translate`"
								+ " `" + prefix + "wiki`"
								+ " `" + prefix + "calc`"
								+ " `" + prefix + "weather`"
								+ " `" + prefix + "urban`", false)
				.appendField("Fun Commands:",
						"`" + prefix + "leet`"
								+ " `" + prefix + "chat`", false)
				.appendField("Image Commands:",
						"`" + prefix + "image`"
								+ " `" + prefix + "suicidegirls`"
								+ " `" + prefix + "gif`", false)
				.appendField("Games Commands:",
						"`" + prefix + "dice`"
								+ " `" + prefix + "slot_machine`"
								+ " `" + prefix + "russian_roulette`"
								+ " `" + prefix + "trivia`"
								+ " `" + prefix + "rps`", false)
				.appendField("Currency Commands:",
						"`" + prefix + "transfer`"
								+ " `" + prefix + "leaderboard`"
								+ " `" + prefix + "coins`", false)
				.appendField("Music Commands:",
						"`" + prefix + "play`"
								+ " `" + prefix + "pause`"
								+ " `" + prefix + "resume`"
								+ " `" + prefix + "stop`"
								+ " `" + prefix + "repeat`"
								+ " `" + prefix + "volume`"
								+ " `" + prefix + "next`"
								+ " `" + prefix + "backward`"
								+ " `" + prefix + "forward`"
								+ " `" + prefix + "name`"
								+ " `" + prefix + "playlist`"
								+ " `" + prefix + "clear`"
								+ " `" + prefix + "shuffle`", false)
				.appendField("Games Stats Commands:",
						"`" + prefix + "overwatch`"
								+ " `" + prefix + "diablo`"
								+ " `" + prefix + "cs`", false)
				.appendField("Info Commands:",
						"`" + prefix + "info`"
								+ " `" + prefix + "userinfo`"
								+ " `" + prefix + "serverinfo`"
								+ " `" + prefix + "ping`", false)
				.appendField("French Commands:",
						"`" + prefix + "dtc`"
								+ " `" + prefix + "blague`"
								+ " `" + prefix + "vacs`", false)
				.withFooterIcon("http://www.urbanleagueneb.org/wp-content/uploads/2016/10/E-mail-Icon.png")
				.withFooterText("You can send me a suggestion, a bug report, anything by using: " + prefix + "report <message>");

		if(context.getAuthorRole().equals(Role.ADMIN) || context.getAuthorRole().equals(Role.OWNER)) {
			builder.appendField("Admin Commands:",
					"`" + prefix + "prune`"
							+ " `" + prefix + "settings`", false);
		}

		if(context.getAuthorRole().equals(Role.OWNER)) {
			builder.appendField("Owner Commands:",
					"`" + prefix + "shutdown`", false);
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
