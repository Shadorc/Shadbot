package me.shadorc.discordbot.command.admin;

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
		//EmbedBuilder doc : https://discord4j.readthedocs.io/en/latest/Making-embedded-content-using-EmbedBuilder/
		EmbedBuilder builder = new EmbedBuilder();

		builder.withAuthorName("Shadbot Admin Aide");
		builder.withAuthorIcon(context.getClient().getOurUser().getAvatarURL());
		builder.withColor(new Color(170, 196, 222));
		builder.withDesc("Aide pour les commandes administrateurs.");
		builder.appendField("Commandes :",
				"`/allows_channel <#channel | all>`", false)
		.withFooterText("GitHub Project Page : https://github.com/Shadorc/Shadbot")
		.withFooterIcon("https://cdn0.iconfinder.com/data/icons/octicons/1024/mark-github-512.png");

		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
