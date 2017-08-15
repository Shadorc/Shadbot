package me.shadorc.discordbot.utils;

import java.util.Random;

public class MathUtils {

	private static final Random RAND = new Random();

	/**
	 * @param min - min value
	 * @param max - max value
	 * @return the next pseudorandom, uniformly distributed int value between min (inclusive) and max (inclusive)
	 */
	public static int rand(int min, int max) {
		return min + MathUtils.rand(max - min + 1);
	}

	public static int rand(int bound) {
		return RAND.nextInt(bound);
	}

	/**
	 * @param num - float to check
	 * @param min - minimum value (inclusive)
	 * @param max - maximum value (exclusive)
	 * @return true if min <= num < max, false otherwise
	 */
	public static boolean inRange(float num, int min, int max) {
		return min <= num && num < max;
	}
}
