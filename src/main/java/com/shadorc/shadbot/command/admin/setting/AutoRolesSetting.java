package com.shadorc.shadbot.command.admin.setting;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.setting.BaseSetting;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.Settings;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.SettingHelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.EnumUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ImmutableEmbedFieldData;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AutoRolesSetting extends BaseSetting {

    private enum Action {
        ADD, REMOVE;
    }

    public AutoRolesSetting() {
        super(List.of("auto_roles", "auto_role"),
                Setting.AUTO_ROLES, "Manage auto assigned role(s).");
    }

    @Override
    public Mono<ImmutableEmbedFieldData> show(Context context, Settings settings) {
        return Flux.fromIterable(settings.getAutoRoleIds())
                .flatMap(roleId -> context.getClient().getRoleById(context.getGuildId(), roleId))
                .map(Role::getMention)
                .collectList()
                .filter(roles -> !roles.isEmpty())
                .map(roles -> String.join(", ", roles))
                .map(value -> ImmutableEmbedFieldData.builder()
                        .name("Auto-roles")
                        .value(value)
                        .build());
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(3);

        final Action action = EnumUtils.parseEnum(Action.class, args.get(1),
                new CommandException(String.format("`%s` is not a valid action. %s",
                        args.get(1), FormatUtils.options(Action.class))));

        return context.getGuild()
                .flatMapMany(guild -> DiscordUtils.extractRoles(guild, args.get(2)))
                .collectList()
                .map(mentionedRoles -> {
                    if (mentionedRoles.isEmpty()) {
                        throw new CommandException(String.format("Role `%s` not found.", args.get(2)));
                    }
                    return mentionedRoles;
                })
                .flatMap(mentionedRoles -> this.checkPermissions(context, mentionedRoles, action))
                .zipWith(DatabaseManager.getGuilds()
                        .getDBGuild(context.getGuildId()))
                .flatMap(TupleUtils.function((mentionedRoles, dbGuild) -> {
                    final List<Snowflake> mentionedRoleIds = mentionedRoles.stream()
                            .map(Role::getId)
                            .collect(Collectors.toList());
                    final Set<Snowflake> autoRoleIds = dbGuild.getSettings().getAutoRoleIds();

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

                    return dbGuild.updateSetting(this.getSetting(), autoRoleIds)
                            .thenReturn(strBuilder.toString());
                }))
                .flatMap(text -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(text, channel)))
                .then();
    }

    private Mono<List<Role>> checkPermissions(Context context, List<Role> roles, Action action) {
        if (action == Action.ADD) {
            return context.getChannel()
                    .flatMap(channel -> DiscordUtils.requirePermissions(channel, Permission.MANAGE_ROLES)
                            .thenReturn(roles.stream().map(Role::getId).collect(Collectors.toSet()))
                            .flatMap(roleIds -> context.getSelfAsMember()
                                    .filterWhen(self -> self.hasHigherRoles(roleIds))
                                    .switchIfEmpty(DiscordUtils.sendMessage(Emoji.WARNING +
                                            " I can't automatically add this role because I'm lower or " +
                                            "at the same level in the role hierarchy.", channel)
                                            .then(Mono.empty()))
                                    .map(ignored -> roles)));
        }
        return Mono.just(roles);
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return SettingHelpBuilder.create(this, context)
                .addArg("action", FormatUtils.format(Action.class, "/"), false)
                .addArg("role(s)", "role names", false)
                .setExample(String.format("`%s%s add @newbie`", context.getPrefix(), this.getCommandName()))
                .build();
    }

}
