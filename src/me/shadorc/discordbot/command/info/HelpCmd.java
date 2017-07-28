package me.shadorc.discordbot.command.info;

import java.awt.Color;

import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;
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
				.withDesc("Aide pour les commandes.")
				.appendField("Commandes Utiles :",
						"`/trad <lang1> <lang2> <texte>`"
								+ "\n`/wiki <recherche>`"
								+ "\n`/vacs <zone>`"
								+ "\n`/calc <calcul>`"
								+ "\n`/meteo <ville>`", false)
				.appendField("Commandes Fun :",
						"`/chat <message>`"
								+ "\n`/gif <tag>`"
								+ "\n`/gif`"
								+ "\n`/dtc`"
								+ "\n`/blague`", false)
				.appendField("Commandes Jeux :",
						"`/dice <mise> <chiffre>`"
								+ "\n`/roulette_russe`"
								+ "\n`/machine_sous`"
								+ "\n`/trivia`", false)
				.appendField("Commandes Argent :",
						"`/transfert <montant> <@utilisateur>`"
								+ "\n`/leaderboard`"
								+ "\n`/coins`", false)
				.appendField("Commandes Musique:",
						"`/play <music>`"
								+ "\n`/volume <0-100>`"
								+ "\n`/pause`"
								+ "\n`/repeat`"
								+ "\n`/stop`"
								+ "\n`/next`"
								+ "\n`/nom`"
								+ "\n`/playlist`", false)
				.appendField("Commandes Infos:",
						"`/overwatch <pc|psn|xbl> <eu|us|cn|kr> <battletag#0000>`"
								+ "\n`/cs <steamID>`"
								+ "\n`/ping`", false)
				.withFooterText("GitHub Project Page : https://github.com/Shadorc/Shadbot")
				.withFooterIcon("https://cdn0.iconfinder.com/data/icons/octicons/1024/mark-github-512.png");

		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
