package me.shadorc.shadbot.utils.embed;

import java.util.Comparator;
import java.util.Map;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;

public class EmbedUtils {

	public static EmbedCreateSpec getDefaultEmbed(String authorName, String authorUrl, String authorIconUrl) {
		return new EmbedCreateSpec()
				.setAuthor(authorName, authorUrl, authorIconUrl)
				.setColor(DiscordUtils.convertColor(Config.BOT_COLOR));
	}

	// TODO: Get Shadbot avatar url
	public static EmbedCreateSpec getDefaultEmbed(String authorName, String authorUrl) {
		return EmbedUtils.getDefaultEmbed(authorName, authorUrl, null);
	}

	public static EmbedCreateSpec getDefaultEmbed(String authorName) {
		return EmbedUtils.getDefaultEmbed(authorName, null);
	}

	public static EmbedCreateSpec getDefaultEmbed() {
		return EmbedUtils.getDefaultEmbed(null);
	}

	public static <K, V extends Number> EmbedCreateSpec getStatsEmbed(Map<K, V> statsMap, String name) {
		EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed(String.format("Stats: %s", name.toLowerCase()));
		if(statsMap == null) {
			return embed.setDescription("No statistics yet.");
		}

		Comparator<? super Map.Entry<K, V>> comparator = (v1, v2) -> Long.compare(v1.getValue().longValue(), v2.getValue().longValue());
		Map<K, V> sortedMap = Utils.sortByValue(statsMap, comparator.reversed());

		return embed.addField("Name", FormatUtils.format(sortedMap.keySet(), key -> key.toString().toLowerCase(), "\n"), true)
				.addField("Value", FormatUtils.format(sortedMap.values(), value -> FormatUtils.formatNum(Long.parseLong(value.toString())), "\n"), true);
	}

}
