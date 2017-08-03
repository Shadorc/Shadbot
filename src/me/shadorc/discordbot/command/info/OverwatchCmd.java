package me.shadorc.discordbot.command.info;

import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.HtmlUtils;
import me.shadorc.discordbot.utils.Log;
import sx.blah.discord.util.EmbedBuilder;

public class OverwatchCmd extends Command {

	public OverwatchCmd() {
		super(false, "overwatch");
	}

	@Override
	public void execute(Context context) {
		if(context.getArg() == null) {
			throw new IllegalArgumentException();
		}

		String[] splitArgs = context.getArg().split(" ", 3);
		if(splitArgs.length != 3) {
			throw new IllegalArgumentException();
		}

		String plateform = splitArgs[0].toLowerCase();
		String region = splitArgs[1].toLowerCase();
		String battletag = splitArgs[2];

		try {
			URL url = new URL("https://playoverwatch.com/fr-fr/career/" + plateform + "/" + region + "/" + battletag.replace("#", "-"));
			String html = HtmlUtils.getHTML(url);

			String icon = HtmlUtils.parseTextHTML(html, "<div class=\"masthead-player\"><img src=\"", "<div class=\"masthead-player\"><img src=\"", "\" class=\"player-portrait\">");
			String level = HtmlUtils.parseTextHTML(html, "class=\"player-level\">", "<div class=\"u-vertical-center\">", "</div>");
			String wins = HtmlUtils.parseTextHTML(html, "<p class=\"masthead-detail h4\"><span>", "<p class=\"masthead-detail h4\"><span>", " parties remportées</span>");
			String topHero = HtmlUtils.parseTextHTML(html, "<div class=\"title\">", "<div class=\"title\">", "</div>");
			String topHeroTime = HtmlUtils.parseTextHTML(html, "<div class=\"description\">", "<div class=\"description\">", "</div>");
			String timePlayed = HtmlUtils.parseTextHTML(html, "<td>Temps de jeu</td>", "<td>Temps de jeu</td><td>", "</td>");
			String rank = HtmlUtils.parseTextHTML(html, "<div class=\"u-align-center h6\">", "<div class=\"u-align-center h6\">", "</div>");

			EmbedBuilder builder = new EmbedBuilder()
					.withAuthorName("Statistiques Overwatch")
					.withAuthorIcon("http://vignette4.wikia.nocookie.net/overwatch/images/b/bd/Overwatch_line_art_logo_symbol-only.png")
					.withThumbnail(icon)
					.withColor(new Color(170, 196, 222))
					.withDesc("Statistiques pour **" + battletag + "**.")
					.appendField("Niveau", level, true)
					.appendField("Rang compétitif", rank, true)
					.appendField("Parties remportées", wins, true)
					.appendField("Temps de jeu", timePlayed, true)
					.appendField("Top héro (Partie Rapide)", topHero + " (" + topHeroTime + ")", true)
					.withFooterText("Lien vers la carrière : " + url.toString());
			BotUtils.sendEmbed(builder.build(), context.getChannel());
		} catch (FileNotFoundException fnf) {
			BotUtils.sendMessage(Emoji.WARNING + "La plateforme, la région ou le Battletag sont incorrects.", context.getChannel());
		} catch (IOException e) {
			Log.error("Une erreur est survenue lors de la récupération des informations sur le profil Overwatch, réessayez plus tard.", e, context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Aide pour la commande /" + context.getArg())
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(new Color(170, 196, 222))
				.appendDescription("**Affiche les statistiques d'un utilisateur pour le jeu Overwatch.**")
				.appendField("Utilisation", "/overwatch <pc|psn|xbl> <eu|us|cn|kr> <battletag#0000>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
