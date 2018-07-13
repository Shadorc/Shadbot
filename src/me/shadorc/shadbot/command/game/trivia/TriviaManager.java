package me.shadorc.shadbot.command.game.trivia;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.apache.http.ParseException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.game.AbstractGameManager;
import me.shadorc.shadbot.data.db.DatabaseManager;
import me.shadorc.shadbot.data.stats.MoneyStatsManager;
import me.shadorc.shadbot.data.stats.MoneyStatsManager.MoneyEnum;
import me.shadorc.shadbot.listener.interceptor.MessageInterceptor;
import me.shadorc.shadbot.listener.interceptor.MessageInterceptorManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.EmbedUtils;

public class TriviaManager extends AbstractGameManager implements MessageInterceptor {

	protected static final int MIN_GAINS = 100;
	protected static final int MAX_BONUS = 100;
	protected static final int LIMITED_TIME = 30;

	private final Integer categoryID;
	private final ConcurrentHashMap<IUser, Boolean> alreadyAnswered;

	private long startTime;
	private String correctAnswer;
	private List<String> answers;

	public TriviaManager(AbstractCommand cmd, String prefix, IChannel channel, IUser author, Integer categoryID) {
		super(cmd, prefix, channel, author);
		this.categoryID = categoryID;
		this.alreadyAnswered = new ConcurrentHashMap<>();
	}

	// Trivia API doc : https://opentdb.com/api_config.php
	@Override
	public void start() throws IOException, ParseException {
		String url = String.format("https://opentdb.com/api.php?amount=1&category=%s", categoryID == null ? "" : categoryID.toString());
		JSONObject resultObj = new JSONObject(NetUtils.getJSON(url)).getJSONArray("results").getJSONObject(0);

		String questionType = resultObj.getString("type");

		correctAnswer = Jsoup.parse(resultObj.getString("correct_answer")).text();

		if("multiple".equals(questionType)) {
			answers = Utils.toList(resultObj.getJSONArray("incorrect_answers"), String.class);
			answers.add(ThreadLocalRandom.current().nextInt(answers.size()), correctAnswer);
		} else {
			answers = List.of("True", "False");
		}

		String description = String.format("**%s**%n%s",
				Jsoup.parse(resultObj.getString("question")).text(),
				FormatUtils.numberedList(answers.size(), answers.size(),
						count -> "\t**" + count + "**. " + Jsoup.parse(answers.get(count - 1)).text()));

		EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
				.withAuthorName("Trivia")
				.appendDescription(description)
				.addField("Category", String.format("`%s`", resultObj.getString("category")), true)
				.addField("Type", String.format("`%s`", questionType), true)
				.addField("Difficulty", String.format("`%s`", resultObj.getString("difficulty")), true)
				.withFooterText(String.format("You have %d seconds to answer.", LIMITED_TIME));

		BotUtils.sendMessage(embed.build(), this.getMessageChannel());

		MessageInterceptorManager.addInterceptor(this.getMessageChannel(), this);

		startTime = System.currentTimeMillis();
		this.schedule(() -> {
			BotUtils.sendMessage(String.format(Emoji.HOURGLASS + " Time elapsed, the correct answer was **%s**.", correctAnswer), this.getMessageChannel());
			this.stop();
		}, LIMITED_TIME, TimeUnit.SECONDS);
	}

	@Override
	public void stop() {
		this.cancelScheduledTask();
		MessageInterceptorManager.removeInterceptor(this.getMessageChannel(), this);
		TriviaCmd.MANAGERS.remove(this.getMessageChannel().getLongID());
	}

	@Override
	public boolean intercept(IMessage message) {
		if(this.isCancelCmd(message)) {
			return true;
		}

		String content = message.getContent();

		// It's a number or a text
		Integer choice = NumberUtils.asIntBetween(content, 1, answers.size());

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
					this.getMessageChannel());
			alreadyAnswered.put(author, true);
		} else if(answer.equalsIgnoreCase(correctAnswer)) {
			this.win(message.getMessageChannel(), message.getAuthor());

		} else {
			BotUtils.sendMessage(String.format(Emoji.THUMBSDOWN + " (**%s**) Wrong answer.", message.getAuthor().getName()), this.getMessageChannel());
			alreadyAnswered.put(author, false);
		}

		return true;
	}

	private void win(IChannel channel, IUser user) {
		float coinsPerSec = (float) MAX_BONUS / LIMITED_TIME;
		long remainingSec = LIMITED_TIME - TimeUnit.MILLISECONDS.toSeconds(TimeUtils.getMillisUntil(startTime));
		int gains = MIN_GAINS + (int) Math.ceil(remainingSec * coinsPerSec);

		BotUtils.sendMessage(Emoji.CLAP + " Correct ! **" + user.getName() + "**, you won **" + gains + " coins**.", channel);
		DatabaseManager.getDBUser(channel.getGuild(), user).addCoins(gains);
		MoneyStatsManager.log(MoneyEnum.MONEY_GAINED, this.getCmdName(), gains);

		this.stop();
	}

}
