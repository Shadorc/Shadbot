package me.shadorc.discordbot.command;

import il.ac.hit.finalproject.classes.IWeatherDataService;
import il.ac.hit.finalproject.classes.Location;
import il.ac.hit.finalproject.classes.WeatherData;
import il.ac.hit.finalproject.classes.WeatherDataServiceFactory;
import il.ac.hit.finalproject.classes.WeatherDataServiceFactory.service;
import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import me.shadorc.discordbot.utility.Utils;

public class WeatherCmd extends Command{

	public WeatherCmd() {
		super("meteo", "météo", "weather");
	}

	@Override
	public void execute(Context context) {
		if(context.getArg() == null) {
			BotUtils.sendMessage("Merci d'indiquer le nom d'une ville.", context.getChannel());
			return;
		}

		IWeatherDataService dataService = WeatherDataServiceFactory.getWeatherDataService(service.OPEN_WEATHER_MAP);
		try {
			WeatherData data = dataService.getWeatherData(new Location(context.getArg(), "FR"));
			BotUtils.sendMessage("__Météo pour la ville de " + data.getCity().getName() + "__ (dernière mise à jour le " + data.getLastUpdate().getValue() + ") :"
					+ "\n\tNuages : " + Utils.translate("en", "fr", data.getClouds().getValue())
					+ "\n\tVent : " + data.getWind().getSpeed().getValue() + "m/s, " + Utils.translate("en", "fr", data.getWind().getSpeed().getName()).toLowerCase()
					+ "\n\tPrécipitations : " + (data.getPrecipitation().getMode().equals("no") ? "Aucune" : data.getPrecipitation().getValue())
					+ "\n\tHumidité : " + data.getHumidity().getValue() + "%"
					+ "\n\tTempérature : " + data.getTemperature().getValue() + "°C", context.getChannel());
		} catch (Exception e) {
			Log.error("Une erreur est survenue lors de la récupération des données météorologiques.", e, context.getChannel());
		}
	}

}
