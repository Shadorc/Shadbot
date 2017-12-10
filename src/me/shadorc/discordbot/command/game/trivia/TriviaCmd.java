package me.shadorc.discordbot.command.game.trivia;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ivkos.wallhaven4j.util.exceptions.ParseException;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.exceptions.MissingArgumentException;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.ExceptionUtils;
import me.shadorc.discordbot.utils.FormatUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

public class TriviaCmd extends AbstractCommand {

	private static final Map<Integer, String> CATEGORIES_MAP = new HashMap<>();

	static {
		try {
			JSONObject mainObj = new JSONObject(NetUtils.getBody("https://opentdb.com/api_category.php"));
			JSONArray categories = mainObj.getJSONArray("trivia_categories");
			for(int i = 0; i < categories.length(); i++) {
				CATEGORIES_MAP.put(categories.getJSONObject(i).getInt("id"), categories.getJSONObject(i).getString("name"));
			}
		} catch (JSONException | IOException err) {
			LogUtils.error("An error occurred while getting Trivia categories.", err);
		}
	}

	public TriviaCmd() {
		super(CommandCategory.GAME, Role.USER, RateLimiter.GAME_COOLDOWN, "trivia");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(context.getArg().equals("categories")) {
			BotUtils.sendMessage(this.getCategoriesEmbed(), context.getChannel());
			return;
		}

		if(context.hasArg()
				&& (!StringUtils.isPositiveInt(context.getArg()) || !CATEGORIES_MAP.containsKey(Integer.parseInt(context.getArg())))) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid ID, use `" + context.getPrefix() + this.getFirstName() + " categories` "
					+ "to see the complete list of categories.", context.getChannel());
			return;
		}

		TriviaManager triviaManager = TriviaManager.CHANNELS_TRIVIA.get(context.getChannel().getLongID());

		if(triviaManager == null) {
			try {
				triviaManager = new TriviaManager(context);
				triviaManager.start();
				TriviaManager.CHANNELS_TRIVIA.putIfAbsent(context.getChannel().getLongID(), triviaManager);
			} catch (ParseException err) {
				BotUtils.sendMessage(Emoji.RED_FLAG + " I can't get a question right now, please try again later.", context.getChannel());
				LogUtils.info("{" + this.getClass().getSimpleName() + "} Empty body.");
			} catch (JSONException | IOException err) {
				ExceptionUtils.manageException("getting a question", context, err);
			}

		} else {
			BotUtils.sendMessage(Emoji.INFO + " A Trivia game has already been started.", context.getChannel());
		}
	}

	private EmbedObject getCategoriesEmbed() {
		List<Integer> idList = new ArrayList<Integer>(CATEGORIES_MAP.keySet());
		Collections.sort(idList);
		return Utils.getDefaultEmbed()
				.withAuthorName("Trivia categories")
				.appendField("ID", FormatUtils.formatList(idList, catID -> Integer.toString(catID), "\n"), true)
				.appendField("Name", FormatUtils.formatList(idList, catID -> CATEGORIES_MAP.get(catID), "\n"), true)
				.build();
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Start a Trivia game in which everyone can participate.**")
				.appendField("Usage", context.getPrefix() + this.getFirstName() + " [<categoryID>]", false)
				.appendField("Argument", "**categoryID** - [OPTIONAL] the category ID of the question", false)
				.appendField("Category", "Use `" + context.getPrefix() + this.getFirstName() + " categories` to see the list of categories", false)
				.appendField("Gains", "The winner gets **" + TriviaManager.MIN_GAINS + " coins** plus a bonus depending on his speed to answer.", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}
