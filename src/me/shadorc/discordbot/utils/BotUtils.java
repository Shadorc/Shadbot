package me.shadorc.discordbot.utils;

import org.json.JSONArray;

import me.shadorc.discordbot.Log;
import me.shadorc.discordbot.Storage;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

public class BotUtils {

	public static void sendMessage(String message, IChannel channel) {
		try {
			if(!message.isEmpty()) {
				RequestBuffer.request(() -> {
					channel.sendMessage(message);
				});
			}
		} catch (MissingPermissionsException e) {
			Log.warn("Missing permissions for channel \"" + channel.getName() + "\" (ID: " + channel.getStringID() + ")");
		} catch (DiscordException e) {
			Log.error("Discord exception while sending message : " + e.getErrorMessage(), e);
		}
	}

	// EmbedBuilder doc : https://discord4j.readthedocs.io/en/latest/Making-embedded-content-using-EmbedBuilder/
	public static void sendEmbed(EmbedObject embed, IChannel channel) {
		try {
			RequestBuffer.request(() -> {
				channel.sendMessage(embed);
			});
		} catch (MissingPermissionsException e) {
			Log.warn("Missing permissions for channel \"" + channel.getName() + "\" (ID: " + channel.getStringID() + ")");
		} catch (DiscordException e) {
			Log.error("Discord exception while sending embed : " + e.getErrorMessage(), e);
		}
	}

	/**
	 * @param guild - the guild of the channel
	 * @param channel - the channel to check
	 * @return true if Shadbot is allowed to send a message in the channel, false otherwise
	 */
	public static boolean isChannelAllowed(IGuild guild, IChannel channel) {
		JSONArray channelsArray = Storage.getAllowedChannels(guild);

		// If no permissions were defined, authorize by default all the channels
		if(channelsArray == null) {
			return true;
		}

		return Utils.convertArrayToList(channelsArray).contains(channel.getStringID());
	}
}
