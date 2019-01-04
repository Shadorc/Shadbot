package me.shadorc.shadbot.command.owner;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.data.stats.StatisticEnum;
import me.shadorc.shadbot.data.stats.core.MapStatistic;
import me.shadorc.shadbot.data.stats.core.TableStatistic;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import reactor.core.publisher.Mono;

@Command(category = CommandCategory.OWNER, permission = CommandPermission.OWNER, names = { "stats" })
public class StatsCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = context.requireArgs(1, 2);

		if(args.get(0).equalsIgnoreCase("average")) {
			return context.getAvatarUrl()
					.map(avatarUrl -> EmbedUtils.getAverageEmbed().setAuthor("Stats: average", null, avatarUrl))
					.flatMap(embed -> context.getChannel()
							.flatMap(channel -> DiscordUtils.sendMessage(embed, channel)))
					.then();
		}

		final StatisticEnum statEnum = Utils.getEnum(StatisticEnum.class, args.get(0));
		if(statEnum == null) {
			throw new CommandException(String.format("`%s` is not a valid category. %s",
					args.get(0), FormatUtils.options(StatisticEnum.class)));
		}

		Map<String, AtomicLong> map;
		if(args.size() == 1) {
			if(statEnum.getStat() instanceof TableStatistic) {
				throw new CommandException(String.format("You need to specify a valid sub-category.%n%s",
						FormatUtils.options(statEnum.getStat().getEnumClass())));
			}

			map = ((MapStatistic<?>) statEnum.getStat()).getMap();
		} else {
			final Enum<?> subStatEnum = Utils.getEnum(statEnum.getStat().getEnumClass(), args.get(1));
			if(subStatEnum == null) {
				throw new CommandException(String.format("`%s` is not a valid sub-category. %s",
						args.get(1), FormatUtils.options(statEnum.getStat().getEnumClass())));
			}

			if(statEnum.getStat() instanceof MapStatistic) {
				map = Map.of(subStatEnum.toString(), ((MapStatistic<?>) statEnum.getStat()).getValue(subStatEnum.toString()));
			} else {
				map = ((TableStatistic<?>) statEnum.getStat()).getMap(subStatEnum.toString());
			}
		}

		final EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed();

		if(map == null || map.isEmpty()) {
			embed.setDescription("No statistics yet.");
		} else {
			final Comparator<? super Map.Entry<String, AtomicLong>> comparator =
					Map.Entry.comparingByValue((v1, v2) -> Long.compare(v1.get(), v2.get()));
			final Map<String, AtomicLong> sortedMap = Utils.sortByValue(map, comparator.reversed());

			embed.addField("Name", FormatUtils.format(sortedMap.keySet(), StringUtils::toLowerCase, "\n"), true)
					.addField("Value", FormatUtils.format(sortedMap.values(), value -> FormatUtils.number(Long.parseLong(value.toString())), "\n"), true);
		}

		return context.getAvatarUrl()
				.map(avatarUrl -> embed.setAuthor(String.format("Stats: %s", StringUtils.toLowerCase(statEnum)), null, avatarUrl))
				.flatMap(embedItr -> context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(embedItr, channel)))
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show statistics for the specified category.")
				.addArg("category", FormatUtils.options(StatisticEnum.class), false)
				.addArg("sub-category", "Needed when checking table statistics or to see a specific statistic", true)
				.addField("Info", "You can also use `average` as a *category* to get average winnings per game", false)
				.build();
	}
}
