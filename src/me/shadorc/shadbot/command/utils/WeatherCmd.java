package me.shadorc.shadbot.command.utils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.ExceptionUtils;
import me.shadorc.shadbot.utils.MathUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import net.aksingh.owmjapis.CurrentWeather;
import net.aksingh.owmjapis.OpenWeatherMap;
import net.aksingh.owmjapis.OpenWeatherMap.Units;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

@RateLimited
@Command(category = CommandCategory.UTILS, names = { "weather" })
public class WeatherCmd extends AbstractCommand {

	private final SimpleDateFormat dateFormatter = new SimpleDateFormat("MMMMM d, yyyy - hh:mm aa", Locale.ENGLISH);

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		try {
			OpenWeatherMap owm = new OpenWeatherMap(Units.METRIC, APIKeys.get(APIKey.OPENWEATHERMAP_API_KEY));
			CurrentWeather weather = owm.currentWeatherByCityName(context.getArg());

			if(weather.isValid()) {
				String clouds = StringUtils.capitalize(weather.getWeatherInstance(0).getWeatherDescription());
				float windSpeed = weather.getWindInstance().getWindSpeed() * 3.6f;
				String windDesc = this.getWindDesc(windSpeed);
				String rain = weather.hasRainInstance() ? String.format("%.1f mm/h", weather.getRainInstance().getRain3h()) : "None";
				float humidity = weather.getMainInstance().getHumidity();
				float temperature = weather.getMainInstance().getTemperature();

				EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
						.withAuthorName("Weather for: " + weather.getCityName())
						.withThumbnail("https://image.flaticon.com/icons/svg/494/494472.svg")
						.withUrl("http://openweathermap.org/city/" + weather.getCityCode())
						.appendDescription("Last update on " + dateFormatter.format(weather.getDateTime()))
						.appendField(Emoji.CLOUD + " Clouds", clouds, true)
						.appendField(Emoji.WIND + " Wind", String.format("%s%n%.1f km/h", windDesc, windSpeed), true)
						.appendField(Emoji.RAIN + " Rain", rain, true)
						.appendField(Emoji.DROPLET + " Humidity", humidity + "%", true)
						.appendField(Emoji.THERMOMETER + " Temperature", String.format("%.1f", temperature) + "°C", true);

				BotUtils.sendMessage(embed.build(), context.getChannel());
			} else {
				BotUtils.sendMessage(TextUtils.noResult(context.getArg()), context.getChannel());
			}
		} catch (IOException err) {
			ExceptionUtils.handle("getting weather information", context, err);
		}
	}

	private String getWindDesc(float windSpeed) {
		if(windSpeed < 1) {
			return "Calm";
		} else if(MathUtils.inRange(windSpeed, 1, 6)) {
			return "Light air";
		} else if(MathUtils.inRange(windSpeed, 6, 12)) {
			return "Light breeze";
		} else if(MathUtils.inRange(windSpeed, 12, 20)) {
			return "Gentle breeze";
		} else if(MathUtils.inRange(windSpeed, 20, 29)) {
			return "Moderate breeze";
		} else if(MathUtils.inRange(windSpeed, 29, 39)) {
			return "Fresh breeze";
		} else if(MathUtils.inRange(windSpeed, 39, 50)) {
			return "Strong breeze";
		} else if(MathUtils.inRange(windSpeed, 50, 62)) {
			return "Near gale";
		} else if(MathUtils.inRange(windSpeed, 62, 75)) {
			return "Gale";
		} else if(MathUtils.inRange(windSpeed, 75, 89)) {
			return "Strong gale";
		} else if(MathUtils.inRange(windSpeed, 89, 103)) {
			return "Storm";
		} else if(MathUtils.inRange(windSpeed, 103, 118)) {
			return "Violent storm";
		} else {
			return "Hurricane";
		}
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show weather report for a city.")
				.addArg("city", false)
				.build();
	}
}
