package me.shadorc.discordbot.command.game;

import java.awt.Color;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Timer;

import org.json.JSONArray;
import org.json.JSONObject;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import me.shadorc.discordbot.utility.NetUtils;
import me.shadorc.discordbot.utility.Utils;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class TriviaCmd extends Command {

	private static final int GAIN = 25;
	private static final Map<IGuild, GuildTriviaManager> GUILDS_TRIVIA = new HashMap<>();

	public TriviaCmd() {
		super(false, "trivia", "quizz", "question");
	}

	@Override
	public void execute(Context context) {
		try {
			GUILDS_TRIVIA.put(context.getGuild(), new GuildTriviaManager(context.getChannel()));
			GUILDS_TRIVIA.get(context.getGuild()).start();
		} catch (IOException e) {
			Log.error("Une erreur est survenue lors de la récupération de la question.", e, context.getChannel());
		}
	}

	public static GuildTriviaManager getGuildTriviaManager(IGuild guild) {
		return GUILDS_TRIVIA.get(guild);
	}

	public class GuildTriviaManager {

		private IChannel channel;
		private ArrayList <IUser> alreadyAnswered;
		private boolean isStarted;
		private String correctAnswer;
		private JSONArray incorrectAnswers;
		private Timer timer;

		private GuildTriviaManager(IChannel channel) {
			this.channel = channel;
			this.alreadyAnswered = new ArrayList<>();
			this.isStarted = false;
			this.timer = new Timer(30*1000, e -> {
				BotUtils.sendMessage(Emoji.HOURGLASS + " Temps écoulé, la bonne réponse était " + correctAnswer + ".", channel);
				this.stop();
			});
		}

		//Trivia API doc : https://opentdb.com/api_config.php
		private void start() throws MalformedURLException, IOException {
			String json = NetUtils.getHTML(new URL("https://opentdb.com/api.php?amount=1"));
			JSONObject result = new JSONObject(json).getJSONArray("results").getJSONObject(0);

			String category = result.getString("category");
			String type = result.getString("type");
			String difficulty = result.getString("difficulty");
			String question = result.getString("question");
			String correct_answer = result.getString("correct_answer");

			this.incorrectAnswers = result.getJSONArray("incorrect_answers");

			StringBuilder strBuilder = new StringBuilder("**" + Utils.convertToUTF8(question) + "**");
			if(type.equals("multiple")) {
				//Place the correct answer randomly in the list
				int index = Utils.rand(incorrectAnswers.length());
				for(int i = 0; i < incorrectAnswers.length(); i++) {
					if(i == index) {
						strBuilder.append("\n\t- " + Utils.convertToUTF8(correct_answer));
					}
					strBuilder.append("\n\t- " + Utils.convertToUTF8((String) incorrectAnswers.get(i)));
				}
			}

			EmbedBuilder builder = new EmbedBuilder()
					.withAuthorName("Trivia")
					.withAuthorIcon(channel.getClient().getOurUser().getAvatarURL())
					.withColor(new Color(170, 196, 222))
					.appendField("Question", strBuilder.toString(), false)
					.appendField("Catégorie",  "`" + category + "`", true)
					.appendField("Type", "`" + type + "`", true)
					.appendField("Difficulté", "`" + difficulty + "`", true)
					.withFooterText("Vous avez " + (timer.getDelay()/1000) + " secondes pour répondre.");

			BotUtils.sendEmbed(builder.build(), channel);

			this.correctAnswer = Utils.convertToUTF8(correct_answer);
			this.isStarted = true;
			this.timer.start();
		}

		public void checkAnswer(IMessage message) {
			if(Utils.convertToList(incorrectAnswers).contains(message.getContent().toLowerCase())) {
				if(alreadyAnswered.contains(message.getAuthor())) {
					BotUtils.sendMessage(Emoji.WARNING + " Désolé " + message.getAuthor().getName() + ", tu ne peux donner qu'une seule réponse.", message.getChannel());
				}
				else {
					BotUtils.sendMessage(Emoji.THUMBSDOWN + " Mauvaise réponse.", channel);
					alreadyAnswered.add(message.getAuthor());
				}
			}
			else if(Utils.getLevenshteinDistance(message.getContent().toLowerCase(), this.correctAnswer.toLowerCase()) < 2) {
				BotUtils.sendMessage(Emoji.CLAP + " Bonne réponse " + message.getAuthor().getName() + " ! Tu gagnes " + GAIN + " coins.", channel);
				Utils.addCoins(message.getGuild(), message.getAuthor(), GAIN);
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
				.withAuthorName("Aide pour la commande /" + context.getArg())
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(new Color(170, 196, 222))
				.appendDescription("**Lance une partie de Trivia. Une fois la partie lancée, tout le monde peut participer.**")
				.appendField("Gains", "Le gagnant remporte " + GAIN + " coins.", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
