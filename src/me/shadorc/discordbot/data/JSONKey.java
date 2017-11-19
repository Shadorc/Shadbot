package me.shadorc.discordbot.data;

public enum JSONKey {

	LIMITED_COMMAND("limited_command"),
	MONEY_GAINS_COMMAND("money_gains_command"),
	MONEY_LOSSES_COMMAND("money_losses_command"),
	HELP_COMMAND("help_command"),
	COMMAND("command"),

	USERS("users"),
	SETTINGS("settings"),
	COINS("coins"),

	USER_ID("userID"),
	GUILD_ID("guildID"),

	POOL("pool"),
	NUM("num"),

	HISTORIC("historic"),
	HISTORIC_POOL("historicPool"),
	HISTORIC_WINNERS_COUNT("historicWinnerCount"),
	HISTORIC_NUM("historicNum");

	private final String key;

	JSONKey(String key) {
		this.key = key;
	}

	@Override
	public String toString() {
		return key;
	}
}
