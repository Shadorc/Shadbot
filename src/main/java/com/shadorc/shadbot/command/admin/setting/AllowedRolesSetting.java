package com.shadorc.shadbot.command.admin.setting;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.setting.BaseSetting;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.db.guilds.entity.Settings;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.SettingHelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AllowedRolesSetting extends BaseSetting {

    private enum Action {
        ADD, REMOVE;
    }

    public AllowedRolesSetting() {
        super(List.of("allowed_roles", "allowed_role"),
                Setting.ALLOWED_ROLES, "Manage role(s) that can interact with Shadbot.");
    }

    @Override
    public Flux<String> show(Context context, Settings settings) {
        return Flux.fromIterable(settings.getAllowedRoleIds())
                .flatMap(roleId -> context.getClient().getRoleById(context.getGuildId(), roleId))
                .map(Role::getMention)
                .collectList()
                .filter(roles -> !roles.isEmpty())
                .map(roles -> String.format("**Allowed roles:**%n\t%s",
                        String.join("\n\t", roles)))
                .flux();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(3);

        final Action action = Utils.parseEnum(Action.class, args.get(1),
                new CommandException(String.format("`%s` is not a valid action. %s",
                        args.get(1), FormatUtils.options(Action.class))));

        return context.getGuild()
                .flatMapMany(guild -> DiscordUtils.extractRoles(guild, args.get(2)))
                .collectList()
                .flatMap(mentionedRoles -> {
                    if (mentionedRoles.isEmpty()) {
                        return Mono.error(new CommandException(String.format("Role `%s` not found.", args.get(2))));
                    }

                    return Mono.zip(Mono.just(mentionedRoles),
                            DatabaseManager.getGuilds().getDBGuild(context.getGuildId()));
                })
                .flatMap(tuple -> {
                    final List<Role> mentionedRoles = tuple.getT1();
                    final DBGuild dbGuild = tuple.getT2();

                    final Set<Snowflake> allowedRoles = dbGuild.getSettings().getAllowedRoleIds();
                    final Set<Snowflake> mentionedRoleIds = mentionedRoles.stream()
                            .map(Role::getId)
                            .collect(Collectors.toUnmodifiableSet());

                    final StringBuilder strBuilder = new StringBuilder();
                    if (action == Action.ADD) {
                        allowedRoles.addAll(mentionedRoleIds);
                        strBuilder.append(String.format(Emoji.CHECK_MARK + " %s will now be able to interact with me.",
                                FormatUtils.format(mentionedRoles, role -> String.format("`@%s`", role.getName()), ", ")));
                    } else {
                        allowedRoles.removeAll(mentionedRoleIds);
                        strBuilder.append(String.format(Emoji.CHECK_MARK + " %s will not be able to interact with me anymore.",
                                FormatUtils.format(mentionedRoles, role -> String.format("`@%s`", role.getName()), ", ")));

                        if (allowedRoles.isEmpty()) {
                            strBuilder.append("\n" + Emoji.INFO + " There are no more allowed roles set, everyone "
                                    + "can now interact with me.");
                        }
                    }

                    return dbGuild.setSetting(this.getSetting(), allowedRoles)
                            .thenReturn(strBuilder);
                })
                .map(StringBuilder::toString)
                .flatMap(text -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(text, channel)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return SettingHelpBuilder.create(this, context)
                .addArg("action", FormatUtils.format(Action.class, "/"), false)
                .addArg("role(s)", "role names", false)
                .setExample(String.format("`%s%s add @role`", context.getPrefix(), this.getCommandName()))
                .addField("Info", "**server owner** and **administrators** "
                        + "will always be able to interact with Shadbot.", false)
                .build();
    }

}
