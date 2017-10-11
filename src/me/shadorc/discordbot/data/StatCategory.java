package me.shadorc.discordbot.data;

public enum StatCategory {
	LIMITED_COMMAND("limited_command"),
	MONEY_GAINS_COMMAND("money_gains_command"),
	MONEY_LOSSES_COMMAND("money_losses_command"),
	HELP_COMMAND("help_command"),
	COMMAND("command");

	private final String key;

	StatCategory(String key) {
		this.key = key;
	}

	@Override
	public String toString() {
		return key;
	}
}