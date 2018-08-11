package me.shadorc.shadbot.utils.embed;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;

public class EmbedUtils {

	public static EmbedCreateSpec getDefaultEmbed() {
		return new EmbedCreateSpec()
				.setColor(Config.BOT_COLOR);
	}

}
