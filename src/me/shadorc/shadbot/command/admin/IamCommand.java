package me.shadorc.shadbot.command.admin;

import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.util.Permission;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import reactor.core.publisher.Mono;

@Command(category = CommandCategory.ADMIN, permission = CommandPermission.ADMIN, names = { "iam" }, permissions = { Permission.MANAGE_ROLES })
public class IamCommand extends AbstractCommand {

	public static final ReactionEmoji ADD_REACTION = ReactionEmoji.unicode("✅");
	public static final ReactionEmoji REMOVE_REACTION = ReactionEmoji.unicode("❌");

	@Override
	public Mono<Void> execute(Context context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		// TODO Auto-generated method stub
		return null;
	}

}
