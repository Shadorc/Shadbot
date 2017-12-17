package me.shadorc.shadbot.command;

public enum Category {

	NONE("None"),
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

	Category(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return this.name;
	}
}
