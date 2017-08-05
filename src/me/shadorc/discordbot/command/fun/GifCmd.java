package me.shadorc.discordbot.command.fun;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONObject;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.ApiKeys;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.HtmlUtils;
import me.shadorc.discordbot.utils.Log;
import sx.blah.discord.util.EmbedBuilder;

public class GifCmd extends Command {

	public GifCmd() {
		super(false, "gif");
	}

	@Override
	public void execute(Context context) {
		// if(!context.getChannel().isNSFW()) {
		// BotUtils.sendMessage(Emoji.WARNING + " Vous devez être dans un salon NSFW pour utiliser les gifs.",
		// context.getChannel());
		// return;
		// }

		if(context.getArg() == null) {
			try {
				String json = HtmlUtils.getHTML(new URL("https://api.giphy.com/v1/gifs/random?"
						+ "api_key=" + Storage.getApiKey(ApiKeys.GIPHY_API_KEY)));
				JSONObject obj = new JSONObject(json);
				String url = obj.getJSONObject("data").getString("url");
				BotUtils.sendMessage(url, context.getChannel());
			} catch (IOException e) {
				Log.error("An arror occured while getting a gif from Giphy.", e, context.getChannel());
			}

			// try {
			// String gifUrl = HtmlUtils.parseHTML(new URL("http://gifland.us"), "<meta name=\"twitter:image:src", "content=\"",
			// "\">");
			// BotUtils.sendMessage(gifUrl, context.getChannel());
			// } catch (IOException e) {
			// Log.error("Une erreur est survenue lors de la récupération d'un gif sur Gifland.", e, context.getChannel());
			// }
		}

		else {
			try {
				String json = HtmlUtils.getHTML(new URL("https://api.giphy.com/v1/gifs/random?"
						+ "api_key=" + Storage.getApiKey(ApiKeys.GIPHY_API_KEY)
						+ "&tag=" + URLEncoder.encode(context.getArg(), "UTF-8")));
				JSONObject obj = new JSONObject(json);
				if(obj.get("data") instanceof JSONArray) {
					BotUtils.sendMessage(Emoji.WARNING + " Aucun résultat pour " + context.getArg(), context.getChannel());
					return;
				}
				String url = obj.getJSONObject("data").getString("url");
				BotUtils.sendMessage(url, context.getChannel());
			} catch (IOException e) {
				Log.error("An arror occured while getting a gif from Giphy.", e, context.getChannel());
			}
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for /" + this.getNames()[0])
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Show a random gif or a gif corresponding to a tag.**")
				.appendField("Usage", "/gif or /gif <tag>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
