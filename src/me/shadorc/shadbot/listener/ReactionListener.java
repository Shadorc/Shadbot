package me.shadorc.shadbot.listener;

import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.command.admin.IamCommand;
import me.shadorc.shadbot.data.database.DatabaseManager;

public class ReactionListener {

	// TODO: Manage errors
	public static void onReactionAddEvent(ReactionAddEvent event) {
		event.getGuildId().ifPresent(guildId -> {
			final Snowflake roleId = DatabaseManager.getDBGuild(guildId).getIamMessages().get(event.getMessageId());
			if(roleId != null && event.getEmoji().equals(IamCommand.ADD_REACTION)) {
				event.getUser()
						.flatMap(user -> user.asMember(guildId))
						.flatMap(member -> member.addRole(roleId))
						.subscribe();
			}
		});
	}

	// TODO: Manage errors
	public static void onReactionRemoveEvent(ReactionRemoveEvent event) {
		event.getGuildId().ifPresent(guildId -> {
			final Snowflake roleId = DatabaseManager.getDBGuild(guildId).getIamMessages().get(event.getMessageId());
			if(roleId != null && event.getEmoji().equals(IamCommand.REMOVE_REACTION)) {
				event.getUser()
						.flatMap(user -> user.asMember(guildId))
						.flatMap(member -> member.removeRole(roleId))
						.subscribe();
			}
		});
	}

}
