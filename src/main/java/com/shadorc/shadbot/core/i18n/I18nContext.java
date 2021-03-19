package com.shadorc.shadbot.core.i18n;

import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.utils.FormatUtil;

import java.util.Locale;
import java.util.MissingResourceException;

public class I18nContext {

    private final Locale locale;

    public I18nContext(Locale locale) {
        this.locale = locale;
    }

    public String localize(String key) {
        try {
            return I18nManager.getInstance().getBundle(this.locale).getString(key);
        } catch (final MissingResourceException err) {
            return I18nManager.getInstance().getBundle(Config.DEFAULT_LOCALE).getString(key);
        }
    }

    public String localize(double number) {
        return FormatUtil.number(number, this.locale);
    }

}
