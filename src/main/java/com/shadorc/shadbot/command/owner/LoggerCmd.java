package com.shadorc.shadbot.command.owner;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.spec.EmbedCreateSpec;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class LoggerCmd extends BaseCmd {

    public LoggerCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, List.of("logger"));
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(2);

        final String name = args.get(0);
        final Level level = Level.toLevel(args.get(1).toUpperCase(), null);
        if (level == null) {
            return Mono.error(new CommandException(String.format("`%s` in not a valid level.",
                    args.get(1))));
        }

        final Logger logger;
        if ("root".equalsIgnoreCase(name)) {
            logger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        } else {
            logger = (Logger) LoggerFactory.getLogger(name);
        }

        logger.setLevel(level);

        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.INFO + " Logger `%s` set to level `%s`.", name, level), channel))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Change the level of a logger.")
                .addArg("name", false)
                .addArg("level", false)
                .build();
    }

}
