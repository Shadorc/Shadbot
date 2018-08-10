package me.shadorc.shadbot.utils;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.HashBasedTable;

import discord4j.core.object.entity.Member;
import me.shadorc.shadbot.data.db.DatabaseManager;
import me.shadorc.shadbot.exception.CommandException;

public class Utils {

	public static final ObjectMapper MAPPER = new ObjectMapper()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
			.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
			.enable(SerializationFeature.INDENT_OUTPUT)
			.setSerializationInclusion(Include.NON_EMPTY);

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
	 * @param value - the string representing the enumeration, case insensitive
	 * @return The enumeration corresponding to the {@code value} from {@code enumClass} or null if it does not exist
	 */
	public static <T extends Enum<T>> T getEnum(Class<T> enumClass, String value) {
		for(T enumeration : enumClass.getEnumConstants()) {
			if(enumeration.toString().equalsIgnoreCase(value)) {
				return enumeration;
			}
		}
		return null;
	}

	/**
	 * @param array - the {@link JSONArray} to convert
	 * @param clazz - the class of the elements contained by {@code array}
	 * @return A {@link List} containing the elements of {@code array} converted to {@code clazz} objects
	 */
	public static <T> List<T> toList(JSONArray array, Class<T> clazz) {
		if(array == null) {
			return null;
		}
		return array.toList().stream().map(clazz::cast).collect(Collectors.toList());
	}

	/**
	 * @param map - the {@link Map} to convert
	 * @return A {@link HashBasedTable} based on {@code map}
	 */
	public static <R, C, V> HashBasedTable<R, C, V> toTable(Map<R, Map<C, V>> map) {
		HashBasedTable<R, C, V> table = HashBasedTable.create();
		for(R rowKey : map.keySet()) {
			Map<C, V> rowMap = map.get(rowKey);
			for(C columnKey : rowMap.keySet()) {
				V value = rowMap.get(columnKey);
				table.put(rowKey, columnKey, value);
			}
		}
		return table;
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
	 * @param member - the member who bet
	 * @param betStr - the string representing the bet
	 * @param maxValue - the maximum bet value
	 * @return An Integer representing {@code betStr} converted as an integer
	 * @throws CommandException - thrown if {@code betStr} cannot be casted to integer, if the {@code user} does not have enough coins or if the bet value
	 *             is superior to {code maxValue}
	 */
	public static int requireBet(Member member, String betStr, int maxValue) {
		Integer bet = NumberUtils.asPositiveInt(betStr);
		if(bet == null) {
			throw new CommandException(String.format("`%s` is not a valid amount for coins.", betStr));
		}

		if(DatabaseManager.getDBMember(member.getGuildId(), member.getId()).getCoins() < bet) {
			throw new CommandException(TextUtils.NOT_ENOUGH_COINS);
		}

		if(bet > maxValue) {
			throw new CommandException(String.format("Sorry, you can't bet more than **%s**.",
					FormatUtils.formatCoins(maxValue)));
		}

		return bet;
	}

	/**
	 * @param list - the list from which to take a random element
	 * @return A random element from the list
	 */
	public static <T> T randValue(List<T> list) {
		return list.get(ThreadLocalRandom.current().nextInt(list.size()));
	}

	/**
	 * @param array - the array from which to take a random element
	 * @return A random element from the array
	 */
	public static <T> T randValue(T[] array) {
		return Utils.randValue(Arrays.asList(array));
	}

}
