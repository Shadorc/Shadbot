package me.shadorc.discordbot.command.utils;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.temporal.ChronoUnit;

import org.json.JSONException;
import org.json.JSONObject;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.ExceptionUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class UrbanCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public UrbanCmd() {
		super(CommandCategory.UTILS, Role.USER, "urban");
		this.setAlias("ud");
		this.rateLimiter = new RateLimiter(RateLimiter.COMMON_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		try {
			JSONObject mainObj = new JSONObject(NetUtils.getBody("https://api.urbandictionary.com/v0/define?"
					+ "term=" + URLEncoder.encode(context.getArg(), "UTF-8")));

			if(mainObj.getString("result_type").equals("no_results")) {
				BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " No results for \"" + context.getArg() + "\"", context.getChannel());
				return;
			}

			JSONObject resultObj = mainObj.getJSONArray("list").getJSONObject(0);

			String definition = resultObj.getString("definition");
			if(definition.length() > EmbedBuilder.DESCRIPTION_CONTENT_LIMIT) {
				definition = definition.substring(0, EmbedBuilder.DESCRIPTION_CONTENT_LIMIT - 3) + "...";
			}

			EmbedBuilder builder = Utils.getDefaultEmbed()
					.withAuthorName("Urban Dictionary: " + resultObj.getString("word"))
					.withThumbnail("http://www.packal.org/sites/default/files/public/styles/icon_large/public/workflow-files/florianurban/icon/icon.png")
					.appendDescription(definition);

			String example = resultObj.getString("example");
			if(!example.isEmpty()) {
				builder.appendField("Example", resultObj.getString("example"), false);
			}

			BotUtils.sendMessage(builder.build(), context.getChannel());

		} catch (JSONException | IOException err) {
			ExceptionUtils.manageException("getting Urban Dictionary definition", context, err);
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show Urban Dictionary definition for a search.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " <search>`", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

}
