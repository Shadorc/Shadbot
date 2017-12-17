package me.shadorc.shadbot.command;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class CustomCommand {

	private final Method method;
	private final List<String> names;
	private final String alias;

	public CustomCommand(Method method) {
		Command annotation = method.getAnnotation(Command.class);
		this.method = method;
		this.names = Arrays.asList(annotation.names());
		this.alias = annotation.alias();
	}

	public Method getMethod() {
		return method;
	}

	public List<String> getNames() {
		return names;
	}

	public String getAlias() {
		return alias;
	}
}
