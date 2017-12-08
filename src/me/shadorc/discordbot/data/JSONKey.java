package me.shadorc.discordbot.data;

public enum JSONKey {

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
	HISTORIC_NUM("historicNum"),

	RELIC_ID("relicID"),
	RELIC_ACTIVATION_MILLIS("activationMillis"),
	RELIC_DURATION("duration"),
	RELIC_EXPIRED("relicExpired"),
	RELIC_TYPE("type");

	private final String key;

	JSONKey(String key) {
		this.key = key;
	}

	@Override
	public String toString() {
		return key;
	}
}
