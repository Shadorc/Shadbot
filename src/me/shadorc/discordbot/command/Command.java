package me.shadorc.discordbot.command;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.json.JSONArray;
import org.json.JSONObject;

import il.ac.hit.finalproject.classes.IWeatherDataService;
import il.ac.hit.finalproject.classes.Location;
import il.ac.hit.finalproject.classes.WeatherData;
import il.ac.hit.finalproject.classes.WeatherDataServiceFactory;
import il.ac.hit.finalproject.classes.WeatherDataServiceFactory.service;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.Main;
import me.shadorc.discordbot.command.Chat.ChatBot;
import me.shadorc.discordbot.storage.Storage;
import me.shadorc.discordbot.storage.Storage.API_KEYS;
import me.shadorc.discordbot.utility.Log;
import me.shadorc.discordbot.utility.Utils;
import me.shadorc.infonet.Infonet;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import twitter4j.TwitterException;

public class Command {

	private IMessage message;
	private IChannel channel;
	private String command;
	private String arg;

	public Command(IMessage message, IChannel channel) {
		this.message = message;
		this.channel = channel;
		this.command = message.getContent().split(" ", 2)[0].replace("/", "").toLowerCase().trim();
		this.arg = message.getContent().contains(" ") ? message.getContent().split(" ", 2)[1].trim() : null;
	}

	public void execute() {
		try {
			Method method = this.getClass().getMethod(command);
			method.invoke(this);
		} catch (NoSuchMethodException e1) {
			BotUtils.sendMessage("Cette commande n'existe pas, pour la liste des commandes disponibles, entrez /help.", channel);
			Log.info("La commande " + command + " a été essayée sans résultat.");
		} catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e2) {
			Log.error("Error while executing method.", e2);
		}
	}

	public void help() {
		BotUtils.sendMessage("__**Commandes disponibles :**__"
				+ "\n\t/trad <lang1> <lang2> <texte>"
				+ "\n\t/wiki <recherche>"
				+ "\n\t/vacances <zone>"
				+ "\n\t/calc <calcul>"
				+ "\n\t/meteo <ville>"
				+ "\n\t/chat <message>"
				+ "\n\t/gif <tag>"
				+ "\n\t/gif"
				+ "\n\t/dtc"
				+ "\n\t/blague"
				+ "\n\t/trivia"
				+ "\n\t/roulette_russe"
				+ "\n\t/machine_sous"
				+ "\n\t/coins"
				, channel);
	}

	public void trad() {
		//Country doc https://www.pastebin.com/NHWLgJ43
		if(arg == null) {
			BotUtils.sendMessage("Merci d'indiquer les 2 langues et le texte à traduire. Exemple : /trad fr en Salut", channel);
			return;
		}

		try {
			String[] args = arg.split(" ", 3);
			if(args.length < 3) {
				BotUtils.sendMessage("Merci d'indiquer les 2 langues et le texte à traduire. Exemple : /trad fr en Salut", channel);
				return;
			}
			String word = Utils.translate(args[0], args[1], args[2]);
			BotUtils.sendMessage("Traduction : " + word, channel);
		} catch (Exception e) {
			Log.error("Une erreur est survenue lors de la traduction.", e, channel);
		}
	}

	public void wiki() {
		if(arg == null) {
			BotUtils.sendMessage("Merci d'indiquer une recherche.", channel);
			return;
		}

		try {
			String searchEncoded = URLEncoder.encode(arg, "UTF-8");
			//Wiki api doc https://en.wikipedia.org/w/api.php?action=help&modules=query%2Bextracts
			String json = Infonet.getHTML(new URL("https://fr.wikipedia.org/w/api.php?"
					+ "action=query"
					+ "&titles=" + searchEncoded
					+ "&prop=extracts"
					+ "&format=json"
					+ "&explaintext=true"
					+ "&exintro=true"));

			JSONObject pagesObj = new JSONObject(json).getJSONObject("query").getJSONObject("pages");
			String pageId = pagesObj.names().getString(0);
			if(pageId.equals("-1")) {
				BotUtils.sendMessage("Aucun résultat pour : " + arg, channel);
				return;
			}
			String description = pagesObj.getJSONObject(pageId).getString("extract");
			BotUtils.sendMessage(description, channel);
		} catch (IOException e) {
			Log.error("Une erreur est survenue lors de la récupération des informations sur Wikipédia.", e, channel);
		}
	}

	public void vacances() {
		if(arg == null) {
			BotUtils.sendMessage("Merci d'indiquer une zone : A, B ou C.", channel);
			return;
		}

		try {
			Main.twitterConnection();
			String holidays = Main.getTwitter().getUserTimeline("Vacances_Zone" + arg.toUpperCase()).get(0).getText().replaceAll("#", "");
			BotUtils.sendMessage(holidays, channel);
		} catch (TwitterException e) {
			if(e.getErrorCode() == 34) {
				BotUtils.sendMessage("La zone indiquée n'existe pas, merci d'entrer A, B ou C.", channel);
			} else {
				Log.error("Une erreur est survenue lors de la récupération des informations concernant les vacances.", e, channel);
			}
		}
	}

	public void calc() {
		if(arg == null) {
			BotUtils.sendMessage("Merci d'entrer un calcul.", channel);
			return;
		}

		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
			BotUtils.sendMessage(arg + " = " + engine.eval(arg), channel);
		} catch (ScriptException e) {
			BotUtils.sendMessage("Calcul incorrect.", channel);
		}
	}

	public void meteo() {
		if(arg == null) {
			BotUtils.sendMessage("Merci d'indiquer le nom d'une ville.", channel);
			return;
		}

		IWeatherDataService dataService = WeatherDataServiceFactory.getWeatherDataService(service.OPEN_WEATHER_MAP);
		try {
			WeatherData data = dataService.getWeatherData(new Location(arg, "FR"));
			BotUtils.sendMessage("__Météo pour la ville de " + data.getCity().getName() + "__ (dernière mise à jour le " + data.getLastUpdate().getValue() + ") :"
					+ "\n\tNuages : " + Utils.translate("en", "fr", data.getClouds().getValue())
					+ "\n\tVent : " + data.getWind().getSpeed().getValue() + "m/s, " + Utils.translate("en", "fr", data.getWind().getSpeed().getName()).toLowerCase()
					+ "\n\tPrécipitations : " + (data.getPrecipitation().getMode().equals("no") ? "Aucune" : data.getPrecipitation().getValue())
					+ "\n\tHumidité : " + data.getHumidity().getValue() + "%"
					+ "\n\tTempérature : " + data.getTemperature().getValue() + "°C", channel);
		} catch (Exception e) {
			Log.error("Une erreur est survenue lors de la récupération des données météorologiques.", e, channel);
		}
	}

	public void chat() {
		Chat.answer(arg, channel);
	}

	public void gif() {
		if(arg == null) {
			try {
				String gifUrl = Infonet.parseHTML(new URL("http://gifland.us"), "<meta name=\"twitter:image:src", "content=\"", "\">");
				BotUtils.sendMessage(gifUrl, channel);
			} catch (IOException e) {
				Log.error("Une erreur est survenue lors de la récupération du gif.", e, channel);
			}
		}

		else {
			try {
				String json = Infonet.getHTML(new URL("https://api.giphy.com/v1/gifs/random?"
						+ "api_key=" + Storage.get(API_KEYS.GIPHY_API_KEY)
						+ "&tag=" + URLEncoder.encode(arg, "UTF-8")));
				JSONObject obj = new JSONObject(json);
				if(obj.get("data") instanceof JSONArray) {
					BotUtils.sendMessage("Aucun résultat pour " + arg, channel);
					return;
				}
				String url = obj.getJSONObject("data").getString("url");
				BotUtils.sendMessage(url, channel);
			} catch (IOException e) {
				Log.error("Une erreur est survenue lors de la récupération d'un gif sur Giphy.", e, channel);
			}
		}
	}

	public void dtc() {
		try {
			String json = Infonet.getHTML(new URL("http://api.danstonchat.com/0.3/view/random?"
					+ "key=" + Storage.get(API_KEYS.DTC_API_KEY)
					+ "&format=json"));
			String quote = new JSONArray(json).getJSONObject(0).getString("content");
			BotUtils.sendMessage("```" + quote + "```", channel);
		} catch (IOException e) {
			Log.error("Une erreur est survenue lors de la récupération d'une quote sur danstonchat.com", e, channel);
		}
	}

	public void blague() {
		try {
			String htmlPage = Infonet.getHTML(new URL("https://www.blague-drole.net/blagues-" + Utils.rand(10)+1 + ".html?tri=top"));
			ArrayList <String> jokesList = Infonet.getAllSubstring(htmlPage, " \"description\": \"", "</script>");
			String joke = jokesList.get(Utils.rand(jokesList.size()));
			joke = joke.substring(0, joke.lastIndexOf("\"")).trim();
			BotUtils.sendMessage("```" + Utils.convertToUTF8(joke) + "```", channel);
		} catch (IOException e) {
			Log.error("Une erreur est survenue lors de la récupération de la blague.", e, channel);
		}
	}

	public void trivia() {
		try {
			Trivia.start(channel);
		} catch (IOException e) {
			Log.error("Une erreur est survenue lors de la récupération de la question.", e, channel);
		}
	}

	public void roulette_russe() {
		String author = message.getAuthor().getName();
		if(Utils.rand(6) == 0) {
			BotUtils.sendMessage("Une goutte de sueur coule sur votre front, vous pressez la détente... **PAN** ... Désolé, vous êtes mort, vous perdez tous vos gains.", channel);
			Storage.store(author, 0);
		} else {
			BotUtils.sendMessage("Une goutte de sueur coule sur votre front, vous pressez la détente... **click** ... Pfiou, vous êtes toujours en vie, vous remportez 50 coins !", channel);
			Utils.gain(author, 50);
		}
	}

	public void machine_sous() {
		SlotMachine.play(message.getAuthor().getName(), channel);
	}

	public void coins() {
		BotUtils.sendMessage("Vous avez " + Storage.get(message.getAuthor().getName()) + " coins.", channel);
	}

	public void set_chatbot() {
		if(message.getAuthor().getName().equals("Shadorc")) {
			if(arg != null) {
				if(arg.equalsIgnoreCase(ChatBot.ALICE.toString())) {
					Chat.setChatbot(ChatBot.ALICE);
				} else if(arg.equalsIgnoreCase(ChatBot.CLEVERBOT.toString())) {
					Chat.setChatbot(ChatBot.CLEVERBOT);
				}
				BotUtils.sendMessage("ChatBot has been set to " + arg.toUpperCase(), channel);
			}
		}
	}
}