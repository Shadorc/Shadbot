package me.shadorc.shadbot.utils.embed;

import java.util.Map;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.utils.FormatUtils;
import sx.blah.discord.util.EmbedBuilder;

public class EmbedUtils {

	public static EmbedBuilder getDefaultEmbed() {
		return new EmbedBuilder()
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR);
	}

	public static EmbedBuilder getStatsEmbed(Map<?, ?> statsMap, String name) {
		EmbedBuilder embed = EmbedUtils.getDefaultEmbed().withAuthorName(String.format("Stats: %s", name.toLowerCase()));
		if(statsMap == null) {
			return embed.withDescription("No statistics yet.");
		}
		return embed.appendField("Name", FormatUtils.format(statsMap.keySet().stream(), key -> key.toString().toLowerCase(), "\n"), true)
				.appendField("Value", FormatUtils.format(statsMap.values().stream(), value -> FormatUtils.formatNum(Long.parseLong(value.toString())), "\n"), true);
	}

}
