package com.locibot.locibot.core.i18n;


import com.locibot.locibot.LociBot;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.utils.FormatUtil;
import com.locibot.locibot.utils.RandUtil;

import java.util.*;

public class I18nManager {

    public static final Locale[] LOCALES = {Locale.ENGLISH, Locale.FRENCH};

    private static final Map<Locale, ResourceBundle> GLOBAL_BUNDLES = I18nManager.initialize("i18n", LOCALES);
    private static final Map<Locale, ResourceBundle> SPAM_BUNDLES = I18nManager.initialize("spam", LOCALES);

    private static Map<Locale, ResourceBundle> initialize(String bundleName, Locale... locales) {
        final Map<Locale, ResourceBundle> map = new HashMap<>(locales.length);
        for (final Locale locale : locales) {
            map.put(locale, ResourceBundle.getBundle("i18n/%s".formatted(bundleName), locale));
        }
        LociBot.DEFAULT_LOGGER.info("Resource bundle '{}' initialized with {} languages", bundleName, map.size());
        return Collections.unmodifiableMap(map);
    }

    public static ResourceBundle getBundle(final Locale locale) {
        return GLOBAL_BUNDLES.get(locale);
    }

    public static String localize(Locale locale, String key) {
        try {
            return I18nManager.getBundle(locale).getString(key);
        } catch (final MissingResourceException ignored) {
            try {
                return I18nManager.getBundle(Config.DEFAULT_LOCALE).getString(key);
            } catch (final MissingResourceException err) {
                LociBot.DEFAULT_LOGGER.error("Can't find resource for key: %s".formatted(key), err);
                return key;
            }
        }
    }

    public static String localize(Locale locale, double number) {
        return FormatUtil.number(locale, number);
    }

    // TODO Clean-up
    public static String getRandomSpam(Locale locale) {
        final ResourceBundle bundle = SPAM_BUNDLES.get(locale);
        String randomKey = RandUtil.randValue(bundle.keySet());
        if (randomKey == null) {
            randomKey = RandUtil.randValue(SPAM_BUNDLES.get(Config.DEFAULT_LOCALE).keySet());
        }
        assert randomKey != null;
        return bundle.getString(randomKey);
    }

}
