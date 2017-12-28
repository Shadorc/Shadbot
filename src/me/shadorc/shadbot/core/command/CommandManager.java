package me.shadorc.shadbot.core.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import me.shadorc.discordbot.command.Role;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.command.Emoji;

public class CommandManager {

	private static final Map<String, AbstractCommand> COMMANDS_MAP = new HashMap<>();

	public static boolean init() {
		LogUtils.info("Initializing commands...");

		Reflections reflections = new Reflections(Shadbot.class.getPackage().getName(), new SubTypesScanner(), new TypeAnnotationsScanner());
		for(Class<?> cmdClass : reflections.getTypesAnnotatedWith(Command.class)) {
			if(!AbstractCommand.class.isAssignableFrom(cmdClass)) {
				LogUtils.errorf("An error occurred while generating command, %s cannot be cast to AbstractCommand.", cmdClass.getSimpleName());
				continue;
			}

			try {
				AbstractCommand cmd = (AbstractCommand) cmdClass.newInstance();

				List<String> names = cmd.getNames();
				if(!cmd.getAlias().isEmpty()) {
					names.add(cmd.getAlias());
				}

				for(String name : names) {
					if(COMMANDS_MAP.containsKey(name)) {
						LogUtils.error(String.format("Command name collision between %s and %s",
								cmd.getClass().getSimpleName(),
								COMMANDS_MAP.get(name).getClass().getSimpleName()));
						continue;
					}
					COMMANDS_MAP.put(name, cmd);
				}
			} catch (InstantiationException | IllegalAccessException err) {
				LogUtils.errorf(err, "An error occurred while initializing command %s.", cmdClass.getDeclaringClass().getSimpleName());
				return false;
			}
		}

		LogUtils.infof("%s initialized.", StringUtils.pluralOf((int) COMMANDS_MAP.values().stream().distinct().count(), "command"));
		return true;
	}

	public void execute(Context context) {
		AbstractCommand cmd = COMMANDS_MAP.get(context.getCommandName());
		if(cmd == null) {
			return;
		}

		Role authorRole = context.getAuthorRole();
		if(cmd.getPermission().getHierarchy() > authorRole.getHierarchy()) {
			BotUtils.sendMessage(Emoji.ACCESS_DENIED + " You do not have the permission to execute this command.", context.getChannel());
			return;
		}

		try {
			cmd.execute(context);
		} catch (IllegalArgumentException err) {
			// TODO
		} catch (MissingArgumentException err) {
			// TODO
		}
	}
}
