package me.shadorc.discordbot.command.admin;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.common.primitives.UnsignedInteger;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.ExceptionUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;

public class PruneCmd extends AbstractCommand {

	public PruneCmd() {
		super(CommandCategory.ADMIN, Role.ADMIN, RateLimiter.DEFAULT_COOLDOWN, "prune");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!BotUtils.hasPermission(context.getChannel(), Permissions.MANAGE_MESSAGES, Permissions.READ_MESSAGE_HISTORY)) {
			BotUtils.sendMessage(Emoji.ACCESS_DENIED + " I can't execute this command due to the lack of permission."
					+ "\nPlease, check my permissions and channel-specific ones to verify that **Manage messages** "
					+ "and **Read message history** are checked.",
					context.getChannel());
			LogUtils.info("{Guild ID: " + context.getChannel().getGuild().getLongID() + "} "
					+ "Shadbot wasn't allowed to manage messages/read message history.");
			return;
		}

		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		Options options = new Options();
		options.addOption("c", "containing", true, "containing these words");

		Option numOpt = new Option("n", "number", true, "number of messages to delete");
		numOpt.setType(UnsignedInteger.class);
		options.addOption(numOpt);

		CommandLine cmd;
		try {
			cmd = new DefaultParser().parse(options, StringUtils.getSplittedArg(context.getArg()));
		} catch (ParseException err) {
			ExceptionUtils.manageException("deleting messages", context, err);
			return;
		}

		String numStr = cmd.getOptionValue("number", "100");
		if(!StringUtils.isPositiveInt(numStr)) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid number.", context.getChannel());
			return;
		}

		String words = cmd.getOptionValue("containing");
		if(words != null && StringUtils.getCharCount(context.getArg(), '"') != 2) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " You must indicate words in quotation marks after '-c'.", context.getChannel());
			return;
		}

		int num = Math.min(100, Integer.parseInt(numStr));
		List<IUser> usersMentioned = context.getMessage().getMentions();

		List<IMessage> messagesList = new ArrayList<IMessage>();
		for(IMessage message : context.getChannel().getMessageHistory(context.getChannel().getMaxInternalCacheCount())) {
			if(messagesList.size() >= num) {
				break;
			}

			if(!usersMentioned.isEmpty() && usersMentioned.contains(message.getAuthor())
					|| words != null && message.getContent().contains(words)) {
				messagesList.add(message);
			}
		}

		if(messagesList.isEmpty()) {
			BotUtils.sendMessage(Emoji.INFO + " There is no message to delete.", context.getChannel());
		} else {
			BotUtils.sendMessage(Emoji.CHECK_MARK + " (Requested by **" + context.getAuthorName() + "**) "
					+ StringUtils.pluralOf(BotUtils.deleteMessages(context.getChannel(), messagesList), "message")
					+ " deleted.", context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Delete messages.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " [@user(s)] [-c \"words\"] [-n num]`", false)
				.appendField("Options", "**num** - [OPTIONAL] number of messages to delete (max: 100)"
						+ "\n**user(s)** - [OPTIONAL] from these users"
						+ "\n**words** - [OPTIONAL] containing these words", false)
				.appendField("Example", "Delete **15** messages from user **@Shadbot** containing **hi guys**:"
						+ "\n`" + context.getPrefix() + "prune @Shadbot -c \"hi guys\" -n 15`", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

}
