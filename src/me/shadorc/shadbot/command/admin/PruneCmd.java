// package me.shadorc.shadbot.command.admin;
//
// import java.security.Permissions;
// import java.util.List;
// import java.util.stream.Collectors;
//
// import me.shadorc.shadbot.core.command.AbstractCommand;
// import me.shadorc.shadbot.core.command.CommandCategory;
// import me.shadorc.shadbot.core.command.CommandPermission;
// import me.shadorc.shadbot.core.command.Context;
// import me.shadorc.shadbot.core.command.annotation.Command;
// import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
// import me.shadorc.shadbot.exception.MissingArgumentException;
// import me.shadorc.shadbot.utils.BotUtils;
// import me.shadorc.shadbot.utils.FormatUtils;
// import me.shadorc.shadbot.utils.NumberUtils;
// import me.shadorc.shadbot.utils.StringUtils;
// import me.shadorc.shadbot.utils.TextUtils;
// import me.shadorc.shadbot.utils.embed.HelpBuilder;
// import me.shadorc.shadbot.utils.embed.log.LogUtils;
// import me.shadorc.shadbot.utils.object.Emoji;
//
// @Command(category = CommandCategory.ADMIN, permission = CommandPermission.ADMIN, names = { "prune" })
// public class PruneCmd extends AbstractCommand {
//
// private static final int MESSAGE_COUNT = 300;
//
// @Override
// public void execute(Context context) {
// if(!BotUtils.hasPermissions(context.getChannel(), Permissions.MANAGE_MESSAGES, Permissions.READ_MESSAGE_HISTORY)) {
// LogUtils.infof("{Guild ID: %d} Shadbot wasn't allowed to manage messages/read message history.", context.getGuild().getLongID());
// BotUtils.sendMessage(TextUtils.missingPerm(Permissions.MANAGE_MESSAGES, Permissions.READ_MESSAGE_HISTORY), context.getChannel());
// return;
// }
//
// List<String> quotedList = StringUtils.getQuotedWords(context.getArg());
// if(context.getArg().contains("\"") && quotedList.isEmpty() || quotedList.size() > 1) {
// throw new IllegalCmdArgumentException("You have forgotten a quote or have specified several quotes in quotation marks.");
// }
// String words = quotedList.isEmpty() ? null : quotedList.get(0);
//
// List<IUser> usersMentioned = context.getMessage().getMentions();
//
// // Remove everything from arg (users mentioned and quoted words) to keep only count if specified
// String argCleaned = StringUtils.remove(context.getArg(),
// FormatUtils.format(usersMentioned, user -> user.mention(false), " "),
// String.format("\"%s\"", words))
// .trim();
//
// Integer count = NumberUtils.asPositiveInt(argCleaned);
// if(!argCleaned.isEmpty() && count == null) {
// throw new IllegalCmdArgumentException(String.format("`%s` is not a valid number. If you want to specify a word or a sentence, "
// + "please include them in quotation marks. See `%shelp %s` for more information.",
// argCleaned, context.getPrefix(), this.getName()));
// }
// count = count == null ? 100 : Math.min(100, count);
//
// List<IMessage> messagesList = context.getChannel().getMessageHistory(MESSAGE_COUNT).stream()
// .filter(msg -> usersMentioned.isEmpty() || usersMentioned.contains(msg.getAuthor()))
// .filter(msg -> words == null || msg.getContent().contains(words) || this.getEmbedContent(msg).contains(words))
// .limit(count)
// .collect(Collectors.toList());
//
// try {
// int deletedMsg = BotUtils.deleteMessages(context.getChannel(), messagesList.toArray(new IMessage[messagesList.size()])).get();
// BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " (Requested by **%s**) %s deleted.",
// context.getUsername(), StringUtils.pluralOf(deletedMsg, "message")),
// context.getChannel());
// } catch (IllegalArgumentException err) {
// BotUtils.sendMessage(Emoji.INFO + " There is no message to delete.", context.getChannel());
// }
// }
//
// private String getEmbedContent(IMessage message) {
// StringBuilder strBuilder = new StringBuilder();
// for(IEmbed embed : message.getEmbeds()) {
// List<IEmbedField> fields = embed.getEmbedFields();
// if(fields == null) {
// continue;
// }
// for(IEmbedField field : fields) {
// strBuilder.append(field.getName() + "\n" + field.getValue() + "\n");
// }
// }
// return strBuilder.toString();
// }
//
// @Override
// public EmbedObject getHelp(String prefix) {
// return new HelpBuilder(this, prefix)
// .setDescription("Delete messages.")
// .addArg("@user(s)", "from these users", true)
// .addArg("\"words\"", "containing these words", true)
// .addArg("number", "number of messages to delete (max: 100)", true)
// .setExample(String.format("Delete **15** messages from user **@Shadbot** containing **hi guys**:"
// + "%n`%s%s @Shadbot \"hi guys\" 15`", prefix, this.getName()))
// .addField("Info", "Messages older than 2 weeks cannot be deleted.", false)
// .build();
// }
//
// }
