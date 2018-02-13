package me.shadorc.shadbot.command.owner;

import java.util.HashMap;
import java.util.Map;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.data.stats.Stats.CommandEnum;
import me.shadorc.shadbot.data.stats.Stats.MoneyEnum;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.Pair;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

@Command(category = CommandCategory.OWNER, permission = CommandPermission.OWNER, names = { "stats" })
public class StatsCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		if(context.getArg().equals("average")) {
			EmbedObject embedAverage = this.getAverage();
			if(embedAverage == null) {
				BotUtils.sendMessage(Emoji.INFO + " Game stats are empty.", context.getChannel());
				return;
			}
			BotUtils.sendMessage(embedAverage, context.getChannel());
			return;
		}

		if(!StatsManager.getKeys().contains(context.getArg())) {
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid category. Options: %s",
					context.getArg(), FormatUtils.format(StatsManager.getKeys(), value -> String.format("`%s`", value), ", ")));
		}

		Map<String, Long> map = StatsManager.get(context.getArg());
		if(map == null || map.isEmpty()) {
			BotUtils.sendMessage(Emoji.INFO + " This category is empty.", context.getChannel());
			return;
		}

		Map<String, Long> sortedMap = Utils.sortByValue(map);

		EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
				.withAuthorName(String.format("Stats (%s)", context.getArg()))
				.appendField("Name", FormatUtils.format(sortedMap.keySet().stream(), Object::toString, "\n"), true)
				.appendField("Value", FormatUtils.format(sortedMap.values().stream(), key -> FormatUtils.formatNum(key), "\n"), true);

		sortedMap.clear();

		BotUtils.sendMessage(embed.build(), context.getChannel());
	}

	private EmbedObject getAverage() {
		Map<String, Long> moneyGained = StatsManager.get(MoneyEnum.MONEY_GAINED.toString());
		Map<String, Long> commandsUsed = StatsManager.get(CommandEnum.COMMAND_USED.toString());

		if(moneyGained == null || moneyGained.isEmpty() || commandsUsed == null || commandsUsed.isEmpty()) {
			return null;
		}

		Map<String, Long> moneyLost = StatsManager.get(MoneyEnum.MONEY_LOST.toString());

		Map<String, Pair<Float, Long>> averageMap = new HashMap<>();
		for(String gameName : moneyGained.keySet()) {
			float average = ((float) moneyGained.get(gameName) - moneyLost.getOrDefault(gameName, 0L)) / commandsUsed.get(gameName);
			averageMap.put(gameName, new Pair<Float, Long>(average, commandsUsed.get(gameName)));
		}

		EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
				.withAuthorName("Average")
				.appendField("Name", FormatUtils.format(averageMap.keySet().stream(), Object::toString, "\n"), true)
				.appendField("Average", FormatUtils.format(averageMap.values().stream().map(Pair::getFirst), num -> FormatUtils.formatNum(num.intValue()), "\n"), true)
				.appendField("Count", FormatUtils.format(averageMap.values().stream().map(Pair::getSecond), num -> FormatUtils.formatNum(num), "\n"), true);

		return embed.build();
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show stats for the specified category or average amount of coins gained with mini-games.")
				.addArg("category", FormatUtils.format(StatsManager.getKeys(), Object::toString, ", "), false)
				.build();
	}
}
