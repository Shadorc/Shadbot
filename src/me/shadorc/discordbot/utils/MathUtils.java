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

	/**
	 * @param bound - the upper bound (exclusive). Must be positive.
	 * @return the next pseudorandom, uniformly distributed int value between zero (inclusive) and bound (exclusive)
	 */
	public static int rand(int bound) {
		return RAND.nextInt(bound);
	}

	/**
	 * @param num - the float to check
	 * @param min - minimum value (inclusive)
	 * @param max - maximum value (exclusive)
	 * @return true if min <= num < max, false otherwise
	 */
	public static boolean inRange(float num, int min, int max) {
		return min <= num && num < max;
	}

	/**
	 * @param start - the starting time in ms
	 * @param duration - the total duration in ms
	 * @return the time remaining before the duration has elapsed in ms
	 */
	public static int remainingTime(long start, long duration) {
		return (int) (duration - (System.currentTimeMillis() - start));
	}
}
