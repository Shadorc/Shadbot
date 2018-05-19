package me.shadorc.shadbot.utils;

import java.util.List;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.data.stats.VariousStatsManager;
import me.shadorc.shadbot.data.stats.VariousStatsManager.VariousEnum;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class BotUtils {

	public static Mono<Message> sendMessage(String content, MessageChannel channel) {
		if(!BotUtils.hasPermissions(channel, Permission.SEND_MESSAGES)) {
			LogUtils.infof("{Channel ID: %d} Shadbot wasn't allowed to send a message.", channel.getId().asLong());
			return Mono.empty();
		}

		VariousStatsManager.log(VariousEnum.MESSAGES_SENT);
		return BotUtils.sendMessage(new MessageCreateSpec().setContent(content), channel);
	}

	public static Mono<Message> sendMessage(EmbedCreateSpec embed, MessageChannel channel) {
		if(!BotUtils.hasPermissions(channel, Permission.SEND_MESSAGES, Permission.EMBED_LINKS)) {
			BotUtils.sendMessage(TextUtils.missingPerm(Permission.EMBED_LINKS), channel);
			LogUtils.infof("{Channel ID: %d} Shadbot wasn't allowed to send embed link.", channel.getId().asLong());
			return Mono.empty();
		}

		VariousStatsManager.log(VariousEnum.EMBEDS_SENT);
		return BotUtils.sendMessage(new MessageCreateSpec().setEmbed(embed), channel);
	}

	public static Mono<Message> sendMessage(MessageCreateSpec message, MessageChannel channel) {
		/*
		 * TODO if(!channel.getShard().isReady()) { if(guild != null) {
		 * LogUtils.infof("{Guild ID: %d} A message couldn't be sent because shard isn't ready, adding it to queue.", guildID);
		 * ShardManager.getShadbotShard(guild.getShard()).queue(message); } return null; }
		 */

		return channel.createMessage(message)
				.doOnError(err -> LogUtils.error(err,
						String.format("{Channel ID: %d} An error occurred while sending a message.",
								channel.getId().asLong())));
	}

	public static Flux<Snowflake> deleteMessages(TextChannel channel, Message... messages) {
		switch (messages.length) {
			case 0:
				return Flux.empty();
			case 1:
				messages[0].delete();
				return Flux.just(messages[0].getId());
			default:
				return channel.bulkDelete(Flux.fromArray(messages).map(Message::getId));
		}
	}

	public static void updatePresence(DiscordClient client) {
		String text = String.format("%shelp | %s", Config.DEFAULT_PREFIX, TextUtils.getTip());
		client.updatePresence(Presence.online(Activity.playing(text)));
	}

	public static List<User> getUsersFrom(Message message) {
		Mono<List<User>> users = message.getUserMentions().collectList();
		Mono<List<Role>> roles = message.getRoleMentions().collectList();
		//TODO: Do I need to get all members and filter them ?
		users.concatWith(message.getGuild().block().getMembers().filter(member -> member.getRoles().has).block()).collectList());
		users = users.stream().distinct().collect(Collectors.toList());
		return users;
	}

	public static boolean isChannelAllowed(Guild guild, MessageChannel channel) {
		long channelID = channel.getId().asLong();
		List<Long> allowedChannels = Database.getDBGuild(guild.getId()).getAllowedChannels();
		// If no permission has been set OR the channel is allowed
		return allowedChannels.isEmpty() || allowedChannels.contains(channelID);
	}

	public static boolean hasAllowedRole(Guild guild, List<Role> roles) {
		List<Long> allowedRoles = Database.getDBGuild(guild.getId()).getAllowedRoles();
		// If the user is an administrator OR no permissions have been set OR the role is allowed
		return roles.stream().anyMatch(role -> role.getPermissions().contains(Permission.ADMINISTRATOR))
				|| allowedRoles.isEmpty()
				|| roles.stream().anyMatch(role -> allowedRoles.contains(role.getId().asLong()));
	}

	public static boolean isCommandAllowed(Guild guild, AbstractCommand cmd) {
		List<String> blacklistedCmd = Database.getDBGuild(guild.getId()).getBlacklistedCmd();
		return cmd.getNames().stream().noneMatch(blacklistedCmd::contains);
	}

	public static boolean hasPermissions(Channel channel, Permission... permissions) {
		return PermissionUtils.hasPermissions(channel, channel.getClient().getOurUser(), permissions);
	}

	public static boolean hasPermissions(Guild guild, Permission... permissions) {
		return PermissionUtils.hasPermissions(guild, guild.getClient().getOurUser(), permissions);
	}

	public static boolean canInteract(Guild guild, User user) {
		return PermissionUtils.hasHierarchicalPermissions(guild, guild.getClient().getOurUser(), guild.getRolesForUser(user));
	}

}
