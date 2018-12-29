package me.shadorc.shadbot.utils;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.nio.file.Files;
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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.HashBasedTable;

public class Utils {

	public static final ObjectMapper MAPPER = new ObjectMapper()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
			.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
			.enable(SerializationFeature.INDENT_OUTPUT)
			.setSerializationInclusion(Include.NON_EMPTY);

	/**
	 * @param enumClass - the enumeration class
	 * @param value - the string representing the enumeration, case insensitive
	 * @return The enumeration corresponding to the {@code value} from {@code enumClass} or null if it does not exist
	 */
	public static <T extends Enum<T>> T getEnum(Class<T> enumClass, String value) {
		for(final T enumeration : enumClass.getEnumConstants()) {
			if(enumeration.toString().equalsIgnoreCase(value)) {
				return enumeration;
			}
		}
		return null;
	}

	/**
	 * @return The percentage of CPU used or {@link Double.NaN} if the value could not be found
	 */
	public static double getProcessCpuLoad() {
		double cpuLoad;
		try {
			final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			final ObjectName name = ObjectName.getInstance("java.lang:type=OperatingSystem");
			final AttributeList list = mbs.getAttributes(name, new String[] { "ProcessCpuLoad" });

			if(list.isEmpty()) {
				return Double.NaN;
			}

			final Attribute att = (Attribute) list.get(0);
			final Double value = (Double) att.getValue();

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
	 * @param list - the list from which to take a random element
	 * @return A random element from the list
	 */
	public static <T> T randValue(List<T> list) {
		if(list.isEmpty()) {
			return null;
		}
		return list.get(ThreadLocalRandom.current().nextInt(list.size()));
	}

	/**
	 * @param array - the array from which to take a random element
	 * @return A random element from the array
	 */
	public static <T> T randValue(T[] array) {
		return Utils.randValue(Arrays.asList(array));
	}

	/**
	 * @param file - The {@link File} to read
	 * @return a String containing the content of the
	 * @throws IOException if an I/O error occurs reading from the stream
	 */
	public static String read(File file) throws IOException {
		return new String(Files.readAllBytes(file.toPath()), Charset.forName("UTF-8"));
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
	 * @param map - the {@link Map} to convert
	 * @return A {@link HashBasedTable} based on {@code map}
	 */
	public static <R, C, V> HashBasedTable<R, C, V> toTable(Map<R, Map<C, V>> map) {
		final HashBasedTable<R, C, V> table = HashBasedTable.create();
		for(final R rowKey : map.keySet()) {
			final Map<C, V> rowMap = map.get(rowKey);
			for(final C columnKey : rowMap.keySet()) {
				final V value = rowMap.get(columnKey);
				table.put(rowKey, columnKey, value);
			}
		}
		return table;
	}

}
