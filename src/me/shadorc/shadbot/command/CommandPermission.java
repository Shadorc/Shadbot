package me.shadorc.shadbot.command;

public enum CommandPermission {
	USER(0),
	ADMIN(1),
	OWNER(2);

	private final int hierarchy;

	CommandPermission(int hierarchy) {
		this.hierarchy = hierarchy;
	}

	public int getHierarchy() {
		return hierarchy;
	}
}