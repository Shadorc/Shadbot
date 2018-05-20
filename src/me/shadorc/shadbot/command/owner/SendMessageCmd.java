package me.shadorc.shadbot.command.owner;

import java.util.List;

import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;

@Command(category = CommandCategory.OWNER, permission = CommandPermission.OWNER, names = { "send" })
public class SendMessageCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		context.requireArg();

		List<String> splitArgs = StringUtils.split(context.getArg().get(), 2);
		if(splitArgs.size() != 2) {
			throw new MissingArgumentException();
		}

		Long userId = NumberUtils.asPositiveLong(splitArgs.get(0));
		if(userId == null) {
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid user ID.", splitArgs.get(0)));
		}

		context.getClient().getUserById(Snowflake.of(userId)).defaultIfEmpty(null).subscribe(user -> {
			if(user == null) {
				BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " User not found.", context.getChannel());
				return;
			}

			context.getSelf().map(User::getId).subscribe(selfId -> {
				if(user.getId().equals(selfId)) {
					throw new IllegalCmdArgumentException("I can't send a private message to myself.");
				}

				if(user.isBot()) {
					throw new IllegalCmdArgumentException("I can't send private message to other bots.");
				}

				BotUtils.sendMessage(splitArgs.get(1), user.getPrivateChannel().cast(MessageChannel.class));
				BotUtils.sendMessage(Emoji.CHECK_MARK + " Message sent.", context.getChannel());
			});

		});
	}

	@Override
	public EmbedCreateSpec getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Send a private message to an user.")
				.addArg("userID", false)
				.addArg("message", false)
				.build();
	}

}
