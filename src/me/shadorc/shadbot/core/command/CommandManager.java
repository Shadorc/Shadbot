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
import discord4j.core.spec.MessageCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.ratelimiter.RateLimiter;
import me.shadorc.shadbot.data.stats.CommandStatsManager;
import me.shadorc.shadbot.data.stats.CommandStatsManager.CommandEnum;
import me.shadorc.shadbot.data.stats.VariousStatsManager;
import me.shadorc.shadbot.data.stats.VariousStatsManager.VariousEnum;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.exception.NoPlayingMusicException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

public class CommandManager {

	private static final Map<String, AbstractCommand> COMMANDS_MAP = new HashMap<>();

	public static boolean init() {
		LogUtils.infof("Initializing commands...");

		Reflections reflections = new Reflections(Shadbot.class.getPackage().getName(), new SubTypesScanner(), new TypeAnnotationsScanner());
		for(Class<?> cmdClass : reflections.getTypesAnnotatedWith(Command.class)) {
			String cmdName = cmdClass.getSimpleName();
			if(!AbstractCommand.class.isAssignableFrom(cmdClass)) {
				LogUtils.error(String.format("An error occurred while generating command, %s cannot be casted to %s.",
						cmdName, AbstractCommand.class.getSimpleName()));
				continue;
			}

			try {
				AbstractCommand cmd = AbstractCommand.class.cast(cmdClass.getConstructor().newInstance());

				List<String> names = cmd.getNames();
				if(!cmd.getAlias().isEmpty()) {
					names.add(cmd.getAlias());
				}

				for(String name : names) {
					if(COMMANDS_MAP.putIfAbsent(name, cmd) != null) {
						LogUtils.error(String.format("Command name collision between %s and %s",
								cmdName, COMMANDS_MAP.get(name).getClass().getSimpleName()));
						continue;
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

	public static void execute(Context context) {
		AbstractCommand command = COMMANDS_MAP.get(context.getCommandName());
		if(command == null) {
			return;
		}

		Snowflake guildId = context.getGuildId().get();
		Snowflake channelId = context.getChannelId();
		Snowflake userId = context.getAuthorId();

		Predicate<? super CommandPermission> hasPermission = userPerm -> {
			if(command.getPermission().isSuperior(userPerm)) {
				BotUtils.sendMessage(Emoji.ACCESS_DENIED + " You do not have the permission to execute this command.", context.getChannel());
				return false;
			}
			return true;
		};

		Predicate<? super CommandPermission> isRateLimited = userPerm -> {
			Optional<RateLimiter> rateLimiter = command.getRateLimiter();
			if(!rateLimiter.isPresent()) {
				return false;
			}

			if(rateLimiter.get().isLimitedAndWarn(context.getClient(), guildId, channelId, userId)) {
				CommandStatsManager.log(CommandEnum.COMMAND_LIMITED, command);
				return true;
			}
			return false;
		};

		Mono.just(command)
				// The command is allowed in the guild
				.filter(cmd -> BotUtils.isCommandAllowed(guildId, cmd))
				.flatMap(cmd -> context.getAuthorPermission())
				// The author has the permission to execute this command
				.filter(hasPermission)
				// The user is not rate limited
				.filter(isRateLimited.negate())
				.subscribe(perm -> {
					try {
						command.execute(context);
						CommandStatsManager.log(CommandEnum.COMMAND_USED, command);
						VariousStatsManager.log(VariousEnum.COMMANDS_EXECUTED);
					} catch (RuntimeException err) {
						CommandManager.onError(context, command, err);
					}
				});
	}

	private static void onError(Context context, AbstractCommand command, RuntimeException exception) {
		Throwable err = exception.getCause() == null ? exception : exception.getCause();
		if(err instanceof IllegalCmdArgumentException) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + err.getMessage(), context.getChannel());
			CommandStatsManager.log(CommandEnum.COMMAND_ILLEGAL_ARG, command);
		} else if(err instanceof MissingArgumentException) {
			command.getHelp(context).subscribe(helpEmbed -> {
				BotUtils.sendMessage(new MessageCreateSpec()
						.setContent(TextUtils.MISSING_ARG)
						.setEmbed(helpEmbed), context.getChannel());
				CommandStatsManager.log(CommandEnum.COMMAND_MISSING_ARG, command);
			});
		} else if(err instanceof NoPlayingMusicException) {
			BotUtils.sendMessage(TextUtils.NO_PLAYING_MUSIC, context.getChannel());
		} else {
			LogUtils.error(context.getClient(), context.getContent(), err,
					String.format("{Guild ID: %d} An unknown error occurred while executing a command.", context.getGuildId().get()));
		}
	}

	public static Map<String, AbstractCommand> getCommands() {
		return COMMANDS_MAP;
	}

	public static AbstractCommand getCommand(String name) {
		return COMMANDS_MAP.get(name);
	}
}
