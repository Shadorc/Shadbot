package me.shadorc.discordbot.command.fun;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import me.shadorc.discordbot.utility.Utils;
import me.shadorc.infonet.Infonet;

public class JokeCmd extends Command {

	public JokeCmd() {
		super("blague", "joke");
	}

	@Override
	public void execute(Context context) {
		try {
			String htmlPage = Infonet.getHTML(new URL("https://www.blague-drole.net/blagues-" + Utils.rand(10)+1 + ".html?tri=top"));
			ArrayList <String> jokesList = Infonet.getAllSubstring(htmlPage, " \"description\": \"", "</script>");
			String joke = jokesList.get(Utils.rand(jokesList.size()));
			joke = joke.substring(0, joke.lastIndexOf("\"")).trim();
			BotUtils.sendMessage("```" + Utils.convertToUTF8(joke) + "```", context.getChannel());
		} catch (IOException e) {
			Log.error("Une erreur est survenue lors de la récupération de la blague.", e, context.getChannel());
		}
	}

}
