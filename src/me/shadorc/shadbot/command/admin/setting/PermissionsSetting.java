package me.shadorc.shadbot.command.admin.setting;

import java.util.List;
import java.util.Set;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.setting.AbstractSetting;
import me.shadorc.shadbot.core.setting.Setting;
import me.shadorc.shadbot.core.setting.SettingEnum;
import me.shadorc.shadbot.data.database.DBGuild;
import me.shadorc.shadbot.data.database.DatabaseManager;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Setting(description = "Manage role(s) that can interact with Shadbot.", setting = SettingEnum.PERMISSIONS)
public class PermissionsSetting extends AbstractSetting {

	private enum Action {
		ADD, REMOVE;
	}

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = context.requireArgs(3);

		final Action action = Utils.getEnum(Action.class, args.get(1));
		if(action == null) {
			throw new CommandException(String.format("`%s` is not a valid action. %s", args.get(1), FormatUtils.options(Action.class)));
		}

		final Set<Snowflake> mentionedRoles = context.getMessage().getRoleMentionIds();
		if(mentionedRoles.isEmpty()) {
			throw new MissingArgumentException();
		}

		final DBGuild dbGuild = DatabaseManager.getDBGuild(context.getGuildId());
		final List<Snowflake> allowedRoles = dbGuild.getAllowedRoles();

		Mono<Message> message;
		if(Action.ADD.equals(action)) {
			allowedRoles.addAll(mentionedRoles);
			message = Flux.fromIterable(allowedRoles)
					.flatMap(roleId -> context.getClient().getRoleById(context.getGuildId(), roleId))
					.map(Role::getName)
					.collectList()
					.flatMap(roleNames -> BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " %s will now be able to interact with me.",
							String.join(", ", roleNames)), context.getChannel()));
		} else {
			allowedRoles.removeAll(mentionedRoles);
			message = Flux.fromIterable(mentionedRoles)
					.flatMap(roleId -> context.getClient().getRoleById(context.getGuildId(), roleId))
					.map(Role::getName)
					.collectList()
					.map(roleNames -> {
						final StringBuilder text = new StringBuilder(String.format(Emoji.CHECK_MARK + " %s will not be able to interact with me anymore.",
								String.join(", ", roleNames)));
						if(roleNames.isEmpty()) {
							text.append("\n" + Emoji.INFO + " There are no more roles set, everyone can now interact with me.");
						}
						return text.toString();
					})
					.flatMap(text -> BotUtils.sendMessage(text, context.getChannel()));
		}

		dbGuild.setSetting(this.getSetting(), allowedRoles);
		return message.then();
	}

	@Override
	public EmbedCreateSpec getHelp(Context context) {
		return EmbedUtils.getDefaultEmbed()
				.addField("Usage", String.format("`%s%s <action> <@role(s)>`", context.getPrefix(), this.getCommandName()), false)
				.addField("Argument", String.format("**action** - %s",
						FormatUtils.format(Action.class, "/")), false)
				.addField("Example", String.format("`%s%s add @admin`", context.getPrefix(), this.getCommandName()), false)
				.addField("Info", "By default, **administrators** will always be able to interact with Shadbot.", false);
	}

}
