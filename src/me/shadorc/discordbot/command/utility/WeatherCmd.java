package me.shadorc.discordbot.command.utility;

import java.awt.Color;

import il.ac.hit.finalproject.classes.IWeatherDataService;
import il.ac.hit.finalproject.classes.Location;
import il.ac.hit.finalproject.classes.WeatherData;
import il.ac.hit.finalproject.classes.WeatherDataServiceFactory;
import il.ac.hit.finalproject.classes.WeatherDataServiceFactory.service;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import me.shadorc.discordbot.utility.Utils;
import sx.blah.discord.util.EmbedBuilder;

public class WeatherCmd extends Command{

	public WeatherCmd() {
		super(false, "meteo", "météo", "weather");
	}

	@Override
	public void execute(Context context) {
		if(context.getArg() == null) {
			throw new IllegalArgumentException();
		}

		IWeatherDataService dataService = WeatherDataServiceFactory.getWeatherDataService(service.OPEN_WEATHER_MAP);
		try {
			WeatherData data = dataService.getWeatherData(new Location(context.getArg(), "FR"));
			String precipitation = data.getPrecipitation().getMode().equals("no") ? "Aucune" : data.getPrecipitation().getValue();
			String clouds = Utils.capitalize(Utils.translate("en", "fr", data.getClouds().getValue()));
			String windName = Utils.translate("en", "fr", data.getWind().getSpeed().getName());
			int windSpeed = (int) (Float.parseFloat(data.getWind().getSpeed().getValue())*3.6f);
			int temperature = (int) Float.parseFloat(data.getTemperature().getValue());

			EmbedBuilder builder = new EmbedBuilder()
					.withAuthorName("Météo pour la ville de " + data.getCity().getName())
					.withDesc("Dernière mise à jour le " + data.getLastUpdate().getValue())
					.withThumbnail("https://image.flaticon.com/icons/svg/494/494472.svg")
					.withAuthorIcon(context.getAuthor().getAvatarURL())
					.withColor(new Color(170, 196, 222))
					.appendField(Emoji.CLOUD + " Nuages", clouds, true)
					.appendField(Emoji.WIND + " Vent", windName + "\n" + windSpeed + " km/h", true)
					.appendField(Emoji.RAIN + " Précipitations", precipitation, true)
					.appendField(Emoji.THERMOMETER + " Température", temperature + "°C", true)
					.withFooterText("Informations provenant du site OpenWeatherMap");

			BotUtils.sendEmbed(builder.build(), context.getChannel());

		} catch (Exception e) {
			Log.error("Une erreur est survenue lors de la récupération des données météorologiques.", e, context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Aide pour la commande /" + context.getArg())
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(new Color(170, 196, 222))
				.appendDescription("**Affiche la météo d'une ville.**")
				.appendField("Utilisation", "/meteo <ville>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
