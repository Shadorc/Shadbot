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
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import net.aksingh.owmjapis.CurrentWeather;
import net.aksingh.owmjapis.OpenWeatherMap;
import net.aksingh.owmjapis.OpenWeatherMap.Units;

@RateLimited
@Command(category = CommandCategory.UTILS, names = { "weather" })
public class WeatherCmd extends AbstractCommand {

	private final SimpleDateFormat dateFormatter = new SimpleDateFormat("MMMMM d, yyyy 'at' hh:mm aa", Locale.ENGLISH);

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		LoadingMessage loadingMsg = new LoadingMessage("Loading weather information...", context.getChannel());
		loadingMsg.send();

		try {
			OpenWeatherMap owm = new OpenWeatherMap(Units.METRIC, APIKeys.get(APIKey.OPENWEATHERMAP_API_KEY));
			CurrentWeather weather = owm.currentWeatherByCityName(context.getArg());

			if(!weather.isValid()) {
				loadingMsg.edit(TextUtils.noResult(context.getArg()));
				return;
			}

			String clouds = StringUtils.capitalize(weather.getWeatherInstance(0).getWeatherDescription());
			float windSpeed = weather.getWindInstance().getWindSpeed() * 3.6f;
			String windDesc = this.getWindDesc(windSpeed);
			String rain = weather.hasRainInstance() ? String.format("%.1f mm/h", weather.getRainInstance().getRain3h()) : "None";
			float humidity = weather.getMainInstance().getHumidity();
			float temperature = weather.getMainInstance().getTemperature();

			EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
					.withAuthorName("Weather for: " + weather.getCityName())
					.withThumbnail("https://image.flaticon.com/icons/svg/494/494472.svg")
					.withAuthorUrl("http://openweathermap.org/city/" + weather.getCityCode())
					.appendDescription("Last updated " + dateFormatter.format(weather.getDateTime()))
					.addField(Emoji.CLOUD + " Clouds", clouds, true)
					.addField(Emoji.WIND + " Wind", String.format("%s%n%.1f km/h", windDesc, windSpeed), true)
					.addField(Emoji.RAIN + " Rain", rain, true)
					.addField(Emoji.DROPLET + " Humidity", String.format("%.1f%%", humidity), true)
					.addField(Emoji.THERMOMETER + " Temperature", String.format("%.1f°C", temperature), true);

			loadingMsg.edit(embed.build());
		} catch (IOException err) {
			loadingMsg.delete();
			Utils.handle("getting weather information", context, err);
		}
	}

	private String getWindDesc(float windSpeed) {
		if(windSpeed < 1) {
			return "Calm";
		} else if(Utils.isInRange(windSpeed, 1, 6)) {
			return "Light air";
		} else if(Utils.isInRange(windSpeed, 6, 12)) {
			return "Light breeze";
		} else if(Utils.isInRange(windSpeed, 12, 20)) {
			return "Gentle breeze";
		} else if(Utils.isInRange(windSpeed, 20, 29)) {
			return "Moderate breeze";
		} else if(Utils.isInRange(windSpeed, 29, 39)) {
			return "Fresh breeze";
		} else if(Utils.isInRange(windSpeed, 39, 50)) {
			return "Strong breeze";
		} else if(Utils.isInRange(windSpeed, 50, 62)) {
			return "Near gale";
		} else if(Utils.isInRange(windSpeed, 62, 75)) {
			return "Gale";
		} else if(Utils.isInRange(windSpeed, 75, 89)) {
			return "Strong gale";
		} else if(Utils.isInRange(windSpeed, 89, 103)) {
			return "Storm";
		} else if(Utils.isInRange(windSpeed, 103, 118)) {
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
				.setSource("http://openweathermap.org/")
				.build();
	}
}
