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
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;

@Setting(description = "Manage auto assigned role(s).", setting = SettingEnum.AUTO_ASSIGN)
public class AutoAssignSetting extends AbstractSetting {

	private enum Action {
		SET, REMOVE;
	}

	@Override
	public void execute(Context context, String arg) throws MissingArgumentException, IllegalCmdArgumentException {
		if(!BotUtils.hasPermissions(context.getChannel(), Permissions.MANAGE_ROLES)) {
			BotUtils.sendMessage(TextUtils.missingPerm(Permissions.MANAGE_ROLES), context.getChannel());
			LogUtils.infof("{Guild ID: %d} Shadbot wasn't allowed to manage roles.", context.getGuild().getLongID());
			return;
		}

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

		List<IRole> roles = context.getMessage().getRoleMentions();
		if(roles.isEmpty()) {
			throw new MissingArgumentException();
		}

		DBGuild dbGuild = Database.getDBGuild(context.getGuild());
		if(Action.SET.equals(action)) {
			dbGuild.setSetting(SettingEnum.AUTO_ASSIGN, new JSONArray(roles.stream().map(IRole::getLongID).collect(Collectors.toList())));
			BotUtils.sendMessage(String.format("New comers will now have role(s): %s",
					FormatUtils.format(roles, IRole::getName, ", ")), context.getChannel());
		} else if(Action.REMOVE.equals(action)) {
			List<Long> oldRoles = dbGuild.getAutoAssignedRoles();
			oldRoles.removeAll(roles.stream().map(IRole::getLongID).collect(Collectors.toList()));
			dbGuild.setSetting(SettingEnum.AUTO_ASSIGN, new JSONArray(oldRoles));
			BotUtils.sendMessage(String.format("`%s` removed from auto-assigned roles.",
					FormatUtils.format(roles, IRole::getName, ", ")), context.getChannel());
		}
	}

	@Override
	public EmbedBuilder getHelp(String prefix) {
		return EmbedUtils.getDefaultEmbed()
				.appendField("Usage", String.format("`%s%s <action> <@role(s)>`", prefix, this.getCmdName()), false)
				.appendField("Argument", String.format("**action** - %s",
						FormatUtils.format(Action.values(), action -> action.toString().toLowerCase(), "/")), false)
				.appendField("Example", String.format("`%s%s set @newbie`", prefix, this.getCmdName()), false);
	}

}
