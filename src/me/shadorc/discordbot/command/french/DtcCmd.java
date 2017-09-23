package me.shadorc.discordbot.command.french;

import java.io.IOException;
import java.time.temporal.ChronoUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.data.Config.APIKey;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class DtcCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public DtcCmd() {
		super(CommandCategory.FRENCH, Role.USER, "dtc");
		this.rateLimiter = new RateLimiter(RateLimiter.COMMON_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		try {
			JSONArray arrayObj = new JSONArray(NetUtils.getBody("http://api.danstonchat.com/0.3/view/random?"
					+ "key=" + Config.get(APIKey.DTC_API_KEY)
					+ "&format=json"));

			JSONObject quoteObj;
			String content;
			do {
				quoteObj = arrayObj.getJSONObject(MathUtils.rand(arrayObj.length()));
				content = quoteObj.getString("content");
				content = content.replace("*", "\\*");
			} while(content.length() > 1000);

			StringBuilder strBuilder = new StringBuilder();
			for(String line : content.split("\n")) {
				strBuilder.append('\n');
				if(line.contains(" ")) {
					strBuilder.append("**" + line.substring(0, line.indexOf(' ')) + "** " + line.substring(line.indexOf(' ') + 1));
				} else {
					strBuilder.append(line);
				}
			}

			EmbedBuilder embed = Utils.getDefaultEmbed()
					.withAuthorName("Quote DansTonChat")
					.withUrl("https://danstonchat.com/" + quoteObj.getString("id") + ".html")
					.withThumbnail("https://danstonchat.com/themes/danstonchat/images/logo2.png")
					.appendDescription(strBuilder.toString());
			BotUtils.sendEmbed(embed.build(), context.getChannel());

		} catch (JSONException | IOException err) {
			LogUtils.error("Something went wrong while getting a quote from DansTonChat.com... Please, try again later.", err, context);
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show a random quote from DansTonChat.com**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
