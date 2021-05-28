package com.shadorc.shadbot.core.i18n;


import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.LogUtil;
import com.shadorc.shadbot.utils.RandUtil;
import reactor.util.Logger;

import java.util.*;

public class I18nManager {

    private static final Logger LOGGER = LogUtil.getLogger(I18nManager.class);

    public static final Locale[] LOCALES = {Locale.ENGLISH, Locale.FRENCH};

    private static final Map<Locale, ResourceBundle> GLOBAL_BUNDLES = I18nManager.initialize("i18n", LOCALES);
    private static final Map<Locale, ResourceBundle> SPAM_BUNDLES = I18nManager.initialize("spam", LOCALES);

    private static Map<Locale, ResourceBundle> initialize(String bundleName, Locale... locales) {
        final Map<Locale, ResourceBundle> map = new HashMap<>(locales.length);
        for (final Locale locale : locales) {
            map.put(locale, ResourceBundle.getBundle("i18n/%s".formatted(bundleName), locale));
        }
        LOGGER.info("Resource bundle '{}' initialized with {} languages", bundleName, map.size());
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
                LOGGER.error("Can't find resource for key: %s".formatted(key), err);
                return key;
            }
        }
    }

    public static String localize(Locale locale, double number) {
        return FormatUtil.number(locale, number);
    }

    public static String getRandomSpam(Locale locale) {
        ResourceBundle bundle = SPAM_BUNDLES.get(locale);
        if (bundle == null || bundle.keySet().isEmpty()) {
            LOGGER.warn("Spam bundle does not contain any key for locale {}", locale.toLanguageTag());
            bundle = SPAM_BUNDLES.get(Config.DEFAULT_LOCALE);
        }
        final String randomKey = RandUtil.randValue(bundle.keySet());

        Objects.requireNonNull(randomKey);
        return bundle.getString(randomKey);
    }

}
