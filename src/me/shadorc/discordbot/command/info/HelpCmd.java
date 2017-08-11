package me.shadorc.discordbot.command.info;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.CommandManager;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import sx.blah.discord.util.EmbedBuilder;

public class HelpCmd extends Command {

	public HelpCmd() {
		super(false, "help", "aide");
	}

	@Override
	public void execute(Context context) {
		if(context.getArg() != null && CommandManager.getInstance().getCommand(context.getArg()) != null) {
			CommandManager.getInstance().getCommand(context.getArg()).showHelp(context);
			return;
		}

		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Shadbot Help")
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.withDesc("Get more information by using /help <command>.")
				.appendField("Utils Commands:",
						"`/translate`"
								+ " `/wiki`"
								+ " `/calc`"
								+ " `/weather`"
								+ " `/urban`", false)
				.appendField("Fun Commands:",
						"`/chat`"
								+ " `/image`"
								+ " `/gif`", false)
				.appendField("Games Commands:",
						"`/dice`"
								+ " `/slot_machine`"
								+ " `/russian_roulette`"
								+ " `/trivia`", false)
				.appendField("Currency Commands:",
						"`/transfer`"
								+ " `/leaderboard`"
								+ " `/coins`", false)
				.appendField("Music Commands:",
						"`/play`"
								+ " `/volume`"
								+ " `/pause`"
								+ " `/repeat`"
								+ " `/stop`"
								+ " `/next`"
								+ " `/name`"
								+ " `/playlist`", false)
				.appendField("Games Stats Commands:",
						"`/overwatch`"
								+ " `/cs`", false)
				.appendField("Info Commands:",
						"`/info`"
								+ " `/ping`", false)
				.appendField("French Commands:",
						"`/dtc`"
								+ " `/blague`"
								+ " `/vacs`", false)
				.withFooterText("GitHub Project Page : https://github.com/Shadorc/Shadbot")
				.withFooterIcon("https://cdn0.iconfinder.com/data/icons/octicons/1024/mark-github-512.png");

		if(context.isAuthorAdmin()) {
			builder.appendField("Admin Commands:",
					"`/allows_channel`"
							+ " `/debug`", false);
		}

		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for /" + this.getNames()[0])
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Show help for all the commands.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
