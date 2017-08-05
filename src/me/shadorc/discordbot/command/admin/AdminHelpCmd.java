package me.shadorc.discordbot.command.admin;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import sx.blah.discord.util.EmbedBuilder;

public class AdminHelpCmd extends Command {

	public AdminHelpCmd() {
		super(true, "admin_help");
	}

	@Override
	public void execute(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Shadbot Admin Help")
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.withDesc("Get more information by using /help <command>.")
				.appendField("Commands:",
						"`/allows_channel`"
								+ " `/debug`", false)
				.withFooterText("GitHub Project Page : https://github.com/Shadorc/Shadbot")
				.withFooterIcon("https://cdn0.iconfinder.com/data/icons/octicons/1024/mark-github-512.png");

		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for /" + this.getNames()[0])
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Show help for admin commands.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
