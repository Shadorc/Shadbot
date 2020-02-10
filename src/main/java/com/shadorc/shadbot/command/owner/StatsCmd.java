package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.stats.entity.DailyCommandStats;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.common.json.EmbedFieldEntity;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

public class StatsCmd extends BaseCmd {

    public StatsCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, List.of("stats"));
    }

    @Override
    public Mono<Void> execute(Context context) {
        return DatabaseManager.getStats()
                .getCommandStats()
                .collectList()
                .map(list -> DiscordUtils.getDefaultEmbed()
                        .andThen(embed -> {
                            embed.setAuthor("Command stats", null, context.getAvatarUrl());
                            if (list.isEmpty()) {
                                embed.setDescription("There are currently no statistics available.");
                            } else {
                                final EmbedFieldEntity dailyField = this.getDailyCommandStats(list);
                                embed.addField(dailyField.getName(), dailyField.getValue(), dailyField.isInline());

                                final EmbedFieldEntity weeklyField = this.getWeeklyCommandStats(list);
                                embed.addField(weeklyField.getName(), weeklyField.getValue(), weeklyField.isInline());

                                final EmbedFieldEntity totalField = this.getTotalCommandStats(list);
                                embed.addField(totalField.getName(), totalField.getValue(), totalField.isInline());
                            }
                        }))
                .flatMap(embed -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(embed, channel)))
                .then();
    }

    private EmbedFieldEntity getDailyCommandStats(List<DailyCommandStats> list) {
        final DailyCommandStats daily = list.get(0);
        return new EmbedFieldEntity("Daily", this.formatMap(daily.getCommandStats()), true);
    }

    private EmbedFieldEntity getWeeklyCommandStats(List<DailyCommandStats> list) {
        final Map<String, Integer> computedMap = new HashMap<>();
        final LocalDate oneWeek = LocalDate.now().plusWeeks(1);
        for (final DailyCommandStats stats : list) {
            if (stats.getDate().isBefore(oneWeek)) {
                for (final Map.Entry<String, Integer> entry : stats.getCommandStats().entrySet()) {
                    computedMap.compute(entry.getKey(),
                            (key, value) -> value == null ? entry.getValue() : value + entry.getValue());
                }
            }
        }

        return new EmbedFieldEntity("Weekly", this.formatMap(computedMap), true);
    }

    private EmbedFieldEntity getTotalCommandStats(List<DailyCommandStats> list) {
        final Map<String, Integer> computedMap = new HashMap<>();
        for (final DailyCommandStats stats : list) {
            for (final Map.Entry<String, Integer> entry : stats.getCommandStats().entrySet()) {
                computedMap.compute(entry.getKey(),
                        (key, value) -> value == null ? entry.getValue() : value + entry.getValue());
            }
        }

        return new EmbedFieldEntity("Total", this.formatMap(computedMap), true);
    }

    private String formatMap(Map<String, Integer> map) {
        final StringBuilder strBuilder = new StringBuilder();
        final Map<String, Integer> sortedMap = Utils.sortMap(map,
                Collections.reverseOrder(Comparator.comparingInt(Map.Entry::getValue)));

        for (final Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
            strBuilder.append(String.format("%d - %s%n", entry.getValue(), entry.getKey()));
        }

        return strBuilder.toString().trim();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Display command usage statistics.")
                .build();
    }
}
