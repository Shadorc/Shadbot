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
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import sx.blah.discord.util.EmbedBuilder;

public class StatsCmd extends AbstractCommand {

	private static final int ROW_SIZE = 25;

	public StatsCmd() {
		super(CommandCategory.OWNER, Role.OWNER, "stats");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		if(context.getArg().equals("average")) {
			JSONObject moneyGainsCommand = StatsManager.getCategory(StatCategory.MONEY_GAINS_COMMAND);
			JSONObject moneyLossesCommand = StatsManager.getCategory(StatCategory.MONEY_LOSSES_COMMAND);
			JSONObject command = StatsManager.getCategory(StatCategory.COMMAND);

			StringBuilder strBuilder = new StringBuilder("```prolog\nAverage:");
			for(Object key : moneyGainsCommand.keySet()) {
				int gain = moneyGainsCommand.optInt(key.toString());
				int loss = moneyLossesCommand.optInt(key.toString());
				int count = command.optInt(key.toString());

				if(gain == 0 || count == 0) {
					continue;
				}

				for(String name : CommandManager.getCommand(key.toString()).getNames()) {
					count += command.optInt(name);
				}
				strBuilder.append("\n" + key.toString() + ": " + (float) (gain - loss) / count);
			}
			strBuilder.append("```");

			BotUtils.sendMessage(strBuilder.toString(), context.getChannel());
			return;
		}

		if(!Arrays.stream(StatCategory.values()).anyMatch(category -> category.toString().equals(context.getArg()))) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Category unknown. (Options: "
					+ StringUtils.formatArray(StatCategory.values(), cat -> "**" + cat.toString() + "**", ", ") + ")", context.getChannel());
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
			statsMap.put(key.toString(), statsObj.getInt(key.toString()));
		}

		List<String> statsList = Utils.sortByValue(statsMap).keySet().stream()
				.map(key -> "**" + key + "**: " + statsMap.get(key))
				.collect(Collectors.toList());

		EmbedBuilder builder = Utils.getDefaultEmbed()
				.setLenient(true)
				.withAuthorName(StringUtils.capitalize(category.toString()) + "'s Stats");

		if(statsList.isEmpty()) {
			builder.appendDescription("There is nothing here.");
		}

		for(int i = 0; i < Math.ceil((float) statsMap.keySet().size() / ROW_SIZE); i++) {
			int index = i * ROW_SIZE;
			builder.appendField("Row N°" + (i + 1),
					StringUtils.formatList(
							statsList.subList(index, index + Math.min(ROW_SIZE, statsList.size() - index)), str -> str, "\n"), true);
		}

		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show stats for the specified category or average amount of coins gained with minigames.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " <category>`"
						+ "\n`" + context.getPrefix() + this.getFirstName() + " average`", false)
				.appendField("Argument", "**category** - " + StringUtils.formatArray(StatCategory.values(), cat -> cat.toString(), ", "), false);
		BotUtils.sendMessage(builder.build(), context.getChannel());

	}
}
