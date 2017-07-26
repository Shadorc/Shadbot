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

import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import me.shadorc.discordbot.utility.Utils;
import me.shadorc.infonet.Infonet;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class TriviaCmd extends Command {

	private static final Map<IGuild, GuildTriviaManager> GUILDS = new HashMap<>();

	public TriviaCmd() {
		super(false, "trivia", "quizz", "question");
	}

	@Override
	public void execute(Context context) {
		try {
			GUILDS.put(context.getGuild(), new GuildTriviaManager(context.getChannel()));
			GUILDS.get(context.getGuild()).start();
		} catch (IOException e) {
			Log.error("Une erreur est survenue lors de la récupération de la question.", e, context.getChannel());
		}
	}

	public static GuildTriviaManager getGuildTriviaManager(IGuild guild) {
		return GUILDS.get(guild);
	}

	public class GuildTriviaManager {

		private IChannel channel;
		private ArrayList <IUser> alreadyAnswered;
		private boolean isStarted;
		private String correctAnswer;
		private Timer timer;

		private GuildTriviaManager(IChannel channel) {
			this.channel = channel;
			this.alreadyAnswered = new ArrayList<>();
			this.isStarted = false;
			this.timer = new Timer(30*1000, e -> {
				BotUtils.sendMessage(":hourglass: Temps écoulé, la bonne réponse était " + correctAnswer, channel);
				this.stop();
			});
		}

		private void start() throws MalformedURLException, IOException {
			//Trivia API doc : https://opentdb.com/api_config.php
			String json = Infonet.getHTML(new URL("https://opentdb.com/api.php?amount=1"));
			JSONArray arrayResults = new JSONObject(json).getJSONArray("results");
			JSONObject result = arrayResults.getJSONObject(0);

			String category = result.getString("category");
			String type = result.getString("type");
			String difficulty = result.getString("difficulty");
			String question = result.getString("question");
			String correct_answer = result.getString("correct_answer");

			StringBuilder quizzMessage = new StringBuilder("**" + Utils.convertToUTF8(question) + "**");

			if(type.equals("multiple")) {
				JSONArray incorrect_answers = result.getJSONArray("incorrect_answers");

				//Place the correct answer randomly in the list
				int index = Utils.rand(incorrect_answers.length());
				for(int i = 0; i < incorrect_answers.length(); i++) {
					if(i == index) {
						quizzMessage.append("\n\t- " + Utils.convertToUTF8(correct_answer));
					}
					quizzMessage.append("\n\t- " + Utils.convertToUTF8((String) incorrect_answers.get(i)));
				}
			}

			EmbedBuilder builder = new EmbedBuilder()
					.withAuthorName("Trivia")
					.withAuthorIcon(channel.getClient().getOurUser().getAvatarURL())
					.withColor(new Color(170, 196, 222))
					.appendField("Question", quizzMessage.toString(), false)
					.appendField("Catégorie",  "`" + category + "`", true)
					.appendField("Type", "`" + type + "`", true)
					.appendField("Difficulté", "`" + difficulty + "`", true);

			BotUtils.sendEmbed(builder.build(), channel);

			this.correctAnswer = Utils.convertToUTF8(correct_answer);
			this.isStarted = true;
			this.timer.start();
		}

		public void checkAnswer(IMessage message) {
			if(alreadyAnswered.contains(message.getAuthor())) {
				BotUtils.sendMessage(":heavy_multiplication_x: Désolé " + message.getAuthor().getName() + ", tu ne peux plus répondre après avoir donné une mauvaise réponse.", message.getChannel());
			}
			else if(Utils.getLevenshteinDistance(message.getContent().toLowerCase(), this.correctAnswer.toLowerCase()) < 2) {
				BotUtils.sendMessage(":clap: Bonne réponse " + message.getAuthor().getName() + " ! Tu gagnes 50 coins.", channel);
				Utils.gain(message.getGuild(), message.getAuthor(), 10);
				this.stop();
			}
			else {
				BotUtils.sendMessage(":thumbsdown: Mauvaise réponse.", channel);
				alreadyAnswered.add(message.getAuthor());
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
}
