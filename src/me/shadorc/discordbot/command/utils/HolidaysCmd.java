package me.shadorc.discordbot.command.utils;


import java.awt.Color;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.Log;
import me.shadorc.discordbot.utils.TwitterUtils;
import sx.blah.discord.util.EmbedBuilder;
import twitter4j.TwitterException;

public class HolidaysCmd extends Command {

	public HolidaysCmd() {
		super(false, "vacances", "vacs", "holidays");
	}

	@Override
	public void execute(Context context) {
		if(context.getArg() == null) {
			throw new IllegalArgumentException();
		}

		try {
			TwitterUtils.connection();
			String holidays = TwitterUtils.getInstance().getUserTimeline("Vacances_Zone" + context.getArg().toUpperCase()).get(0).getText().replaceAll("#", "");
			BotUtils.sendMessage(Emoji.BEACH + " " + holidays, context.getChannel());
		} catch (TwitterException e) {
			if(e.getErrorCode() == 34) {
				throw new IllegalArgumentException();
			} else {
				Log.error("Une erreur est survenue lors de la récupération des informations concernant les vacances.", e, context.getChannel());
			}
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Aide pour la commande /" + context.getArg())
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(new Color(170, 196, 222))
				.appendDescription("**Affiche le nombre de jours restants avant les prochaines vacances scolaires pour la zone indiquée.**")
				.appendField("Utilisation", "/vacs <A|B|C>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
