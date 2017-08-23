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
import sx.blah.discord.util.MessageHistory;

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

		int num = Integer.parseInt(numArg);
		if(num < 0) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " You cannot delete a negative number of messages, that doesn't make any sense.", context.getChannel());
			return;
		}

		//bulkDelete() cannot delete more than 100 messages
		num = Math.min(100, num);

		MessageHistory history = context.getChannel().getMessageHistory(context.getChannel().getMaxInternalCacheCount());
		List<IMessage> historyList = new ArrayList<IMessage>(history);

		for(IMessage message : history) {
			if(!usersMentioned.contains(message.getAuthor())) {
				historyList.remove(message);
			}
		}

		historyList = historyList.subList(0, Math.min(num, historyList.size()));

		if(historyList.isEmpty()) {
			BotUtils.sendMessage(Emoji.INFO + " There is no message to delete.", context.getChannel());
			return;
		} else if(historyList.size() == 1) {
			//MessageList#bulkDelete(List<IMessage> messages) cannot delete a single message
			historyList.get(0).delete();
			BotUtils.sendMessage(Emoji.CHECK_MARK + " " + historyList.size() + " message deleted.", context.getChannel());
		} else {
			context.getChannel().bulkDelete(historyList);
			BotUtils.sendMessage(Emoji.CHECK_MARK + " " + historyList.size() + " message(s) deleted.", context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Delete user's messages.**")
				.appendField("Usage", context.getPrefix() + "prune <@user(s)> <num>", false)
				.appendField("Arguments", "num - Number of messages to delete (max: 100)", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
