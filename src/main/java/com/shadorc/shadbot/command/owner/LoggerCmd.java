package com.shadorc.shadbot.command.owner;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import discord4j.rest.util.ApplicationCommandOptionType;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

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
        this.addOption(option -> option.name("name")
                .description("Can be 'root' to change root logger")
                .required(true)
                .type(ApplicationCommandOptionType.STRING.getValue()));
        this.addOption(option -> option.name("level")
                .description("The new logger level")
                .required(true)
                .type(ApplicationCommandOptionType.STRING.getValue())
                .choices(DiscordUtil.toOptions(LogLevel.class)));
    }

    @Override
    public Mono<?> execute(Context context) {
        final String name = context.getOptionAsString("name").orElseThrow();
        final LogLevel logLevel = context.getOptionAsEnum(LogLevel.class, "level").orElseThrow();
        final Level level = Level.toLevel(logLevel.name(), null);
        if (level == null) {
            return Mono.error(new CommandException("`%s` in not a valid level.".formatted(logLevel)));
        }

        final Logger logger;
        if ("root".equalsIgnoreCase(name)) {
            logger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        } else {
            logger = (Logger) LoggerFactory.getLogger(name);
        }

        logger.setLevel(level);

        DEFAULT_LOGGER.info("Logger '{}' set to level {}", name, level);
        return context.createFollowupMessage(Emoji.INFO, "Logger `%s` set to level `%s`.".formatted(name, level));
    }

}
