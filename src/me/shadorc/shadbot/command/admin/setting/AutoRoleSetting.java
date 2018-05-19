package me.shadorc.shadbot.command.admin.setting;

import java.security.Permissions;
import java.util.Arrays;
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
import me.shadorc.shadbot.utils.object.Emoji;

@Setting(description = "Manage auto assigned role(s).", setting = SettingEnum.AUTO_ROLE)
public class AutoRoleSetting extends AbstractSetting {

	private enum Action {
		ADD, REMOVE;
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

		List<IRole> mentionedRoles = context.getMessage().getRoleMentions();
		if(mentionedRoles.isEmpty()) {
			throw new MissingArgumentException();
		}

		DBGuild dbGuild = Database.getDBGuild(context.getGuild());
		List<Long> roles = dbGuild.getAutoRoles();

		if(Action.ADD.equals(action)) {
			for(IRole role : mentionedRoles) {
				if(!PermissionUtils.hasHierarchicalPermissions(context.getGuild(), context.getSelf(), Arrays.asList(role))) {
					throw new IllegalCmdArgumentException(String.format("%s is a higher role in the role hierarchy than mine, I can't auto-assign it.",
							role.mention()));
				}
			}

			roles.addAll(mentionedRoles.stream().map(IRole::getLongID).collect(Collectors.toList()));
			BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " New comers will now have role(s): %s",
					FormatUtils.format(roles, role -> context.getGuild().getRoleByID(role).mention(), ", ")), context.getChannel());
		} else {
			roles.removeAll(mentionedRoles.stream().map(IRole::getLongID).collect(Collectors.toList()));
			BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " %s removed from auto-assigned roles.",
					FormatUtils.format(mentionedRoles, IRole::mention, ", ")), context.getChannel());
		}

		dbGuild.setSetting(SettingEnum.AUTO_ROLE, new JSONArray(roles));
	}

	@Override
	public EmbedBuilder getHelp(String prefix) {
		return EmbedUtils.getDefaultEmbed()
				.addField("Usage", String.format("`%s%s <action> <@role(s)>`", prefix, this.getCmdName()), false)
				.addField("Argument", String.format("**action** - %s",
						FormatUtils.format(Action.values(), action -> action.toString().toLowerCase(), "/")), false)
				.addField("Example", String.format("`%s%s add @newbie`", prefix, this.getCmdName()), false);
	}

}
