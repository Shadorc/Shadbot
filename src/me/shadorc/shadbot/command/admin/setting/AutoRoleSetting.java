package me.shadorc.shadbot.command.admin.setting;

import java.util.List;
import java.util.Set;

import org.json.JSONArray;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.setting.AbstractSetting;
import me.shadorc.shadbot.core.setting.Setting;
import me.shadorc.shadbot.core.setting.SettingEnum;
import me.shadorc.shadbot.data.db.DBGuild;
import me.shadorc.shadbot.data.db.DatabaseManager;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Setting(description = "Manage auto assigned role(s).", setting = SettingEnum.AUTO_ROLE)
public class AutoRoleSetting extends AbstractSetting {

	private enum Action {
		ADD, REMOVE;
	}

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = this.requireArg(context, 2);

		final Action action = Utils.getEnum(Action.class, args.get(0));
		if(action == null) {
			throw new CommandException(String.format("`%s` is not a valid action. %s", args.get(0), FormatUtils.formatOptions(Action.class)));
		}

		final Set<Snowflake> mentionedRoles = context.getMessage().getRoleMentionIds();
		if(mentionedRoles.isEmpty()) {
			throw new MissingArgumentException();
		}

		final DBGuild dbGuild = DatabaseManager.getDBGuild(context.getGuildId());
		final List<Snowflake> autoRoles = dbGuild.getAutoRoles();

		Mono<Message> message;
		if(Action.ADD.equals(action)) {
			autoRoles.addAll(mentionedRoles);
			message = Flux.fromIterable(autoRoles)
					.flatMap(roleId -> context.getClient().getRoleById(context.getGuildId(), roleId))
					.map(Role::getName)
					.collectList()
					.flatMap(roles -> BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " New comers will now have role(s): %s",
							String.join(", ", roles)), context.getChannel()));
		} else {
			autoRoles.removeAll(mentionedRoles);
			message = Flux.fromIterable(mentionedRoles)
					.flatMap(roleId -> context.getClient().getRoleById(context.getGuildId(), roleId))
					.map(Role::getName)
					.collectList()
					.flatMap(roles -> BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " %s removed from auto-assigned roles.",
							String.join(", ", roles)), context.getChannel()));
		}

		dbGuild.setSetting(this.getSetting(), new JSONArray(autoRoles));
		return message.then();
	}

	@Override
	public EmbedCreateSpec getHelp(Context context) {
		return EmbedUtils.getDefaultEmbed()
				.addField("Usage", String.format("`%s%s <action> <@role(s)>`", context.getPrefix(), this.getCommandName()), false)
				.addField("Argument", String.format("**action** - %s",
						FormatUtils.format(Action.class, "/")), false)
				.addField("Example", String.format("`%s%s add @newbie`", context.getPrefix(), this.getCommandName()), false);
	}

}
