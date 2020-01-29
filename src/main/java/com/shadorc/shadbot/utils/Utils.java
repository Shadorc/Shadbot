package com.shadorc.shadbot.utils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mongodb.MongoClientSettings;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.codec.IamCodec;
import com.shadorc.shadbot.db.codec.LongCodec;
import com.shadorc.shadbot.db.codec.SnowflakeCodec;
import com.shadorc.shadbot.db.guilds.entity.DBMember;
import com.shadorc.shadbot.exception.CommandException;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.JsonWriterSettings;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class Utils {

    public static final JsonWriterSettings JSON_WRITER_SETTINGS = JsonWriterSettings.builder()
            .int64Converter((value, writer) -> writer.writeNumber(value.toString()))
            .build();

    public static final CodecRegistry CODEC_REGISTRY = CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromCodecs(new SnowflakeCodec(), new LongCodec(), new IamCodec()));

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setSerializationInclusion(Include.NON_EMPTY);

    /**
     * @param enumClass the {@link Enum} class
     * @param value     the string representing the enumeration, case insensitive
     * @return The {@link Enum} corresponding to the {@code value} from {@code enumClass} or null if it does not exist.
     */
    @Nullable
    public static <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value) {
        for (final T enumeration : enumClass.getEnumConstants()) {
            if (enumeration.toString().equalsIgnoreCase(value)) {
                return enumeration;
            }
        }
        return null;
    }

    /**
     * @param enumClass the {@link Enum} class
     * @param value     the string representing the enumeration, case insensitive
     * @param exception the exception to be thrown
     * @return The {@link Enum} corresponding to the {@code value} from {@code enumClass}.
     * @throws X if the value is null
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
     * @param list the list from which to take a random element
     * @return A random element from the list or {@code null} if the list is empty.
     */
    @Nullable
    public static <T> T randValue(List<T> list) {
        if (list.isEmpty()) {
            return null;
        }
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    /**
     * @param array the array from which to take a random element
     * @return A random element from the array or {@code null} if the array is empty.
     */
    @Nullable
    public static <T> T randValue(T[] array) {
        return Utils.randValue(Arrays.asList(array));
    }

    /**
     * @param map        the map to sort
     * @param comparator a {@link Comparator} to be used to compare stream elements
     * @return A {@link LinkedHashMap} containing the elements of the {@code map} sorted by value using {@code
     * comparator}.
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
     * @param guildId the {@link Snowflake} ID of the {@link Guild} in which the {@link User} made the bet
     * @param userId  the {@link Snowflake} ID of the {@link User} who made the bet
     * @param betStr  the string representing the bet
     * @return A long representing {@code betStr}.
     * @throws CommandException thrown if {@code betStr} cannot be casted to a long or if the user does not have
     *                          enough coins.
     */
    public static Mono<Long> requireValidBet(Snowflake guildId, Snowflake userId, String betStr) {
        final Long bet = NumberUtils.toPositiveLongOrNull(betStr);
        if (bet == null) {
            throw new CommandException(String.format("`%s` is not a valid amount of coins.", betStr));
        }
        return Utils.requireValidBet(guildId, userId, bet);
    }

    /**
     * @param guildId the {@link Snowflake} ID of the {@link Guild} in which the {@link User} made the bet
     * @param userId  the {@link Snowflake} ID of the {@link User} who made the bet
     * @param bet     the bet
     * @return The bet.
     * @throws CommandException thrown if the user does not have enough coins.
     */
    public static Mono<Long> requireValidBet(Snowflake guildId, Snowflake userId, long bet) {
        return DatabaseManager.getGuilds()
                .getDBMember(guildId, userId)
                .map(DBMember::getCoins)
                .map(coins -> {
                    if (coins < bet) {
                        throw new CommandException(TextUtils.NOT_ENOUGH_COINS);
                    }
                    return bet;
                });
    }

}
