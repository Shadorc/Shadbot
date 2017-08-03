package me.shadorc.discordbot.command.info;

import java.awt.Color;

import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import sx.blah.discord.util.EmbedBuilder;

public class HelpCmd extends Command {

	public HelpCmd() {
		super(false, "help", "aide");
	}

	@Override
	public void execute(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Shadbot Aide")
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(new Color(170, 196, 222))
				.withDesc("Obtenez plus d'informations en utilisant /help <commande>.")
				.appendField("Commandes Utiles :",
						"`/trad`"
								+ " `/wiki`"
								+ " `/vacs`"
								+ " `/calc`"
								+ " `/meteo`", false)
				.appendField("Commandes Fun :",
						"`/chat`"
								+ " `/gif`"
								+ " `/dtc`"
								+ " `/blague`", false)
				.appendField("Commandes Jeux :",
						"`/dice`"
								+ " `/roulette_russe`"
								+ " `/machine_sous`"
								+ " `/trivia`", false)
				.appendField("Commandes Argent :",
						"`/transfert`"
								+ " `/leaderboard`"
								+ " `/coins`", false)
				.appendField("Commandes Musique:",
						"`/play`"
								+ " `/volume`"
								+ " `/pause`"
								+ " `/repeat`"
								+ " `/stop`"
								+ " `/next`"
								+ " `/nom`"
								+ " `/playlist`", false)
				.appendField("Commandes Infos:",
						"`/overwatch`"
								+ " `/cs`"
								+ " `/ping`", false)
				.withFooterText("GitHub Project Page : https://github.com/Shadorc/Shadbot")
				.withFooterIcon("https://cdn0.iconfinder.com/data/icons/octicons/1024/mark-github-512.png");

		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Aide pour la commande /" + context.getArg())
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(new Color(170, 196, 222))
				.appendDescription("**Affiche l'aide pour les commandes.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
