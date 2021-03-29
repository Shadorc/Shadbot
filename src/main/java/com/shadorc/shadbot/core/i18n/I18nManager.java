package com.shadorc.shadbot.core.i18n;


import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.utils.FormatUtil;

import java.util.*;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class I18nManager {

    private static final Map<Locale, ResourceBundle> BUNDLES_MAP = I18nManager.initialize(Locale.ENGLISH, Locale.FRENCH);

    private static Map<Locale, ResourceBundle> initialize(Locale... locales) {
        final Map<Locale, ResourceBundle> map = new HashMap<>(locales.length);
        for (final Locale locale : locales) {
            map.put(locale, ResourceBundle.getBundle("i18n/i18n", locale));
        }
        DEFAULT_LOGGER.info("{} languages initialized", map.size());
        return Collections.unmodifiableMap(map);
    }

    public static ResourceBundle getBundle(final Locale locale) {
        return BUNDLES_MAP.get(locale);
    }

    public static String localize(Locale locale, String key) {
        try {
            return I18nManager.getBundle(locale).getString(key);
        } catch (final MissingResourceException err) {
            return I18nManager.getBundle(Config.DEFAULT_LOCALE).getString(key);
        }
    }

    public static String localize(Locale locale, double number) {
        return FormatUtil.number(locale, number);
    }

}
