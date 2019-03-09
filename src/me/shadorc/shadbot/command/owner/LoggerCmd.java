package me.shadorc.shadbot.command.owner;

import java.util.List;
import java.util.function.Consumer;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

public class LoggerCmd extends BaseCmd {

	public LoggerCmd() {
		super(CommandCategory.OWNER, CommandPermission.OWNER, List.of("logger"));
	}

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = context.requireArgs(2);

		final String name = args.get(0);
		final Level level = Level.toLevel(args.get(1).toUpperCase(), null);
		if(level == null) {
			return Mono.error(new CommandException(String.format("`%s` in not a valid level.",
					args.get(1))));
		}

		Logger logger;
		if(name.equalsIgnoreCase("root")) {
			logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
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
