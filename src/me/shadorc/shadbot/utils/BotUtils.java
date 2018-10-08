package me.shadorc.shadbot.utils;

import java.util.List;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.data.database.DatabaseManager;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.data.stats.enums.VariousEnum;
import reactor.core.publisher.Mono;

public class BotUtils {

	public static Mono<Message> sendMessage(String content, Mono<MessageChannel> channel) {
		return BotUtils.sendMessage(content, null, channel);
	}

	public static Mono<Message> sendMessage(EmbedCreateSpec embed, Mono<MessageChannel> channel) {
		return BotUtils.sendMessage(null, embed, channel)
				.doOnSuccess(msg -> StatsManager.VARIOUS_STATS.log(VariousEnum.EMBEDS_SENT));
	}

	public static Mono<Message> sendMessage(String content, EmbedCreateSpec embed, Mono<MessageChannel> channelMono) {
		final MessageCreateSpec spec = new MessageCreateSpec();
		if(content != null) {
			spec.setContent(content);
		}
		if(embed != null) {
			spec.setEmbed(embed);
		}

		return channelMono.flatMap(channel -> channel.createMessage(spec))
				.doOnSuccess(message -> StatsManager.VARIOUS_STATS.log(VariousEnum.MESSAGES_SENT));
	}

	public static boolean hasAllowedRole(Snowflake guildId, List<Role> roles) {
		final List<Snowflake> allowedRoles = DatabaseManager.getDBGuild(guildId).getAllowedRoles();
		// If the user is an administrator OR no permissions have been set OR the role is allowed
		return allowedRoles.isEmpty()
				|| roles.stream().anyMatch(role -> role.getPermissions().contains(Permission.ADMINISTRATOR))
				|| roles.stream().anyMatch(role -> allowedRoles.contains(role.getId()));
	}

	public static boolean isTextChannelAllowed(Snowflake guildId, Snowflake channelId) {
		final List<Snowflake> allowedTextChannels = DatabaseManager.getDBGuild(guildId).getAllowedTextChannels();
		// If no permission has been set OR the text channel is allowed
		return allowedTextChannels.isEmpty() || allowedTextChannels.contains(channelId);
	}

	public static boolean isVoiceChannelAllowed(Snowflake guildId, Snowflake channelId) {
		final List<Snowflake> allowedVoiceChannels = DatabaseManager.getDBGuild(guildId).getAllowedVoiceChannels();
		// If no permission has been set OR the voice channel is allowed
		return allowedVoiceChannels.isEmpty() || allowedVoiceChannels.contains(channelId);
	}

	public static boolean isCommandAllowed(Snowflake guildId, AbstractCommand cmd) {
		final List<String> blacklistedCmd = DatabaseManager.getDBGuild(guildId).getBlacklistedCmd();
		return cmd.getNames().stream().noneMatch(blacklistedCmd::contains);
	}

}
