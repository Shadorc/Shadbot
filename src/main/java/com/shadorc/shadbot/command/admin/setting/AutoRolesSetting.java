package com.shadorc.shadbot.command.admin.setting;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.setting.BaseSetting;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AutoRolesSetting extends BaseSetting {

    private enum Action {
        ADD, REMOVE;
    }

    public AutoRolesSetting() {
        super(Setting.AUTO_ROLES, "Manage auto assigned role(s).");
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(3);

        final Action action = Utils.parseEnum(Action.class, args.get(1),
                new CommandException(String.format("`%s` is not a valid action. %s",
                        args.get(1), FormatUtils.options(Action.class))));

        return context.getGuild()
                .flatMapMany(guild -> DiscordUtils.extractRoles(guild, args.get(2)))
                .flatMap(roleId -> context.getClient().getRoleById(context.getGuildId(), roleId))
                .collectList()
                .map(mentionedRoles -> {
                    if (mentionedRoles.isEmpty()) {
                        throw new CommandException(String.format("Role `%s` not found.", args.get(2)));
                    }
                    return mentionedRoles;
                })
                .zipWith(DatabaseManager.getGuilds()
                        .getDBGuild(context.getGuildId()))
                .flatMap(tuple -> {
                    final List<Role> mentionedRoles = tuple.getT1();
                    final DBGuild dbGuild = tuple.getT2();

                    final List<Snowflake> mentionedRoleIds = mentionedRoles.stream()
                            .map(Role::getId)
                            .collect(Collectors.toList());
                    final List<Snowflake> autoRoleIds = dbGuild.getSettings().getAutoRoleIds();

                    final StringBuilder strBuilder = new StringBuilder();
                    if (action == Action.ADD) {
                        autoRoleIds.addAll(mentionedRoleIds);
                        strBuilder.append(String.format(Emoji.CHECK_MARK + " %s added to auto-assigned roles.",
                                FormatUtils.format(mentionedRoles, role -> String.format("`@%s`", role.getName()), ", ")));
                    } else {
                        autoRoleIds.removeAll(mentionedRoleIds);
                        strBuilder.append(String.format(Emoji.CHECK_MARK + " %s removed from auto-assigned roles.",
                                FormatUtils.format(mentionedRoles, role -> String.format("`@%s`", role.getName()), ", ")));
                    }

                    return dbGuild.setSetting(this.getSetting(), autoRoleIds)
                            .thenReturn(strBuilder.toString());
                })
                .flatMap(text -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(text, channel)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return DiscordUtils.getDefaultEmbed()
                .andThen(embed -> embed.addField("Usage", String.format("`%s%s <action> <@role(s)>`", context.getPrefix(), this.getCommandName()), false)
                        .addField("Argument", String.format("**action** - %s",
                                FormatUtils.format(Action.class, "/")), false)
                        .addField("Example", String.format("`%s%s add @newbie`", context.getPrefix(), this.getCommandName()), false));
    }

}
