package me.shadorc.shadbot.command.admin.setting;

import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;

import me.shadorc.shadbot.command.admin.setting.core.AbstractSetting;
import me.shadorc.shadbot.command.admin.setting.core.Setting;
import me.shadorc.shadbot.command.admin.setting.core.SettingEnum;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.db.DBGuild;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.util.EmbedBuilder;

@Setting(description = "Manage role(s) that can interact with Shadbot.", setting = SettingEnum.PERMISSIONS)
public class PermissionsSetting extends AbstractSetting {

	private enum Action {
		ADD, REMOVE;
	}

	@Override
	public void execute(Context context, String arg) throws MissingArgumentException, IllegalCmdArgumentException {
		if(arg == null) {
			throw new MissingArgumentException();
		}

		List<String> splitArgs = StringUtils.split(arg);
		if(splitArgs.size() != 2) {
			throw new MissingArgumentException();
		}

		Action action = Utils.getValueOrNull(Action.class, splitArgs.get(0));
		if(action == null) {
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid action. %s",
					splitArgs.get(0), FormatUtils.formatOptions(Action.class)));
		}

		List<IRole> mentionedRoles = context.getMessage().getRoleMentions();
		if(mentionedRoles.isEmpty()) {
			throw new MissingArgumentException();
		}

		DBGuild dbGuild = Database.getDBGuild(context.getGuild());
		List<Long> allowedRoles = dbGuild.getAllowedRoles();

		if(Action.ADD.equals(action)) {
			allowedRoles.addAll(mentionedRoles.stream().map(IRole::getLongID).collect(Collectors.toList()));
			BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " %s will now be able to interact with me.",
					FormatUtils.format(allowedRoles, role -> context.getGuild().getRoleByID(role).mention(), ", ")), context.getChannel());
		} else {
			allowedRoles.removeAll(mentionedRoles.stream().map(IRole::getLongID).collect(Collectors.toList()));
			StringBuilder text = new StringBuilder(String.format(Emoji.CHECK_MARK + " %s will not be able to interact with me anymore.",
					FormatUtils.format(mentionedRoles, IRole::mention, ", ")));
			if(allowedRoles.isEmpty()) {
				text.append("\n" + Emoji.INFO + " There are no more roles set, everyone can now interact with me.");
			}
			BotUtils.sendMessage(text.toString(), context.getChannel());
		}

		dbGuild.setSetting(SettingEnum.PERMISSIONS, new JSONArray(allowedRoles));
	}

	@Override
	public EmbedBuilder getHelp(String prefix) {
		return EmbedUtils.getDefaultEmbed()
				.appendField("Usage", String.format("`%s%s <action> <@role(s)>`", prefix, this.getCmdName()), false)
				.appendField("Argument", String.format("**action** - %s",
						FormatUtils.format(Action.values(), action -> action.toString().toLowerCase(), "/")), false)
				.appendField("Example", String.format("`%s%s add @admin`", prefix, this.getCmdName()), false)
				.appendField("Info", "By default, **administrators** will always be able to interact with Shadbot.", false);
	}

}
