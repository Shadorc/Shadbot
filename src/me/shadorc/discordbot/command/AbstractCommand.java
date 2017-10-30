package me.shadorc.discordbot.command;

import java.util.Arrays;
import java.util.List;

import me.shadorc.discordbot.utils.command.MissingArgumentException;

public abstract class AbstractCommand {

	private final CommandCategory category;
	private final Role role;
	private final List<String> names;

	private String alias;

	public AbstractCommand(CommandCategory category, Role role, String name, String... names) {
		this.category = category;
		this.role = role;
		this.names = Arrays.asList(names);
		this.names.add(name);
	}

	public abstract void execute(Context context) throws MissingArgumentException;

	public abstract void showHelp(Context context);

	public CommandCategory getCategory() {
		return category;
	}

	public Role getRole() {
		return role;
	}

	public List<String> getNames() {
		return names;
	}

	public String getFirstName() {
		return names.get(0);
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
		this.names.add(alias);
	}
}
