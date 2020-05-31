package com.shadorc.shadbot.utils;

import reactor.util.annotation.Nullable;

public class EnumUtils {

    /**
     * @param enumClass The {@link Enum} class.
     * @param value The string representing the enumeration, case insensitive, may be {@code null}.
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

    /**
     * @param enumClass The {@link Enum} class.
     * @param value The string representing the enumeration, case insensitive, may be {@code null}.
     * @param exception The exception to be thrown.
     * @return The {@link Enum} corresponding to the {@code value} from {@code enumClass}.
     * @throws X if the value is null.
     */
    public static <T extends Enum<T>, X extends Throwable> T parseEnum(Class<T> enumClass, @Nullable String value, X exception) throws X {
        final T enumValue = EnumUtils.parseEnum(enumClass, value);
        if (enumValue != null) {
            return enumValue;
        } else {
            throw exception;
        }
    }

}
