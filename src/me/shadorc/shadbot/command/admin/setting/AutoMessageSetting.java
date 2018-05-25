// TODO
// package me.shadorc.shadbot.command.admin.setting;
//
// import java.util.List;
//
// import me.shadorc.shadbot.command.admin.setting.core.AbstractSetting;
// import me.shadorc.shadbot.command.admin.setting.core.Setting;
// import me.shadorc.shadbot.command.admin.setting.core.SettingEnum;
// import me.shadorc.shadbot.core.command.Context;
// import me.shadorc.shadbot.data.db.DBGuild;
// import me.shadorc.shadbot.data.db.Database;
// import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
// import me.shadorc.shadbot.exception.MissingArgumentException;
// import me.shadorc.shadbot.utils.BotUtils;
// import me.shadorc.shadbot.utils.FormatUtils;
// import me.shadorc.shadbot.utils.StringUtils;
// import me.shadorc.shadbot.utils.Utils;
// import me.shadorc.shadbot.utils.embed.EmbedUtils;
// import me.shadorc.shadbot.utils.object.Emoji;
//
// @Setting(description = "Manage auto messages on user join/leave.", setting = SettingEnum.AUTO_MESSAGE)
// public class AutoMessageSetting extends AbstractSetting {
//
// private enum Action {
// ENABLE, DISABLE;
// }
//
// private enum Type {
// CHANNEL, JOIN_MESSAGE, LEAVE_MESSAGE;
// }
//
// @Override
// public void execute(Context context, String arg) throws MissingArgumentException, IllegalCmdArgumentException {
// context.requireArg();
//
// List<String> splitArgs = StringUtils.split(arg, 3);
// if(splitArgs.size() < 2) {
// throw new MissingArgumentException();
// }
//
// Action action = Utils.getValueOrNull(Action.class, splitArgs.get(0));
// if(action == null) {
// throw new IllegalCmdArgumentException(String.format("`%s` is not a valid action. %s",
// splitArgs.get(0), FormatUtils.formatOptions(Action.class)));
// }
//
// Type type = Utils.getValueOrNull(Type.class, splitArgs.get(1));
// if(type == null) {
// throw new IllegalCmdArgumentException(String.format("`%s` is not a valid type. %s",
// splitArgs.get(1), FormatUtils.formatOptions(Type.class)));
// }
//
// switch (type) {
// case CHANNEL:
// this.channel(context, action);
// break;
// case JOIN_MESSAGE:
// this.updateJoinMessage(context, action, splitArgs);
// break;
// case LEAVE_MESSAGE:
// this.updateLeaveMessage(context, action, splitArgs);
// break;
// }
// }
//
// private void channel(Context context, Action action) throws MissingArgumentException {
// List<IChannel> channelsMentioned = context.getMessage().getChannelMentions();
// if(channelsMentioned.size() != 1) {
// throw new MissingArgumentException();
// }
//
// DBGuild dbGuild = Database.getDBGuild(context.getGuildId().get());
// IChannel channel = channelsMentioned.get(0);
// if(Action.ENABLE.equals(action)) {
// dbGuild.setSetting(SettingEnum.MESSAGE_CHANNEL_ID, channel.getLongID());
// BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " %s is now the default channel for join/leave messages.",
// channel.mention()), context.getChannel());
// } else if(Action.DISABLE.equals(action)) {
// dbGuild.removeSetting(SettingEnum.MESSAGE_CHANNEL_ID);
// BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Auto-messages disabled. I will no longer send automatic messages "
// + "until a new channel is defined.", channel.mention()), context.getChannel());
// }
// }
//
// private void updateJoinMessage(Context context, Action action, List<String> args) throws MissingArgumentException {
// DBGuild dbGuild = Database.getDBGuild(context.getGuildId().get());
// if(Action.ENABLE.equals(action)) {
// if(args.size() < 3) {
// throw new MissingArgumentException();
// }
// String message = args.get(2);
// dbGuild.setSetting(SettingEnum.JOIN_MESSAGE, message);
// BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Join message set to `%s`", message), context.getChannel());
//
// } else if(Action.DISABLE.equals(action)) {
// dbGuild.removeSetting(SettingEnum.JOIN_MESSAGE);
// BotUtils.sendMessage(Emoji.CHECK_MARK + " Join message disabled.", context.getChannel());
// }
// }
//
// private void updateLeaveMessage(Context context, Action action, List<String> args) throws MissingArgumentException {
// DBGuild dbGuild = Database.getDBGuild(context.getGuild());
// if(Action.ENABLE.equals(action)) {
// if(args.size() < 3) {
// throw new MissingArgumentException();
// }
// String message = args.get(2);
// dbGuild.setSetting(SettingEnum.LEAVE_MESSAGE, message);
// BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Leave message set to `%s`", message), context.getChannel());
//
// } else if(Action.DISABLE.equals(action)) {
// dbGuild.removeSetting(SettingEnum.LEAVE_MESSAGE);
// BotUtils.sendMessage(Emoji.CHECK_MARK + " Leave message disabled.", context.getChannel());
// }
// }
//
// @Override
// public EmbedBuilder getHelp(String prefix) {
// return EmbedUtils.getDefaultEmbed()
// .addField("Usage", String.format("`%s%s <action> <type> [<value>]`", prefix, this.getCmdName()), false)
// .addField("Argument", String.format("**action** - %s"
// + "%n**type** - %s"
// + "%n**value** - a message for *%s* and *%s* or a @channel for *%s*",
// FormatUtils.format(Action.values(), action -> action.toString().toLowerCase(), "/"),
// FormatUtils.format(Type.values(), type -> type.toString().toLowerCase(), "/"),
// Type.JOIN_MESSAGE.toString().toLowerCase(),
// Type.LEAVE_MESSAGE.toString().toLowerCase(),
// Type.CHANNEL.toString().toLowerCase()), false)
// .addField("Info", "You don't need to specify *value* to disable a type.", false)
// .addField("Example", String.format("`%s%s enable join_message Hello you (:`"
// + "%n`%s%s disable leave_message`", prefix, this.getCmdName(), prefix, this.getCmdName()), false);
// }
// }
