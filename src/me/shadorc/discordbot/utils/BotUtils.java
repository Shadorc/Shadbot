package me.shadorc.discordbot.utils;

import java.util.List;

import org.json.JSONArray;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.data.Storage;
import me.shadorc.discordbot.data.Storage.Setting;
import me.shadorc.discordbot.events.ShardListener;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.schedule.ScheduledMessage.Reason;
import me.shadorc.discordbot.utils.schedule.Scheduler;
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
			LogUtils.info("Shard isn't ready, adding message to queue.");
			Scheduler.scheduleMessages(message, channel, Reason.SHARD_NOT_READY);
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
				if(err.getErrorMessage().contains("Discord didn't return a response") || err.getErrorMessage().contains("400 Bad Request")) {
					LogUtils.info("A message could not be send now, adding it to queue.");
					Scheduler.scheduleMessages(message, channel, Reason.API_ERROR);
					return null;
				}
				LogUtils.error("Discord exception while sending message.", err);
			}
			return null;
		});
	}

	// EmbedBuilder doc: https://discord4j.readthedocs.io/en/latest/Making-embedded-content-using-EmbedBuilder/
	public static RequestFuture<IMessage> sendMessage(EmbedObject embed, IChannel channel) {
		if(!ShardListener.isShardConnected(channel.getShard())) {
			LogUtils.info("Shard isn't ready, adding embed link to queue.");
			Scheduler.scheduleMessages(embed, channel, Reason.SHARD_NOT_READY);
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
				if(err.getErrorMessage().contains("Discord didn't return a response") || err.getErrorMessage().contains("400 Bad Request")) {
					LogUtils.info("An embed link could not be send now, adding it to queue.");
					Scheduler.scheduleMessages(embed, channel, Reason.API_ERROR);
					return null;
				}
				LogUtils.error("Discord exception while sending embed link.", err);
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
		if(messages.isEmpty()) {
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
		JSONArray channelsArray = (JSONArray) Storage.getSetting(guild, Setting.ALLOWED_CHANNELS);

		// If no permissions were defined, authorize all the channels by default.
		if(channelsArray.length() == 0) {
			return true;
		}

		return Utils.convertToLongList(channelsArray).contains(channel.getLongID());
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
