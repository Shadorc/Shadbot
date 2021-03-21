package com.shadorc.shadbot.core.i18n;


import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.utils.FormatUtil;

import java.util.*;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class I18nManager {

    private static I18nManager instance;

    static {
        I18nManager.instance = new I18nManager();
    }

    private final Map<Locale, ResourceBundle> bundlesMap;

    private I18nManager() {
        this.bundlesMap = I18nManager.initialize(Locale.ENGLISH, Locale.FRENCH);
    }

    private static Map<Locale, ResourceBundle> initialize(Locale... locales) {
        final Map<Locale, ResourceBundle> map = new HashMap<>(locales.length);
        for (final Locale locale : locales) {
            map.put(locale, ResourceBundle.getBundle("i18n/i18n", locale));
        }
        DEFAULT_LOGGER.info("{} locales initialized", map.size());
        return Collections.unmodifiableMap(map);
    }

    public ResourceBundle getBundle(final Locale locale) {
        return this.bundlesMap.get(locale);
    }

    public String localize(Locale locale, String key) {
        try {
            return this.getBundle(locale).getString(key);
        } catch (final MissingResourceException err) {
            return this.getBundle(Config.DEFAULT_LOCALE).getString(key);
        }
    }

    public String localize(Locale locale, double number) {
        return FormatUtil.number(number, locale);
    }

    public static I18nManager getInstance() {
        return I18nManager.instance;
    }

}
