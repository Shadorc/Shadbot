package me.shadorc.shadbot.utils;

import java.util.List;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.data.stats.enums.VariousEnum;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

public class BotUtils {

	public static boolean hasAllowedRole(Snowflake guildId, List<Role> roles) {
		final List<Snowflake> allowedRoles = Shadbot.getDatabase().getDBGuild(guildId).getAllowedRoles();
		// If the user is an administrator OR no permissions have been set OR the role is allowed
		return allowedRoles.isEmpty()
				|| roles.stream().anyMatch(role -> role.getPermissions().contains(Permission.ADMINISTRATOR))
				|| roles.stream().anyMatch(role -> allowedRoles.contains(role.getId()));
	}

	public static boolean isCommandAllowed(Snowflake guildId, AbstractCommand cmd) {
		final List<String> blacklistedCmd = Shadbot.getDatabase().getDBGuild(guildId).getBlacklistedCmd();
		return cmd.getNames().stream().noneMatch(blacklistedCmd::contains);
	}

	public static boolean isTextChannelAllowed(Snowflake guildId, Snowflake channelId) {
		final List<Snowflake> allowedTextChannels = Shadbot.getDatabase().getDBGuild(guildId).getAllowedTextChannels();
		// If no permission has been set OR the text channel is allowed
		return allowedTextChannels.isEmpty() || allowedTextChannels.contains(channelId);
	}

	public static boolean isVoiceChannelAllowed(Snowflake guildId, Snowflake channelId) {
		final List<Snowflake> allowedVoiceChannels = Shadbot.getDatabase().getDBGuild(guildId).getAllowedVoiceChannels();
		// If no permission has been set OR the voice channel is allowed
		return allowedVoiceChannels.isEmpty() || allowedVoiceChannels.contains(channelId);
	}

	public static Mono<Message> sendMessage(EmbedCreateSpec embed, MessageChannel channel) {
		return BotUtils.sendMessage(null, embed, channel);
	}

	public static Mono<Message> sendMessage(String content, EmbedCreateSpec embed, MessageChannel channel) {
		final Snowflake selfId = channel.getClient().getSelfId().get();
		return Mono.zip(
				DiscordUtils.hasPermission(channel, selfId, Permission.SEND_MESSAGES),
				DiscordUtils.hasPermission(channel, selfId, Permission.EMBED_LINKS))
				.flatMap(tuple -> {
					final boolean canSendMessage = tuple.getT1();
					final boolean canSendEmbed = tuple.getT2();

					if(!canSendMessage) {
						return Mono.empty();
					}

					if(!canSendEmbed && embed != null) {
						return BotUtils.sendMessage(String.format(Emoji.ACCESS_DENIED + " I cannot send embed links.%nPlease, check my permissions "
								+ "and channel-specific ones to verify that **%s** is checked.",
								StringUtils.capitalizeEnum(Permission.EMBED_LINKS)), channel);
					}

					return channel.createMessage(spec -> {
						if(content != null) {
							spec.setContent(content);
						}
						if(embed != null) {
							spec.setEmbed(embed);
						}
					});
				})
				.doOnSuccess(message -> {
					if(!message.getEmbeds().isEmpty()) {
						StatsManager.VARIOUS_STATS.log(VariousEnum.EMBEDS_SENT);
					}
					StatsManager.VARIOUS_STATS.log(VariousEnum.MESSAGES_SENT);
				});
	}

	public static Mono<Message> sendMessage(String content, MessageChannel channel) {
		return BotUtils.sendMessage(content, null, channel);
	}

}
