package me.shadorc.discordbot.utils;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
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

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.data.Config;
import sx.blah.discord.util.EmbedBuilder;

public class Utils {

	/**
	 * @return double representing process CPU load percentage value, Double.NaN if not available
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
	 * @param instant - the temporal object
	 * @return the amount of ms between fromDate and now
	 */
	public static int getPing(Instant instant) {
		return (int) Math.abs(ChronoUnit.MILLIS.between(LocalDateTime.now(), instant));
	}

	/**
	 * @param name - the name format
	 * @return a Thread Factory with name as name format
	 */
	public static ThreadFactory getThreadFactoryNamed(String name) {
		return new ThreadFactoryBuilder().setNameFormat(name).build();
	}

	/**
	 * @return the default embed builder (with author icon and color)
	 */
	public static EmbedBuilder getDefaultEmbed() {
		return new EmbedBuilder()
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR);
	}

	/**
	 * @param command - the command
	 * @return the default command embed builder (with author name, footer text, author icon and color)
	 */
	public static EmbedBuilder getDefaultEmbed(AbstractCommand command) {
		EmbedBuilder builder = Utils.getDefaultEmbed()
				.withAuthorName("Help for " + command.getFirstName() + " command");
		if(command.getAlias() != null) {
			builder.withFooterText("Alias: " + command.getAlias());
		}
		return builder;
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

	/**
	 * @param array - JSONArray to convert
	 * @return List<T> containing array elements
	 */
	public static <T> List<T> convertToList(JSONArray array, Class<T> listClass) {
		if(array == null) {
			return null;
		}
		List<T> list = new ArrayList<>();
		for(int i = 0; i < array.length(); i++) {
			if(listClass.isInstance(array.get(i))) {
				list.add(listClass.cast(array.get(i)));
			} else {
				throw new IllegalArgumentException("Array's elements cannot be casted to " + listClass.getSimpleName() + ".");
			}
		}
		return list;
	}

	/**
	 * @param instant - the instant to convert
	 * @return instant converted to local date time
	 */
	public static LocalDateTime convertToLocalDate(Instant instant) {
		return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
	}

	/**
	 * @param key - first object to compare
	 * @param objs - other objects to compare
	 * @return true if key is equal to all the objs, false otherwise
	 */
	public static boolean allEqual(Object key, Object... objs) {
		for(Object obj : objs) {
			if(!obj.equals(key)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @param duration - the duration in ms to sleep
	 */
	public static void sleep(long duration) {
		try {
			Thread.sleep(duration);
		} catch (InterruptedException ignored) {
		}
	}
}