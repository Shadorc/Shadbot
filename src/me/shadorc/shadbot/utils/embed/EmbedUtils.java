package me.shadorc.shadbot.utils.embed;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import sx.blah.discord.util.EmbedBuilder;

public class EmbedUtils {

	public static EmbedBuilder getDefaultEmbed() {
		return new EmbedBuilder()
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR);
	}

	public static <K, V extends Number> EmbedBuilder getStatsEmbed(Map<K, V> statsMap, String name) {
		EmbedBuilder embed = EmbedUtils.getDefaultEmbed().withAuthorName(String.format("Stats: %s", name.toLowerCase()));
		if(statsMap == null) {
			return embed.withDescription("No statistics yet.");
		}

		Comparator<? super Map.Entry<K, V>> comparator =
				(v1, v2) -> new BigDecimal(v1.getValue().toString()).compareTo(new BigDecimal(v2.getValue().toString()));
		Map<K, V> sortedMap = Utils.sortByValue(statsMap, comparator.reversed());

		return embed.appendField("Name", FormatUtils.format(sortedMap.keySet().stream(), key -> key.toString().toLowerCase(), "\n"), true)
				.appendField("Value", FormatUtils.format(sortedMap.values().stream(), value -> FormatUtils.formatNum(Long.parseLong(value.toString())), "\n"), true);
	}

}
