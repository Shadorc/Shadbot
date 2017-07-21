package me.shadorc.discordbot.command;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.Main;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import twitter4j.TwitterException;

public class HolidaysCommand extends Command {

	public HolidaysCommand() {
		super("vacances", "vacs");
	}

	@Override
	public void execute(Context context) {
		if(context.getArg() == null) {
			BotUtils.sendMessage("Merci d'indiquer une zone : A, B ou C.", context.getChannel());
			return;
		}

		try {
			Main.twitterConnection();
			String holidays = Main.getTwitter().getUserTimeline("Vacances_Zone" + context.getArg().toUpperCase()).get(0).getText().replaceAll("#", "");
			BotUtils.sendMessage(holidays, context.getChannel());
		} catch (TwitterException e) {
			if(e.getErrorCode() == 34) {
				BotUtils.sendMessage("La zone indiquée n'existe pas, merci d'entrer A, B ou C.", context.getChannel());
			} else {
				Log.error("Une erreur est survenue lors de la récupération des informations concernant les vacances.", e, context.getChannel());
			}
		}		
	}

}
