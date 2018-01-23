package me.shadorc.shadbot.utils;

import java.lang.management.ManagementFactory;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.jsoup.HttpStatusException;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.utils.object.Emoji;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;

public class Utils {

	public static ThreadFactory getThreadFactoryNamed(String name) {
		return new ThreadFactoryBuilder().setNameFormat(name).build();
	}

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

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		return ImmutableSortedMap.copyOf(map, Ordering.natural().onResultOf(Functions.forMap(map)).reverse());
	}

	public static <T> List<T> removeAndGet(T[] array, T elmt) {
		return Arrays.stream(array).filter(element -> !element.equals(elmt)).collect(Collectors.toList());
	}

	public static void handle(String action, Context context, Throwable err) {
		String msg;
		if(err instanceof HttpStatusException && ((HttpStatusException) err).getStatusCode() == 503) {
			msg = "Mmmh... This service is currently unavailable... This is not my fault, I promise ! Try again later.";
		} else if(err instanceof SocketTimeoutException) {
			msg = String.format("Mmmh... %s takes too long... This is not my fault, I promise ! Try again later.",
					StringUtils.capitalize(action));
		} else {
			msg = String.format("Sorry, something went wrong while %s... My developer has been warned.", action);
		}
		LogUtils.errorf(context.getContent(), context.getChannel(), err, msg);
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

}
