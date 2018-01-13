package me.shadorc.shadbot.command.game.trivia;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ivkos.wallhaven4j.util.exceptions.ParseException;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.ratelimiter.RateLimiter;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.CastUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import sx.blah.discord.api.internal.json.objects.EmbedObject;

@RateLimited(cooldown = RateLimiter.GAME_COOLDOWN, max = 1)
@Command(category = CommandCategory.GAME, names = { "trivia" })
public class TriviaCmd extends AbstractCommand {

	protected static final ConcurrentHashMap<Long, TriviaManager> MANAGERS = new ConcurrentHashMap<>();

	private final Map<Integer, String> categories = new TreeMap<>();

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		if(categories.isEmpty()) {
			this.load();
		}

		if(context.getArg().equals("categories")) {
			EmbedObject embed = EmbedUtils.getDefaultEmbed()
					.withAuthorName("Trivia categories")
					.appendField("ID", FormatUtils.format(categories.keySet().toArray(), Object::toString, "\n"), true)
					.appendField("Name", FormatUtils.format(categories.keySet().toArray(), categories::get, "\n"), true)
					.build();
			BotUtils.sendMessage(embed, context.getChannel());
			return;
		}

		Integer categoryID = CastUtils.asPositiveInt(context.getArg());

		if(context.hasArg() && (categoryID == null || !categories.containsKey(categoryID))) {
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid ID. Use `%s%s categories` to see the complete list "
					+ "of categories.", context.getArg(), context.getPrefix(), this.getName()));
		}

		TriviaManager triviaManager = MANAGERS.get(context.getChannel().getLongID());
		if(triviaManager == null) {
			triviaManager = new TriviaManager(this, context.getChannel(), context.getAuthor(), categoryID);
		}

		if(MANAGERS.putIfAbsent(context.getChannel().getLongID(), triviaManager) == null) {
			try {
				triviaManager.start();
			} catch (ParseException err) {
				BotUtils.sendMessage(Emoji.RED_FLAG + " I can't get a question right now, please try again later.", context.getChannel());
				LogUtils.infof("{%s} Empty body.", this.getClass().getSimpleName());
			} catch (JSONException | IOException err) {
				Utils.handle("getting a question", context, err);
			}
		} else {
			BotUtils.sendMessage(Emoji.INFO + " A Trivia game has already been started.", context.getChannel());
		}
	}

	private void load() {
		try {
			JSONObject mainObj = new JSONObject(NetUtils.getBody("https://opentdb.com/api_category.php"));
			JSONArray categoriesArray = mainObj.getJSONArray("trivia_categories");
			categoriesArray.forEach(obj -> categories.put(((JSONObject) obj).getInt("id"), ((JSONObject) obj).getString("name")));
		} catch (JSONException | IOException err) {
			LogUtils.errorf(err, "An error occurred while getting Trivia categories.");
		}
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Start a Trivia game in which everyone can participate.")
				.addArg("categoryID", "the category ID of the question", true)
				.appendField("Category", String.format("Use `%s%s categories` to see the list of categories", prefix, this.getName()), false)
				.setGains("The winner gets **%d coins** plus a bonus depending on his speed to answer.", TriviaManager.MIN_GAINS)
				.build();
	}
}
