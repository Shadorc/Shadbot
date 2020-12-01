package com.shadorc.shadbot.utils;

import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.annotation.Nullable;

public class LogUtils {

    private static final String ROOT_NAME = "shadbot";
    private static final String DELIMITER = ".";

    public enum Category {
        DATABASE, TEST, MUSIC;
    }

    public static <T> Logger getLogger() {
        return LogUtils.getLogger(null, new Category[]{});
    }

    public static <T> Logger getLogger(Category... categories) {
        return LogUtils.getLogger(null, categories);
    }

    public static <T> Logger getLogger(@Nullable Class<T> classType, Category... categories) {
        final StringBuilder strBuilder = new StringBuilder(ROOT_NAME);
        if (categories.length > 0) {
            strBuilder.append(DELIMITER);
            strBuilder.append(FormatUtils.format(categories, category -> category.name().toLowerCase(), DELIMITER));
        }
        if (classType != null) {
            strBuilder.append(DELIMITER);
            strBuilder.append(classType.getSimpleName());
        }
        return Loggers.getLogger(strBuilder.toString());
    }

}
