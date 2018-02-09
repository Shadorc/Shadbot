package me.shadorc.shadbot.core.command;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.data.stats.Stats.CommandEnum;
import me.shadorc.shadbot.data.stats.Stats.VariousEnum;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import sx.blah.discord.util.MessageBuilder;

public class CommandManager {

	private static final Map<String, AbstractCommand> COMMANDS_MAP = new HashMap<>();

	public static boolean init() {
		LogUtils.infof("Initializing commands...");

		Reflections reflections = new Reflections(Shadbot.class.getPackage().getName(), new SubTypesScanner(), new TypeAnnotationsScanner());
		for(Class<?> cmdClass : reflections.getTypesAnnotatedWith(Command.class)) {
			if(!AbstractCommand.class.isAssignableFrom(cmdClass)) {
				LogUtils.error(String.format("An error occurred while generating command, %s cannot be cast to AbstractCommand.",
						cmdClass.getSimpleName()));
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
						LogUtils.error(String.format("Command name collision between %s and %s",
								cmdClass.getSimpleName(), COMMANDS_MAP.get(name).getClass().getSimpleName()));
						continue;
					}
				}
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException err) {
				LogUtils.error(err, String.format("An error occurred while initializing command %s.",
						cmdClass.getDeclaringClass().getSimpleName()));
				return false;
			}
		}

		LogUtils.infof("%s initialized.", StringUtils.pluralOf((int) COMMANDS_MAP.values().stream().distinct().count(), "command"));
		return true;
	}

	public static void execute(Context context) {
		AbstractCommand cmd = COMMANDS_MAP.get(context.getCommandName());
		if(cmd == null) {
			return;
		}

		if(!BotUtils.isCommandAllowed(context.getGuild(), cmd)) {
			return;
		}

		CommandPermission authorPermission = context.getAuthorPermission();
		if(cmd.getPermission().isSuperior(authorPermission)) {
			BotUtils.sendMessage(Emoji.ACCESS_DENIED + " You do not have the permission to execute this command.", context.getChannel());
			return;
		}

		if(cmd.getRateLimiter() != null && cmd.getRateLimiter().isLimited(context.getChannel(), context.getAuthor())) {
			StatsManager.increment(CommandEnum.COMMAND_LIMITED, cmd.getName());
			return;
		}

		try {
			cmd.execute(context);
			StatsManager.increment(CommandEnum.COMMAND_USED, cmd.getName());
			StatsManager.increment(VariousEnum.COMMANDS_EXECUTED);
		} catch (IllegalCmdArgumentException err) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + err.getMessage(), context.getChannel());
			StatsManager.increment(CommandEnum.COMMAND_ILLEGAL_ARG, cmd.getName());
		} catch (MissingArgumentException err) {
			BotUtils.sendMessage(new MessageBuilder(context.getClient())
					.withChannel(context.getChannel())
					.withContent(TextUtils.MISSING_ARG)
					.withEmbed(cmd.getHelp(context.getPrefix())));
			StatsManager.increment(CommandEnum.COMMAND_MISSING_ARG, cmd.getName());
		}
	}

	public static Map<String, AbstractCommand> getCommands() {
		return COMMANDS_MAP;
	}

	public static AbstractCommand getCommand(String name) {
		return COMMANDS_MAP.get(name);
	}
}
