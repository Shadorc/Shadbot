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

public class AllowedRolesSetting extends BaseSetting {

    private enum Action {
        ADD, REMOVE;
    }

    public AllowedRolesSetting() {
        super(Setting.ALLOWED_ROLES, "Manage role(s) that can interact with Shadbot.");
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

                    final List<Snowflake> allowedRoles = dbGuild.getSettings().getAllowedRoleIds();
                    final List<Snowflake> mentionedRoleIds = mentionedRoles.stream()
                            .map(Role::getId)
                            .collect(Collectors.toList());

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
                            strBuilder.append("\n" + Emoji.INFO + " There are no more allowed roles set, everyone can now interact with me.");
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
        return DiscordUtils.getDefaultEmbed()
                .andThen(embed -> embed.addField("Usage", String.format("`%s%s <action> <role(s)>`", context.getPrefix(), this.getCommandName()), false)
                        .addField("Argument", String.format("**action** - %s",
                                FormatUtils.format(Action.class, "/")), false)
                        .addField("Example", String.format("`%s%s add @role`", context.getPrefix(), this.getCommandName()), false)
                        .addField("Info", "By default, **server owner** and **administrators** will always be able to interact with Shadbot.", false));
    }

}
