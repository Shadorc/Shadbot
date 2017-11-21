package me.shadorc.discordbot.stats;

public enum StatsEnum {
	MONEY_LOST("money_gains_command"),
	MONEY_GAINED("money_losses_command"),

	COMMAND("command"),
	LIMITED("limited_command"),
	HELP("help_command"),

	VARIOUS("various"),
	MUSICS_LOADED("musicsLoaded"),
	COMMANDS_EXECUTED("commandsExecuted"),
	RESPONSE_TIME("responseTime"),
	MESSAGES_RECEIVED("messagesReceived"),
	PRIVATE_MESSAGES_RECEIVED("privateMessagesReceived");

	private final String key;

	StatsEnum(String key) {
		this.key = key;
	}

	@Override
	public String toString() {
		return key;
	}
}