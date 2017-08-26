package me.shadorc.discordbot.command.french;

import java.io.IOException;
import java.net.URL;
import java.time.temporal.ChronoUnit;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.ApiKeys;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import sx.blah.discord.util.EmbedBuilder;

public class DtcCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public DtcCmd() {
		super(Role.USER, "dtc");
		this.rateLimiter = new RateLimiter(2, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isLimited(context.getGuild(), context.getAuthor())) {
			if(!rateLimiter.isWarned(context.getGuild(), context.getAuthor())) {
				rateLimiter.warn("Take it easy, don't spam :)", context);
			}
			return;
		}

		try {
			String url = "http://api.danstonchat.com/0.3/view/random?"
					+ "key=" + Storage.getApiKey(ApiKeys.DTC_API_KEY)
					+ "&format=json";
			JSONArray arrayObj = new JSONArray(IOUtils.toString(new URL(url), "UTF-8"));
			BotUtils.sendMessage("```" + arrayObj.getJSONObject(0).getString("content") + "```", context.getChannel());
		} catch (JSONException | IOException err) {
			LogUtils.error("Something went wrong while getting a quote from DansTonChat.com... Please, try again later.", err, context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Show a random quote from DansTonChat.com**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
