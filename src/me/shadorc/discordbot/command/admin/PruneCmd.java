package me.shadorc.discordbot.command.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MessageHistory;
import sx.blah.discord.util.RequestBuffer;

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

		List<String> argsList = Arrays.asList(context.getArg().split(" "));

		String word = null;
		if(argsList.contains("-c")) {
			List<String> wordsList = StringUtils.getQuotedWords(context.getArg());
			if(wordsList.isEmpty()) {
				throw new MissingArgumentException();
			}
			word = wordsList.get(0);
		}

		int num = -1;
		if(argsList.contains("-n")) {
			if(argsList.indexOf("-n") + 1 >= argsList.size()) {
				throw new MissingArgumentException();
			}
			String numStr = argsList.get(argsList.indexOf("-n") + 1);
			if(!StringUtils.isPositiveInt(numStr)) {
				throw new MissingArgumentException();
			}
			num = Integer.parseInt(numStr);
		}

		List<IUser> usersMentioned = context.getMessage().getMentions();

		MessageHistory historyList = context.getChannel().getMessageHistory(context.getChannel().getMaxInternalCacheCount());
		List<IMessage> toDeleteList = new ArrayList<IMessage>();

		int count = 0;
		for(IMessage message : historyList) {
			if(num != -1 && count >= num) {
				break;
			}
			if(!usersMentioned.isEmpty() && !usersMentioned.contains(message.getAuthor())) {
				continue;
			}
			if(word != null && !message.getContent().contains(word)) {
				continue;
			}
			toDeleteList.add(message);
			count++;
		}

		final List<IMessage> truncDeleteList = new ArrayList<IMessage>(toDeleteList.subList(0, Math.min(100, toDeleteList.size())));

		RequestBuffer.request(() -> {
			if(truncDeleteList.isEmpty()) {
				BotUtils.sendMessage(Emoji.INFO + " There is no message to delete.", context.getChannel());
			} else if(truncDeleteList.size() == 1) {
				// MessageList#bulkDelete(List<IMessage> messages) cannot delete a single message
				truncDeleteList.get(0).delete();
				BotUtils.sendMessage(Emoji.CHECK_MARK + " 1 message deleted.", context.getChannel());
			} else {
				context.getChannel().bulkDelete(truncDeleteList);
				BotUtils.sendMessage(Emoji.CHECK_MARK + " " + truncDeleteList.size() + " messages deleted.", context.getChannel());
			}
		});
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Delete messages.**")
				.appendField("Usage", context.getPrefix() + "prune [*@user(s)*] [-c *\"words\"*] [-n *num*]", false)
				.appendField("Options", "**num** - number of messages to delete (max: 100)"
						+ "\n**user(s)** - from these users"
						+ "\n**words** - containing these words", false)
				.appendField("Example", "Delete **15** messages from user **@Shadbot** containing **hi guys**:"
						+ "\n" + context.getPrefix() + "prune @Shadbot -c \"hi guys\" -n 15", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
