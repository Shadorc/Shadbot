package me.shadorc.discordbot.utils;

import org.json.JSONArray;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Log;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.Setting;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

public class BotUtils {

	public static void sendMessage(String message, IChannel channel) {
		try {
			if(!message.isEmpty() && message.length() <= IMessage.MAX_MESSAGE_LENGTH) {
				RequestBuffer.request(() -> {
					channel.sendMessage(message);
				});
			}

			if(message.length() > IMessage.MAX_MESSAGE_LENGTH) {
				BotUtils.sendMessage("I've tried to send a huge message... This is weird, I'm going to look into that. Sorry for the inconvenience.", channel);
			}

			if(message.length() > 1000) {
				Log.warn("Shadbot sent a huge message (length:" + message.length() + "):\n" + message);
			}
		} catch (NullPointerException e) {
			Log.error("NullPointerException while sending message... Investigating...", e);
			Log.error(Thread.getAllStackTraces().toString());
			Thread.dumpStack();
		} catch (MissingPermissionsException e) {
			Log.error("Missing permissions for guild \"" + channel.getGuild() + "\" (ID: " + channel.getGuild().getStringID() + ")", e);
		} catch (DiscordException e) {
			Log.error("Discord exception while sending message : " + e.getErrorMessage(), e);
		}
	}

	// EmbedBuilder doc : https://discord4j.readthedocs.io/en/latest/Making-embedded-content-using-EmbedBuilder/
	public static void sendEmbed(EmbedObject embed, IChannel channel) {
		if(!Shadbot.hasPermission(channel.getGuild(), Permissions.EMBED_LINKS)) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " I'm not allowed to send Embed links in this channel :(", channel);
			Log.warn("Shadbot wasn't allowed to post Embed links in Guild : \"" + channel.getGuild() + "\"");
			return;
		}

		try {
			RequestBuffer.request(() -> {
				channel.sendMessage(embed);
			});
		} catch (MissingPermissionsException e) {
			Log.error("Missing permissions for guild \"" + channel.getGuild() + "\" (ID: " + channel.getGuild().getStringID() + ")", e);
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
		JSONArray channelsArray = (JSONArray) Storage.getSetting(guild, Setting.ALLOWED_CHANNELS);

		// If no permissions were defined, authorize all the channels by default.
		if(channelsArray == null) {
			return true;
		}

		return JsonUtils.convertArrayToList(channelsArray).contains(channel.getStringID());
	}
}
