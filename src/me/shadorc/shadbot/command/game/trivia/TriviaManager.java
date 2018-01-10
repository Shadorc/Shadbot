package me.shadorc.shadbot.command.game.trivia;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.http.ParseException;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.game.AbstractGameManager;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.data.stats.Stats.MoneyEnum;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.message.MessageListener;
import me.shadorc.shadbot.message.MessageManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.CastUtils;
import me.shadorc.shadbot.utils.DateUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.JSONUtils;
import me.shadorc.shadbot.utils.MathUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class TriviaManager extends AbstractGameManager implements MessageListener {

	protected static final int MIN_GAINS = 100;
	protected static final int MAX_BONUS = 100;
	protected static final int LIMITED_TIME = 30;

	private final Integer categoryID;
	private final ConcurrentHashMap<IUser, Boolean> alreadyAnswered;

	private long startTime;
	private String correctAnswer;
	private List<String> answers;

	public TriviaManager(AbstractCommand cmd, IChannel channel, IUser author, Integer categoryID) {
		super(cmd, channel, author);
		this.categoryID = categoryID;
		this.alreadyAnswered = new ConcurrentHashMap<>();
	}

	// Trivia API doc : https://opentdb.com/api_config.php
	@Override
	public void start() throws JSONException, IOException, ParseException {
		String url = String.format("https://opentdb.com/api.php?amount=1&category=%s", categoryID == null ? "" : categoryID.toString());
		String jsonStr = NetUtils.getBody(url);
		if(jsonStr.isEmpty()) {
			throw new ParseException("Body is empty.");
		}

		JSONObject resultObj = new JSONObject(jsonStr).getJSONArray("results").getJSONObject(0);

		String questionType = resultObj.getString("type");

		correctAnswer = Jsoup.parse(resultObj.getString("correct_answer")).text();

		if("multiple".equals(questionType)) {
			answers = JSONUtils.toList(resultObj.getJSONArray("incorrect_answers"), String.class);
			answers.add(MathUtils.rand(answers.size()), correctAnswer);
		} else {
			answers = new ArrayList<>(Arrays.asList("True", "False"));
		}

		String description = String.format("**%s**%n%s",
				Jsoup.parse(resultObj.getString("question")).text(),
				FormatUtils.numberedList(answers.size(), answers.size(),
						count -> "\t**" + count + "**. " + Jsoup.parse(answers.get(count - 1)).text()));

		EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
				.withAuthorName("Trivia")
				.appendDescription(description)
				.appendField("Category", String.format("`%s`", resultObj.getString("category")), true)
				.appendField("Type", String.format("`%s`", questionType), true)
				.appendField("Difficulty", String.format("`%s`", resultObj.getString("difficulty")), true)
				.withFooterText(String.format("You have %d seconds to answer.", LIMITED_TIME));

		BotUtils.sendMessage(embed.build(), this.getChannel());

		MessageManager.addListener(this.getChannel(), this);

		startTime = System.currentTimeMillis();
		this.schedule(() -> {
			BotUtils.sendMessage(String.format(Emoji.HOURGLASS + " Time elapsed, the correct answer was **%s**.", correctAnswer), this.getChannel());
			this.stop();
		}, LIMITED_TIME, TimeUnit.SECONDS);
	}

	@Override
	public void stop() {
		this.cancelScheduledTask();
		MessageManager.removeListener(this.getChannel(), this);
		TriviaCmd.MANAGERS.remove(this.getChannel().getLongID());
	}

	@Override
	public boolean intercept(IMessage message) {
		String content = message.getContent();

		// It's a number or a text
		Integer choice = CastUtils.asIntBetween(content, 1, answers.size());

		// Message is a text and doesn't match any answers, ignore it
		if(choice == null && !answers.stream().anyMatch(content::equalsIgnoreCase)) {
			return false;
		}

		IUser author = message.getAuthor();

		// If the user has already answered and has been warned, ignore him
		if(alreadyAnswered.containsKey(author) && alreadyAnswered.get(author)) {
			return false;
		}

		String answer = choice == null ? content : answers.get(choice - 1);

		if(alreadyAnswered.containsKey(author)) {
			BotUtils.sendMessage(String.format(Emoji.GREY_EXCLAMATION + " (**%s**) You can only answer once.", author.getName()),
					this.getChannel());
			alreadyAnswered.put(author, true);
		} else if(answer.equalsIgnoreCase(correctAnswer)) {
			this.win(message.getChannel(), message.getAuthor());

		} else {
			BotUtils.sendMessage(String.format(Emoji.THUMBSDOWN + " (**%s**) Wrong answer.", message.getAuthor().getName()), this.getChannel());
			alreadyAnswered.put(author, false);
		}

		return true;
	}

	private void win(IChannel channel, IUser user) {
		float coinsPerSec = (float) MAX_BONUS / LIMITED_TIME;
		long remainingSec = LIMITED_TIME - TimeUnit.MILLISECONDS.toSeconds(DateUtils.getMillisUntil(startTime));
		int gains = MIN_GAINS + (int) Math.ceil(remainingSec * coinsPerSec);

		BotUtils.sendMessage(Emoji.CLAP + " Correct ! **" + user.getName() + "**, you won **" + gains + " coins**.", channel);
		Database.getDBUser(channel.getGuild(), user).addCoins(gains);
		StatsManager.increment(MoneyEnum.MONEY_GAINED, this.getCmdName(), gains);

		this.stop();
	}

}
