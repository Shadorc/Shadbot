package me.shadorc.discordbot.command.owner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.CommandManager;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.StatCategory;
import me.shadorc.discordbot.data.StatsManager;
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

		if(!Arrays.stream(StatCategory.values()).anyMatch(category -> category.toString().equals(context.getArg()))) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Category unknown. (Options: "
					+ FormatUtils.formatArray(StatCategory.values(), cat -> "**" + cat.toString() + "**", ", ") + ")", context.getChannel());
			return;
		}

		StatCategory category = StatCategory.valueOf(context.getArg().toUpperCase());

		JSONObject statsObj = StatsManager.getCategory(category);
		if(statsObj.length() == 0) {
			BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " This category is empty.", context.getChannel());
			return;
		}

		final Map<String, Integer> statsMap = new HashMap<>();
		for(Object key : statsObj.keySet()) {
			String firstName = CommandManager.getCommand(key.toString()).getFirstName();
			statsMap.put(firstName, statsMap.getOrDefault(firstName, 0) + statsObj.getInt(key.toString()));
		}

		EmbedBuilder builder = Utils.getDefaultEmbed()
				.setLenient(true)
				.withAuthorName(StringUtils.capitalize(category.toString()) + "'s Stats");

		if(statsMap.isEmpty()) {
			builder.appendDescription("There is nothing here.");
		}

		List<String> statsList = Utils.sortByValue(statsMap).keySet().stream()
				.map(key -> "**" + key + "**: " + statsMap.get(key))
				.collect(Collectors.toList());

		for(int i = 0; i < Math.ceil((float) statsMap.keySet().size() / ROW_SIZE); i++) {
			int minIndex = i * ROW_SIZE;
			int size = Math.min(ROW_SIZE, statsList.size() - minIndex);
			builder.appendField("Row N°" + (i + 1),
					FormatUtils.formatList(statsList.subList(minIndex, minIndex + size), str -> str, "\n"), true);
		}

		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

	private EmbedObject getAverage() {
		JSONObject moneyGainsCommandObj = StatsManager.getCategory(StatCategory.MONEY_GAINS_COMMAND);
		JSONObject moneyLossesCommandObj = StatsManager.getCategory(StatCategory.MONEY_LOSSES_COMMAND);
		JSONObject commandObj = StatsManager.getCategory(StatCategory.COMMAND);

		EmbedBuilder builder = Utils.getDefaultEmbed()
				.withAuthorName("Stats average");

		StringBuilder nameStr = new StringBuilder();
		StringBuilder averageStr = new StringBuilder();
		StringBuilder countStr = new StringBuilder();
		for(Object key : moneyGainsCommandObj.keySet()) {
			int gains = moneyGainsCommandObj.optInt(key.toString());
			int losses = moneyLossesCommandObj.optInt(key.toString());
			int count = CommandManager.getCommand(key.toString()).getNames().stream().mapToInt(name -> commandObj.optInt(name)).sum();

			if(gains == 0 || count == 0) {
				continue;
			}

			float average = (float) (gains - losses) / count;
			nameStr.append(key.toString() + "\n");
			averageStr.append(FormatUtils.formatNum(Math.ceil(average)) + "\n");
			countStr.append(FormatUtils.formatNum(count) + "\n");
		}

		builder.appendField("__Name__", nameStr.toString(), true);
		builder.appendField("__Average__", averageStr.toString(), true);
		builder.appendField("__Count__", countStr.toString(), true);

		return builder.build();
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show stats for the specified category or average amount of coins gained with minigames.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " <category>`"
						+ "\n`" + context.getPrefix() + this.getFirstName() + " average`", false)
				.appendField("Argument", "**category** - " + FormatUtils.formatArray(StatCategory.values(), cat -> cat.toString(), ", "), false);
		BotUtils.sendMessage(builder.build(), context.getChannel());

	}
}
