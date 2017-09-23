package me.shadorc.discordbot.command;

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