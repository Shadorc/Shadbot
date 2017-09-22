package me.shadorc.discordbot.command;

public enum CommandCategory {

	HIDDEN("Hidden"),
	UTILS("Utility"),
	FUN("Fun"),
	IMAGE("Image"),
	GAME("Game"),
	CURRENCY("Currency"),
	MUSIC("Music"),
	GAMESTATS("Game Stats"),
	INFO("Info"),
	FRENCH("French"),
	ADMIN("Admin"),
	OWNER("Owner");

	private final String name;

	CommandCategory(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return this.name;
	}
}
