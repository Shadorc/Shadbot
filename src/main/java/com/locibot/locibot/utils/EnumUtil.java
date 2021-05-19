package com.locibot.locibot.utils;

import reactor.util.annotation.Nullable;

public class EnumUtil {

    /**
     * @param enumClass The {@link Enum} class.
     * @param value     The string representing the enumeration, case insensitive, may be {@code null}.
     * @return The {@link Enum} corresponding to the {@code value} from {@code enumClass} or null if it does not exist.
     */
    @Nullable
    public static <T extends Enum<T>> T parseEnum(Class<T> enumClass, @Nullable String value) {
        for (final T enumeration : enumClass.getEnumConstants()) {
            if (enumeration.name().equalsIgnoreCase(value)) {
                return enumeration;
            }
        }
        return null;
    }

}
