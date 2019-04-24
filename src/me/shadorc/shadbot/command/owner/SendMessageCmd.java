package me.shadorc.shadbot.command.owner;

import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.exception.ExceptionUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class SendMessageCmd extends BaseCmd {

	public SendMessageCmd() {
		super(CommandCategory.OWNER, CommandPermission.OWNER, List.of("send_message", "send-message", "sendmessage"));
	}

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = context.requireArgs(2);

		final Long userId = NumberUtils.asPositiveLong(args.get(0));
		if(userId == null) {
			return Mono.error(new CommandException(String.format("`%s` is not a valid user ID.",
					args.get(0))));
		}

		if(Snowflake.of(userId).equals(context.getSelfId())) {
			return Mono.error(new CommandException("I can't send a private message to myself."));
		}

		return context.getClient().getUserById(Snowflake.of(userId))
				.switchIfEmpty(Mono.error(new CommandException("User not found.")))
				.flatMap(user -> {
					if(user.isBot()) {
						return Mono.error(new CommandException("I can't send private message to other bots."));
					}

					return user.getPrivateChannel()
							.cast(MessageChannel.class)
							.flatMap(privateChannel -> DiscordUtils.sendMessage(args.get(1), privateChannel))
							.onErrorMap(ExceptionUtils::isDiscordForbidden, err -> new CommandException("I'm not allowed to send a private message to this user."));
				})
				.then(context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(Emoji.CHECK_MARK + " Message sent.", channel)))
				.then();
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Send a private message to an user.")
				.addArg("userID", false)
				.addArg("message", false)
				.build();
	}

}
