package me.shadorc.discordbot.command.owner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

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

		if(!Arrays.stream(StatsEnum.values()).anyMatch(stats -> stats.toString().equals(context.getArg()))) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Category unknown. (Options: "
					+ FormatUtils.formatArray(StatsEnum.values(), cat -> "**" + cat.toString() + "**", ", ") + ")", context.getChannel());
			return;
		}

		if(context.getArg().equals(StatsEnum.VARIOUS.toString())) {
			EmbedBuilder builder = Utils.getDefaultEmbed()
					.setLenient(true)
					.withAuthorName(StringUtils.capitalize(StatsEnum.VARIOUS.toString()) + "'s Stats");

			Map<String, AtomicLong> statsMap = StatsManager.get(StatsEnum.VARIOUS);
			if(statsMap.isEmpty()) {
				builder.appendDescription("There is nothing here.");
			}

			statsMap.keySet().stream().forEach(key -> builder.appendDescription("**" + key + "**: " + statsMap.get(key)));
			BotUtils.sendMessage(builder.build(), context.getChannel());
			return;
		}

		StatsEnum category = StatsEnum.valueOf(context.getArg().toUpperCase());

		Map<String, AtomicLong> statsMap = StatsManager.get(category);
		if(statsMap.isEmpty()) {
			BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " This category is empty.", context.getChannel());
			return;
		}

		Map<String, Long> homogenizedStatsMap = new HashMap<>();

		for(Object key : statsMap.keySet()) {
			String firstName = CommandManager.getCommand(key.toString()).getFirstName();
			homogenizedStatsMap.put(firstName,
					homogenizedStatsMap.getOrDefault(firstName, 0L) + statsMap.get(key.toString()).get());
		}

		EmbedBuilder builder = Utils.getDefaultEmbed()
				.setLenient(true)
				.withAuthorName(StringUtils.capitalize(category.toString()) + "'s Stats");

		if(statsMap.isEmpty()) {
			builder.appendDescription("There is nothing here.");
		}

		List<String> statsList = Utils.sortByValue(homogenizedStatsMap).keySet().stream()
				.map(key -> "**" + key + "**: " + homogenizedStatsMap.get(key))
				.collect(Collectors.toList());

		for(int i = 0; i < Math.ceil((float) homogenizedStatsMap.keySet().size() / ROW_SIZE); i++) {
			int minIndex = i * ROW_SIZE;
			int size = Math.min(ROW_SIZE, statsList.size() - minIndex);
			builder.appendField("Row N°" + (i + 1),
					FormatUtils.formatList(statsList.subList(minIndex, minIndex + size), str -> str, "\n"), true);
		}

		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

	private EmbedObject getAverage() {
		Map<String, AtomicLong> moneyGainsCommandObj = StatsManager.get(StatsEnum.MONEY_GAINED);
		Map<String, AtomicLong> moneyLossesCommandObj = StatsManager.get(StatsEnum.MONEY_LOST);
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

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show stats for the specified category or average amount of coins gained with minigames.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " <category>`"
						+ "\n`" + context.getPrefix() + this.getFirstName() + " average`", false)
				.appendField("Argument", "**category** - "
						+ FormatUtils.formatArray(StatsEnum.values(), cat -> "`" + cat.toString() + "`", ", "), false);
		BotUtils.sendMessage(builder.build(), context.getChannel());

	}
}
