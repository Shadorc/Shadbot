package me.shadorc.shadbot.command.owner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

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
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

@Command(category = CommandCategory.OWNER, permission = CommandPermission.OWNER, names = { "stats" })
public class StatsCmd extends AbstractCommand {

	private static final int ROW_SIZE = 25;

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

		Map<String, Integer> map = StatsManager.get(context.getArg());
		if(map == null || map.isEmpty()) {
			BotUtils.sendMessage(Emoji.INFO + " This category is empty.", context.getChannel());
			return;
		}

		Map<String, Integer> sortedMap = Utils.sortByValue(map);
		List<List<String>> lists = Lists.partition(new ArrayList<>(sortedMap.keySet()), ROW_SIZE);

		EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
				.withAuthorName(String.format("Stats (%s)", context.getArg()));
		for(List<String> list : lists) {
			embed.appendField("Name", FormatUtils.format(list, Object::toString, "\n"), true);
			embed.appendField("Value", FormatUtils.format(list, key -> FormatUtils.formatNum(sortedMap.get(key)), "\n"), true);
		}

		BotUtils.sendMessage(embed.build(), context.getChannel());
	}

	private EmbedObject getAverage() {
		Map<String, Integer> moneyGained = StatsManager.get(MoneyEnum.MONEY_GAINED.toString());
		Map<String, Integer> gameUsed = StatsManager.get(CommandEnum.COMMAND_USED.toString());

		if(moneyGained == null || moneyGained.isEmpty() || gameUsed == null || gameUsed.isEmpty()) {
			return null;
		}

		Map<String, Integer> moneyLost = StatsManager.get(MoneyEnum.MONEY_LOST.toString());

		Map<String, String> averagesMap = new HashMap<>();
		for(String key : moneyGained.keySet()) {
			float average = ((float) moneyGained.get(key) - moneyLost.getOrDefault(key, 0)) / gameUsed.getOrDefault(key, 1);
			averagesMap.put(key, String.format("%.1f", average));
		}

		EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
				.withAuthorName("Average")
				.appendField("Name", FormatUtils.format(averagesMap.keySet().stream(), Object::toString, "\n"), true)
				.appendField("Average", FormatUtils.format(averagesMap.values().stream(), Object::toString, "\n"), true)
				.appendField("Count", FormatUtils.format(gameUsed.values().stream(), Object::toString, "\n"), true);

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
