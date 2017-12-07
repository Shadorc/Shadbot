package me.shadorc.discordbot.command.owner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.CommandManager;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.stats.StatsEnum;
import me.shadorc.discordbot.stats.StatsManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.FormatUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

public class StatsCmd extends AbstractCommand {

	private static final int ROW_SIZE = 25;
	private static final List<StatsEnum> CATEGORIES =
			Arrays.stream(StatsEnum.values()).filter(statsEnum -> statsEnum.isCategory()).collect(Collectors.toList());

	public StatsCmd() {
		super(CommandCategory.OWNER, Role.OWNER, RateLimiter.DEFAULT_COOLDOWN, "stats");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		if(context.getArg().equals("average")) {
			BotUtils.sendMessage(this.getAverage(), context.getChannel());
			return;
		}

		if(!CATEGORIES.stream().anyMatch(stats -> stats.toString().equals(context.getArg()))) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Category unknown. (Options: "
					+ FormatUtils.formatList(CATEGORIES, cat -> "**" + cat.toString() + "**", ", ") + ")", context.getChannel());
			return;
		}

		StatsEnum category = StatsEnum.valueOf(context.getArg().toUpperCase());

		if(StatsManager.get(category).isEmpty()) {
			BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " This category is empty.", context.getChannel());
			return;
		}

		if(category.equals(StatsEnum.VARIOUS)) {
			BotUtils.sendMessage(this.getVarious(), context.getChannel());
			return;
		}

		BotUtils.sendMessage(this.getCommandsStats(category), context.getChannel());
	}

	private EmbedObject getAverage() {
		Map<String, AtomicLong> moneyGainsCommandObj = StatsManager.get(StatsEnum.MONEY_GAINS_COMMAND);
		Map<String, AtomicLong> moneyLossesCommandObj = StatsManager.get(StatsEnum.MONEY_LOSSES_COMMAND);
		Map<String, AtomicLong> commandObj = StatsManager.get(StatsEnum.COMMAND);

		EmbedBuilder builder = Utils.getDefaultEmbed()
				.withAuthorName("Stats average");

		StringBuilder nameStr = new StringBuilder();
		StringBuilder averageStr = new StringBuilder();
		StringBuilder countStr = new StringBuilder();
		for(Object key : moneyGainsCommandObj.keySet()) {
			long gains = moneyGainsCommandObj.getOrDefault(key.toString(), new AtomicLong(0)).get();
			long losses = moneyLossesCommandObj.getOrDefault(key.toString(), new AtomicLong(0)).get();
			long count = CommandManager.getCommand(key.toString()).getNames().stream().mapToLong(
					name -> commandObj.getOrDefault(name, new AtomicLong(0)).get()).sum();

			if(gains == 0 || count == 0) {
				continue;
			}

			float average = (float) (gains - losses) / count;
			nameStr.append(key.toString() + "\n");
			averageStr.append(FormatUtils.formatNum(Math.ceil(average)) + "\n");
			countStr.append(FormatUtils.formatNum(count) + "\n");
		}

		if(nameStr.length() == 0 || averageStr.length() == 0 || countStr.length() == 0) {
			builder.appendDescription("There is nothing here.");
		} else {
			builder.appendField("__Name__", nameStr.toString(), true);
			builder.appendField("__Average__", averageStr.toString(), true);
			builder.appendField("__Count__", countStr.toString(), true);
		}

		return builder.build();
	}

	private EmbedObject getVarious() {
		EmbedBuilder builder = Utils.getDefaultEmbed()
				.withAuthorName("Various stats");

		StringBuilder nameStr = new StringBuilder();
		StringBuilder countStr = new StringBuilder();

		Map<String, AtomicLong> statsMap = StatsManager.get(StatsEnum.VARIOUS);
		statsMap.keySet().stream().forEach(stat -> {
			nameStr.append(stat + "\n");
			countStr.append(FormatUtils.formatNum(statsMap.get(stat).get()) + "\n");
		});

		builder.appendField("__Name__", nameStr.toString(), true);
		builder.appendField("__Count__", countStr.toString(), true);

		return builder.build();
	}

	private EmbedObject getCommandsStats(StatsEnum category) {
		Map<String, AtomicLong> statsMap = StatsManager.get(category);
		Map<String, Long> homogenizedStatsMap = new HashMap<>();

		for(Object key : statsMap.keySet()) {
			AbstractCommand cmd = CommandManager.getCommand(key.toString());
			if(cmd == null) {
				continue;
			}
			homogenizedStatsMap.put(cmd.getFirstName(),
					homogenizedStatsMap.getOrDefault(cmd.getFirstName(), 0L) + statsMap.get(key.toString()).get());
		}

		List<String> orderedStatsList = Utils.sortByValue(homogenizedStatsMap).keySet().stream()
				.map(key -> "**" + key + "**: " + FormatUtils.formatNum(homogenizedStatsMap.get(key)))
				.collect(Collectors.toList());

		EmbedBuilder builder = Utils.getDefaultEmbed()
				.setLenient(true)
				.withAuthorName(StringUtils.capitalize(category.toString()) + "'s Stats");

		Lists.partition(orderedStatsList, ROW_SIZE).stream()
				.forEach(sublist -> builder.appendField("Row", FormatUtils.formatList(sublist, str -> str, "\n"), true));

		return builder.build();
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show stats for the specified category or average amount of coins gained with minigames.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " <category>`"
						+ "\n`" + context.getPrefix() + this.getFirstName() + " average`", false)
				.appendField("Argument", "**category** - "
						+ FormatUtils.formatList(CATEGORIES, cat -> "`" + cat.toString() + "`", ", "), false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}
