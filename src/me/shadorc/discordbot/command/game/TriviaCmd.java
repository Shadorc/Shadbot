package me.shadorc.discordbot.command.game;

import java.io.IOException;
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
import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.data.StatCategory;
import me.shadorc.discordbot.data.StatsManager;
import me.shadorc.discordbot.message.MessageListener;
import me.shadorc.discordbot.message.MessageManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.ExceptionUtils;
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

	protected static final ConcurrentHashMap<Long, TriviaManager> CHANNELS_TRIVIA = new ConcurrentHashMap<>();
	protected static final int MIN_GAINS = 150;
	protected static final int MAX_BONUS = 100;
	protected static final int LIMITED_TIME = 30;

	public TriviaCmd() {
		super(CommandCategory.GAME, Role.USER, RateLimiter.GAME_COOLDOWN, "trivia", "quizz", "question");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		TriviaManager triviaManager = CHANNELS_TRIVIA.get(context.getChannel().getLongID());

		if(triviaManager == null) {
			try {
				triviaManager = new TriviaManager(context.getChannel());
				triviaManager.start();
				CHANNELS_TRIVIA.putIfAbsent(context.getChannel().getLongID(), triviaManager);

			} catch (JSONException | IOException err) {
				ExceptionUtils.manageException("getting a question", context, err);
			}

		} else {
			BotUtils.sendMessage(Emoji.INFO + " A Trivia game has already been started.", context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Start a Trivia game in which everyone can participate.**")
				.appendField("Gains", "The winner gets **" + MIN_GAINS + " coins** plus a bonus depending on his speed to answer.", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

	public class TriviaManager implements MessageListener {

		private final IChannel channel;
		private final List<IUser> alreadyAnswered;
		private final Timer timer;

		private long startTime;
		private String correctAnswer;
		private List<String> incorrectAnswers;

		protected TriviaManager(IChannel channel) {
			this.channel = channel;
			this.alreadyAnswered = new ArrayList<>();
			this.timer = new Timer((int) TimeUnit.SECONDS.toMillis(LIMITED_TIME), event -> {
				BotUtils.sendMessage(Emoji.HOURGLASS + " Time elapsed, the correct answer was **" + correctAnswer + "**.", channel);
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

			BotUtils.sendMessage(builder.build(), channel);

			MessageManager.addListener(channel, this);

			this.correctAnswer = Jsoup.parse(correctAnswer).text();
			this.startTime = System.currentTimeMillis();
			this.timer.start();
		}

		private void stop() {
			MessageManager.removeListener(channel, this);
			timer.stop();
			CHANNELS_TRIVIA.remove(channel.getLongID());
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
				BotUtils.sendMessage(Emoji.THUMBSDOWN + " Wrong answer.", channel);
				alreadyAnswered.add(author);
				return true;

			} else if(goodAnswer) {
				int gains = MIN_GAINS + (int) Math.ceil((LIMITED_TIME - (System.currentTimeMillis() - startTime) / 1000) * (float) (MAX_BONUS / LIMITED_TIME));
				BotUtils.sendMessage(Emoji.CLAP + " Correct ! **" + author.getName() + "**, you won **" + gains + " coins**.", channel);
				DatabaseManager.addCoins(message.getGuild(), author, gains);
				StatsManager.increment(StatCategory.MONEY_GAINS_COMMAND, TriviaCmd.this.getFirstName(), gains);
				this.stop();
				return true;
			}
			return false;
		}
	}
}
