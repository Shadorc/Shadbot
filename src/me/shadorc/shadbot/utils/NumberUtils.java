package me.shadorc.shadbot.utils;

public class NumberUtils {

    /**
     * @param str - the string to convert, may be null
     * @return The Integer represented by the string, or null if conversion fails
     */
    public static Integer asInt(String str) {
        if (str == null) {
            return null;
        }

        try {
            return Integer.parseInt(str);
        } catch (final NumberFormatException err) {
            return null;
        }
    }

    /**
     * @param str - the string to convert, may be null
     * @param min - the minimum value
     * @param max - the maximum value
     * @return The Integer represented by the string, or null if conversion fails
     */
    public static Integer asIntBetween(String str, int min, int max) {
        if (str == null) {
            return null;
        }

        try {
            final int nbr = Integer.parseInt(str);
            if (!NumberUtils.isInRange(nbr, min, max)) {
                return null;
            }
            return nbr;
        } catch (final NumberFormatException err) {
            return null;
        }
    }

    /**
     * @param str - the string to convert, may be null
     * @return The positive Integer represented by the string, or null if conversion fails
     */
    public static Integer asPositiveInt(String str) {
        if (str == null) {
            return null;
        }

        try {
            final int nbr = Integer.parseInt(str);
            return nbr > 0 ? nbr : null;
        } catch (final NumberFormatException err) {
            return null;
        }
    }

    /**
     * @param str - the string to convert, may be null
     * @return The positive Long represented by the string, or null if conversion fails
     */
    public static Long asPositiveLong(String str) {
        if (str == null) {
            return null;
        }

        try {
            final long nbr = Long.parseLong(str);
            return nbr > 0 ? nbr : null;
        } catch (final NumberFormatException err) {
            return null;
        }
    }

    /**
     * @param num - the double to convert as an int between {@code min} and {@code max}
     * @param min - the minimum value
     * @param max - the maximum value
     * @return The double converted as an int between {@code min} and {@code max}
     */
    public static int between(double num, double min, double max) {
        return (int) Math.max(min, Math.min(num, max));
    }

    /**
     * @param num - the double to check
     * @param min - the minimum value, inclusive
     * @param max - the maximum value, inclusive
     * @return true if {@code num} is between {@code min}, inclusive, and {@code max}, inclusive, false otherwise
     */
    public static boolean isInRange(double num, double min, double max) {
        return num >= min && num <= max;
    }

    /**
     * @param str - the string to check, may be null
     * @return true if the string represents a positive long, false otherwise
     */
    public static boolean isPositiveLong(String str) {
        return NumberUtils.asPositiveInt(str) != null;
    }

}
