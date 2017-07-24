package me.shadorc.discordbot.command.utility;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import me.shadorc.discordbot.utility.Utils;

public class TranslateCmd extends Command {

	public TranslateCmd() {
		super(false, "trad", "translate", "traduire");
	}

	@Override
	public void execute(Context context) {
		//Country doc https://www.pastebin.com/NHWLgJ43
		if(context.getArg() == null) {
			BotUtils.sendMessage("Merci d'indiquer les 2 langues et le texte à traduire. Exemple : /trad fr en Salut", context.getChannel());
			return;
		}

		try {
			String[] args = context.getArg().split(" ", 3);
			if(args.length < 3) {
				BotUtils.sendMessage("Merci d'indiquer les 2 langues et le texte à traduire. Exemple : /trad fr en Salut", context.getChannel());
				return;
			}
			String word = Utils.translate(args[0], args[1], args[2]);
			BotUtils.sendMessage("Traduction : " + word, context.getChannel());
		} catch (Exception e) {
			Log.error("Une erreur est survenue lors de la traduction.", e, context.getChannel());
		}
	}

}
