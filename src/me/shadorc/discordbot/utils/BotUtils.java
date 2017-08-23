package me.shadorc.discordbot.utils;

import org.json.JSONArray;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.Setting;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

public class BotUtils {

	public static void sendMessage(String message, IChannel channel) {
		if(!(channel.getShard().isReady() && channel.getShard().isLoggedIn())) {
			LogUtils.info("Shard isn't ready/connected, aborting attempt to send message.");
			return;
		}

		if(!channel.isPrivate() && !BotUtils.hasPermission(channel, Permissions.SEND_MESSAGES)) {
			LogUtils.info("{Guild: " + channel.getGuild().getName() + " (ID: " + channel.getGuild().getStringID() + ")} "
					+ "Shadbot wasn't allowed to send message.");
			return;
		}

		RequestBuffer.request(() -> {
			try {
				channel.sendMessage(message);
			} catch (MissingPermissionsException e) {
				LogUtils.error("{Guild: " + channel.getGuild().getName() + " (ID: " + channel.getGuild().getStringID() + ")} "
						+ "Missing permissions.", e);
			} catch (DiscordException e) {
				LogUtils.error("Discord exception while sending message : " + e.getErrorMessage(), e);
			}
		});
	}

	// EmbedBuilder doc : https://discord4j.readthedocs.io/en/latest/Making-embedded-content-using-EmbedBuilder/
	public static void sendEmbed(EmbedObject embed, IChannel channel) {
		if(!(channel.getShard().isReady() && channel.getShard().isLoggedIn())) {
			LogUtils.info("Shard isn't ready/connected, aborting attempt to send message.");
			return;
		}

		if(!channel.isPrivate() && !BotUtils.hasPermission(channel, Permissions.EMBED_LINKS)) {
			BotUtils.sendMessage(Emoji.ACCESS_DENIED + " I cannot send embed links due to the lack of permission."
					+ "\nPlease, check my permissions and channel-specific ones to verify that **Send Embed links** is checked.", channel);
			LogUtils.info("{Guild: " + channel.getGuild().getName() + " (ID: " + channel.getGuild().getStringID() + ")} "
					+ "Shadbot wasn't allowed to send embed link.");
			return;
		}

		RequestBuffer.request(() -> {
			try {
				channel.sendMessage(embed);
			} catch (MissingPermissionsException e) {
				LogUtils.error("{Guild: " + channel.getGuild().getName() + " (ID: " + channel.getGuild().getStringID() + ")} "
						+ "Missing permissions.", e);
			} catch (DiscordException e) {
				LogUtils.error("Discord exception while sending embed : " + e.getErrorMessage(), e);
			}
		});
	}

	/**
	 * @param guild - the channel's guild
	 * @param channel - the channel to check
	 * @return true if Shadbot is allowed to send a message in channel, false otherwise
	 */
	public static boolean isChannelAllowed(IGuild guild, IChannel channel) {
		JSONArray channelsArray = (JSONArray) Storage.getSetting(guild, Setting.ALLOWED_CHANNELS);

		// If no permissions were defined, authorize all the channels by default.
		if(channelsArray == null) {
			return true;
		}

		return Utils.convertArrayToList(channelsArray).contains(channel.getStringID());
	}

	/**
	 * @param channel - the channel to check
	 * @param permission - permission to check
	 * @param permissions - optional permissions
	 * @return true if Shadbot has all permissions in channel
	 */
	public static boolean hasPermission(IChannel channel, Permissions permission, Permissions... permissions) {
		for(Permissions perm : permissions) {
			if(!channel.getModifiedPermissions(Shadbot.getClient().getOurUser()).contains(perm)) {
				return false;
			}
		}
		return channel.getModifiedPermissions(Shadbot.getClient().getOurUser()).contains(permission);
	}

	/**
	 * @param voiceChannel - the voice channel to check
	 * @param permission - permission to check
	 * @param permissions - optional permissions
	 * @return true if Shadbot has all permissions in channel
	 */
	public static boolean hasPermission(IVoiceChannel voiceChannel, Permissions permission, Permissions... permissions) {
		for(Permissions perm : permissions) {
			if(!voiceChannel.getModifiedPermissions(Shadbot.getClient().getOurUser()).contains(perm)) {
				return false;
			}
		}
		return voiceChannel.getModifiedPermissions(Shadbot.getClient().getOurUser()).contains(permission);
	}
}
