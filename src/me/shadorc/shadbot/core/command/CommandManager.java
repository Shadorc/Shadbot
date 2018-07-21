package me.shadorc.shadbot.core.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.ExceptionHandler;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.ratelimiter.RateLimiter;
import me.shadorc.shadbot.data.stats.CommandStatsManager;
import me.shadorc.shadbot.data.stats.CommandStatsManager.CommandEnum;
import me.shadorc.shadbot.data.stats.VariousStatsManager;
import me.shadorc.shadbot.data.stats.VariousStatsManager.VariousEnum;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Mono;

public class CommandManager {

	private static final Map<String, AbstractCommand> COMMANDS_MAP = new HashMap<>();

	public static boolean init() {
		Reflections reflections = new Reflections(Shadbot.class.getPackage().getName(), new SubTypesScanner(), new TypeAnnotationsScanner());
		for(Class<?> cmdClass : reflections.getTypesAnnotatedWith(Command.class)) {
			String cmdName = cmdClass.getSimpleName();
			if(!AbstractCommand.class.isAssignableFrom(cmdClass)) {
				LogUtils.error(String.format("An error occurred while generating command: %s cannot be casted to %s.",
						cmdName, AbstractCommand.class.getSimpleName()));
				continue;
			}

			try {
				AbstractCommand cmd = (AbstractCommand) cmdClass.getConstructor().newInstance();

				List<String> names = cmd.getNames();
				if(!cmd.getAlias().isEmpty()) {
					names.add(cmd.getAlias());
				}

				for(String name : names) {
					if(COMMANDS_MAP.putIfAbsent(name, cmd) != null) {
						LogUtils.error(String.format("Command name collision between %s and %s.",
								cmdName, COMMANDS_MAP.get(name).getClass().getSimpleName()));
					}
				}
			} catch (Exception err) {
				LogUtils.error(err, String.format("An error occurred while initializing command %s.", cmdName));
				return false;
			}
		}

		LogUtils.infof("%s initialized.", StringUtils.pluralOf(COMMANDS_MAP.values().stream().distinct().count(), "command"));
		return true;
	}

	public static Mono<Void> execute(Context context) {
		AbstractCommand command = COMMANDS_MAP.get(context.getCommandName());
		if(command == null) {
			return Mono.empty();
		}

		final Snowflake guildId = context.getGuildId();

		final Predicate<? super CommandPermission> hasPermission = userPerm -> {
			if(command.getPermission().isSuperior(userPerm)) {
				BotUtils.sendMessage(Emoji.ACCESS_DENIED + " You do not have the permission to execute this command.", context.getChannel()).subscribe();
				return false;
			}
			return true;
		};

		final Predicate<? super AbstractCommand> isRateLimited = cmd -> {
			Optional<RateLimiter> rateLimiter = cmd.getRateLimiter();
			if(!rateLimiter.isPresent()) {
				return false;
			}

			if(rateLimiter.get().isLimitedAndWarn(context.getClient(), guildId, context.getChannelId(), context.getAuthorId())) {
				CommandStatsManager.log(CommandEnum.COMMAND_LIMITED, cmd);
				return true;
			}
			return false;
		};

		return context.getPermission()
				// The author has the permission to execute this command
				.filter(hasPermission)
				.flatMap(perm -> Mono.just(command))
				// The command is allowed in the guild
				.filter(cmd -> BotUtils.isCommandAllowed(guildId, cmd))
				// The user is not rate limited
				.filter(isRateLimited.negate())
				.flatMap(cmd -> cmd.execute(context))
				.onErrorResume(err -> new ExceptionHandler(err, command, context).handle().then())
				.doOnSuccess(perm -> {
					CommandStatsManager.log(CommandEnum.COMMAND_USED, command);
					VariousStatsManager.log(VariousEnum.COMMANDS_EXECUTED);
				});
	}

	public static Map<String, AbstractCommand> getCommands() {
		return COMMANDS_MAP;
	}

	public static AbstractCommand getCommand(String name) {
		return CommandManager.getCommands().get(name);
	}
}
