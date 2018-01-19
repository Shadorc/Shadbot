package me.shadorc.shadbot.command.owner;

import java.util.List;

import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.CastUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IUser;

@Command(category = CommandCategory.OWNER, permission = CommandPermission.OWNER, names = { "send" })
public class SendMessageCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		List<String> splitArgs = StringUtils.split(context.getArg(), 2);
		if(splitArgs.size() != 2) {
			throw new MissingArgumentException();
		}

		Long userID = CastUtils.asPositiveLong(splitArgs.get(0));
		if(userID == null) {
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid user ID.", splitArgs.get(0)));
		}

		IUser user = Shadbot.getClient().getUserByID(userID);
		if(user == null) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " User not found.", context.getChannel());
			return;
		}

		if(user.equals(context.getOurUser())) {
			throw new IllegalCmdArgumentException("I can't send a private message to myself.");
		}

		if(user.isBot()) {
			throw new IllegalCmdArgumentException("I can't send private message to other bots.");
		}

		context.getClient().getOrCreatePMChannel(user).sendMessage(splitArgs.get(1));
		BotUtils.sendMessage(Emoji.CHECK_MARK + " Message sent.", context.getChannel());
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Send a private message to an user.")
				.addArg("userID", false)
				.addArg("message", false)
				.build();
	}

}
