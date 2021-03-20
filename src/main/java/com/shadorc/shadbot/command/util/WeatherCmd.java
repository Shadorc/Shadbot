package com.shadorc.shadbot.command.util;

import com.shadorc.shadbot.api.wrapper.WeatherWrapper;
import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.EnumUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import com.shadorc.shadbot.utils.StringUtil;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import net.aksingh.owmjapis.api.APIException;
import net.aksingh.owmjapis.core.OWM;
import net.aksingh.owmjapis.core.OWM.Country;
import net.aksingh.owmjapis.core.OWM.Unit;
import org.apache.http.HttpStatus;
import reactor.core.publisher.Mono;

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

        return context.reply(Emoji.HOURGLASS, context.localize("weather.loading"))
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
                .flatMap(context::editReply)
                .onErrorResume(WeatherCmd.isNotFound(), err -> {
                    if (countryOpt.isPresent()) {
                        return context.editReply(Emoji.MAGNIFYING_GLASS, context.localize("weather.exception.country")
                                        .formatted(city, countryOpt.orElseThrow()));
                    }
                    return context.editReply(Emoji.MAGNIFYING_GLASS, context.localize("weather.exception.city")
                                    .formatted(city));
                })
                .onErrorMap(APIException.class, IOException::new);
    }

    private static Predicate<Throwable> isNotFound() {
        return err -> err instanceof APIException && ((APIException) err).getCode() == HttpStatus.SC_NOT_FOUND;
    }

    private Consumer<EmbedCreateSpec> formatEmbed(Context context, WeatherWrapper weather) {
        final DateTimeFormatter formatter = this.dateFormatter.withLocale(context.getDbGuild().getLocale());

        final String title = context.localize("weather.title")
                .formatted(weather.getCityName(), weather.getCountryCode());
        final String url = "https://openweathermap.org/city/%d".formatted(weather.getCityId());
        final String lastUpdated = formatter.format(weather.getDateTime());

        final String clouds = StringUtil.capitalize(weather.getCloudsDescription());

        final String windDesc = weather.getWindDescription(context.getI18nContext());
        final String wind = context.localize("weather.wind.speed")
                .formatted(windDesc, context.localize(weather.getWindSpeed()));

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
