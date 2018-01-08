package me.shadorc.shadbot.command.owner;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
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

		if(!StatsManager.getKeys().contains(context.getArg())) {
			throw new IllegalCmdArgumentException(String.format("Invalid category. Options: %s",
					FormatUtils.format(StatsManager.getKeys(), Object::toString, ", ")));
		}

		ConcurrentHashMap<String, AtomicInteger> concurrentMap = StatsManager.get(context.getArg());
		if(concurrentMap == null || concurrentMap.isEmpty()) {
			BotUtils.sendMessage(Emoji.INFO + " This category is empty.", context.getChannel());
			return;
		}

		Map<String, Integer> unsortedMap = new HashMap<>();
		concurrentMap.keySet().stream().forEach(key -> unsortedMap.put(key, concurrentMap.get(key).get()));

		Map<String, Integer> sortedMap = Utils.sortByValue(unsortedMap);

		EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
				.withAuthorName(String.format("Stats (%s)", context.getArg()))
				.appendField("Name", FormatUtils.format(sortedMap.keySet().stream(), Object::toString, "\n"), true)
				.appendField("Value", FormatUtils.format(sortedMap.values().stream(), Object::toString, "\n"), true);

		BotUtils.sendMessage(embed.build(), context.getChannel());
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show stats for the specified category or average amount of coins gained with minigames.")
				.addArg("category", FormatUtils.format(StatsManager.getKeys(), Object::toString, ", "), false)
				.build();
	}
}
