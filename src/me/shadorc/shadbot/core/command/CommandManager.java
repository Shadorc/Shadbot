package me.shadorc.shadbot.core.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.reflections.Reflections;

import me.shadorc.discordbot.command.Role;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.command.Emoji;

public class CommandManager {

	private final Map<String, AbstractCommand> commandsMap;

	public CommandManager() throws InstantiationException, IllegalAccessException {
		commandsMap = new HashMap<>();

		for(Class<?> cmdClass : new Reflections(this.getClass().getPackage()).getTypesAnnotatedWith(Command.class)) {
			if(!cmdClass.isAssignableFrom(AbstractCommand.class)) {
				LogUtils.errorf("An error occurred while generating command, %s cannot be cast to AbstractCommand.", cmdClass.getSimpleName());
				continue;
			}

			AbstractCommand cmd = (AbstractCommand) cmdClass.newInstance();

			List<String> names = cmd.getNames();
			if(!cmd.getAlias().isEmpty()) {
				names.add(cmd.getAlias());
			}

			for(String name : names) {
				if(commandsMap.containsKey(name)) {
					LogUtils.error(String.format("Command name collision between %s and %s",
							cmd.getClass().getSimpleName(),
							commandsMap.get(name).getClass().getSimpleName()));
					continue;
				}
				commandsMap.put(name, cmd);
			}
		}
	}

	public void execute(Context context) {
		AbstractCommand cmd = commandsMap.get(context.getCommandName());
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
