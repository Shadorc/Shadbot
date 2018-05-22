// package me.shadorc.shadbot.command.admin;
//
// import java.security.Permissions;
// import java.util.List;
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
// import me.shadorc.shadbot.utils.StringUtils;
// import me.shadorc.shadbot.utils.TextUtils;
// import me.shadorc.shadbot.utils.embed.HelpBuilder;
// import me.shadorc.shadbot.utils.object.Emoji;
//
// @Command(category = CommandCategory.ADMIN, permission = CommandPermission.ADMIN, names = { "softban" })
// public class SoftBanCmd extends AbstractCommand {
//
// @Override
// public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
// if(!context.hasArg()) {
// throw new MissingArgumentException();
// }
//
// List<IUser> mentionedUsers = context.getMessage().getMentions();
// if(mentionedUsers.isEmpty()) {
// throw new MissingArgumentException();
// }
//
// if(!PermissionUtils.hasPermissions(context.getChannel(), context.getAuthor(), Permissions.BAN)) {
// throw new IllegalArgumentException("You don't have permission to ban.");
// }
//
// if(!BotUtils.hasPermissions(context.getChannel(), Permissions.BAN)) {
// BotUtils.sendMessage(TextUtils.missingPerm(Permissions.BAN), context.getChannel());
// return;
// }
//
// if(mentionedUsers.contains(context.getAuthor())) {
// throw new IllegalCmdArgumentException("You cannot softban yourself.");
// }
//
// for(IUser mentionedUser : mentionedUsers) {
// if(!PermissionUtils.isUserHigher(context.getGuild(), context.getAuthor(), mentionedUser)) {
// throw new IllegalCmdArgumentException(String.format("You can't softban **%s** because he has the same or a higher role "
// + "position than you in the role hierarchy.",
// mentionedUser.getName()));
// }
// if(!BotUtils.canInteract(context.getGuild(), mentionedUser)) {
// throw new IllegalCmdArgumentException(String.format("I cannot softban **%s** because he has the same or a higher role "
// + "position than me in the role hierarchy.",
// mentionedUser.getName()));
// }
// }
//
// StringBuilder reason = new StringBuilder();
// reason.append(StringUtils.remove(context.getArg(), FormatUtils.format(mentionedUsers, user -> user.mention(false), " ")).trim());
// if(reason.length() > Ban.MAX_REASON_LENGTH) {
// throw new IllegalCmdArgumentException(String.format("Reason cannot exceed **%d characters**.", Ban.MAX_REASON_LENGTH));
// }
//
// if(reason.length() == 0) {
// reason.append("Reason not specified.");
// }
//
// for(IUser user : mentionedUsers) {
// if(!user.isBot()) {
// BotUtils.sendMessage(String.format(Emoji.INFO + " You were softbanned from the server **%s** by **%s**. Reason: `%s`",
// context.getGuild().getName(), context.getUsername(), reason), user.getOrCreatePMChannel());
// }
// RequestBuffer.request(() -> {
// context.getGuild().banUser(user, reason.toString(), 7);
// });
// RequestBuffer.request(() -> {
// context.getGuild().pardonUser(user.getLongID());
// });
// }
//
// BotUtils.sendMessage(String.format(Emoji.INFO + " (Requested by **%s**) **%s** got softbanned. Reason: `%s`",
// context.getUsername(), FormatUtils.format(mentionedUsers, IUser::getName, ", "), reason), context.getChannel());
// }
//
// @Override
// public EmbedObject getHelp(String prefix) {
// return new HelpBuilder(this, prefix)
// .setDescription("Ban and instantly unban user(s).\nIt's like kicking him/them but it also deletes his/their messages "
// + "from the last 7 days.")
// .addArg("@user(s)", false)
// .addArg("reason", true)
// .build();
// }
//
// }
