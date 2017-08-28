package me.shadorc.discordbot.command.game;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Timer;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.message.MessageListener;
import me.shadorc.discordbot.message.MessageManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class TriviaCmd extends AbstractCommand {

	protected static final int GAINS = 25;

	public TriviaCmd() {
		super(Role.USER, "trivia", "quizz", "question");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		try {
			new GuildTriviaManager(context.getChannel()).start();
		} catch (JSONException | IOException err) {
			LogUtils.error("Something went wrong while getting a question.... Please, try again later.", err, context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Start a Trivia game. Once one started, everyone can participate.**")
				.appendField("Gains", "The winner gets " + GAINS + " coins.", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

	public class GuildTriviaManager implements MessageListener {

		private final IChannel channel;
		private final List<IUser> alreadyAnswered;
		private final Timer timer;

		private String correctAnswer;
		private List<String> incorrectAnswers;

		protected GuildTriviaManager(IChannel channel) {
			this.channel = channel;
			this.alreadyAnswered = new ArrayList<>();
			this.timer = new Timer(30 * 1000, event -> {
				BotUtils.sendMessage(Emoji.HOURGLASS + " Time elapsed, the good answer was **" + correctAnswer + "**.", channel);
				this.stop();
			});
		}

		// Trivia API doc : https://opentdb.com/api_config.php
		protected void start() throws JSONException, IOException {
			JSONObject mainObj = new JSONObject(IOUtils.toString(new URL("https://opentdb.com/api.php?amount=1"), "UTF-8"));
			JSONObject resultObj = mainObj.getJSONArray("results").getJSONObject(0);

			String category = resultObj.getString("category");
			String type = resultObj.getString("type");
			String difficulty = resultObj.getString("difficulty");
			String question = resultObj.getString("question");
			String correctAnswer = resultObj.getString("correct_answer");

			this.incorrectAnswers = Utils.convertArrayToList(resultObj.getJSONArray("incorrect_answers"));

			StringBuilder strBuilder = new StringBuilder("**" + StringUtils.convertHtmlToUTF8(question) + "**");
			if("multiple".equals(type)) {
				// Place the correct answer randomly in the list
				int index = MathUtils.rand(incorrectAnswers.size());
				for(int i = 0; i < incorrectAnswers.size(); i++) {
					if(i == index) {
						strBuilder.append("\n\t- " + StringUtils.convertHtmlToUTF8(correctAnswer));
					}
					strBuilder.append("\n\t- " + StringUtils.convertHtmlToUTF8(incorrectAnswers.get(i)));
				}
			}

			EmbedBuilder builder = Utils.getDefaultEmbed()
					.withAuthorName("Trivia")
					.appendField("Question", strBuilder.toString(), false)
					.appendField("Category", "`" + category + "`", true)
					.appendField("Type", "`" + type + "`", true)
					.appendField("Difficulty", "`" + difficulty + "`", true)
					.withFooterText("You have " + (timer.getDelay() / 1000) + " seconds to answer.");

			BotUtils.sendEmbed(builder.build(), channel);

			MessageManager.addListener(channel, this);

			this.correctAnswer = StringUtils.convertHtmlToUTF8(correctAnswer);
			this.timer.start();
		}

		private void stop() {
			MessageManager.removeListener(channel);
			timer.stop();
		}

		@Override
		public boolean onMessageReceived(IMessage message) {
			boolean wrongAnswer = incorrectAnswers.stream().anyMatch(message.getContent()::equalsIgnoreCase);
			boolean goodAnswer = message.getContent().equalsIgnoreCase(this.correctAnswer);
			IUser author = message.getAuthor();

			if(alreadyAnswered.contains(author) && (wrongAnswer || goodAnswer)) {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " Sorry " + author.getName() + ", you can only answer once.", message.getChannel());

			} else if(wrongAnswer) {
				BotUtils.sendMessage(Emoji.THUMBSDOWN + " Wrong answer.", channel);
				alreadyAnswered.add(author);

			} else if(goodAnswer) {
				BotUtils.sendMessage(Emoji.CLAP + " Correct ! " + author.getName() + ", you won **" + GAINS + " coins**.", channel);
				Storage.getPlayer(message.getGuild(), author).addCoins(GAINS);
				this.stop();
			}
			return true;
		}
	}
}
