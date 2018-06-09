package me.shadorc.shadbot.utils;

public class NumberUtils {

	/**
	 * @param str - the string to convert to integer
	 * @return The string converted as an Integer or null if this was not possible
	 */
	public static Integer asInt(String str) {
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException err) {
			return null;
		}
	}

	/**
	 * @param str - the string to convert as an integer between {@code min} and {@code max}
	 * @param min - the minimum value
	 * @param max - the maximum value
	 * @return The string converted as an integer or null if the string was not convertible or if it was not in the specified range
	 */
	public static Integer asIntBetween(String str, int min, int max) {
		try {
			Integer nbr = Integer.parseInt(str);
			if(!NumberUtils.isInRange(nbr, min, max)) {
				return null;
			}
			return nbr;
		} catch (NumberFormatException err) {
			return null;
		}
	}

	/**
	 * @param num - the number to convert as an integer between {@code min} and {@code max}
	 * @param min - the minimum value
	 * @param max - the maximum value
	 * @return The number converted as an integer between {@code min} and {@code max}
	 */
	public static int between(double num, double min, double max) {
		return (int) Math.max(min, Math.min(num, max));
	}

	/**
	 * @param str - the string to convert as a positive integer
	 * @return The string converted as a positive integer or null if the string was not convertible or if it was negative
	 */
	public static Integer asPositiveInt(String str) {
		try {
			Integer nbr = Integer.parseInt(str);
			return nbr > 0 ? nbr : null;
		} catch (NumberFormatException err) {
			return null;
		}
	}

	/**
	 * @param str - the string to convert as a positive long
	 * @return The string converted as a positive long or null if the string was not convertible or if it was negative
	 */
	public static Long asPositiveLong(String str) {
		try {
			Long nbr = Long.parseLong(str);
			return nbr > 0 ? nbr : null;
		} catch (NumberFormatException err) {
			return null;
		}
	}

	/**
	 * @param str - the string to check
	 * @return true if the string is convertible as a positive long, false otherwise
	 */
	public static boolean isPositiveLong(String str) {
		try {
			return Long.parseLong(str) > 0;
		} catch (NumberFormatException err) {
			return false;
		}
	}

	/**
	 * @param num - the number to check
	 * @param min - the minimum value, inclusive
	 * @param max - the maximum value, inclusive
	 * @return true if {@code num} is between {@code min}, inclusive, and {@code max}, inclusive, false otherwise
	 */
	public static boolean isInRange(float num, float min, float max) {
		return num >= min && num <= max;
	}

}
