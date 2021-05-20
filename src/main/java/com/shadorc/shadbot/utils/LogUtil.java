package com.shadorc.shadbot.utils;

import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.annotation.Nullable;

public class LogUtil {

    private static final String ROOT_NAME = "shadbot";
    private static final String DELIMITER = ".";

    public enum Category {
        DATABASE, TEST, MUSIC, COMMAND, TASK, SERVICE
    }

    public static Logger getLogger() {
        return LogUtil.getLogger(null, new Category[]{});
    }

    public static Logger getLogger(Category... categories) {
        return LogUtil.getLogger(null, categories);
    }

    public static <T> Logger getLogger(@Nullable Class<T> classType, Category... categories) {
        final StringBuilder strBuilder = new StringBuilder(ROOT_NAME);
        if (categories.length > 0) {
            strBuilder.append(DELIMITER)
                    .append(FormatUtil.format(categories, category -> category.name().toLowerCase(), DELIMITER));
        }
        if (classType != null) {
            strBuilder.append(DELIMITER)
                    .append(classType.getSimpleName());
        }
        return Loggers.getLogger(strBuilder.toString());
    }

}
