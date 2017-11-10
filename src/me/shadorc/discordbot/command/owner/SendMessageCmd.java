package me.shadorc.discordbot.command.owner;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class SendMessageCmd extends AbstractCommand {

	public SendMessageCmd() {
		super(CommandCategory.OWNER, Role.OWNER, RateLimiter.DEFAULT_COOLDOWN, "send");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		String[] splitArgs = StringUtils.getSplittedArg(context.getArg(), 2);
		if(splitArgs.length != 2) {
			throw new MissingArgumentException();
		}

		String userIDStr = splitArgs[0];
		if(!StringUtils.isPositiveLong(userIDStr)) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid user ID.", context.getChannel());
			return;
		}

		IUser user = Shadbot.getClient().getUserByID(Long.parseLong(userIDStr));
		if(user == null) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " User not found.", context.getChannel());
			return;
		}

		Shadbot.getClient().getOrCreatePMChannel(user).sendMessage(splitArgs[1]);
		BotUtils.sendMessage(Emoji.CHECK_MARK + " Message sent.", context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Send a private message to an user.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " <userID> <message>`", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

}
