package me.shadorc.shadbot.utils;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.json.JSONArray;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.MessageChannel;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

public class Utils {

	/**
	 * @return The percentage of CPU used or {@link Double.NaN} if the value could not be found
	 */
	public static double getProcessCpuLoad() {
		double cpuLoad;
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			ObjectName name = ObjectName.getInstance("java.lang:type=OperatingSystem");
			AttributeList list = mbs.getAttributes(name, new String[] { "ProcessCpuLoad" });

			if(list.isEmpty()) {
				return Double.NaN;
			}

			Attribute att = (Attribute) list.get(0);
			Double value = (Double) att.getValue();

			if(value == -1.0) {
				return Double.NaN;
			}

			cpuLoad = value * 100d;
		} catch (InstanceNotFoundException | ReflectionException | MalformedObjectNameException err) {
			cpuLoad = Double.NaN;
		}

		return cpuLoad;
	}

	/**
	 * @param enumClass - the enumeration class
	 * @param value - the string representation of the enumeration, case insensitive
	 * @return The enumeration corresponding to the {@code value} from {@code enumClass} or null if it does not exist
	 */
	public static <T extends Enum<T>> T getValueOrNull(Class<T> enumClass, String value) {
		for(T enumeration : enumClass.getEnumConstants()) {
			if(enumeration.toString().equalsIgnoreCase(value)) {
				return enumeration;
			}
		}
		return null;
	}

	/**
	 * @param array - the array to convert
	 * @param listClass - the class of the elements contained by {@code array}
	 * @return A list containing the elements of {@code array} converted to {@code listClass} objects
	 */
	public static <T> List<T> toList(JSONArray array, Class<T> listClass) {
		if(array == null) {
			return null;
		}

		List<T> list = new ArrayList<>();
		for(int i = 0; i < array.length(); i++) {
			if(!listClass.isInstance(array.get(i))) {
				throw new IllegalArgumentException(String.format("Array's elements cannot be casted to %s.", listClass.getSimpleName()));
			}
			list.add(listClass.cast(array.get(i)));
		}
		return list;
	}

	/**
	 * @param map - the map to sort
	 * @param comparator - a {@link Comparator} to be used to compare stream elements
	 * @return A {@link LinkedHashMap} containing the elements of the {@code map} sorted by value using {@code comparator}
	 */
	public static <K, V> Map<K, V> sortByValue(Map<K, V> map, Comparator<? super Entry<K, V>> comparator) {
		return map.entrySet()
				.stream()
				.sorted(comparator)
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						Map.Entry::getValue,
						(value1, value2) -> value1,
						LinkedHashMap::new));
	}

	/**
	 * @param map - the map to sort
	 * @return A {@link LinkedHashMap} containing the elements of the {@code map} sorted by ascending value
	 */
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		return Utils.sortByValue(map, Map.Entry.comparingByValue(Collections.reverseOrder()));
	}

	/**
	 * @param channel - the channel where to send the error message if an error occurred
	 * @param member - the member who has bet
	 * @param betStr - the bet value as string
	 * @param maxValue - the maximum bet value
	 * @return An Integer representing {@code betStr} converted as an integer if no error occurred or {@code null} otherwise
	 * @throws CommandException - thrown if {@code betStr} cannot be casted to integer, if the {@code user} does not have enough coins or if the bet value
	 *             is superior to {code maxValue}
	 */
	public static Integer checkAndGetBet(Mono<MessageChannel> channel, Member member, String betStr, int maxValue) {
		Integer bet = NumberUtils.asPositiveInt(betStr);
		if(bet == null) {
			throw new CommandException(String.format("`%s` is not a valid amount for coins.", betStr));
		}

		if(Database.getDBMember(member.getGuildId(), member.getId()).getCoins() < bet) {
			BotUtils.sendMessage(TextUtils.notEnoughCoins(member), channel);
			return null;
		}

		if(bet > maxValue) {
			BotUtils.sendMessage(String.format(Emoji.BANK + " Sorry, you can't bet more than **%s**.",
					FormatUtils.formatCoins(maxValue)), channel);
			return null;
		}

		return bet;
	}

	/**
	 * @param threadName - the naming format to use
	 * @return A daemon {@link ThreadFactory} with the name format sets as {@code threadName}
	 */
	public static ThreadFactory createDaemonThreadFactory(String threadName) {
		return new ThreadFactoryBuilder().setNameFormat(threadName).setDaemon(true).build();
	}

	/**
	 * @param array - the array from which to take a random element
	 * @return A random element from the array
	 */
	public static <T> T randArray(T[] array) {
		return array[ThreadLocalRandom.current().nextInt(array.length)];
	}

}
