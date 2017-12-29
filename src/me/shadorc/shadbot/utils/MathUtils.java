package me.shadorc.shadbot.utils;

import java.util.Random;

public class MathUtils {

	private static final Random RAND = new Random();

	public static int rand(int min, int max) {
		return min + MathUtils.rand(max - min + 1);
	}

	public static int rand(int bound) {
		return RAND.nextInt(bound);
	}

	public static boolean inRange(float nbr, float min, float max) {
		return nbr >= min && nbr <= max;
	}
}
