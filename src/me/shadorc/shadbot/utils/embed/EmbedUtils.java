package me.shadorc.shadbot.utils.embed;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;

import java.util.function.Consumer;

public class EmbedUtils {

    public static Consumer<EmbedCreateSpec> getDefaultEmbed() {
        return spec -> spec.setColor(Config.BOT_COLOR);
    }

    /*
    public static Consumer<EmbedCreateSpec> getAverageEmbed() {
        return embed -> {
            final Map<String, AtomicLong> moneyGained = StatsManager.MONEY_STATS.getMap(MoneyEnum.MONEY_GAINED);
            final Map<String, AtomicLong> moneyLost = StatsManager.MONEY_STATS.getMap(MoneyEnum.MONEY_LOST);

            final Set<String> gameNames = new HashSet<>();
            gameNames.addAll(moneyGained.keySet());
            gameNames.addAll(moneyLost.keySet());

            EmbedUtils.getDefaultEmbed().accept(embed);
            if (gameNames.isEmpty()) {
                embed.setDescription("No statistics yet.");
                return;
            }

            final Map<String, AtomicLong> commandsUsed = StatsManager.COMMAND_STATS.getMap(CommandEnum.COMMAND_USED);

            final Map<String, Tuple2<Float, Long>> averageMap = new HashMap<>();
            for (final String gameName : gameNames) {
                final long gains = moneyGained.getOrDefault(gameName, new AtomicLong(0)).get();
                final long losses = moneyLost.getOrDefault(gameName, new AtomicLong(0)).get();
                final long usages = commandsUsed.get(gameName).get();
                final float average = ((float) gains - losses) / usages;
                averageMap.put(gameName, Tuples.of(average, usages));
            }

            final Comparator<Entry<String, Tuple2<Float, Long>>> comparator = Comparator.comparingLong(v -> v.getValue().getT2());
            final Map<String, Tuple2<Float, Long>> sortedMap = Utils.sortByValue(averageMap, comparator.reversed());

            embed.addField("Name", String.join("\n", sortedMap.keySet()), true);
            embed.addField("Average", FormatUtils.format(sortedMap.values().stream().map(Tuple2::getT1), num -> FormatUtils.number(num.intValue()), "\n"), true);
            embed.addField("Count", FormatUtils.format(sortedMap.values().stream().map(Tuple2::getT2), (Function<Long, String>) FormatUtils::number, "\n"), true);
        };
    }
     */

}
