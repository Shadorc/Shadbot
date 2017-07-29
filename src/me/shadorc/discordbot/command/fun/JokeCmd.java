package me.shadorc.discordbot.command.fun;

import java.awt.Color;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import me.shadorc.discordbot.utility.NetUtils;
import me.shadorc.discordbot.utility.Utils;
import sx.blah.discord.util.EmbedBuilder;

public class JokeCmd extends Command {

	public JokeCmd() {
		super(false, "blague", "joke");
	}

	@Override
	public void execute(Context context) {
		try {
			String htmlPage = NetUtils.getHTML(new URL("https://www.blague-drole.net/blagues-" + Utils.rand(1, 10) + ".html?tri=top"));
			ArrayList <String> jokesList = NetUtils.getAllSubstring(htmlPage, " \"description\": \"", "</script>");
			String joke = jokesList.get(Utils.rand(jokesList.size()));
			joke = joke.substring(0, joke.lastIndexOf("\"")).trim();
			BotUtils.sendMessage("```" + Utils.convertToUTF8(joke).replace("\n\n", "\n") + "```", context.getChannel());
		} catch (IOException e) {
			Log.error("Une erreur est survenue lors de la récupération de la blague.", e, context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Aide pour la commande /" + context.getArg())
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(new Color(170, 196, 222))
				.appendDescription("**Affiche une blague aléatoire provenant du site blague-drole.net.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
