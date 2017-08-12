package me.shadorc.discordbot.command.french;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Log;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.HtmlUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.StringUtils;
import sx.blah.discord.util.EmbedBuilder;

public class JokeCmd extends Command {

	public JokeCmd() {
		super(false, "blague", "joke");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		try {
			String htmlPage = HtmlUtils.getHTML(new URL("https://www.blague-drole.net/blagues-" + MathUtils.rand(1, 10) + ".html?tri=top"));
			List<String> jokesList = HtmlUtils.getAllSubstring(htmlPage, " \"description\": \"", "</script>");
			String joke = jokesList.get(MathUtils.rand(jokesList.size()));
			joke = joke.substring(0, joke.lastIndexOf("\"")).trim();
			BotUtils.sendMessage("```" + StringUtils.convertHtmlToUTF8(joke).replace("\n\n", "\n") + "```", context.getChannel());
		} catch (IOException e) {
			Log.error("An error occured while getting a joke.", e, context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Show a random joke from blague-drole.net.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
