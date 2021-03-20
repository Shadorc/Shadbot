package com.shadorc.shadbot.api.wrapper;

import com.shadorc.shadbot.core.i18n.I18nContext;
import com.shadorc.shadbot.utils.NumberUtil;
import com.shadorc.shadbot.utils.TimeUtil;
import net.aksingh.owmjapis.model.CurrentWeather;
import net.aksingh.owmjapis.model.param.Rain;
import net.aksingh.owmjapis.model.param.Weather;

import java.time.LocalDateTime;
import java.util.Optional;

@SuppressWarnings("ConstantConditions") // Removes NullPointerException warnings
public class WeatherWrapper {

    private final CurrentWeather currentWeather;
    private final Weather weather;

    public WeatherWrapper(CurrentWeather currentWeather) {
        this.currentWeather = currentWeather;
        this.weather = currentWeather.getWeatherList().get(0);
    }

    public String getIconLink() {
        return this.weather.getIconLink();
    }

    public String getCountryCode() {
        return this.currentWeather.getSystemData().getCountryCode();
    }

    public String getCityName() {
        return this.currentWeather.getCityName();
    }

    public int getCityId() {
        return this.currentWeather.getCityId();
    }

    public LocalDateTime getDateTime() {
        return TimeUtil.toLocalDateTime(this.currentWeather.getDateTime().toInstant());
    }

    public String getCloudsDescription() {
        // TODO: I18n
        return this.weather.getDescription();
    }

    public double getWindSpeed() {
        return this.currentWeather.getWindData().getSpeed() * 3.6;
    }

    public Optional<Double> getPrecipVol3h() {
        return Optional.ofNullable(this.currentWeather.getRainData())
                .map(Rain::getPrecipVol3h);
    }

    public double getHumidity() {
        return this.currentWeather.getMainData().getHumidity();
    }

    public double getTemp() {
        return this.currentWeather.getMainData().getTemp();
    }

    public String getWindDescription(I18nContext i18nContext) {
        if (this.getWindSpeed() < 1) {
            return i18nContext.localize("weather.calm");
        } else if (NumberUtil.isBetween(this.getWindSpeed(), 1, 6)) {
            return i18nContext.localize("weather.light.air");
        } else if (NumberUtil.isBetween(this.getWindSpeed(), 6, 12)) {
            return i18nContext.localize("weather.light.breeze");
        } else if (NumberUtil.isBetween(this.getWindSpeed(), 12, 20)) {
            return i18nContext.localize("weather.gentle.breeze");
        } else if (NumberUtil.isBetween(this.getWindSpeed(), 20, 29)) {
            return i18nContext.localize("weather.moderate.breeze");
        } else if (NumberUtil.isBetween(this.getWindSpeed(), 29, 39)) {
            return i18nContext.localize("weather.fresh.breeze");
        } else if (NumberUtil.isBetween(this.getWindSpeed(), 39, 50)) {
            return i18nContext.localize("weather.strong.breeze");
        } else if (NumberUtil.isBetween(this.getWindSpeed(), 50, 62)) {
            return i18nContext.localize("weather.near.gale");
        } else if (NumberUtil.isBetween(this.getWindSpeed(), 62, 75)) {
            return i18nContext.localize("weather.gale");
        } else if (NumberUtil.isBetween(this.getWindSpeed(), 75, 89)) {
            return i18nContext.localize("weather.strong.gale");
        } else if (NumberUtil.isBetween(this.getWindSpeed(), 89, 103)) {
            return i18nContext.localize("weather.storm");
        } else if (NumberUtil.isBetween(this.getWindSpeed(), 103, 118)) {
            return i18nContext.localize("weather.violent.storm");
        } else {
            return i18nContext.localize("weather.hurricane");
        }
    }

}
