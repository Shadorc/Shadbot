package me.shadorc.shadbot.utils;

import java.util.Arrays;
import java.util.List;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.data.db.DBGuild;
import me.shadorc.shadbot.data.db.Database;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.RequestBuffer;

public class BotUtils {

	public static void sendMessage(String message, IChannel channel) {
		RequestBuffer.request(() -> {
			try {
				channel.sendMessage(message);
			} catch (DiscordException err) {
				LogUtils.errorf(err, "An error occurred while sending message.");
			}
		});
	}

	public static void sendMessage(EmbedObject embed, IChannel channel) {
		RequestBuffer.request(() -> {
			try {
				channel.sendMessage(embed);
			} catch (DiscordException err) {
				LogUtils.errorf(err, "An error occurred while sending message.");
			}
		});
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
		return Arrays.stream(permissions).anyMatch(perm -> !channel.getModifiedPermissions(channel.getClient().getOurUser()).contains(perm));
	}

}
