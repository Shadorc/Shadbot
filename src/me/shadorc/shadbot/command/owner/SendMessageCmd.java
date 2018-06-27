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
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

@Command(category = CommandCategory.OWNER, permission = CommandPermission.OWNER, names = { "send" })
public class SendMessageCmd extends AbstractCommand {

	@Override
	public void execute(Context context) {
		List<String> args = context.requireArgs(2);

		Long userId = NumberUtils.asPositiveLong(args.get(0));
		if(userId == null) {
			throw new CommandException(String.format("`%s` is not a valid user ID.", args.get(0)));
		}

		context.getClient().getUserById(Snowflake.of(userId)).defaultIfEmpty(null).subscribe(user -> {
			if(user == null) {
				BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " User not found.", context.getChannel());
				return;
			}

			if(user.getId().equals(context.getSelfId())) {
				throw new CommandException("I can't send a private message to myself.");
			}

			if(user.isBot()) {
				throw new CommandException("I can't send private message to other bots.");
			}

			BotUtils.sendMessage(args.get(1), user.getPrivateChannel().cast(MessageChannel.class));
			BotUtils.sendMessage(Emoji.CHECK_MARK + " Message sent.", context.getChannel());

		});
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
