package me.shadorc.discordbot.command.admin;

import java.util.ArrayList;
import java.util.List;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.StringUtils;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;

public class PruneCmd extends AbstractCommand {

	public PruneCmd() {
		super(Role.ADMIN, "prune");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!BotUtils.hasPermission(context.getChannel(), Permissions.MANAGE_MESSAGES, Permissions.READ_MESSAGE_HISTORY)) {
			BotUtils.sendMessage(Emoji.ACCESS_DENIED + " I can't execute this command due to the lack of permission."
					+ "\nPlease, check my permissions and channel-specific ones to verify that **Manage messages** "
					+ "and **Read message history** are checked.",
					context.getChannel());
			LogUtils.info("{Guild: " + context.getChannel().getGuild().getName() + " (ID: " + context.getChannel().getGuild().getStringID() + ")} "
					+ "Shadbot wasn't allowed to manage messages/read message history.");
			return;
		}

		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		String[] splitArgs = context.getArg().split(" ");
		if(splitArgs.length < 2) {
			throw new MissingArgumentException();
		}

		List<IUser> usersMentioned = context.getMessage().getMentions();
		if(usersMentioned.isEmpty()) {
			throw new MissingArgumentException();
		}

		String numArg = splitArgs[splitArgs.length - 1];
		if(!StringUtils.isInteger(numArg)) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " Invalid number.", context.getChannel());
			return;
		}

		int num = Math.min(100, Integer.parseInt(numArg));

		// If channel contains less than num messages, it throws an ArrayIndexOutOfBoundsException
		List<IMessage> history;
		try {
			history = new ArrayList<IMessage>(context.getChannel().getMessageHistory(num));
		} catch (ArrayIndexOutOfBoundsException e) {
			LogUtils.warn("{PruneCmd} {Guild: " + context.getGuild().getName() + " (ID: " + context.getGuild().getStringID() + ")} "
					+ "Getting full message history.");
			history = new ArrayList<IMessage>(context.getChannel().getFullMessageHistory());
		}

		for(int i = 0; i < Math.min(num, history.size()); i++) {
			if(!usersMentioned.contains(history.get(i).getAuthor())) {
				history.remove(i);
			}
		}

		if(history.isEmpty()) {
			BotUtils.sendMessage(Emoji.INFO + " There is no message to delete.", context.getChannel());
			return;
		}

		context.getChannel().bulkDelete(history);
		BotUtils.sendMessage(Emoji.CHECK_MARK + " " + history.size() + " message(s) deleted.", context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Delete user's messages.**")
				.appendField("Usage", context.getPrefix() + "prune <@user(s)> <num>", false)
				.appendField("Arguments", "num - Number of messages to check (max: 100)", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
