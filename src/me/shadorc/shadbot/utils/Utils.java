package me.shadorc.shadbot.utils;

import java.lang.management.ManagementFactory;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.json.JSONException;
import org.jsoup.HttpStatusException;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.utils.object.Emoji;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;

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

	public static <T extends Enum<T>> T getValueOrNull(Class<T> enumClass, String value) {
		for(T enumeration : enumClass.getEnumConstants()) {
			if(enumeration.toString().equalsIgnoreCase(value)) {
				return enumeration;
			}
		}
		return null;
	}

	public static <T> List<T> toList(JSONArray array, Class<T> listClass) {
		if(array == null) {
			return null;
		}

		List<T> list = new ArrayList<>();
		for(int i = 0; i < array.length(); i++) {
			if(listClass.isInstance(array.get(i))) {
				list.add(listClass.cast(array.get(i)));
			} else {
				throw new IllegalArgumentException(String.format("Array's elements cannot be casted to %s.", listClass.getSimpleName()));
			}
		}
		return list;
	}

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

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		return Utils.sortByValue(map, Map.Entry.comparingByValue(Collections.reverseOrder()));
	}

	public static void handle(String action, Context context, Throwable err) {
		final long guildID = context.getGuild().getLongID();

		String msg;
		if(err instanceof JSONException || err instanceof HttpStatusException && ((HttpStatusException) err).getStatusCode() == NetUtils.JSON_ERROR_CODE) {
			msg = "Mmmh... This service is currently unavailable... This is not my fault, I promise ! Try again later.";
			LogUtils.warnf("{Guild ID: %d} %s", guildID, err.getMessage());
		}

		else if(err instanceof ConnectException || err instanceof HttpStatusException && ((HttpStatusException) err).getStatusCode() == 503) {
			msg = "Mmmh... This service is currently unavailable... This is not my fault, I promise ! Try again later.";
			LogUtils.warnf("{Guild ID: %d} Service unavailable while %s.", guildID, action);
		}

		else if(err instanceof SocketTimeoutException) {
			msg = String.format("Mmmh... %s takes too long... This is not my fault, I promise ! Try again later.", StringUtils.capitalize(action));
			LogUtils.warnf("{Guild ID: %d} A SocketTimeoutException occurred while %s.", guildID, action);
		}

		else {
			msg = String.format("Sorry, something went wrong while %s... My developer has been warned.", action);
			LogUtils.error(context.getContent(), err, String.format("{Guild ID: %d} %s", guildID, msg));
		}

		BotUtils.sendMessage(Emoji.RED_FLAG + " " + msg, context.getChannel());
	}

	public static Integer checkAndGetBet(IChannel channel, IUser user, String betStr, int maxValue) throws IllegalCmdArgumentException {
		Integer bet = CastUtils.asPositiveInt(betStr);
		if(bet == null) {
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid amount for coins.", betStr));
		}

		if(Database.getDBUser(channel.getGuild(), user).getCoins() < bet) {
			BotUtils.sendMessage(TextUtils.notEnoughCoins(user), channel);
			return null;
		}

		if(bet > maxValue) {
			BotUtils.sendMessage(String.format(Emoji.BANK + " Sorry, you can't bet more than **%s**.",
					FormatUtils.formatCoins(maxValue)), channel);
			return null;
		}

		return bet;
	}

	public static boolean isInRange(float nbr, float min, float max) {
		return nbr >= min && nbr <= max;
	}

	public static ThreadFactory createDaemonThreadFactory(String threadName) {
		return new ThreadFactoryBuilder().setNameFormat(threadName).setDaemon(true).build();
	}

}
