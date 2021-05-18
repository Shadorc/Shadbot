package com.shadorc.shadbot.command.setting;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.*;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import com.shadorc.shadbot.utils.EnumUtil;
import com.shadorc.shadbot.utils.FormatUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RestrictedChannelsSetting extends BaseCmd {

    private enum Action {
        ADD, REMOVE
    }

    private enum Type {
        COMMAND, CATEGORY
    }

    public RestrictedChannelsSetting() {
        super(CommandCategory.SETTING, CommandPermission.ADMIN,
                "restricted_channels", "Restrict commands to specific channels");

        this.addOption(option -> option.name("action")
                .description("Whether to add or remove a channel from the restricted ones")
                .required(true)
                .type(ApplicationCommandOptionType.STRING.getValue())
                .choices(DiscordUtil.toOptions(Action.class)));
        this.addOption(option -> option.name("type")
                .description("Restrict a command or a category")
                .required(true)
                .type(ApplicationCommandOptionType.STRING.getValue())
                .choices(DiscordUtil.toOptions(Type.class)));
        this.addOption(option -> option.name("name")
                .description("The name of the command/category")
                .required(true)
                .type(ApplicationCommandOptionType.STRING.getValue()));
        this.addOption(option -> option.name("channel")
                .description("The channel")
                .required(true)
                .type(ApplicationCommandOptionType.CHANNEL.getValue()));
    }

    @Override
    public Mono<?> execute(Context context) {
        final Action action = context.getOptionAsEnum(Action.class, "action").orElseThrow();
        final Type type = context.getOptionAsEnum(Type.class, "type").orElseThrow();
        final String name = context.getOptionAsString("name").orElseThrow();
        final Mono<TextChannel> getChannel = context.getOptionAsChannel("channel")
                .ofType(TextChannel.class);

        final Set<BaseCmd> commands = new HashSet<>();
        switch (type) {
            case COMMAND -> {
                final BaseCmd command = CommandManager.getCommand(name);
                if (command == null || command instanceof BaseCmdGroup) {
                    return Mono.error(new CommandException(context.localize("restrictedchannels.invalid.command")
                            .formatted(name)));
                }

                commands.addAll(command.getCommands());
            }
            case CATEGORY -> {
                final CommandCategory category = EnumUtil.parseEnum(CommandCategory.class, name);
                if (category == null) {
                    return Mono.error(new CommandException(context.localize("restrictedchannels.invalid.category")
                            .formatted(name, FormatUtil.format(CommandCategory.class, FormatUtil::capitalizeEnum, ", "))));
                }
                commands.addAll(CommandManager.getCommands().values().stream()
                        .filter(cmd -> cmd.getCategory() == category)
                        .flatMap(cmd -> cmd.getCommands().stream())
                        .collect(Collectors.toSet()));
            }
        }

        return getChannel
                .switchIfEmpty(Mono.error(new CommandException(context.localize("restrictedchannels.exception.category"))))
                .flatMap(channel -> {
                    final StringBuilder strBuilder = new StringBuilder();
                    final Map<Snowflake, Set<BaseCmd>> restrictedCategories = context.getDbGuild().getSettings()
                            .getRestrictedChannels();

                    switch (action) {
                        case ADD -> {
                            if (!restrictedCategories.computeIfAbsent(channel.getId(), __ -> new HashSet<>()).addAll(commands)) {
                                return context.createFollowupMessage(Emoji.GREY_EXCLAMATION,
                                        context.localize("restrictedchannels.already.added"));
                            }
                            strBuilder.append(context.localize("restrictedchannels.added")
                                    .formatted(FormatUtil.format(commands, cmd -> "`%s`".formatted(cmd.getName()), " "),
                                            channel.getMention()));
                        }
                        case REMOVE -> {
                            if (!restrictedCategories.containsKey(channel.getId())
                                    || !restrictedCategories.get(channel.getId()).removeAll(commands)) {
                                return context.createFollowupMessage(Emoji.GREY_EXCLAMATION,
                                        context.localize("restrictedchannels.already.removed"));
                            }
                            strBuilder.append(context.localize("restrictedchannels.removed")
                                    .formatted(FormatUtil.format(commands, cmd -> "`%s`".formatted(cmd.getName()), " ")));
                        }
                    }

                    final Map<String, Set<String>> setting = restrictedCategories
                            .entrySet()
                            .stream()
                            .collect(Collectors.toMap(
                                    entry -> entry.getKey().asString(),
                                    entry -> entry.getValue().stream()
                                            .map(BaseCmd::getName)
                                            .collect(Collectors.toSet())));

                    return context.getDbGuild().updateSetting(Setting.RESTRICTED_CHANNELS, setting)
                            .then(context.createFollowupMessage(Emoji.CHECK_MARK, strBuilder.toString()));
                });
    }
}
