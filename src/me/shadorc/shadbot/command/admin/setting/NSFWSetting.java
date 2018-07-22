// TODO
// package me.shadorc.shadbot.command.admin.setting;
//
// import java.security.Permissions;
//
// import me.shadorc.shadbot.command.admin.setting.core.AbstractSetting;
// import me.shadorc.shadbot.command.admin.setting.core.Setting;
// import me.shadorc.shadbot.command.admin.setting.core.SettingEnum;
// import me.shadorc.shadbot.core.command.Context;
// import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
// import me.shadorc.shadbot.exception.MissingArgumentException;
// import me.shadorc.shadbot.utils.BotUtils;
// import me.shadorc.shadbot.utils.FormatUtils;
// import me.shadorc.shadbot.utils.TextUtils;
// import me.shadorc.shadbot.utils.Utils;
// import me.shadorc.shadbot.utils.embed.EmbedUtils;
// import me.shadorc.shadbot.utils.embed.log.LogUtils;
// import me.shadorc.shadbot.utils.object.Emoji;
//
// @Setting(description = "Manage current channel's NSFW state.", setting = SettingEnum.NSFW)
// public class NSFWSetting extends AbstractSetting {
//
// private enum Action {
// TOGGLE, ENABLE, DISABLE;
// }
//
// @Override
// public void execute(Context context, String arg) {
// if(!BotUtils.hasPermissions(context.getChannel(), Permissions.MANAGE_CHANNELS)) {
// BotUtils.sendMessage(TextUtils.missingPerm(Permissions.MANAGE_CHANNEL), context.getChannel());
// LogUtils.infof("{Guild ID: %d} Shadbot wasn't allowed to manage channel.", context.getGuild().getLongID());
// return;
// }
//
// context.requireArg();
//
// Action action = Utils.getValueOrNull(Action.class, arg);
// if(action == null) {
// throw new IllegalCmdArgumentException(String.format("`%s` is not a valid action. %s",
// arg, FormatUtils.formatOptions(Action.class)));
// }
//
// boolean isNSFW = false;
// switch (action) {
// case TOGGLE:
// isNSFW = !context.getChannel().isNSFW();
// break;
// case ENABLE:
// isNSFW = true;
// break;
// case DISABLE:
// isNSFW = false;
// break;
// }
//
// context.getChannel().changeNSFW(isNSFW);
// BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " This channel is now %sSFW.", isNSFW ? "N" : ""), context.getChannel());
// }
//
// @Override
// public EmbedBuilder getHelp(String prefix) {
// return EmbedUtils.getDefaultEmbed()
// .addField("Usage", String.format("`%s%s <action>`", prefix, this.getCmdName()), false)
// .addField("Argument", String.format("**action** - %s",
// FormatUtils.format(Action.class, "/")), false)
// .addField("Example", String.format("`%s%s toggle`", prefix, this.getCmdName()), false);
// }
//
// }
