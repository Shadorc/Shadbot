package me.shadorc.discordbot.command.info;

import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import me.shadorc.discordbot.utility.NetUtils;
import sx.blah.discord.util.EmbedBuilder;

public class OverwatchCmd extends Command {

	public OverwatchCmd() {
		super(false, "overwatch");
	}

	@Override
	public void execute(Context context) {
		if(context.getArg() == null) {
			BotUtils.sendMessage(Emoji.WARNING + " Veuillez indiquer la plateforme, la région et le Battletag d'un utilisateur.", context.getChannel());
			return;
		}

		String[] splitArgs = context.getArg().split(" ", 3);
		if(splitArgs.length != 3) {
			BotUtils.sendMessage(Emoji.WARNING + " Veuillez indiquer la région, la plateforme et le Battletag d'un utilisateur.", context.getChannel());
			return;
		}

		String plateform = splitArgs[0].toLowerCase();
		String region = splitArgs[1].toLowerCase();
		String battletag = splitArgs[2];

		try {
			URL url = new URL("https://playoverwatch.com/fr-fr/career/" + plateform + "/" + region + "/" + battletag.replace("#", "-"));
			String html = NetUtils.getHTML(url);

			String icon = NetUtils.parseTextHTML(html, "<div class=\"masthead-player\"><img src=\"", "<div class=\"masthead-player\"><img src=\"", "\" class=\"player-portrait\">");
			String level = NetUtils.parseTextHTML(html, "class=\"player-level\">", "<div class=\"u-vertical-center\">", "</div>");
			String wins = NetUtils.parseTextHTML(html, "<p class=\"masthead-detail h4\"><span>", "<p class=\"masthead-detail h4\"><span>", " parties remportées</span>");
			String topHero = NetUtils.parseTextHTML(html, "<div class=\"title\">", "<div class=\"title\">", "</div>");
			String topHeroTime = NetUtils.parseTextHTML(html, "<div class=\"description\">", "<div class=\"description\">", "</div>");
			String timePlayed = NetUtils.parseTextHTML(html, "<td>Temps de jeu</td>", "<td>Temps de jeu</td><td>", "</td>");

			EmbedBuilder builder = new EmbedBuilder()
					.withAuthorName("Statistiques Overwatch")
					.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
					.withThumbnail(icon)
					.withColor(new Color(170, 196, 222))
					.withDesc("Statistiques en Partie Rapide pour **" + battletag + "**.")
					.appendField("Niveau", level, true)
					.appendField("Parties remportées", wins, true)
					.appendField("Temps de jeu", timePlayed, true)
					.appendField("Top héro", topHero + " (" + topHeroTime + ")", true)
					.withFooterIcon("http://vignette4.wikia.nocookie.net/overwatch/images/b/bd/Overwatch_line_art_logo_symbol-only.png")
					.withFooterText("Lien vers la carrière : " + url.toString());
			BotUtils.sendEmbed(builder.build(), context.getChannel());
		} catch (FileNotFoundException fnf) {
			BotUtils.sendMessage(Emoji.WARNING + "La plateforme, la région ou le Battletag sont incorrects.", context.getChannel());
		} catch (IOException e) {
			Log.error("Une erreur est survenue lors de la récupération des informations sur le profil Overwatch, réessayez plus tard.", e, context.getChannel());
		}
	}

}
