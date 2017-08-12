package me.shadorc.discordbot.command.utils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.ApiKeys;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.Log;
import me.shadorc.discordbot.utils.StringUtils;
import net.aksingh.owmjapis.CurrentWeather;
import net.aksingh.owmjapis.OpenWeatherMap;
import net.aksingh.owmjapis.OpenWeatherMap.Units;
import sx.blah.discord.util.EmbedBuilder;

public class WeatherCmd extends Command {

	private final static SimpleDateFormat SDF = new SimpleDateFormat("MMMMM d, yyyy - hh:mm aa", Locale.ENGLISH);

	public WeatherCmd() {
		super(false, "weather", "meteo");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(context.getArg() == null) {
			throw new MissingArgumentException();
		}

		try {
			OpenWeatherMap owm = new OpenWeatherMap(Units.METRIC, Storage.getApiKey(ApiKeys.OPENWEATHERMAP_API_KEY));
			CurrentWeather weather = owm.currentWeatherByCityName(context.getArg());

			if(weather.isValid()) {
				String clouds = StringUtils.capitalize(weather.getWeatherInstance(0).getWeatherDescription());
				float windSpeed = weather.getWindInstance().getWindSpeed() * 3.6f;
				String windDesc = this.getWindDesc(windSpeed);
				String rain = weather.hasRainInstance() ? String.format("%.1f mm/h", weather.getRainInstance().getRain3h()) : "None";
				float humidity = weather.getMainInstance().getHumidity();
				float temperature = weather.getMainInstance().getTemperature();

				EmbedBuilder builder = new EmbedBuilder()
						.withAuthorName("Weather in " + weather.getCityName() + " City")
						.withDesc("Last updatee on " + SDF.format(weather.getDateTime()))
						.withThumbnail("https://image.flaticon.com/icons/svg/494/494472.svg")
						.withAuthorIcon(context.getAuthor().getAvatarURL())
						.withColor(Config.BOT_COLOR)
						.appendField(Emoji.CLOUD + " Clouds", clouds, true)
						.appendField(Emoji.WIND + " Wind", windDesc + "\n" + String.format("%.1f", windSpeed) + " km/h", true)
						.appendField(Emoji.RAIN + " Rain", rain, true)
						.appendField(Emoji.WATER + " Humidity", humidity + "%", true)
						.appendField(Emoji.THERMOMETER + " Temperature", String.format("%.1f", temperature) + "°C", true)
						.withFooterText("Information obtained from OpenWeatherMap.org");

				BotUtils.sendEmbed(builder.build(), context.getChannel());
			} else {
				BotUtils.sendMessage(Emoji.WARNING + " City not found.", context.getChannel());
			}
		} catch (IOException e) {
			Log.error("An error occured while getting weather information.", e, context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for /" + this.getNames()[0])
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Show weather report for a city.**")
				.appendField("Usage", "/weather <city>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

	private String getWindDesc(float windSpeed) {
		if(windSpeed >= 0 && windSpeed < 1) {
			return "Calm";
		} else if(windSpeed >= 1 && windSpeed < 6) {
			return "Light air";
		} else if(windSpeed >= 6 && windSpeed < 12) {
			return "Light breeze";
		} else if(windSpeed >= 12 && windSpeed < 20) {
			return "Gentle breeze";
		} else if(windSpeed >= 20 && windSpeed < 29) {
			return "Moderate breeze";
		} else if(windSpeed >= 29 && windSpeed < 39) {
			return "Fresh breeze";
		} else if(windSpeed >= 39 && windSpeed < 50) {
			return "Strong breeze";
		} else if(windSpeed >= 50 && windSpeed < 62) {
			return "Near gale";
		} else if(windSpeed >= 62 && windSpeed < 75) {
			return "Gale";
		} else if(windSpeed >= 75 && windSpeed < 89) {
			return "Strong gale";
		} else if(windSpeed >= 89 && windSpeed < 103) {
			return "Storm";
		} else if(windSpeed >= 103 && windSpeed < 118) {
			return "Violent storm";
		} else {
			return "Hurricane";
		}
	}
}
