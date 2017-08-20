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
				.withAuthorName("Shadbot Help (Prefix: " + context.getPrefix() + ")")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.withDesc("Get more information by using " + context.getPrefix() + "help <command>.")
				.appendField("Utils Commands:",
						"`translate`"
								+ " `wiki`"
								+ " `calc`"
								+ " `weather`"
								+ " `urban`", false)
				.appendField("Fun Commands:",
						"`chat`", false)
				.appendField("Image Commands:",
						"`image`"
								+ " `suicidegirls`"
								+ " `gif`", false)
				.appendField("Games Commands:",
						"`dice`"
								+ " `slot_machine`"
								+ " `russian_roulette`"
								+ " `trivia`"
								+ " `rps`", false)
				.appendField("Currency Commands:",
						"`transfer`"
								+ " `leaderboard`"
								+ " `coins`", false)
				.appendField("Music Commands:",
						"`play`"
								+ " `pause`"
								+ " `resume`"
								+ " `stop`"
								+ " `repeat`"
								+ " `volume`"
								+ " `next`"
								+ " `backward`"
								+ " `forward`"
								+ " `name`"
								+ " `playlist`"
								+ " `clear`"
								+ " `shuffle`"
								+ " `join`", false)
				.appendField("Games Stats Commands:",
						"`overwatch`"
								+ " `diablo`"
								+ " `cs`", false)
				.appendField("Info Commands:",
						"`info`"
								+ " `userinfo`"
								+ " `serverinfo`"
								+ " `suggest`"
								+ " `ping`", false)
				.appendField("French Commands:",
						"`dtc`"
								+ " `blague`"
								+ " `vacs`", false);

		if(context.getAuthorRole().equals(Role.ADMIN)) {
			builder.appendField("Admin Commands:",
					"`prune`"
							+ " `settings`", false);
		}

		if(context.getAuthorRole().equals(Role.OWNER)) {
			builder.appendField("Owner Commands:",
					"`shutdown`", false);
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
