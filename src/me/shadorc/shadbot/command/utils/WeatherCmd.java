package me.shadorc.shadbot.command.utils;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.credential.Credential;
import me.shadorc.shadbot.data.credential.Credentials;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import net.aksingh.owmjapis.api.APIException;
import net.aksingh.owmjapis.core.OWM;
import net.aksingh.owmjapis.core.OWM.Country;
import net.aksingh.owmjapis.core.OWM.Unit;
import net.aksingh.owmjapis.model.CurrentWeather;
import net.aksingh.owmjapis.model.param.Main;
import net.aksingh.owmjapis.model.param.Weather;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.UTILS, names = { "weather" })
public class WeatherCmd extends AbstractCommand {

	private final SimpleDateFormat dateFormatter = new SimpleDateFormat("MMMMM d, yyyy 'at' hh:mm aa", Locale.ENGLISH);

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = context.requireArgs(1, 2, ",");

		final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			final OWM owm = new OWM(Credentials.get(Credential.OPENWEATHERMAP_API_KEY));
			owm.setUnit(Unit.METRIC);

			CurrentWeather currentWeather;
			if(args.size() == 2) {
				final Country country = Utils.getEnum(Country.class, args.get(1).replace(" ", "_"));
				if(country == null) {
					return loadingMsg.send(String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) Country `%s` not found.",
							context.getUsername(), args.get(1))).then();
				}
				currentWeather = owm.currentWeatherByCityName(args.get(0), country);
			} else {
				currentWeather = owm.currentWeatherByCityName(args.get(0));
			}

			final Weather weather = currentWeather.getWeatherList().get(0);
			final Main main = currentWeather.getMainData();

			final double windSpeed = currentWeather.getWindData().getSpeed() * 3.6;
			final String windDesc = this.getWindDesc(windSpeed);
			final String rain = currentWeather.hasRainData() && currentWeather.getRainData().hasPrecipVol3h() ? String.format("%.1f mm/h", currentWeather.getRainData().getPrecipVol3h()) : "None";
			final String countryCode = currentWeather.getSystemData().getCountryCode();

			return context.getAvatarUrl()
					.map(avatarUrl -> {
						final Consumer<? super EmbedCreateSpec> embedConsumer = embed -> {
							EmbedUtils.getDefaultEmbed().accept(embed);
							embed.setAuthor(String.format("Weather: %s (%s)", currentWeather.getCityName(), countryCode),
										String.format("http://openweathermap.org/city/%d", currentWeather.getCityId()),
										avatarUrl)
								.setThumbnail(weather.getIconLink())
								.setDescription(String.format("Last updated %s", this.dateFormatter.format(currentWeather.getDateTime())))
								.addField(Emoji.CLOUD + " Clouds", StringUtils.capitalize(weather.getDescription()), true)
								.addField(Emoji.WIND + " Wind", String.format("%s%n%.1f km/h", windDesc, windSpeed), true)
								.addField(Emoji.RAIN + " Rain", rain, true)
								.addField(Emoji.DROPLET + " Humidity", String.format("%.1f%%", main.getHumidity()), true)
								.addField(Emoji.THERMOMETER + " Temperature", String.format("%.1f°C", main.getTemp()), true);
						};
						
						return embedConsumer;
					})
					.flatMap(loadingMsg::send)
					.then();

		} catch (final APIException err) {
			if(err.getCode() == 404) {
				return loadingMsg.send(String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) City `%s` not found.",
						context.getUsername(), args.get(0))).then();
			}
			loadingMsg.stopTyping();
			throw Exceptions.propagate(err);
		}
	}

	private String getWindDesc(double windSpeed) {
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
	public Mono<Consumer<? super EmbedCreateSpec>> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show weather report for a city.")
				.setDelimiter(", ")
				.addArg("city", false)
				.addArg("country", true)
				.setSource("http://openweathermap.org/")
				.build();
	}
}
