package me.shadorc.shadbot.command.admin;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.CastUtils;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;

@Command(category = CommandCategory.ADMIN, permission = CommandPermission.ADMIN, names = { "prune" })
public class PruneCmd extends AbstractCommand {

	private static final int MESSAGE_COUNT = 300;

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!BotUtils.hasPermissions(context.getChannel(), Permissions.MANAGE_MESSAGES, Permissions.READ_MESSAGE_HISTORY)) {
			LogUtils.infof("{Guild ID: %d} Shadbot wasn't allowed to manage messages/read message history.", context.getGuild().getLongID());
			BotUtils.sendMessage(TextUtils.missingPerm(Permissions.MANAGE_MESSAGES, Permissions.READ_MESSAGE_HISTORY), context.getChannel());
			return;
		}

		List<String> quotedList = StringUtils.getQuotedWords(context.getArg());
		if(context.getArg().contains("\"") && quotedList.isEmpty() || quotedList.size() > 1) {
			throw new IllegalArgumentException("You have forgotten a quote or have specified several quotes in quotation marks.");
		}
		String words = quotedList.isEmpty() ? null : quotedList.get(0);

		List<IUser> usersMentioned = context.getMessage().getMentions();

		// Remove everything from arg (users mentioned and quoted words) to keep only count if specified
		String argCleaned = StringUtils.remove(context.getArg(),
				usersMentioned.stream().map(user -> String.format("<@%s>", user.getLongID())).collect(Collectors.joining(" ")),
				String.format("\"%s\"", words))
				.trim();

		Integer count = CastUtils.asPositiveInt(argCleaned);
		if(!argCleaned.isEmpty() && count == null) {
			throw new IllegalArgumentException(String.format("Invalid number. If you want to specify a word or a sentence, "
					+ "please include them in quotation marks. See `%shelp %s` for more information.", context.getPrefix(), this.getName()));
		}
		count = count == null ? 100 : Math.min(100, count);

		Stream<IMessage> messagesStream = context.getChannel().getMessageHistory(MESSAGE_COUNT).stream();

		if(!usersMentioned.isEmpty()) {
			messagesStream = messagesStream.filter(msg -> usersMentioned.contains(msg.getAuthor()));
		}
		if(words != null) {
			messagesStream = messagesStream.filter(msg -> msg.getContent().contains(words));
		}

		List<IMessage> messagesList = messagesStream.limit(count).collect(Collectors.toList());

		int deletedMsg = BotUtils.deleteMessages(context.getChannel(), messagesList);
		if(deletedMsg == 0) {
			BotUtils.sendMessage(Emoji.INFO + " There is no message to delete.", context.getChannel());
		} else {
			BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " (Requested by **%s**) %s deleted.",
					context.getAuthorName(), StringUtils.pluralOf(deletedMsg, "message")),
					context.getChannel());
		}
	}

	@Override
	public EmbedObject getHelp(Context context) {
		return new HelpBuilder(this, context.getPrefix())
				.setDescription("Delete messages.")
				.addArg("@user(s)", "from these users", true)
				.addArg("words", "containing these words", true)
				.addArg("number", "number of messages to delete (max: 100)", true)
				.setExample(String.format("Delete **15** messages from user **@Shadbot** containing **hi guys**:"
						+ "%n`%s%s @Shadbot \"hi guys\" 15`", context.getPrefix(), this.getName()))
				.build();
	}

}
