package me.shadorc.discordbot.command.utility;

import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import me.shadorc.discordbot.utility.TwitterUtils;
import twitter4j.TwitterException;

public class HolidaysCmd extends Command {

	public HolidaysCmd() {
		super(false, "vacances", "vacs", "holidays");
	}

	@Override
	public void execute(Context context) {
		if(context.getArg() == null) {
			BotUtils.sendMessage("Merci d'indiquer une zone : A, B ou C.", context.getChannel());
			return;
		}

		try {
			TwitterUtils.connection();
			String holidays = TwitterUtils.getInstance().getUserTimeline("Vacances_Zone" + context.getArg().toUpperCase()).get(0).getText().replaceAll("#", "");
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
