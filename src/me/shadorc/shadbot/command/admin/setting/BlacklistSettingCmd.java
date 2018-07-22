// TODO
// package me.shadorc.shadbot.command.admin.setting;
//
// import java.util.List;
// import java.util.stream.Collectors;
//
// import org.json.JSONArray;
//
// import me.shadorc.shadbot.command.admin.setting.core.AbstractSetting;
// import me.shadorc.shadbot.command.admin.setting.core.Setting;
// import me.shadorc.shadbot.command.admin.setting.core.SettingEnum;
// import me.shadorc.shadbot.core.command.CommandManager;
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
// @Setting(description = "Manage blacklisted commands.", setting = SettingEnum.BLACKLIST)
// public class BlacklistSettingCmd extends AbstractSetting {
//
// private enum Action {
// ADD, REMOVE;
// }
//
// @Override
// public void execute(Context context, String arg) {
// context.requireArg();
//
// List<String> splitArgs = StringUtils.split(arg, 2);
// if(splitArgs.size() != 2) {
// throw new MissingArgumentException();
// }
//
// Action action = Utils.getValueOrNull(Action.class, splitArgs.get(0));
// if(action == null) {
// throw new IllegalCmdArgumentException(String.format("`%s` is not a valid action. %s",
// splitArgs.get(0), FormatUtils.formatOptions(Action.class)));
// }
//
// List<String> commands = StringUtils.split(splitArgs.get(1).toLowerCase());
//
// List<String> unknownCmds = commands.stream().filter(cmd -> CommandManager.getCommand(cmd) == null).collect(Collectors.toList());
// if(!unknownCmds.isEmpty()) {
// throw new IllegalCmdArgumentException(String.format("Command %s doesn't exist.",
// FormatUtils.format(unknownCmds, cmd -> String.format("`%s`", cmd), ", ")));
// }
//
// DBGuild dbGuild = Database.getDBGuild(context.getGuild());
// List<String> blacklist = dbGuild.getBlacklistedCmd();
// if(Action.ADD.equals(action)) {
// blacklist.addAll(commands);
// BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Command(s) `%s` added to the blacklist.",
// FormatUtils.format(commands, cmd -> String.format("`%s`", cmd), ", ")), context.getChannel());
// } else if(Action.REMOVE.equals(action)) {
// blacklist.removeAll(commands);
// BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Command(s) `%s` removed from the blacklist.",
// FormatUtils.format(commands, cmd -> String.format("`%s`", cmd), ", ")), context.getChannel());
// }
//
// dbGuild.setSetting(this.getSetting(), new JSONArray(blacklist));
// }
//
// @Override
// public EmbedBuilder getHelp(String prefix) {
// return EmbedUtils.getDefaultEmbed()
// .addField("Usage", String.format("`%s%s <action> <command(s)>`", prefix, this.getCmdName()), false)
// .addField("Argument", String.format("**action** - %s",
// FormatUtils.format(Action.class, "/")), false)
// .addField("Example", String.format("`%s%s add rule34 russian_roulette`", prefix, this.getCmdName()), false);
// }
//
// }
