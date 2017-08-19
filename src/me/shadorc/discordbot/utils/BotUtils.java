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
			LogUtils.info("Shadbot has not established a connection with the Discord gateway on all shards yet, aborting attempt to send message.");
			return;
		}

		if(!channel.isPrivate() && !BotUtils.hasPermission(channel, Permissions.SEND_MESSAGES)) {
			LogUtils.warn("Shadbot wasn't allowed to send message in guild: "
					+ "\"" + channel.getGuild().getName() + "\" (ID: " + channel.getGuild().getStringID() + ")");
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
		if(!(channel.getShard().isReady() && channel.getShard().isLoggedIn())) {
			LogUtils.info("Shadbot has not established a connection with the Discord gateway on all shards yet, aborting attempt to send embed.");
			return;
		}

		if(!channel.isPrivate() && !BotUtils.hasPermission(channel, Permissions.EMBED_LINKS)) {
			BotUtils.sendMessage(Emoji.ACCESS_DENIED + " I cannot send embed links due to the lack of permission. :("
					+ " Please, check my permissions and channel-specific ones to verify that \"Send Embed links\" is checked.", channel);
			LogUtils.warn("Shadbot wasn't allowed to send embed link in guild: "
					+ "\"" + channel.getGuild().getName() + "\" (ID: " + channel.getGuild().getStringID() + ")");
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

		return JSONUtils.convertArrayToList(channelsArray).contains(channel.getStringID());
	}

	public static boolean hasPermission(IChannel channel, Permissions permission, Permissions... permissions) {
		if(channel == null) {
			LogUtils.warn("Shadbot tried to check permission on a non-existing channel.");
			return false;
		}

		for(Permissions perm : permissions) {
			if(!channel.getModifiedPermissions(Shadbot.getClient().getOurUser()).contains(perm)) {
				return false;
			}
		}
		return channel.getModifiedPermissions(Shadbot.getClient().getOurUser()).contains(permission);
	}

	public static boolean hasPermission(IVoiceChannel voiceChannel, Permissions permission, Permissions... permissions) {
		if(voiceChannel == null) {
			LogUtils.warn("Shadbot tried to check permission on a non-existing voice channel.");
			return false;
		}

		for(Permissions perm : permissions) {
			if(!voiceChannel.getModifiedPermissions(Shadbot.getClient().getOurUser()).contains(perm)) {
				return false;
			}
		}
		return voiceChannel.getModifiedPermissions(Shadbot.getClient().getOurUser()).contains(permission);
	}
}
