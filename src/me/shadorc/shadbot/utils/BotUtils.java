package me.shadorc.shadbot.utils;

import java.util.List;
import java.util.stream.Collectors;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.data.db.DBGuild;
import me.shadorc.shadbot.data.db.Database;
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
		if(!BotUtils.hasPermissions(channel, Permissions.SEND_MESSAGES)) {
			LogUtils.infof("{Guild ID: %d} Shadbot wasn't allowed to send a message.", channel.getGuild().getLongID());
			return null;
		}

		return RequestBuffer.request(() -> {
			try {
				return channel.sendMessage(message);
			} catch (MissingPermissionsException err) {
				LogUtils.infof("{Guild ID: %d} %s", channel.getGuild().getLongID(), err.getMessage());
			} catch (DiscordException err) {
				LogUtils.errorf(err, "An error occurred while sending message.");
			}
			return null;
		});
	}

	public static RequestFuture<IMessage> sendMessage(EmbedObject embed, IChannel channel) {
		if(!BotUtils.hasPermissions(channel, Permissions.SEND_MESSAGES, Permissions.EMBED_LINKS)) {
			BotUtils.sendMessage(TextUtils.missingPerm(Permissions.EMBED_LINKS), channel);
			LogUtils.infof("{Guild ID: %d} Shadbot wasn't allowed to send embed link.", channel.getGuild().getLongID());
			return null;
		}

		return RequestBuffer.request(() -> {
			try {
				return channel.sendMessage(embed);
			} catch (MissingPermissionsException err) {
				LogUtils.infof("{Guild ID: %d} %s", channel.getGuild().getLongID(), err.getMessage());
			} catch (DiscordException err) {
				LogUtils.errorf(err, "An error occurred while sending message.");
			}
			return null;
		});
	}

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

	public static boolean isChannelAllowed(IGuild guild, IChannel channel) {
		DBGuild dbGuild = Database.getDBGuild(guild);
		List<Long> allowedChannels = dbGuild.getAllowedChannels();

		// If no permission has been set, allow all channels
		if(allowedChannels.isEmpty()) {
			return true;
		}

		return allowedChannels.contains(channel.getLongID());
	}

	public static boolean isCommandAllowed(IGuild guild, AbstractCommand cmd) {
		DBGuild dbGuild = Database.getDBGuild(guild);
		List<String> blacklistedCmd = dbGuild.getBlacklistedCmd();

		if(blacklistedCmd.isEmpty()) {
			return true;
		}

		return cmd.getNames().stream().anyMatch(cmdName -> blacklistedCmd.stream().anyMatch(cmdName::equalsIgnoreCase));
	}

	public static boolean hasPermissions(IChannel channel, Permissions... permissions) {
		for(Permissions perm : permissions) {
			if(!channel.getModifiedPermissions(channel.getClient().getOurUser()).contains(perm)) {
				return false;
			}
		}
		return true;
	}

}
