package me.shadorc.shadbot.command.owner;

import java.util.List;

import discord4j.core.object.entity.MessageChannel;
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

@Command(category = CommandCategory.OWNER, permission = CommandPermission.OWNER, names = { "send" })
public class SendMessageCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = context.requireArgs(2);

		final Long userId = NumberUtils.asPositiveLong(args.get(0));
		if(userId == null) {
			throw new CommandException(String.format("`%s` is not a valid user ID.", args.get(0)));
		}

		if(Snowflake.of(userId).equals(context.getSelfId())) {
			throw new CommandException("I can't send a private message to myself.");
		}

		return context.getClient().getUserById(Snowflake.of(userId))
				.flatMap(user -> {
					if(user.isBot()) {
						throw new CommandException("I can't send private message to other bots.");
					}

					return user.getPrivateChannel()
							.cast(MessageChannel.class)
							.flatMap(privateChannel -> DiscordUtils.sendMessage(args.get(1), privateChannel));

				})
				.then(context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(Emoji.CHECK_MARK + " Message sent.", channel)))
				.onErrorMap(ExceptionUtils::isNotFound, err -> new CommandException("User not found."))
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Send a private message to an user.")
				.addArg("userID", false)
				.addArg("message", false)
				.build();
	}

}
