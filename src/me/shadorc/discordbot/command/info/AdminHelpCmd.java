package me.shadorc.discordbot.command.info;

import java.awt.Color;

import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;
import sx.blah.discord.util.EmbedBuilder;

public class AdminHelpCmd extends Command {

	public AdminHelpCmd() {
		super(true, "admin_help");
	}

	@Override
	public void execute(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Shadbot Admin Aide")
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(new Color(170, 196, 222))
				.withDesc("Aide pour les commandes administrateurs.")
				.appendField("Commandes :",
						"`/allows_channel <#channel | all>`", false)
				.withFooterText("GitHub Project Page : https://github.com/Shadorc/Shadbot")
				.withFooterIcon("https://cdn0.iconfinder.com/data/icons/octicons/1024/mark-github-512.png");

		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
