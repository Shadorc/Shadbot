package me.shadorc.discordbot.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.data.Storage;
import me.shadorc.discordbot.data.Storage.Setting;
import me.shadorc.discordbot.events.ShardListener;
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

	private static final ConcurrentHashMap<IChannel, List<String>> MESSAGE_QUEUE = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<IChannel, List<EmbedObject>> EMBED_QUEUE = new ConcurrentHashMap<>();

	public static void sendMessage(String message, IChannel channel) {
		if(!ShardListener.isShardConnected(channel.getShard())) {
			List<String> messageList = MESSAGE_QUEUE.getOrDefault(channel, new ArrayList<>());
			messageList.add(message);
			MESSAGE_QUEUE.put(channel, messageList);
			LogUtils.info("Shard isn't ready, message added to queue.");
			return;
		}

		if(!channel.isPrivate() && !BotUtils.hasPermission(channel, Permissions.SEND_MESSAGES)) {
			LogUtils.info("{Guild ID: " + channel.getGuild().getLongID() + "} Shadbot wasn't allowed to send message.");
			return;
		}

		RequestBuffer.request(() -> {
			try {
				channel.sendMessage(message);
			} catch (MissingPermissionsException err) {
				LogUtils.error("{Guild ID: " + channel.getGuild().getLongID() + "} Missing permissions.", err);
			} catch (DiscordException err) {
				LogUtils.error("Discord exception while sending message.", err);
			}
		});
	}

	// EmbedBuilder doc: https://discord4j.readthedocs.io/en/latest/Making-embedded-content-using-EmbedBuilder/
	public static RequestFuture<IMessage> sendEmbed(EmbedObject embed, IChannel channel) {
		if(!ShardListener.isShardConnected(channel.getShard())) {
			List<EmbedObject> embedList = EMBED_QUEUE.getOrDefault(channel, new ArrayList<>());
			embedList.add(embed);
			EMBED_QUEUE.put(channel, embedList);
			LogUtils.info("Shard isn't ready, embed link added to queue.");
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
				LogUtils.error("Discord exception while sending embed link.", err);
			}
			return null;
		});
	}

	public static void sendQueues() {
		if(!MESSAGE_QUEUE.isEmpty()) {
			LogUtils.info("Sending pending messages...");
			for(IChannel channel : MESSAGE_QUEUE.keySet()) {
				for(String message : MESSAGE_QUEUE.get(channel)) {
					BotUtils.sendMessage(message, channel);
				}
			}
			LogUtils.info("Pending messages sent.");
			MESSAGE_QUEUE.clear();
		}

		if(!EMBED_QUEUE.isEmpty()) {
			LogUtils.info("Sending pending embed...");
			for(IChannel channel : EMBED_QUEUE.keySet()) {
				for(EmbedObject embed : EMBED_QUEUE.get(channel)) {
					BotUtils.sendEmbed(embed, channel);
				}
			}
			LogUtils.info("Pending embed sent.");
			EMBED_QUEUE.clear();
		}
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
