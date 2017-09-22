package me.shadorc.discordbot.command;

import me.shadorc.discordbot.utils.command.MissingArgumentException;

public abstract class AbstractCommand {

	private final CommandCategory category;
	private final Role role;
	private final String[] names;

	public enum Role {
		USER(0),
		ADMIN(1),
		OWNER(2);

		private final int hierarchy;

		Role(int hierarchy) {
			this.hierarchy = hierarchy;
		}

		public int getHierarchy() {
			return hierarchy;
		}
	}

	public AbstractCommand(CommandCategory category, Role role, String name, String... names) {
		this.category = category;
		this.role = role;
		this.names = new String[names.length + 1];
		this.names[0] = name;
		System.arraycopy(names, 0, this.names, 1, names.length);
	}

	public abstract void execute(Context context) throws MissingArgumentException;

	public abstract void showHelp(Context context);

	public CommandCategory getCategory() {
		return category;
	}

	public Role getRole() {
		return role;
	}

	public String[] getNames() {
		return names.clone();
	}
}
