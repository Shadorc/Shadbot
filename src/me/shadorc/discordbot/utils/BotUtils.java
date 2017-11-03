package me.shadorc.discordbot.utils;

import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.data.Setting;
import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.events.ShardListener;
import me.shadorc.discordbot.utils.command.Emoji;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;
import sx.blah.discord.util.RequestBuffer.RequestFuture;

public class BotUtils {

	public static RequestFuture<IMessage> sendMessage(String message, IChannel channel) {
		if(!ShardListener.isShardConnected(channel.getShard())) {
			ExceptionUtils.manageMessageException(message, channel, new DiscordException("Attempt to send message before shard is ready!"));
			return null;
		}

		if(!channel.isPrivate() && !BotUtils.hasPermission(channel, Permissions.SEND_MESSAGES)) {
			LogUtils.info("{Guild ID: " + channel.getGuild().getLongID() + "} Shadbot wasn't allowed to send message.");
			return null;
		}

		return RequestBuffer.request(() -> {
			try {
				return channel.sendMessage(message);
			} catch (MissingPermissionsException err) {
				LogUtils.error("{Guild ID: " + channel.getGuild().getLongID() + "} Missing permissions.", err);
			} catch (DiscordException err) {
				ExceptionUtils.manageMessageException(message, channel, err);
			}
			return null;
		});
	}

	// EmbedBuilder doc: https://discord4j.readthedocs.io/en/latest/Making-embedded-content-using-EmbedBuilder/
	public static RequestFuture<IMessage> sendMessage(EmbedObject embed, IChannel channel) {
		if(!ShardListener.isShardConnected(channel.getShard())) {
			ExceptionUtils.manageMessageException(embed, channel, new DiscordException("Attempt to send message before shard is ready!"));
			return null;
		}

		if(!channel.isPrivate() && !BotUtils.hasPermission(channel, Permissions.SEND_MESSAGES, Permissions.EMBED_LINKS)) {
			BotUtils.sendMessage(Emoji.ACCESS_DENIED + " I cannot send embed links due to the lack of permission."
					+ "\nPlease, check my permissions and channel-specific ones to verify that **Send Embed links** is checked.", channel);
			LogUtils.info("{Guild ID: " + channel.getGuild().getLongID() + "} Shadbot wasn't allowed to send embed link.");
			return null;
		}

		return RequestBuffer.request(() -> {
			try {
				return channel.sendMessage(embed);
			} catch (MissingPermissionsException err) {
				LogUtils.error("{Guild ID: " + channel.getGuild().getLongID() + "} Missing permissions.", err);
			} catch (DiscordException err) {
				ExceptionUtils.manageMessageException(embed, channel, err);
			}
			return null;
		});
	}

	/**
	 * @param channel - the messages' channel
	 * @param messages - the List of messages to delete
	 * @return the number of deleted messages
	 */
	public static int deleteMessages(IChannel channel, List<IMessage> messages) {
		// Only keeps messages that are at most 2 weeks old
		List<IMessage> toDelete = messages.stream()
				.filter(msg -> msg.getLongID() >= (((System.currentTimeMillis() - 14 * 24 * 60 * 60 * 1000) - 1420070400000L) << 22))
				.distinct()
				.collect(Collectors.toList());

		if(toDelete.isEmpty()) {
			return 0;
		}

		return RequestBuffer.request(() -> {
			return channel.bulkDelete(messages).size();
		}).get();
	}

	/**
	 * @param guild - the channel's guild
	 * @param channel - the channel to check
	 * @return true if Shadbot is allowed to send a message in channel, false otherwise
	 */
	public static boolean isChannelAllowed(IGuild guild, IChannel channel) {
		JSONArray channelsArray = (JSONArray) DatabaseManager.getSetting(guild, Setting.ALLOWED_CHANNELS);

		// If no permissions were defined, authorize all the channels by default.
		if(channelsArray.length() == 0) {
			return true;
		}

		return Utils.convertToLongList(channelsArray).contains(channel.getLongID());
	}

	/**
	 * @param guild - the guild
	 * @param cmd - the command to check
	 * @return true if Shadbot is allowed to use this command, false otherwise
	 */
	public static boolean isCommandAllowed(IGuild guild, AbstractCommand cmd) {
		JSONArray blacklistArray = (JSONArray) DatabaseManager.getSetting(guild, Setting.BLACKLIST);

		if(blacklistArray.length() == 0) {
			return true;
		}

		for(String blacklistName : Utils.convertToStringList(blacklistArray)) {
			for(String cmdName : cmd.getNames()) {
				if(blacklistName.equalsIgnoreCase(cmdName)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * @param channel - the channel to check
	 * @param permission - the permission to check
	 * @param permissions - the optional permissions
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
}
