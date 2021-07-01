package com.locibot.locibot.command.util;

import com.locibot.locibot.api.wrapper.WeatherWrapper;
import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.data.credential.Credential;
import com.locibot.locibot.data.credential.CredentialManager;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.CreateHeatMap;
import com.locibot.locibot.utils.EnumUtil;
import com.locibot.locibot.utils.ShadbotUtil;
import com.locibot.locibot.utils.StringUtil;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import net.aksingh.owmjapis.api.APIException;
import net.aksingh.owmjapis.core.OWM;
import net.aksingh.owmjapis.core.OWM.Country;
import net.aksingh.owmjapis.core.OWM.Unit;
import org.apache.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class WeatherCmd extends BaseCmd {

    private final DateTimeFormatter dateFormatter;
    private final OWM owm;

    public WeatherCmd() {
        super(CommandCategory.UTILS, "weather", "Search weather report for a city");
        this.addOption("city", "The city", true, ApplicationCommandOptionType.STRING);
        this.addOption("country", "The country", false, ApplicationCommandOptionType.STRING);

        this.dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.MEDIUM);
        final String apiKey = CredentialManager.get(Credential.OPENWEATHERMAP_API_KEY);
        if (apiKey != null) {
            this.owm = new OWM(apiKey);
            this.owm.setUnit(Unit.METRIC);
        } else {
            this.owm = null;
        }
    }

    private static Predicate<Throwable> isNotFound() {
        return thr -> thr instanceof APIException err && err.getCode() == HttpStatus.SC_NOT_FOUND;
    }

    @Override
    public Mono<?> execute(Context context) {
        final String city = context.getOptionAsString("city").orElseThrow();
        final Optional<String> countryOpt = context.getOptionAsString("country");

        return context.createFollowupMessage(Emoji.HOURGLASS, context.localize("weather.loading"))
                .then(Mono.fromCallable(() -> {
                    if (countryOpt.isPresent()) {
                        final String countryStr = countryOpt.get();
                        final Country country = EnumUtil.parseEnum(Country.class,
                                countryStr.replace(" ", "_"));
                        if (country == null) {
                            throw new CommandException(
                                    context.localize("weather.country.not.found").formatted(countryStr));
                        }
                        return this.owm.currentWeatherByCityName(city, country);
                    } else {
                        return this.owm.currentWeatherByCityName(city);
                    }
                }))
                .map(WeatherWrapper::new)
                .map(weather -> this.formatEmbed(context, weather))
                .flatMap(context::editFollowupMessage)
                .onErrorResume(WeatherCmd.isNotFound(), err -> {
                    if (countryOpt.isPresent()) {
                        return context.editFollowupMessage(Emoji.MAGNIFYING_GLASS, context.localize("weather.exception.country")
                                .formatted(city, countryOpt.orElseThrow()));
                    }
                    return context.editFollowupMessage(Emoji.MAGNIFYING_GLASS, context.localize("weather.exception.city")
                            .formatted(city));
                })
                .onErrorMap(APIException.class, IOException::new)
                .then(context.getChannel().flatMap(textChannel ->
                        textChannel.createMessage(messageCreateSpec -> {
                            try {
                                byte[] bytes = CreateHeatMap.create(city, this.owm);
                                if (bytes.length > 0)
                                    messageCreateSpec.addFile("temperature.png",
                                            new ByteArrayInputStream(bytes));
                                else messageCreateSpec.setContent("Please provide a real city ;)");
                            } catch (IOException ignored) {
                            }
                        })));
    }

    private Consumer<EmbedCreateSpec> formatEmbed(Context context, WeatherWrapper weather) {
        final DateTimeFormatter formatter = this.dateFormatter.withLocale(context.getLocale());

        final String title = context.localize("weather.title")
                .formatted(weather.getCityName(), weather.getCountryCode());
        final String url = "https://openweathermap.org/city/%d".formatted(weather.getCityId());
        final String lastUpdated = formatter.format(weather.getDateTime());

        final String clouds = StringUtil.capitalize(weather.getCloudsDescription());
        final String wind = context.localize("weather.wind.speed")
                .formatted(weather.getWindDescription(context), context.localize(weather.getWindSpeed()));
        final String rain = weather.getPrecipVol3h()
                .map(data -> context.localize("weather.precip.volume").formatted(context.localize(data)))
                .orElse(context.localize("weather.none"));
        final String humidity = "%s%%".formatted(context.localize(weather.getHumidity()));
        final String temperature = "%s°C".formatted(context.localize(weather.getTemp()));

        return ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor(title, url, context.getAuthorAvatar())
                        .setThumbnail(weather.getIconLink())
                        .setDescription(context.localize("weather.last.updated").formatted(lastUpdated))
                        .addField(Emoji.CLOUD + " " + context.localize("weather.clouds"), clouds, true)
                        .addField(Emoji.WIND + " " + context.localize("weather.wind"), wind, true)
                        .addField(Emoji.RAIN + " " + context.localize("weather.rain"), rain, true)
                        .addField(Emoji.DROPLET + " " + context.localize("weather.humidity"), humidity, true)
                        .addField(Emoji.THERMOMETER + " " + context.localize("weather.temperature"), temperature, true));
    }

}
