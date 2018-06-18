package me.shadorc.shadbot.utils.embed;

import java.util.Comparator;
import java.util.Map;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;

public class EmbedUtils {

	public static EmbedCreateSpec getDefaultEmbed() {
		return new EmbedCreateSpec()
				.setColor(DiscordUtils.convertColor(Config.BOT_COLOR));
	}

	public static <K, V extends Number> EmbedCreateSpec getStatsEmbed(Map<K, V> statsMap, String name) {
		EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed();
		embed.setAuthor(String.format("Stats: %s", name.toLowerCase()), null, null);
		if(statsMap == null) {
			return embed.setDescription("No statistics yet.");
		}

		Comparator<? super Map.Entry<K, V>> comparator = (v1, v2) -> Long.compare(v1.getValue().longValue(), v2.getValue().longValue());
		Map<K, V> sortedMap = Utils.sortByValue(statsMap, comparator.reversed());

		return embed.addField("Name", FormatUtils.format(sortedMap.keySet(), key -> key.toString().toLowerCase(), "\n"), true)
				.addField("Value", FormatUtils.format(sortedMap.values(), value -> FormatUtils.formatNum(Long.parseLong(value.toString())), "\n"), true);
	}

}
