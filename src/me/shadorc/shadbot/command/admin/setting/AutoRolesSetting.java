package me.shadorc.shadbot.command.admin.setting;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.setting.AbstractSetting;
import me.shadorc.shadbot.core.setting.Setting;
import me.shadorc.shadbot.core.setting.SettingEnum;
import me.shadorc.shadbot.data.database.DBGuild;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

@Setting(description = "Manage auto assigned role(s).", setting = SettingEnum.AUTO_ROLES)
public class AutoRolesSetting extends AbstractSetting {

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

		final DBGuild dbGuild = Shadbot.getDatabase().getDBGuild(context.getGuildId());
		final List<Long> autoRoles = dbGuild.getAutoRoles();

		return context.getGuild()
				.flatMapMany(guild -> DiscordUtils.extractRoles(guild, args.get(2)))
				.flatMap(roleId -> context.getClient().getRoleById(context.getGuildId(), roleId))
				.collectList()
				.map(mentionedRoles -> {
					if(mentionedRoles.isEmpty()) {
						throw new CommandException(String.format("Role `%s` not found.", args.get(2)));
					}

					final List<Long> mentionedRoleIds = mentionedRoles.stream()
							.map(Role::getId)
							.map(Snowflake::asLong)
							.collect(Collectors.toList());

					final StringBuilder strBuilder = new StringBuilder();
					if(Action.ADD.equals(action)) {
						autoRoles.addAll(mentionedRoleIds);
						strBuilder.append(String.format(Emoji.CHECK_MARK + " %s added to auto-assigned roles.",
								FormatUtils.format(mentionedRoles, role -> String.format("`@%s`", role.getMention()), ", ")));
					} else {
						autoRoles.removeAll(mentionedRoleIds);
						strBuilder.append(String.format(Emoji.CHECK_MARK + " %s removed from auto-assigned roles.",
								FormatUtils.format(mentionedRoles, role -> String.format("`@%s`", role.getMention()), ", ")));
					}

					dbGuild.setSetting(this.getSetting(), autoRoles);
					return strBuilder.toString();
				})
				.flatMap(text -> context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(text, channel)))
				.then();
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return embed -> {
			EmbedUtils.getDefaultEmbed().accept(embed);
			embed.addField("Usage", String.format("`%s%s <action> <@role(s)>`", context.getPrefix(), this.getCommandName()), false)
					.addField("Argument", String.format("**action** - %s",
							FormatUtils.format(Action.class, "/")), false)
					.addField("Example", String.format("`%s%s add @newbie`", context.getPrefix(), this.getCommandName()), false);
		};
	}

}
