package me.shadorc.discordbot.command;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.utility.BotUtils;

public class HelpCmd extends Command {

	public HelpCmd() {
		super("help", "aide");
	}

	@Override
	public void execute(Context context) {
		BotUtils.sendMessage("__**Commandes disponibles :**__"
				+ "\n**Utile**"
				+ "\n\t/trad <lang1> <lang2> <texte>"
				+ "\n\t/wiki <recherche>"
				+ "\n\t/vacances <zone>"
				+ "\n\t/calc <calcul>"
				+ "\n\t/meteo <ville>"
				+ "\n**Fun**"
				+ "\n\t/chat <message>"
				+ "\n\t/gif <tag>"
				+ "\n\t/gif"
				+ "\n\t/dtc"
				+ "\n\t/blague"
				+ "\n**Coins**"
				+ "\n\t/roulette_russe"
				+ "\n\t/machine_sous"
				+ "\n\t/trivia"
				+ "\n\t/coins"
				+ "\n**Music**"
				+ "\n\t/join"
				+ "\n\t/leave"
				+ "\n\t/play <music>"
				+ "\n\t/volume <1-100>"
				, context.getChannel());
	}

}
