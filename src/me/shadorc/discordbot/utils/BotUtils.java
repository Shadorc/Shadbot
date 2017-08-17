package me.shadorc.discordbot.utils;

import org.json.JSONArray;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.Setting;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

public class BotUtils {

	public static void sendMessage(String message, IChannel channel) {
		if(!Shadbot.getClient().isReady() || !Shadbot.getClient().isLoggedIn()) {
			LogUtils.info("Shadbot has not established a connection with the Discord gateway on all shards yet, aborting attempt to send message.");
			return;
		}

		if(!channel.isPrivate() && !BotUtils.hasPermission(channel.getGuild(), Permissions.SEND_MESSAGES)) {
			LogUtils.warn("Shadbot wasn't allowed to send message in Guild : \"" + channel.getGuild() + "\"");
			return;
		}

		RequestBuffer.request(() -> {
			try {
				channel.sendMessage(message);
			} catch (MissingPermissionsException e) {
				LogUtils.error("Missing permissions for guild \"" + channel.getGuild() + "\" (ID: " + channel.getGuild().getStringID() + ")", e);
			} catch (DiscordException e) {
				LogUtils.error("Discord exception while sending message : " + e.getErrorMessage(), e);
			}
		});
	}

	// EmbedBuilder doc : https://discord4j.readthedocs.io/en/latest/Making-embedded-content-using-EmbedBuilder/
	public static void sendEmbed(EmbedObject embed, IChannel channel) {
		if(!Shadbot.getClient().isReady() || !Shadbot.getClient().isLoggedIn()) {
			LogUtils.info("Shadbot has not established a connection with the Discord gateway on all shards yet, aborting attempt to send embed.");
			return;
		}

		if(!channel.isPrivate() && !BotUtils.hasPermission(channel.getGuild(), Permissions.EMBED_LINKS)) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " I cannot send embed links due to the lack of permission. :(", channel);
			LogUtils.warn("Shadbot wasn't allowed to send Embed links in Guild : \"" + channel.getGuild() + "\"");
			return;
		}

		RequestBuffer.request(() -> {
			try {
				channel.sendMessage(embed);
			} catch (MissingPermissionsException e) {
				LogUtils.error("Missing permissions for guild \"" + channel.getGuild() + "\" (ID: " + channel.getGuild().getStringID() + ")", e);
			} catch (DiscordException e) {
				LogUtils.error("Discord exception while sending embed : " + e.getErrorMessage(), e);
			}
		});
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

	public static boolean hasPermission(IGuild guild, Permissions permission) {
		return Shadbot.getClient().getOurUser().getPermissionsForGuild(guild).contains(permission);
	}
}
