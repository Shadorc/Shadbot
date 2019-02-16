package me.shadorc.shadbot.command.owner;

import java.util.function.Consumer;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.exception.ExceptionUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

@Command(category = CommandCategory.OWNER, permission = CommandPermission.OWNER, names = { "leave" })
public class LeaveCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		final Long guildId = NumberUtils.asPositiveLong(arg);
		if(guildId == null) {
			throw new CommandException(String.format("`%s` is not a valid guild ID.", arg));
		}

		return context.getClient().getGuildById(Snowflake.of(guildId))
				.onErrorMap(ExceptionUtils::isDiscordForbidden,
						err -> new CommandException("Guild not found."))
				.flatMap(Guild::leave)
				.and(context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(Emoji.INFO + " Guild left.", channel)));
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Leave a guild.")
				.addArg("guildID", false)
				.build();
	}

}
