package com.shadorc.shadbot.command.admin.setting;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandManager;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.setting.BaseSetting;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RestrictedRolesSetting extends BaseSetting {

    private enum Action {
        ADD, REMOVE;
    }

    public RestrictedRolesSetting() {
        super(List.of("restricted_roles", "restricted_ole"),
                Setting.RESTRICTED_ROLES, "Restrict commands to specific roles.");
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(4);

        final Action action = Utils.parseEnum(Action.class, args.get(1),
                new CommandException(String.format("`%s` is not a valid action. %s",
                        args.get(1), FormatUtils.options(Action.class))));

        final BaseCmd cmd = CommandManager.getInstance().getCommand(args.get(2));
        if (cmd == null) {
            return Mono.error(new CommandException(String.format("`%s` is not a valid command.", args.get(2))));
        }

        return context.getGuild()
                .flatMapMany(guild -> DiscordUtils.extractRoles(guild, args.get(3)))
                .collectList()
                .flatMap(mentionedRoles -> {
                    if (mentionedRoles.isEmpty()) {
                        return Mono.error(new CommandException(String.format("Role `%s` not found.", args.get(3))));
                    }
                    return Mono.zip(Mono.just(mentionedRoles.get(0)),
                            DatabaseManager.getGuilds().getDBGuild(context.getGuildId()));
                })
                .flatMap(tuple -> {
                    final Role mentionedRole = tuple.getT1();
                    final DBGuild dbGuild = tuple.getT2();

                    final StringBuilder strBuilder = new StringBuilder();
                    final Map<Snowflake, Set<BaseCmd>> restrictedRoles = dbGuild.getSettings()
                            .getRestrictedRoles();

                    switch (action) {
                        case ADD:
                            restrictedRoles.computeIfAbsent(mentionedRole.getId(),
                                    ignored -> Collections.emptySet())
                                    .add(cmd);
                            strBuilder.append(
                                    String.format("The command `%s` can now be only used by role **%s**.",
                                            cmd.getName(), mentionedRole.getName()));
                            break;
                        case REMOVE:
                            if (restrictedRoles.containsKey(mentionedRole.getId())) {
                                restrictedRoles.get(mentionedRole.getId()).remove(cmd);
                            }
                            strBuilder.append(
                                    String.format("The command category `%s` can now be used by everyone.", cmd.getName()));
                            break;
                        default:
                            throw new IllegalStateException(String.format("Unknown action: %s", action));
                    }

                    final Map<String, Set<String>> setting = restrictedRoles
                            .entrySet()
                            .stream()
                            .collect(Collectors.toMap(
                                    entry -> entry.getKey().asString(),
                                    entry -> entry.getValue().stream()
                                            .map(BaseCmd::getName)
                                            .collect(Collectors.toSet())));

                    return dbGuild.setSetting(Setting.RESTRICTED_ROLES, setting)
                            .and(context.getChannel()
                                    .flatMap(channel -> DiscordUtils.sendMessage(Emoji.CHECK_MARK + " " + strBuilder, channel)));
                })
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return DiscordUtils.getDefaultEmbed()
                .andThen(embed -> embed.addField("Usage", String.format("`%s%s <action> <command> <role>`",
                        context.getPrefix(), this.getCommandName()), false)
                        .addField("Argument",
                                String.format("**action** - %s", FormatUtils.format(Action.class, "/"))
                                        + "%n**command** - command's name"
                                        + String.format("%n**channel** - the role to %s", FormatUtils.format(Action.class, "/")),
                                false)
                        .addField("Example", String.format("`%s%s add play @admin`",
                                context.getPrefix(), this.getCommandName()), false));
    }
}
