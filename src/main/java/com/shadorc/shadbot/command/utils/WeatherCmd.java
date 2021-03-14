package com.shadorc.shadbot.command.utils;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.EnumUtil;
import com.shadorc.shadbot.utils.NumberUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import com.shadorc.shadbot.utils.StringUtil;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import net.aksingh.owmjapis.api.APIException;
import net.aksingh.owmjapis.core.OWM;
import net.aksingh.owmjapis.core.OWM.Country;
import net.aksingh.owmjapis.core.OWM.Unit;
import net.aksingh.owmjapis.model.CurrentWeather;
import net.aksingh.owmjapis.model.param.Main;
import net.aksingh.owmjapis.model.param.Weather;
import org.apache.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

public class WeatherCmd extends BaseCmd {

    private final SimpleDateFormat dateFormatter;
    private final OWM owm;

    public WeatherCmd() {
        super(CommandCategory.UTILS, "weather", "Search weather report for a city");
        this.addOption("city", "The city", true, ApplicationCommandOptionType.STRING);
        this.addOption("country", "The country", false, ApplicationCommandOptionType.STRING);

        this.dateFormatter = new SimpleDateFormat("MMMMM d, yyyy 'at' hh:mm aa", Locale.ENGLISH);
        final String apiKey = CredentialManager.getInstance().get(Credential.OPENWEATHERMAP_API_KEY);
        if (apiKey != null) {
            this.owm = new OWM(apiKey);
            this.owm.setUnit(Unit.METRIC);
        } else {
            this.owm = null;
        }
    }

    @Override
    public Mono<?> execute(Context context) {
        final String city = context.getOptionAsString("city").orElseThrow();
        final Optional<String> countryOpt = context.getOptionAsString("country");

        return context.createFollowupMessage(Emoji.HOURGLASS + " (**%s**) Loading weather report...", context.getAuthorName())
                .flatMap(messageId -> Mono
                        .fromCallable(() -> {
                            if (countryOpt.isPresent()) {
                                final String countryStr = countryOpt.get();
                                final Country country = EnumUtil.parseEnum(Country.class,
                                        countryStr.replace(" ", "_"));
                                if (country == null) {
                                    throw new IllegalArgumentException("Country `%s` not found".formatted(countryStr));
                                }
                                return this.owm.currentWeatherByCityName(city, country);
                            } else {
                                return this.owm.currentWeatherByCityName(city);
                            }
                        })
                        .map(currentWeather -> this.formatEmbed(context.getAuthorAvatarUrl(), currentWeather))
                        .onErrorMap(APIException.class, err -> {
                            if (err.getCode() == HttpStatus.SC_NOT_FOUND) {
                                final StringBuilder strBuilder = new StringBuilder(
                                        Emoji.MAGNIFYING_GLASS + " (**%s**) City `%s`".formatted(context.getAuthorName(), city));
                                countryOpt.ifPresent(country -> strBuilder.append(" in country `%s`".formatted(country)));
                                strBuilder.append(" not found.");
                                return new IllegalArgumentException(strBuilder.toString());
                            }
                            return new IOException(err);
                        })
                        .onErrorResume(IllegalArgumentException.class, err -> context.editFollowupMessage(messageId,
                                Emoji.MAGNIFYING_GLASS + " (**%s**) %s.", context.getAuthorName(), err.getMessage())
                                .then(Mono.empty()))
                        .flatMap(embed -> context.editFollowupMessage(messageId, embed)));
    }

    @SuppressWarnings("ConstantConditions") // Removes NullPointerException warnings
    private Consumer<EmbedCreateSpec> formatEmbed(String avatarUrl, CurrentWeather currentWeather) {
        final Weather weather = currentWeather.getWeatherList().get(0);
        final Main main = currentWeather.getMainData();

        final String countryCode = currentWeather.getSystemData().getCountryCode();
        final String title = "Weather: %s (%s)".formatted(currentWeather.getCityName(), countryCode);
        final String url = "https://openweathermap.org/city/%d".formatted(currentWeather.getCityId());
        final String lastUpdated = this.dateFormatter.format(currentWeather.getDateTime());
        final String clouds = StringUtil.capitalize(weather.getDescription());
        final double windSpeed = currentWeather.getWindData().getSpeed() * 3.6;
        final String windDesc = WeatherCmd.getWindDesc(windSpeed);
        final String wind = "%s%n%.1f km/h".formatted(windDesc, windSpeed);
        final String rain = currentWeather.hasRainData() && currentWeather.getRainData().hasPrecipVol3h() ?
                "%.1f mm/h".formatted(currentWeather.getRainData().getPrecipVol3h()) : "None";
        final String humidity = "%.1f%%".formatted(main.getHumidity());
        final String temperature = "%.1f°C".formatted(main.getTemp());

        return ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor(title, url, avatarUrl)
                        .setThumbnail(weather.getIconLink())
                        .setDescription(String.format("Last updated %s", lastUpdated))
                        .addField(Emoji.CLOUD + " Clouds", clouds, true)
                        .addField(Emoji.WIND + " Wind", wind, true)
                        .addField(Emoji.RAIN + " Rain", rain, true)
                        .addField(Emoji.DROPLET + " Humidity", humidity, true)
                        .addField(Emoji.THERMOMETER + " Temperature", temperature, true));
    }

    private static String getWindDesc(double windSpeed) {
        if (windSpeed < 1) {
            return "Calm";
        } else if (NumberUtil.isBetween(windSpeed, 1, 6)) {
            return "Light air";
        } else if (NumberUtil.isBetween(windSpeed, 6, 12)) {
            return "Light breeze";
        } else if (NumberUtil.isBetween(windSpeed, 12, 20)) {
            return "Gentle breeze";
        } else if (NumberUtil.isBetween(windSpeed, 20, 29)) {
            return "Moderate breeze";
        } else if (NumberUtil.isBetween(windSpeed, 29, 39)) {
            return "Fresh breeze";
        } else if (NumberUtil.isBetween(windSpeed, 39, 50)) {
            return "Strong breeze";
        } else if (NumberUtil.isBetween(windSpeed, 50, 62)) {
            return "Near gale";
        } else if (NumberUtil.isBetween(windSpeed, 62, 75)) {
            return "Gale";
        } else if (NumberUtil.isBetween(windSpeed, 75, 89)) {
            return "Strong gale";
        } else if (NumberUtil.isBetween(windSpeed, 89, 103)) {
            return "Storm";
        } else if (NumberUtil.isBetween(windSpeed, 103, 118)) {
            return "Violent storm";
        } else {
            return "Hurricane";
        }
    }

}
