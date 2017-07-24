package me.shadorc.discordbot.command;

import java.awt.Color;

import me.shadorc.discordbot.utility.BotUtils;
import sx.blah.discord.util.EmbedBuilder;

public class HelpCmd extends Command {

	public HelpCmd() {
		super(false, "help", "aide");
	}

	@Override
	public void execute(Context context) {
		//EmbedBuilder doc : https://discord4j.readthedocs.io/en/latest/Making-embedded-content-using-EmbedBuilder/
		EmbedBuilder builder = new EmbedBuilder();

		builder.withAuthorName("Shadbot Aide");
		builder.withAuthorIcon(context.getClient().getOurUser().getAvatarURL());
		builder.withColor(new Color(170, 196, 222));
		builder.withDesc("Aide pour les commandes. Pour plus d'informations, utilisez /help <commande>.");
		builder.appendField("Commandes Utiles :",
				"`/trad <lang1> <lang2> <texte>`"
						+ "\n`/wiki <recherche>`"
						+ "\n`/vacs <zone>`"
						+ "\n`/calc <calcul>`"
						+ "\n`/meteo <ville>`", false);
		builder.appendField("Commandes Fun :",
				"`/chat <message>`"
						+ "\n`/gif <tag>`"
						+ "\n`/gif`"
						+ "\n`/dtc`"
						+ "\n`/blague`", false);
		builder.appendField("Commandes Jeux :",
				"`/transfert <montant> <utilisateur>`"
						+ "\n`/roulette_russe`"
						+ "\n`/machine_sous`"
						+ "\n`/trivia`"
						+ "\n`/coins`", false);
		builder.appendField("Commandes Musique:",
				"`/play <music>`"
						+ "\n`/volume <0-100>`"
						+ "\n`/pause`"
						+ "\n`/stop`"
						+ "\n`/next`"
						+ "\n`/nom`"
						+ "\n`/playlist`", false);

		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
