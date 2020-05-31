package com.shadorc.shadbot.command.admin.setting;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandManager;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RestrictedRolesSetting extends BaseSetting {

    private enum Action {
        ADD, REMOVE;
    }

    private enum Type {
        COMMAND, CATEGORY;
    }

    public RestrictedRolesSetting() {
        super(List.of("restricted_roles", "restricted_ole"),
                Setting.RESTRICTED_ROLES, "Restrict commands to specific roles.");
    }

    @Override
    public Mono<ImmutableEmbedFieldData> show(Context context, Settings settings) {
        return Flux.fromIterable(settings.getRestrictedRoles().entrySet())
                .flatMap(entry -> Mono.zip(
                        context.getClient().getRoleById(context.getGuildId(), entry.getKey()).map(Role::getMention),
                        Mono.just(FormatUtils.format(entry.getValue(), BaseCmd::getName, ", "))))
                .map(TupleUtils.function((roleMention, cmds) -> String.format("%s: %s", roleMention, cmds)))
                .reduce("", (value, text) -> value + "\n" + text)
                .filter(value -> !value.isBlank())
                .map(value -> ImmutableEmbedFieldData.builder()
                        .name("Restricted roles")
                        .value(value)
                        .build());
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(5);

        final Action action = EnumUtils.parseEnum(Action.class, args.get(1),
                new CommandException(String.format("`%s` is not a valid action. %s",
                        args.get(1), FormatUtils.options(Action.class))));

        final Type type = EnumUtils.parseEnum(Type.class, args.get(2),
                new CommandException(String.format("`%s` is not a valid type. %s",
                        args.get(2), FormatUtils.options(Type.class))));

        final String name = args.get(3);
        final Set<BaseCmd> commands = new HashSet<>();
        switch (type) {
            case COMMAND:
                final BaseCmd command = CommandManager.getInstance().getCommand(name);
                if (command == null) {
                    return Mono.error(new CommandException(String.format("`%s` is not a valid command.", name)));
                }
                commands.add(command);
                break;
            case CATEGORY:
                final CommandCategory category = EnumUtils.parseEnum(CommandCategory.class, name,
                        new CommandException(String.format("`%s` is not a valid category. %s",
                                name, FormatUtils.options(CommandCategory.class))));
                commands.addAll(CommandManager.getInstance().getCommands().values().stream()
                        .filter(cmd -> cmd.getCategory() == category)
                        .collect(Collectors.toSet()));
                break;
        }

        return context.getGuild()
                .flatMapMany(guild -> DiscordUtils.extractRoles(guild, args.get(4)))
                .collectList()
                .flatMap(mentionedRoles -> {
                    if (mentionedRoles.isEmpty()) {
                        return Mono.error(new CommandException(String.format("Role `%s` not found.", args.get(4))));
                    }
                    return Mono.zip(Mono.just(mentionedRoles.get(0)),
                            DatabaseManager.getGuilds().getDBGuild(context.getGuildId()));
                })
                .flatMap(TupleUtils.function((mentionedRole, dbGuild) -> {
                    final StringBuilder strBuilder = new StringBuilder();
                    final Map<Snowflake, Set<BaseCmd>> restrictedRoles = dbGuild.getSettings()
                            .getRestrictedRoles();

                    switch (action) {
                        case ADD:
                            restrictedRoles.computeIfAbsent(mentionedRole.getId(), ignored -> new HashSet<>())
                                    .addAll(commands);
                            strBuilder.append(
                                    String.format("Command(s) %s can now only be used by role **%s**.",
                                            FormatUtils.format(commands, cmd -> String.format("`%s`", cmd.getName()), " "),
                                            mentionedRole.getName()));
                            break;
                        case REMOVE:
                            if (restrictedRoles.containsKey(mentionedRole.getId())) {
                                restrictedRoles.get(mentionedRole.getId()).removeAll(commands);
                            }
                            strBuilder.append(
                                    String.format("Command(s) %s can now be used everywhere.",
                                            FormatUtils.format(commands, cmd -> String.format("`%s`", cmd.getName()), " ")));
                            break;
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
                }))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return SettingHelpBuilder.create(this, context)
                .addArg("action", FormatUtils.format(Action.class, "/"), false)
                .addArg("type", FormatUtils.format(Type.class, "/"), false)
                .addArg("name", "command/category name", false)
                .addArg("role", String.format("the role to %s", FormatUtils.format(Action.class, "/")), false)
                .setExample(String.format("`%s%s add command play @admin`" +
                                "%n`%s%s add category music @dj`",
                        context.getPrefix(), this.getCommandName(), context.getPrefix(), this.getCommandName()))
                .build();
    }
}
