package com.shadorc.shadbot.command.owner;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.FormatUtil;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class LoggerCmd extends BaseCmd {

    private enum LogLevel {
        OFF,
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    public LoggerCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, "logger", "Change the level of a logger");
    }

    @Override
    public ApplicationCommandRequest build(ImmutableApplicationCommandRequest.Builder builder) {
        return builder
                .addOption(ApplicationCommandOptionData.builder()
                        .name("name")
                        .description("Can be `root` to change root logger")
                        .type(ApplicationCommandOptionType.STRING.getValue())
                        .required(true)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("level")
                        .description(FormatUtil.format(LogLevel.class, ", "))
                        .type(ApplicationCommandOptionType.STRING.getValue())
                        .required(true)
                        .build())
                .build();
    }

    @Override
    public Mono<?> execute(Context context) {
        final String name = context.getOption("name").orElseThrow();
        final String levelStr = context.getOption("level").orElseThrow();
        final Level level = Level.toLevel(levelStr.toUpperCase(), null);
        if (level == null) {
            return Mono.error(new CommandException(String.format("`%s` in not a valid level. %s",
                    levelStr, FormatUtil.options(LogLevel.class))));
        }

        final Logger logger;
        if ("root".equalsIgnoreCase(name)) {
            logger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        } else {
            logger = (Logger) LoggerFactory.getLogger(name);
        }

        logger.setLevel(level);

        return context.createFollowupMessage(Emoji.INFO + " Logger `%s` set to level `%s`.", name, level);
    }

}
