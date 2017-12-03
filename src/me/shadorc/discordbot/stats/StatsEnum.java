package me.shadorc.discordbot.stats;

public enum StatsEnum {
	MONEY_GAINS_COMMAND("money_gains_command", true),
	MONEY_LOSSES_COMMAND("money_losses_command", true),

	COMMAND("command", true),
	LIMITED_COMMAND("limited_command", true),
	HELP_COMMAND("help_command", true),

	VARIOUS("various", true),

	MUSICS_LOADED("musics_loaded", false),
	COMMANDS_EXECUTED("command_executed", false),
	RESPONSE_TIME("response_time", false),
	MESSAGES_RECEIVED("messages_received", false),
	MESSAGES_SENT("messages_sent", false),
	EMBEDS_SENT("embeds_sent", false);

	private final String key;
	private final boolean isCategory;

	StatsEnum(String key, boolean isCategory) {
		this.key = key;
		this.isCategory = isCategory;
	}

	public boolean isCategory() {
		return isCategory;
	}

	@Override
	public String toString() {
		return key;
	}
}