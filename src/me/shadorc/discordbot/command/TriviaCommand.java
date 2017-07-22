package me.shadorc.discordbot.command;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.Timer;

import org.json.JSONArray;
import org.json.JSONObject;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import me.shadorc.discordbot.utility.Utils;
import me.shadorc.infonet.Infonet;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

public class TriviaCommand extends Command {

	private static ArrayList <IUser> alreadyAnswered = new ArrayList <> ();
	public static boolean QUIZZ_STARTED = false;

	private static String CORRECT_ANSWER;
	private static IChannel CHANNEL;

	private final static Timer timer = new Timer(30*1000, e -> {
		BotUtils.sendMessage("Temps écoulé, la bonne réponse était " + CORRECT_ANSWER, CHANNEL);
		TriviaCommand.stop();
	});

	public TriviaCommand() {
		super("trivia", "quizz", "question");
	}

	@Override
	public void execute(Context context) {
		try {
			this.start(context.getChannel());
		} catch (IOException e) {
			Log.error("Une erreur est survenue lors de la récupération de la question.", e, context.getChannel());
		}
	}

	private void start(IChannel channel) throws MalformedURLException, IOException {
		//Trivia API doc : https://opentdb.com/api_config.php
		String json = Infonet.getHTML(new URL("https://opentdb.com/api.php?amount=1"));
		JSONArray arrayResults = new JSONObject(json).getJSONArray("results");
		JSONObject result = arrayResults.getJSONObject(0);

		String category = result.getString("category");
		String type = result.getString("type");
		String difficulty = result.getString("difficulty");
		String question = result.getString("question");
		String correct_answer = result.getString("correct_answer");

		StringBuilder quizzMessage = new StringBuilder();

		quizzMessage.append("Catégorie : " + category
				+ ", type : " + type
				+ ", difficulté : " + difficulty
				+ "\nQuestion : **" + Utils.convertToUTF8(question) + "**\n");

		if(type.equals("multiple")) {
			JSONArray incorrect_answers = result.getJSONArray("incorrect_answers");

			//Place the correct answer randomly in the list
			int index = Utils.rand(incorrect_answers.length());
			for(int i = 0; i < incorrect_answers.length(); i++) {
				if(i == index) {
					quizzMessage.append("\t- " + Utils.convertToUTF8(correct_answer) + "\n");
				}
				quizzMessage.append("\t- " + Utils.convertToUTF8((String) incorrect_answers.get(i)) + "\n");
			}
		}

		BotUtils.sendMessage(quizzMessage.toString(), channel);

		CORRECT_ANSWER = Utils.convertToUTF8(correct_answer);
		CHANNEL = channel;
		this.start();
	}

	public static void checkAnswer(IMessage message) {
		if(alreadyAnswered.contains(message.getAuthor())) {
			BotUtils.sendMessage("Désolé " + message.getAuthor().getName() + ", tu ne peux plus répondre après avoir donné une mauvaise réponse.", message.getChannel());
		}
		else if(Utils.getLevenshteinDistance(message.getContent().toLowerCase(), TriviaCommand.CORRECT_ANSWER.toLowerCase()) < 2) {
			BotUtils.sendMessage("Bonne réponse " + message.getAuthor().getName() + " ! Tu gagnes 10 coins.", CHANNEL);
			Utils.gain(message.getAuthor().getName(), 10);
			TriviaCommand.stop();
		}
		else {
			BotUtils.sendMessage("Mauvaise réponse.", CHANNEL);
			alreadyAnswered.add(message.getAuthor());
		}
	}

	private void start() {
		QUIZZ_STARTED = true;
		timer.start();
	}

	private static void stop() {
		QUIZZ_STARTED = false;
		alreadyAnswered.clear();
		timer.stop();
	}

}
