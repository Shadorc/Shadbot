package me.shadorc.shadbot.utils;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

public class Utils {

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

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		return map.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						Map.Entry::getValue,
						(value1, value2) -> value1,
						LinkedHashMap::new));
	}

	public static <T extends Enum<T>> T getValueOrNull(Class<T> enumClass, String value) {
		for(T enumeration : enumClass.getEnumConstants()) {
			if(enumeration.toString().equalsIgnoreCase(value)) {
				return enumeration;
			}
		}
		return null;
	}

	public static <T> List<T> removeAndGet(T array[], T elmt) {
		return Arrays.stream(array).filter(element -> !element.equals(elmt)).collect(Collectors.toList());
	}

}
