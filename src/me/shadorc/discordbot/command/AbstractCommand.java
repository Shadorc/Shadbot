package me.shadorc.discordbot.command;

import me.shadorc.discordbot.MissingArgumentException;

public abstract class AbstractCommand {

	private final String[] names;
	private final Role role;

	public enum Role {
		USER,
		ADMIN,
		OWNER;
	}

	public AbstractCommand(Role role, String name, String... names) {
		this.role = role;
		this.names = new String[names.length + 1];
		this.names[0] = name;
		System.arraycopy(names, 0, this.names, 1, names.length);
	}

	public abstract void execute(Context context) throws MissingArgumentException;

	public abstract void showHelp(Context context);

	public Role getRole() {
		return role;
	}

	public String[] getNames() {
		return names.clone();
	}
}
