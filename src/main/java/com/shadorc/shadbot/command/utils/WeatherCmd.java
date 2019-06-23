package com.shadorc.shadbot.command.utils;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.Credentials;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.object.message.LoadingMessage;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import com.shadorc.shadbot.utils.StringUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.spec.EmbedCreateSpec;
import net.aksingh.owmjapis.api.APIException;
import net.aksingh.owmjapis.core.OWM;
import net.aksingh.owmjapis.core.OWM.Country;
import net.aksingh.owmjapis.core.OWM.Unit;
import net.aksingh.owmjapis.model.CurrentWeather;
import net.aksingh.owmjapis.model.param.Main;
import net.aksingh.owmjapis.model.param.Weather;
import org.apache.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class WeatherCmd extends BaseCmd {

    private final SimpleDateFormat dateFormatter;

    public WeatherCmd() {
        super(CommandCategory.UTILS, List.of("weather"));
        this.setDefaultRateLimiter();

        this.dateFormatter = new SimpleDateFormat("MMMMM d, yyyy 'at' hh:mm aa", Locale.ENGLISH);
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(1, 2, ",");

        final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());
        return Mono.fromCallable(() -> {
            final OWM owm = new OWM(Credentials.get(Credential.OPENWEATHERMAP_API_KEY));
            owm.setUnit(Unit.METRIC);

            CurrentWeather currentWeather;
            if (args.size() == 2) {
                final Country country = Utils.parseEnum(Country.class, args.get(1).replace(" ", "_"));
                if (country == null) {
                    return loadingMsg.setContent(String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) Country `%s` not found.",
                            context.getUsername(), args.get(1)));
                }
                currentWeather = owm.currentWeatherByCityName(args.get(0), country);
            } else {
                currentWeather = owm.currentWeatherByCityName(args.get(0));
            }

            final Weather weather = currentWeather.getWeatherList().get(0);
            final Main main = currentWeather.getMainData();

            final double windSpeed = currentWeather.getWindData().getSpeed() * 3.6;
            final String windDesc = WeatherCmd.getWindDesc(windSpeed);
            final String rain = currentWeather.hasRainData() && currentWeather.getRainData().hasPrecipVol3h() ? String.format("%.1f mm/h", currentWeather.getRainData().getPrecipVol3h()) : "None";
            final String countryCode = currentWeather.getSystemData().getCountryCode();

            return loadingMsg.setEmbed(DiscordUtils.getDefaultEmbed()
                    .andThen(embed -> embed.setAuthor(String.format("Weather: %s (%s)", currentWeather.getCityName(), countryCode),
                            String.format("http://openweathermap.org/city/%d", currentWeather.getCityId()),
                            context.getAvatarUrl())
                            .setThumbnail(weather.getIconLink())
                            .setDescription(String.format("Last updated %s", this.dateFormatter.format(currentWeather.getDateTime())))
                            .addField(Emoji.CLOUD + " Clouds", StringUtils.capitalize(weather.getDescription()), true)
                            .addField(Emoji.WIND + " Wind", String.format("%s%n%.1f km/h", windDesc, windSpeed), true)
                            .addField(Emoji.RAIN + " Rain", rain, true)
                            .addField(Emoji.DROPLET + " Humidity", String.format("%.1f%%", main.getHumidity()), true)
                            .addField(Emoji.THERMOMETER + " Temperature", String.format("%.1f°C", main.getTemp()), true)));
        })
                .onErrorResume(APIException.class, err -> {
                    if (err.getCode() == HttpStatus.SC_NOT_FOUND) {
                        final StringBuilder strBuilder = new StringBuilder(
                                String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) City `%s`", context.getUsername(), args.get(0)));
                        if (args.size() == 2) {
                            strBuilder.append(String.format(" in country `%s`", args.get(1)));
                        }
                        strBuilder.append(" not found.");
                        return Mono.just(loadingMsg.setContent(strBuilder.toString()));
                    }
                    return Mono.error(err);
                })
                .flatMap(LoadingMessage::send)
                .doOnTerminate(loadingMsg::stopTyping)
                .then();
    }

    private static String getWindDesc(double windSpeed) {
        if (windSpeed < 1) {
            return "Calm";
        } else if (NumberUtils.isBetween(windSpeed, 1, 6)) {
            return "Light air";
        } else if (NumberUtils.isBetween(windSpeed, 6, 12)) {
            return "Light breeze";
        } else if (NumberUtils.isBetween(windSpeed, 12, 20)) {
            return "Gentle breeze";
        } else if (NumberUtils.isBetween(windSpeed, 20, 29)) {
            return "Moderate breeze";
        } else if (NumberUtils.isBetween(windSpeed, 29, 39)) {
            return "Fresh breeze";
        } else if (NumberUtils.isBetween(windSpeed, 39, 50)) {
            return "Strong breeze";
        } else if (NumberUtils.isBetween(windSpeed, 50, 62)) {
            return "Near gale";
        } else if (NumberUtils.isBetween(windSpeed, 62, 75)) {
            return "Gale";
        } else if (NumberUtils.isBetween(windSpeed, 75, 89)) {
            return "Strong gale";
        } else if (NumberUtils.isBetween(windSpeed, 89, 103)) {
            return "Storm";
        } else if (NumberUtils.isBetween(windSpeed, 103, 118)) {
            return "Violent storm";
        } else {
            return "Hurricane";
        }
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Show weather report for a city.")
                .setDelimiter(", ")
                .addArg("city", false)
                .addArg("country", true)
                .setSource("http://openweathermap.org/")
                .build();
    }
}
