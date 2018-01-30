package me.shadorc.shadbot.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.data.db.DBGuild;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.data.stats.Stats.VariousEnum;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.shard.ShardManager;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.ActivityType;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.handle.obj.StatusType;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.PermissionUtils;
import sx.blah.discord.util.RequestBuffer;
import sx.blah.discord.util.RequestBuffer.RequestFuture;

public class BotUtils {

	public static RequestFuture<IMessage> sendMessage(String content, IChannel channel) {
		if(!BotUtils.hasPermissions(channel, Permissions.SEND_MESSAGES)) {
			LogUtils.infof("{Guild ID: %d} Shadbot wasn't allowed to send a message.", channel.getGuild().getLongID());
			return null;
		}

		StatsManager.increment(VariousEnum.MESSAGES_SENT);

		return BotUtils.sendMessage(new MessageBuilder(channel.getClient()).withChannel(channel).withContent(content));
	}

	public static RequestFuture<IMessage> sendMessage(EmbedObject embed, IChannel channel) {
		if(!BotUtils.hasPermissions(channel, Permissions.SEND_MESSAGES, Permissions.EMBED_LINKS)) {
			BotUtils.sendMessage(TextUtils.missingPerm(Permissions.EMBED_LINKS), channel);
			LogUtils.infof("{Guild ID: %d} Shadbot wasn't allowed to send embed link.", channel.getGuild().getLongID());
			return null;
		}

		StatsManager.increment(VariousEnum.EMBEDS_SENT);

		return BotUtils.sendMessage(new MessageBuilder(channel.getClient()).withChannel(channel).withEmbed(embed));
	}

	public static RequestFuture<IMessage> sendMessage(MessageBuilder message) {
		return BotUtils.sendMessage(message, 3);
	}

	public static RequestFuture<IMessage> sendMessage(MessageBuilder message, int retry) {
		if(retry == 0) {
			LogUtils.warnf("{Guild ID: %d} Abort attempt to send message (3 failed requests).", message.getChannel().getGuild());
			return null;
		}

		IGuild guild = message.getChannel().isPrivate() ? null : message.getChannel().getGuild();
		if(!message.getChannel().getShard().isReady()) {
			if(guild != null) {
				LogUtils.infof("{Guild ID: %d} A message couldn't be sent because shard isn't ready, adding it to queue.", guild.getLongID());
				ShardManager.getShadbotShard(guild.getShard()).queue(message);
			}
			return null;
		}

		return RequestBuffer.request(() -> {
			try {
				return message.send();
			} catch (MissingPermissionsException err) {
				BotUtils.sendMessage(TextUtils.missingPerm(err.getMissingPermissions()), message.getChannel());
				LogUtils.infof("{Guild ID: %d} %s", guild.getLongID(), err.getMessage());
			} catch (DiscordException err) {
				if(err.getMessage().contains("Message was unable to be sent (Discord didn't return a response)")) {
					LogUtils.infof("{Guild ID: %d} A message could not be send because Discord didn't return a response, retrying.",
							guild.getLongID());
					return BotUtils.sendMessage(message, retry - 1).get();
				} else if(err.getMessage().contains("Failed to make a 400 failed request after 5 tries!")) {
					LogUtils.warnf("{Guild ID: %d} %s", guild.getLongID(), err.getMessage());
				} else {
					LogUtils.error(err, "An error occurred while sending message.");
				}
			}
			return null;
		});
	}

	public static RequestFuture<Integer> deleteMessages(IChannel channel, IMessage... messages) {
		// Only keeps messages that are at most 2 weeks old
		List<IMessage> toDelete = Arrays.asList(messages).stream()
				.filter(msg -> msg != null && TimeUtils.getMillisUntil(msg.getCreationDate()) < TimeUnit.DAYS.toMillis(7 * 2))
				.distinct()
				.collect(Collectors.toList());

		if(toDelete.isEmpty()) {
			throw new IllegalArgumentException("There is no message to delete.");
		}

		return RequestBuffer.request(() -> {
			return channel.bulkDelete(toDelete).size();
		});
	}

	public static void updatePresence() {
		if(Shadbot.getClient().isReady()) {
			Shadbot.getClient().changePresence(StatusType.ONLINE, ActivityType.PLAYING,
					String.format("%shelp | %s", Config.DEFAULT_PREFIX, TextUtils.getTip()));
		}
	}

	public static List<IUser> getUsersFrom(IMessage message) {
		List<IUser> users = new ArrayList<>(message.getMentions());
		for(IRole role : message.getRoleMentions()) {
			users.addAll(message.getGuild().getUsersByRole(role));
		}
		users = users.stream().distinct().collect(Collectors.toList());
		return users;
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
		List<String> blacklistedCmd = Database.getDBGuild(guild).getBlacklistedCmd();
		if(blacklistedCmd.isEmpty()) {
			return true;
		}

		return cmd.getNames().stream().noneMatch(blacklistedCmd::contains);
	}

	public static boolean hasPermissions(IChannel channel, Permissions... permissions) {
		return PermissionUtils.hasPermissions(channel, channel.getClient().getOurUser(), permissions);
	}

	public static boolean hasPermissions(IGuild guild, Permissions... permissions) {
		return PermissionUtils.hasPermissions(guild, guild.getClient().getOurUser(), permissions);
	}

	public static boolean canInteract(IGuild guild, IUser user) {
		return PermissionUtils.isUserHigher(guild, guild.getClient().getOurUser(), user);
	}

}
