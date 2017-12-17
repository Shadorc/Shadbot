package me.shadorc.shadbot.command;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;

import me.shadorc.discordbot.utils.LogUtils;

public class CommandManager {

	private final Map<String, CustomCommand> COMMANDS_MAP = new LinkedHashMap<>();

	public CommandManager() {
		for(Method method : new Reflections(new MethodAnnotationsScanner()).getMethodsAnnotatedWith(Command.class)) {
			CustomCommand cmd = new CustomCommand(method);

			List<String> names = new ArrayList<>();
			names.addAll(cmd.getNames());
			names.add(cmd.getAlias());

			for(String name : names) {
				if(COMMANDS_MAP.containsKey(name)) {
					LogUtils.error("Command name collision between " + cmd.getClass().getSimpleName()
							+ " and " + COMMANDS_MAP.get(name).getClass().getSimpleName());
					continue;
				}
				COMMANDS_MAP.put(name, cmd);
			}
		}
	}

	public void execute(Context context) {
		CustomCommand cmd = COMMANDS_MAP.get(context.getCommandName());
		if(cmd == null) {
			return;
		}

		try {
			cmd.getMethod().invoke(null, context);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			// TODO
			e.printStackTrace();
		}
	}
}
