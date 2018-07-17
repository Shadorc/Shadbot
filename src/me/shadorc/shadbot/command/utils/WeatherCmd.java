package me.shadorc.shadbot.command.utils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.message.LoadingMessage;
import net.aksingh.owmjapis.CurrentWeather;
import net.aksingh.owmjapis.OpenWeatherMap;
import net.aksingh.owmjapis.OpenWeatherMap.Units;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.UTILS, names = { "weather" })
public class WeatherCmd extends AbstractCommand {

	private final SimpleDateFormat dateFormatter = new SimpleDateFormat("MMMMM d, yyyy 'at' hh:mm aa", Locale.ENGLISH);

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			OpenWeatherMap owm = new OpenWeatherMap(Units.METRIC, APIKeys.get(APIKey.OPENWEATHERMAP_API_KEY));
			CurrentWeather weather = owm.currentWeatherByCityName(arg);

			if(!weather.isValid()) {
				return context.getAuthorName()
						.flatMap(username -> loadingMsg.send(
								String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) City `%s` not found.", username, arg)))
						.then();
			}

			final String clouds = StringUtils.capitalizeFully(weather.getWeatherInstance(0).getWeatherDescription());
			final float windSpeed = weather.getWindInstance().getWindSpeed() * 3.6f;
			final String windDesc = this.getWindDesc(windSpeed);
			final String rain = weather.hasRainInstance() ? String.format("%.1f mm/h", weather.getRainInstance().getRain3h()) : "None";
			final float humidity = weather.getMainInstance().getHumidity();
			final float temperature = weather.getMainInstance().getTemperature();

			return context.getAuthorAvatarUrl()
					.map(avatarUrl -> EmbedUtils.getDefaultEmbed()
							.setAuthor(String.format("Weather for: %s", weather.getCityName()),
									String.format("http://openweathermap.org/city/%d", weather.getCityCode()),
									avatarUrl)
							.setThumbnail("https://image.flaticon.com/icons/svg/494/494472.svg")
							.setDescription("Last updated " + dateFormatter.format(weather.getDateTime()))
							.addField(Emoji.CLOUD + " Clouds", clouds, true)
							.addField(Emoji.WIND + " Wind", String.format("%s%n%.1f km/h", windDesc, windSpeed), true)
							.addField(Emoji.RAIN + " Rain", rain, true)
							.addField(Emoji.DROPLET + " Humidity", String.format("%.1f%%", humidity), true)
							.addField(Emoji.THERMOMETER + " Temperature", String.format("%.1f°C", temperature), true))
					.flatMap(loadingMsg::send)
					.then();

		} catch (IOException err) {
			loadingMsg.stopTyping();
			throw Exceptions.propagate(err);
		}
	}

	private String getWindDesc(float windSpeed) {
		if(windSpeed < 1) {
			return "Calm";
		} else if(NumberUtils.isInRange(windSpeed, 1, 6)) {
			return "Light air";
		} else if(NumberUtils.isInRange(windSpeed, 6, 12)) {
			return "Light breeze";
		} else if(NumberUtils.isInRange(windSpeed, 12, 20)) {
			return "Gentle breeze";
		} else if(NumberUtils.isInRange(windSpeed, 20, 29)) {
			return "Moderate breeze";
		} else if(NumberUtils.isInRange(windSpeed, 29, 39)) {
			return "Fresh breeze";
		} else if(NumberUtils.isInRange(windSpeed, 39, 50)) {
			return "Strong breeze";
		} else if(NumberUtils.isInRange(windSpeed, 50, 62)) {
			return "Near gale";
		} else if(NumberUtils.isInRange(windSpeed, 62, 75)) {
			return "Gale";
		} else if(NumberUtils.isInRange(windSpeed, 75, 89)) {
			return "Strong gale";
		} else if(NumberUtils.isInRange(windSpeed, 89, 103)) {
			return "Storm";
		} else if(NumberUtils.isInRange(windSpeed, 103, 118)) {
			return "Violent storm";
		} else {
			return "Hurricane";
		}
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show weather report for a city.")
				.addArg("city", false)
				.setSource("http://openweathermap.org/")
				.build();
	}
}
