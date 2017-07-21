package me.shadorc.discordbot;

public abstract class Command {

	private final String[] names;

	public Command(String name, String... names) {
		this.names = new String[names.length + 1];
		this.names[0] = name;
		System.arraycopy(names, 0, this.names, 1, names.length);
	}

	public abstract void execute(Context context);

	public String[] getNames() {
		return this.names;
	}
}
