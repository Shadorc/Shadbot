package me.shadorc.discordbot.command.game.trivia;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import com.ivkos.wallhaven4j.util.exceptions.ParseException;

import me.shadorc.discordbot.command.CommandManager;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.message.MessageListener;
import me.shadorc.discordbot.message.MessageManager;
import me.shadorc.discordbot.stats.StatsManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.FormatUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

class TriviaManager implements MessageListener {

	protected static final ConcurrentHashMap<Long, TriviaManager> CHANNELS_TRIVIA = new ConcurrentHashMap<>();

	protected static final int MIN_GAINS = 100;
	protected static final int MAX_BONUS = 100;
	protected static final int LIMITED_TIME = 30;

	private final RateLimiter rateLimiter;
	private final Context context;
	private final List<IUser> alreadyAnswered;

	private ScheduledExecutorService executor;
	private long startTime;
	private String correctAnswer;
	private List<String> answers;

	protected TriviaManager(Context context) {
		this.rateLimiter = new RateLimiter(LIMITED_TIME, ChronoUnit.SECONDS);
		this.context = context;
		this.alreadyAnswered = new ArrayList<>();
	}

	// Trivia API doc : https://opentdb.com/api_config.php
	protected void start() throws JSONException, IOException, ParseException {
		String jsonStr = NetUtils.getBody("https://opentdb.com/api.php?amount=1");
		if(jsonStr.isEmpty()) {
			throw new ParseException("Body is empty.");
		}

		JSONObject resultObj = new JSONObject(jsonStr).getJSONArray("results").getJSONObject(0);

		String type = resultObj.getString("type");

		correctAnswer = Jsoup.parse(resultObj.getString("correct_answer")).text();

		StringBuilder strBuilder = new StringBuilder("**" + Jsoup.parse(resultObj.getString("question")).text() + "**");
		if("multiple".equals(type)) {
			answers = Utils.convertToList(resultObj.getJSONArray("incorrect_answers"), String.class);
			answers.add(MathUtils.rand(answers.size()), correctAnswer);
		} else {
			answers = new ArrayList<>(Arrays.asList("True", "False"));
		}

		strBuilder.append(FormatUtils.formatList(answers,
				answer -> "\n\t**" + (answers.indexOf(answer) + 1) + "**. " + Jsoup.parse(answer).text(), ""));

		EmbedBuilder builder = Utils.getDefaultEmbed()
				.withAuthorName("Trivia")
				.appendDescription(strBuilder.toString())
				.appendField("Category", "`" + resultObj.getString("category") + "`", true)
				.appendField("Type", "`" + type + "`", true)
				.appendField("Difficulty", "`" + resultObj.getString("difficulty") + "`", true)
				.withFooterText("You have " + LIMITED_TIME + " seconds to answer.");

		BotUtils.sendMessage(builder.build(), context.getChannel());

		MessageManager.addListener(context.getChannel(), this);

		startTime = System.currentTimeMillis();
		executor = Executors.newSingleThreadScheduledExecutor(Utils.getThreadFactoryNamed("Shadbot-TriviaManager@" + this.hashCode()));
		executor.schedule(() -> {
			BotUtils.sendMessage(Emoji.HOURGLASS + " Time elapsed, the correct answer was **" + correctAnswer + "**.", context.getChannel());
			this.stop();
		}, LIMITED_TIME, TimeUnit.SECONDS);
	}

	private void stop() {
		executor.shutdownNow();
		MessageManager.removeListener(context.getChannel(), this);
		CHANNELS_TRIVIA.remove(context.getChannel().getLongID());
	}

	private void win(IChannel channel, IUser user) {
		float coinsPerSec = (float) MAX_BONUS / LIMITED_TIME;
		long remainingSec = TimeUnit.MILLISECONDS.toSeconds(MathUtils.remainingTime(startTime, TimeUnit.SECONDS.toMillis(LIMITED_TIME)));
		int gains = MIN_GAINS + (int) Math.ceil(remainingSec * coinsPerSec);

		BotUtils.sendMessage(Emoji.CLAP + " Correct ! **" + user.getName() + "**, you won **" + gains + " coins**.", context.getChannel());
		DatabaseManager.addCoins(channel, user, gains);
		StatsManager.increment(CommandManager.getFirstName(context.getCommand()), gains);

		this.stop();
	}

	@Override
	public boolean onMessageReceived(IMessage message) {
		String content = message.getContent();

		boolean isValidInt = StringUtils.isIntBetween(content, 1, answers.size());
		if(!answers.stream().anyMatch(content::equalsIgnoreCase) && !isValidInt) {
			return false;
		}

		boolean isGoodAnswer;
		if(isValidInt) {
			isGoodAnswer = answers.get(Integer.parseInt(content) - 1).equalsIgnoreCase(correctAnswer);
		} else {
			isGoodAnswer = content.equalsIgnoreCase(correctAnswer);
		}

		IUser author = message.getAuthor();
		if(alreadyAnswered.contains(author)) {
			if(rateLimiter.isLimited(message.getGuild(), message.getAuthor())) {
				return false;
			}

			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Sorry **" + author.getName() + "**, you can only answer once.", message.getChannel());
			return true;

		} else if(isGoodAnswer) {
			this.win(message.getChannel(), message.getAuthor());
			return true;

		} else {
			BotUtils.sendMessage(Emoji.THUMBSDOWN + " Wrong answer.", context.getChannel());
			alreadyAnswered.add(author);
			return true;
		}
	}
}
