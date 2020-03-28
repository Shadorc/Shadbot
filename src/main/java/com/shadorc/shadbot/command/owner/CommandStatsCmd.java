package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.stats.entity.command.DailyCommandStats;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ImmutableEmbedFieldData;
import discord4j.discordjson.possible.Possible;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

public class CommandStatsCmd extends BaseCmd {

    public CommandStatsCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, List.of("command_stats", "command-stats", "commandstats"), "cmd_stats");
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
                                final ImmutableEmbedFieldData dailyField = this.getDailyCommandStats(list);
                                embed.addField(dailyField.name(), dailyField.value(), dailyField.inline().get());

                                final ImmutableEmbedFieldData weeklyField = this.getWeeklyCommandStats(list);
                                embed.addField(weeklyField.name(), weeklyField.value(), weeklyField.inline().get());

                                final ImmutableEmbedFieldData totalField = this.getTotalCommandStats(list);
                                embed.addField(totalField.name(), totalField.value(), totalField.inline().get());
                            }
                        }))
                .flatMap(embed -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(embed, channel)))
                .then();
    }

    private ImmutableEmbedFieldData getDailyCommandStats(List<DailyCommandStats> list) {
        final DailyCommandStats daily = list.get(0);
        return ImmutableEmbedFieldData.of("Daily", this.formatMap(daily.getCommandStats()), Possible.of(true));
    }

    private ImmutableEmbedFieldData getWeeklyCommandStats(List<DailyCommandStats> list) {
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

        return ImmutableEmbedFieldData.of("Weekly", this.formatMap(computedMap), Possible.of(true));
    }

    private ImmutableEmbedFieldData getTotalCommandStats(List<DailyCommandStats> list) {
        final Map<String, Integer> computedMap = new HashMap<>();
        for (final DailyCommandStats stats : list) {
            for (final Map.Entry<String, Integer> entry : stats.getCommandStats().entrySet()) {
                computedMap.compute(entry.getKey(),
                        (key, value) -> value == null ? entry.getValue() : value + entry.getValue());
            }
        }

        return ImmutableEmbedFieldData.of("Total", this.formatMap(computedMap), Possible.of(true));
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
