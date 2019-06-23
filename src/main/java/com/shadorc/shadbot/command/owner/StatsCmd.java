package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.stats.StatisticEnum;
import com.shadorc.shadbot.data.stats.core.MapStatistic;
import com.shadorc.shadbot.data.stats.core.TableStatistic;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.StringUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class StatsCmd extends BaseCmd {

    public StatsCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, List.of("stats"));
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(1, 2);

        final StatisticEnum statEnum = Utils.parseEnum(StatisticEnum.class, args.get(0),
                new CommandException(String.format("`%s` is not a valid category. %s",
                        args.get(0), FormatUtils.options(StatisticEnum.class))));

        final Map<String, AtomicLong> map;
        if (args.size() == 1) {
            if (statEnum.getStat() instanceof TableStatistic) {
                return Mono.error(new CommandException(String.format("You need to specify a valid sub-category.%n%s",
                        FormatUtils.options(statEnum.getStat().getEnumClass()))));
            }

            map = ((MapStatistic<?>) statEnum.getStat()).getMap();
        } else {
            final Enum<?> subStatEnum = Utils.parseEnum(statEnum.getStat().getEnumClass(), args.get(1),
                    new CommandException(String.format("`%s` is not a valid sub-category. %s",
                            args.get(1), FormatUtils.options(statEnum.getStat().getEnumClass()))));

            if (statEnum.getStat() instanceof MapStatistic) {
                map = Map.of(subStatEnum.toString(), ((MapStatistic<?>) statEnum.getStat()).getValue(subStatEnum.toString()));
            } else {
                map = ((TableStatistic<?>) statEnum.getStat()).getMap(subStatEnum.toString());
            }
        }

        final Consumer<EmbedCreateSpec> embedConsumer = DiscordUtils.getDefaultEmbed()
                .andThen(embed -> {
                    embed.setAuthor(String.format("Stats: %s", StringUtils.toLowerCase(statEnum)), null, context.getAvatarUrl());

                    if (map == null || map.isEmpty()) {
                        embed.setDescription("No statistics yet.");
                    } else {
                        final Comparator<? super Map.Entry<String, AtomicLong>> comparator =
                                Map.Entry.comparingByValue(Comparator.comparingLong(AtomicLong::get));
                        final Map<String, AtomicLong> sortedMap = Utils.sortByValue(map, comparator.reversed());

                        embed.addField("Name", FormatUtils.format(sortedMap.keySet(), StringUtils::toLowerCase, "\n"), true)
                                .addField("Value", FormatUtils.format(sortedMap.values(), value -> FormatUtils.number(Long.parseLong(value.toString())), "\n"), true);
                    }
                });

        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(embedConsumer, channel))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Show statistics for the specified category.")
                .addArg("category", FormatUtils.options(StatisticEnum.class), false)
                .addArg("sub-category", "Needed when checking table statistics or to see a specific statistic", true)
                .build();
    }
}
