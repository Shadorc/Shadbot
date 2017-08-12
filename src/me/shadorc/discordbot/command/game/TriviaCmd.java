package me.shadorc.discordbot.command.game;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Timer;

import org.json.JSONObject;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.HtmlUtils;
import me.shadorc.discordbot.utils.Log;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class TriviaCmd extends Command {

	private static final int GAINS = 25;
	private static final Map<IGuild, GuildTriviaManager> GUILDS_TRIVIA = new HashMap<>();

	public TriviaCmd() {
		super(false, "trivia", "quizz", "question");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		try {
			GUILDS_TRIVIA.put(context.getGuild(), new GuildTriviaManager(context.getChannel()));
			GUILDS_TRIVIA.get(context.getGuild()).start();
		} catch (IOException e) {
			Log.error("An error occured while getting a question.", e, context.getChannel());
		}
	}

	public static GuildTriviaManager getGuildTriviaManager(IGuild guild) {
		return GUILDS_TRIVIA.get(guild);
	}

	public class GuildTriviaManager {

		private final IChannel channel;
		private final List<IUser> alreadyAnswered;
		private final Timer timer;
		private boolean isStarted;
		private String correctAnswer;
		private List<String> incorrectAnswers;

		private GuildTriviaManager(IChannel channel) {
			this.channel = channel;
			this.alreadyAnswered = new ArrayList<>();
			this.isStarted = false;
			this.timer = new Timer(30 * 1000, e -> {
				BotUtils.sendMessage(Emoji.HOURGLASS + " Time elapsed, the good answer was " + correctAnswer + ".", channel);
				this.stop();
			});
		}

		// Trivia API doc : https://opentdb.com/api_config.php
		private void start() throws MalformedURLException, IOException {
			String json = HtmlUtils.getHTML(new URL("https://opentdb.com/api.php?amount=1"));
			JSONObject result = new JSONObject(json).getJSONArray("results").getJSONObject(0);

			String category = result.getString("category");
			String type = result.getString("type");
			String difficulty = result.getString("difficulty");
			String question = result.getString("question");
			String correct_answer = result.getString("correct_answer");

			this.incorrectAnswers = Utils.convertArrayToList(result.getJSONArray("incorrect_answers"));

			StringBuilder strBuilder = new StringBuilder("**" + StringUtils.convertHtmlToUTF8(question) + "**");
			if(type.equals("multiple")) {
				// Place the correct answer randomly in the list
				int index = MathUtils.rand(incorrectAnswers.size());
				for(int i = 0; i < incorrectAnswers.size(); i++) {
					if(i == index) {
						strBuilder.append("\n\t- " + StringUtils.convertHtmlToUTF8(correct_answer));
					}
					strBuilder.append("\n\t- " + StringUtils.convertHtmlToUTF8(incorrectAnswers.get(i)));
				}
			}

			EmbedBuilder builder = new EmbedBuilder()
					.withAuthorName("Trivia")
					.withAuthorIcon(channel.getClient().getOurUser().getAvatarURL())
					.withColor(Config.BOT_COLOR)
					.appendField("Question", strBuilder.toString(), false)
					.appendField("Category", "`" + category + "`", true)
					.appendField("Type", "`" + type + "`", true)
					.appendField("Difficulty", "`" + difficulty + "`", true)
					.withFooterText("You have " + (timer.getDelay() / 1000) + " seconds to answer.");

			BotUtils.sendEmbed(builder.build(), channel);

			this.correctAnswer = StringUtils.convertHtmlToUTF8(correct_answer);
			this.isStarted = true;
			this.timer.start();
		}

		public void checkAnswer(IMessage message) {
			if(alreadyAnswered.contains(message.getAuthor())) {
				BotUtils.sendMessage(Emoji.WARNING + " Sorry " + message.getAuthor().getName() + ", you can only answer once.", message.getChannel());
			} else if(incorrectAnswers.stream().anyMatch(message.getContent()::equalsIgnoreCase)) {
				BotUtils.sendMessage(Emoji.THUMBSDOWN + " Wrong answer.", channel);
				alreadyAnswered.add(message.getAuthor());
			} else if(message.getContent().equalsIgnoreCase(this.correctAnswer)) {
				BotUtils.sendMessage(Emoji.CLAP + " Correct ! " + message.getAuthor().getName() + ", you won **" + GAINS + " coins**.", channel);
				Storage.getUser(message.getGuild(), message.getAuthor()).addCoins(GAINS);
				this.stop();
			}
		}

		private void stop() {
			this.alreadyAnswered.clear();
			this.isStarted = false;
			this.timer.stop();
		}

		public boolean isStarted() {
			return isStarted;
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for /" + this.getNames()[0])
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Start a Trivia game. Once one started, everyone can participate.**")
				.appendField("Gains", "The winner gets " + GAINS + " coins.", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
