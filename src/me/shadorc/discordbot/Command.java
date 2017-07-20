package me.shadorc.discordbot;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLEncoder;

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
import me.shadorc.discordbot.Storage.API_KEYS;
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
			Bot.sendMessage("Cette commande n'existe pas, pour la liste des commandes disponibles, entrez /help.", channel);
		} catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e2) {
			System.err.println("Error while executing method.");
			e2.printStackTrace();
		}
	}

	public void help() {
		Bot.sendMessage("__**Commandes disponibles :**__"
				+ "\n\t/trad <lang1> <lang2> <texte>"
				+ "\n\t/wiki <recherche>"
				+ "\n\t/vacances <zone>"
				+ "\n\t/calc <calcul>"
				+ "\n\t/meteo <ville>"
				+ "\n\t/gif"
				+ "\n\t/gif <tag>"
				+ "\n\t/giphy <tag>"
				+ "\n\t/dtc"
				+ "\n\t/trivia"
				+ "\n\t/roulette_russe"
				+ "\n\t/coins"
				+ "\n\t/chat <message>"
				, channel);
	}

	public void gif() {
		if(arg == null) {
			try {
				String gifUrl = Infonet.parseHTML(new URL("http://gifland.us"), "<meta name=\"twitter:image:src", "content=\"", "\">");
				Bot.sendMessage(gifUrl, channel);
			} catch (IOException e) {
				Utils.error(e, "Une erreur est survenue lors de la récupération du gif.", channel);
			}
		} 

		else {
			try {
				String json = Infonet.getHTML(new URL("https://api.giphy.com/v1/gifs/random?tag=" + URLEncoder.encode(arg, "UTF-8") + "&api_key=" + Storage.get(API_KEYS.GIPHY_API_KEY)));
				JSONObject obj = new JSONObject(json);
				if(obj.get("data") instanceof JSONArray) {
					Bot.sendMessage("Aucun résultat pour " + arg, channel);
					return;
				}
				String url = obj.getJSONObject("data").getString("url");
				Bot.sendMessage(url, channel);
			} catch (IOException e) {
				Utils.error(e, "Une erreur est survenue lors de la récupération d'un gif sur Giphy.", channel);
			}
		}
	}

	public void wiki() {
		if(arg == null) {
			Bot.sendMessage("Merci d'indiquer une recherche.", channel);
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
			String description = pagesObj.getJSONObject(pageId).getString("extract");
			Bot.sendMessage(description, channel);
		} catch (IOException e) {
			Utils.error(e, "Une erreur est survenue lors de la récupération des informations sur Wikipédia.", channel);
		} catch (StringIndexOutOfBoundsException e1) {
			Bot.sendMessage("Aucun résultat pour : " + arg, channel);
		}
	}

	public void vacances() {
		if(arg == null) {
			Bot.sendMessage("Merci d'indiquer une zone : A, B ou C.", channel);
			return;
		}

		try {
			Main.twitterConnection();
			String holidays = Main.getTwitter().getUserTimeline("Vacances_Zone" + arg.toUpperCase()).get(0).getText().replaceAll("#", "");
			Bot.sendMessage(holidays, channel);
		} catch (TwitterException e) {
			if(e.getErrorCode() == 34) {
				Bot.sendMessage("La zone indiquée n'existe pas, merci d'entrer A, B ou C.", channel);
			} else {
				Utils.error(e, "Une erreur est survenue lors de la récupération des informations concernant les vacances.", channel);
			}
		}
	}

	public void calc() {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
			Bot.sendMessage(arg + " = " + engine.eval(arg), channel);
		} catch (ScriptException e) {
			Bot.sendMessage("Calcul incorrect.", channel);
		}
	}

	public void trad() {
		//Country doc https://www.pastebin.com/NHWLgJ43
		if(arg == null) {
			Bot.sendMessage("Merci d'indiquer les 2 langues et le texte à traduire. Exemple : /trad fr en Salut", channel);
			return;
		}

		try {
			String[] args = arg.split(" ", 3);
			if(args.length < 3) {
				Bot.sendMessage("Merci d'indiquer les 2 langues et le texte à traduire. Exemple : /trad fr en Salut", channel);
				return;
			}
			String word = Utils.translate(args[0], args[1], args[2]);
			Bot.sendMessage("Traduction : " + word, channel);
		} catch (Exception e) {
			Utils.error(e, "Une erreur est survenue lors de la traduction.", channel);
		}
	} 

	public void trivia() {
		try {
			Trivia.start(channel);
		} catch (IOException e) {
			Utils.error(e, "Une erreur est survenue lors de la récupération de la question.", channel);
		}
	}

	public void roulette_russe() {
		String author = message.getAuthor().getName();
		if(Utils.rand(6) == 0) {
			Bot.sendMessage("Une goutte de sueur coule sur votre front, vous pressez la détente... **PAN** ... Désolé, vous êtes mort, vous perdez tous vos gains.", channel);
			Storage.store(author, 0);
		} else {
			Bot.sendMessage("Une goutte de sueur coule sur votre front, vous pressez la détente... **click** ... Pfiou, vous êtes toujours en vie, vous remportez 50 coins !", channel);
			Utils.gain(author, 50);
		}
	}

	public void coins() {
		Bot.sendMessage("Vous avez " + Storage.get(message.getAuthor().getName()) + " coins.", channel);
	}

	public void meteo() {
		IWeatherDataService dataService = WeatherDataServiceFactory.getWeatherDataService(service.OPEN_WEATHER_MAP);
		try {
			WeatherData data = dataService.getWeatherData(new Location(arg, "FR"));
			Bot.sendMessage("__Météo pour la ville de " + data.getCity().getName() + "__ (dernière mise à jour le " + data.getLastUpdate().getValue() + ") :"
					+ "\n\tNuages : " + Utils.translate("en", "fr", data.getClouds().getValue()) 
					+ "\n\tVent : " + data.getWind().getSpeed().getValue() + "m/s, " + Utils.translate("en", "fr", data.getWind().getSpeed().getName()).toLowerCase()
					+ "\n\tPrécipitations : " + (data.getPrecipitation().getMode().equals("no") ? "Aucune" : data.getPrecipitation().getValue())
					+ "\n\tHumidité : " + data.getHumidity().getValue() + "%"
					+ "\n\tTempérature : " + data.getTemperature().getValue() + "°C", channel);
		} catch (Exception e) {
			Utils.error(e, "Une erreur est survenue lors de la récupération des données météorologiques.", channel);
		}
	}

	public void dtc() {
		try {
			String json = Infonet.getHTML(new URL("http://api.danstonchat.com/0.3/view/random?key=" + Storage.get(API_KEYS.DTC_API_KEY) + "&format=json"));
			String quote = new JSONArray(json).getJSONObject(0).getString("content");
			Bot.sendMessage("```" + quote + "```", channel);
		} catch (IOException e) {
			Utils.error(e, "Une erreur est survenue lors de la récupération d'une quote sur danstonchat.com", channel);
		}
	}

	public void chat() {
		CleverbotChat.answer(arg, channel);
	}

	public void enable_translation() {
		if(arg != null) {
			CleverbotChat.setTranslationEnabled(Boolean.getBoolean(arg));
		}
	}
}