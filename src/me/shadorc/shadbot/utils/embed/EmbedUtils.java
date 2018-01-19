package me.shadorc.shadbot.utils.embed;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import sx.blah.discord.util.EmbedBuilder;

public class EmbedUtils {

	public static EmbedBuilder getDefaultEmbed() {
		return new EmbedBuilder()
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR);
	}

}
