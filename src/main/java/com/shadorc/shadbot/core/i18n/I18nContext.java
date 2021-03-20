package com.shadorc.shadbot.core.i18n;

import java.util.Locale;

public interface I18nContext {

    Locale getLocale();

    String localize(String key);

    String localize(double number);

}
