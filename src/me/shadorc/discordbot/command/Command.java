package me.shadorc.discordbot.command;

public abstract class Command {

	private final String[] names;
	private final boolean isAdminCmd;

	public Command(boolean isAdminCmd, String name, String... names) {
		this.isAdminCmd = isAdminCmd;
		this.names = new String[names.length + 1];
		this.names[0] = name;
		System.arraycopy(names, 0, this.names, 1, names.length);
	}

	public abstract void execute(Context context);
	public abstract void showHelp(Context context);

	public boolean isAdminCmd() {
		return isAdminCmd;
	}

	public String[] getNames() {
		return this.names;
	}
}
