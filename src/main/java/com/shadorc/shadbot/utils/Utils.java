package com.shadorc.shadbot.utils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.HashBasedTable;
import com.shadorc.shadbot.data.database.DatabaseManager;
import com.shadorc.shadbot.exception.CommandException;
import discord4j.core.object.entity.Member;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class Utils {

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setSerializationInclusion(Include.NON_EMPTY);

    /**
     * @param enumClass - the {@link Enum} class
     * @param value     - the string representing the enumeration, case insensitive
     * @return The {@link Enum} corresponding to the {@code value} from {@code enumClass} or null if it does not exist
     */
    public static <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value) {
        for (final T enumeration : enumClass.getEnumConstants()) {
            if (enumeration.toString().equalsIgnoreCase(value)) {
                return enumeration;
            }
        }
        return null;
    }

    /**
     * @param enumClass - the {@link Enum} class
     * @param value     - the string representing the enumeration, case insensitive
     * @param exception - the exception to be thrown
     * @return The {@link Enum} corresponding to the {@code value} from {@code enumClass}
     * @throws X - if the value is null
     */
    public static <T extends Enum<T>, X extends Throwable> T parseEnum(Class<T> enumClass, String value, X exception) throws X {
        final T enumValue = Utils.parseEnum(enumClass, value);
        if (enumValue != null) {
            return enumValue;
        } else {
            throw exception;
        }
    }

    /**
     * @return The percentage of CPU used or {@link Double#NaN} if the value could not be found
     */
    public static double getProcessCpuLoad() {
        try {
            final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            final ObjectName name = ObjectName.getInstance("java.lang:type=OperatingSystem");
            final AttributeList list = mbs.getAttributes(name, new String[]{"ProcessCpuLoad"});

            if (list.isEmpty()) {
                return Double.NaN;
            }

            final Attribute att = (Attribute) list.get(0);
            final Double value = (Double) att.getValue();

            if (value < 0) {
                return Double.NaN;
            }

            return value * 100.0d;
        } catch (final InstanceNotFoundException | ReflectionException | MalformedObjectNameException err) {
            return Double.NaN;
        }
    }

    /**
     * @param list - the list from which to take a random element
     * @return A random element from the list
     */
    public static <T> T randValue(List<T> list) {
        if (list.isEmpty()) {
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
     * @param map        - the map to sort
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
        for (final Entry<R, Map<C, V>> rowEntry : map.entrySet()) {
            for (final Entry<C, V> columnEntry : rowEntry.getValue().entrySet()) {
                table.put(rowEntry.getKey(), columnEntry.getKey(), columnEntry.getValue());
            }
        }
        return table;
    }

    /**
     * @param member - the member who bets
     * @param betStr - the string representing the bet
     * @return A long representing {@code betStr} converted as an int
     * @throws CommandException - thrown if {@code betStr} cannot be casted to an long
     *                          or if the {@code user} does not have enough coins.
     */
    public static long requireValidBet(Member member, String betStr) {
        final Long bet = NumberUtils.asPositiveLong(betStr);
        if (bet == null) {
            throw new CommandException(String.format("`%s` is not a valid amount of coins.", betStr));
        }

        if (DatabaseManager.getInstance().getDBMember(member.getGuildId(), member.getId()).getCoins() < bet) {
            throw new CommandException(TextUtils.NOT_ENOUGH_COINS);
        }

        return bet;
    }

}
