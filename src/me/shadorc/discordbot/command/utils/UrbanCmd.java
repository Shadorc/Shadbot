package me.shadorc.discordbot.command.utils;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.temporal.ChronoUnit;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import sx.blah.discord.util.EmbedBuilder;

public class UrbanCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public UrbanCmd() {
		super(Role.USER, "urban", "ud");
		this.rateLimiter = new RateLimiter(5, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		if(rateLimiter.isLimited(context.getGuild(), context.getAuthor())) {
			if(!rateLimiter.isWarned(context.getGuild(), context.getAuthor())) {
				rateLimiter.warn("Take it easy, don't spam :)", context);
			}
			return;
		}

		try {
			JSONObject mainObj = new JSONObject(IOUtils.toString(new URL("https://api.urbandictionary.com/v0/define?"
					+ "term=" + URLEncoder.encode(context.getArg(), "UTF-8")), "UTF-8"));

			if(mainObj.getString("result_type").equals("no_results")) {
				BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " No results for \"" + context.getArg() + "\"", context.getChannel());
				return;
			}

			JSONObject resultObj = mainObj.getJSONArray("list").getJSONObject(0);

			String definition = resultObj.getString("definition");
			if(definition.length() > EmbedBuilder.DESCRIPTION_CONTENT_LIMIT) {
				definition = definition.substring(0, EmbedBuilder.DESCRIPTION_CONTENT_LIMIT - 3) + "...";
			}

			EmbedBuilder builder = new EmbedBuilder()
					.withAuthorName("Urban Dictionary: " + resultObj.getString("word"))
					.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
					.withThumbnail("http://www.packal.org/sites/default/files/public/styles/icon_large/public/workflow-files/florianurban/icon/icon.png")
					.withColor(Config.BOT_COLOR)
					.withDescription(definition);

			String example = resultObj.getString("example");
			if(!example.isEmpty()) {
				builder.appendField("Example", resultObj.getString("example"), false);
			}

			BotUtils.sendEmbed(builder.build(), context.getChannel());

		} catch (JSONException | IOException err) {
			LogUtils.error("Something went wrong while getting Urban Dictionary definition... Please, try again later.", err, context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Search a definition of a word with Urban Dictionary.**")
				.appendField("Usage", context.getPrefix() + "urban <word>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
