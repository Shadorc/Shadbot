package me.shadorc.shadbot.utils;

public class CastUtils {

	public static Integer asInt(String str) {
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException err) {
			return null;
		}
	}

	public static Integer asIntBetween(String str, int min, int max) {
		try {
			Integer nbr = Integer.parseInt(str);
			if(!Utils.isInRange(nbr, min, max)) {
				return null;
			}
			return nbr;
		} catch (NumberFormatException err) {
			return null;
		}
	}

	public static Integer asPositiveInt(String str) {
		try {
			Integer nbr = Integer.parseInt(str);
			return nbr > 0 ? nbr : null;
		} catch (NumberFormatException err) {
			return null;
		}
	}

	public static Long asPositiveLong(String str) {
		try {
			Long nbr = Long.parseLong(str);
			return nbr > 0 ? nbr : null;
		} catch (NumberFormatException err) {
			return null;
		}
	}

	public static boolean isPositiveLong(String str) {
		try {
			return Long.parseLong(str) > 0;
		} catch (NumberFormatException err) {
			return false;
		}
	}

}
