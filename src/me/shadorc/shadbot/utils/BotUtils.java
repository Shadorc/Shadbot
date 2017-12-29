package me.shadorc.shadbot.utils;

import me.shadorc.shadbot.utils.embed.LogUtils;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.RequestBuffer;

public class BotUtils {

	public static void sendMessage(String message, IChannel channel) {
		RequestBuffer.request(() -> {
			try {
				channel.sendMessage(message);
			} catch (DiscordException err) {
				LogUtils.error("An error occurred while sending message.", err);
			}
		});
	}

	public static void sendMessage(EmbedObject embed, IChannel channel) {
		RequestBuffer.request(() -> {
			try {
				channel.sendMessage(embed);
			} catch (DiscordException err) {
				LogUtils.error("An error occurred while sending message.", err);
			}
		});
	}

}
