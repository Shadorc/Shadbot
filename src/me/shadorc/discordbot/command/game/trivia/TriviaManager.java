package me.shadorc.discordbot.command.game.trivia;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.swing.Timer;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import me.shadorc.discordbot.command.CommandManager;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.data.StatsManager;
import me.shadorc.discordbot.message.MessageListener;
import me.shadorc.discordbot.message.MessageManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

class TriviaManager implements MessageListener {

	protected static final ConcurrentHashMap<Long, TriviaManager> CHANNELS_TRIVIA = new ConcurrentHashMap<>();

	protected static final int MIN_GAINS = 150;
	protected static final int MAX_BONUS = 100;
	protected static final int LIMITED_TIME = 30;

	private final Context context;
	private final List<IUser> alreadyAnswered;
	private final Timer timer;

	private long startTime;
	private String correctAnswer;
	private List<String> incorrectAnswers;

	protected TriviaManager(Context context) {
		this.context = context;
		this.alreadyAnswered = new ArrayList<>();
		this.timer = new Timer((int) TimeUnit.SECONDS.toMillis(LIMITED_TIME), event -> {
			BotUtils.sendMessage(Emoji.HOURGLASS + " Time elapsed, the correct answer was **" + correctAnswer + "**.", context.getChannel());
			this.stop();
		});
	}

	// Trivia API doc : https://opentdb.com/api_config.php
	protected void start() throws JSONException, IOException {
		JSONObject mainObj = new JSONObject(NetUtils.getBody("https://opentdb.com/api.php?amount=1"));
		JSONObject resultObj = mainObj.getJSONArray("results").getJSONObject(0);

		String category = resultObj.getString("category");
		String type = resultObj.getString("type");
		String difficulty = resultObj.getString("difficulty");
		String question = resultObj.getString("question");
		String correctAnswer = resultObj.getString("correct_answer");

		this.incorrectAnswers = Utils.convertToList(resultObj.getJSONArray("incorrect_answers"), String.class);

		StringBuilder strBuilder = new StringBuilder("**" + Jsoup.parse(question).text() + "**");
		if("multiple".equals(type)) {
			// Place the correct answer randomly in the list
			int index = MathUtils.rand(incorrectAnswers.size());
			for(int i = 0; i < incorrectAnswers.size(); i++) {
				if(i == index) {
					strBuilder.append("\n\t- " + Jsoup.parse(correctAnswer).text());
				}
				strBuilder.append("\n\t- " + Jsoup.parse(incorrectAnswers.get(i)).text());
			}
		}

		EmbedBuilder builder = Utils.getDefaultEmbed()
				.withAuthorName("Trivia")
				.appendField("Question", strBuilder.toString(), false)
				.appendField("Category", "`" + category + "`", true)
				.appendField("Type", "`" + type + "`", true)
				.appendField("Difficulty", "`" + difficulty + "`", true)
				.withFooterText("You have " + LIMITED_TIME + " seconds to answer.");

		BotUtils.sendMessage(builder.build(), context.getChannel());

		MessageManager.addListener(context.getChannel(), this);

		this.correctAnswer = Jsoup.parse(correctAnswer).text();
		this.startTime = System.currentTimeMillis();
		this.timer.start();
	}

	private void stop() {
		MessageManager.removeListener(context.getChannel(), this);
		timer.stop();
		CHANNELS_TRIVIA.remove(context.getChannel().getLongID());
	}

	@Override
	public boolean onMessageReceived(IMessage message) {
		boolean wrongAnswer = incorrectAnswers.stream().anyMatch(message.getContent()::equalsIgnoreCase);
		boolean goodAnswer = message.getContent().equalsIgnoreCase(this.correctAnswer);
		IUser author = message.getAuthor();

		if(alreadyAnswered.contains(author) && (wrongAnswer || goodAnswer)) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Sorry **" + author.getName() + "**, you can only answer once.", message.getChannel());
			return true;

		} else if(wrongAnswer) {
			BotUtils.sendMessage(Emoji.THUMBSDOWN + " Wrong answer.", context.getChannel());
			alreadyAnswered.add(author);
			return true;

		} else if(goodAnswer) {
			int gains = MIN_GAINS + (int) Math.ceil((LIMITED_TIME - (System.currentTimeMillis() - startTime) / 1000) * (float) (MAX_BONUS / LIMITED_TIME));
			BotUtils.sendMessage(Emoji.CLAP + " Correct ! **" + author.getName() + "**, you won **" + gains + " coins**.", context.getChannel());
			DatabaseManager.addCoins(message.getChannel(), author, gains);
			StatsManager.updateGameStats(CommandManager.getFirstName(context.getCommand()), gains);
			this.stop();
			return true;
		}
		return false;
	}
}
