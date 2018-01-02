package me.shadorc.shadbot.command.owner;

import java.util.List;

import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.CastUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IUser;

@Command(category = CommandCategory.OWNER, permission = CommandPermission.OWNER, names = { "send" })
public class SendMessageCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		List<String> splitArgs = StringUtils.split(context.getArg(), 2);
		if(splitArgs.size() != 2) {
			throw new MissingArgumentException();
		}

		Integer userID = CastUtils.asPositiveInt(splitArgs.get(0));
		if(userID == null) {
			throw new IllegalArgumentException("Invalid user ID.");
		}

		IUser user = Shadbot.getClient().getUserByID(userID);
		if(user == null) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " User not found.", context.getChannel());
			return;
		}

		context.getClient().getOrCreatePMChannel(user).sendMessage(splitArgs.get(1));
		BotUtils.sendMessage(Emoji.CHECK_MARK + " Message sent.", context.getChannel());
	}

	@Override
	public EmbedObject getHelp(Context context) {
		return new HelpBuilder(this, context.getPrefix())
				.setDescription("Send a private message to an user.")
				.addArg("userID", false)
				.addArg("message", false)
				.build();
	}

}
