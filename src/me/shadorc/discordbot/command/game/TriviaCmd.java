package me.shadorc.discordbot.command.game;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.swing.Timer;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.Stats;
import me.shadorc.discordbot.data.StatCategory;
import me.shadorc.discordbot.data.Storage;
import me.shadorc.discordbot.message.MessageListener;
import me.shadorc.discordbot.message.MessageManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class TriviaCmd extends AbstractCommand {

	protected static final ConcurrentHashMap<IChannel, TriviaManager> CHANNELS_TRIVIA = new ConcurrentHashMap<>();
	protected static final int GAINS = 250;

	private final RateLimiter rateLimiter;

	public TriviaCmd() {
		super(CommandCategory.GAME, Role.USER, "trivia", "quizz", "question");
		this.rateLimiter = new RateLimiter(RateLimiter.GAME_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		TriviaManager triviaManager = CHANNELS_TRIVIA.get(context.getChannel());

		if(triviaManager == null) {
			try {
				triviaManager = new TriviaManager(context.getChannel());
				triviaManager.start();
				CHANNELS_TRIVIA.putIfAbsent(context.getChannel(), triviaManager);

			} catch (JSONException | IOException err) {
				LogUtils.error("Something went wrong while getting a question.... Please, try again later.", err, context);
			}

		} else {
			BotUtils.sendMessage(Emoji.INFO + " A Trivia game has already been started.", context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Start a Trivia game in which everyone can participate.**")
				.appendField("Gains", "The winner gets **" + GAINS + " coins**.", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

	public class TriviaManager implements MessageListener {

		private final IChannel channel;
		private final List<IUser> alreadyAnswered;
		private final Timer timer;

		private String correctAnswer;
		private List<String> incorrectAnswers;

		protected TriviaManager(IChannel channel) {
			this.channel = channel;
			this.alreadyAnswered = new ArrayList<>();
			this.timer = new Timer((int) TimeUnit.SECONDS.toMillis(30), event -> {
				BotUtils.sendMessage(Emoji.HOURGLASS + " Time elapsed, the good answer was **" + correctAnswer + "**.", channel);
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

			this.incorrectAnswers = Utils.convertToStringList(resultObj.getJSONArray("incorrect_answers"));

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
					.withFooterText("You have " + TimeUnit.MILLISECONDS.toSeconds(timer.getDelay()) + " seconds to answer.");

			BotUtils.sendMessage(builder.build(), channel);

			MessageManager.addListener(channel, this);

			this.correctAnswer = Jsoup.parse(correctAnswer).text();
			this.timer.start();
		}

		private void stop() {
			MessageManager.removeListener(channel);
			timer.stop();
			CHANNELS_TRIVIA.remove(channel);
		}

		@Override
		public boolean onMessageReceived(IMessage message) {
			boolean wrongAnswer = incorrectAnswers.stream().anyMatch(message.getContent()::equalsIgnoreCase);
			boolean goodAnswer = message.getContent().equalsIgnoreCase(this.correctAnswer);
			IUser author = message.getAuthor();

			if(alreadyAnswered.contains(author) && (wrongAnswer || goodAnswer)) {
				BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Sorry **" + author.getName() + "**, you can only answer once.", message.getChannel());

			} else if(wrongAnswer) {
				BotUtils.sendMessage(Emoji.THUMBSDOWN + " Wrong answer.", channel);
				alreadyAnswered.add(author);

			} else if(goodAnswer) {
				BotUtils.sendMessage(Emoji.CLAP + " Correct ! **" + author.getName() + "**, you won **" + GAINS + " coins**.", channel);
				Storage.getUser(message.getGuild(), author).addCoins(GAINS);
				Stats.increment(StatCategory.MONEY_GAINS_COMMAND, TriviaCmd.this.getNames()[0], GAINS);
				this.stop();
			}
			return true;
		}
	}
}
