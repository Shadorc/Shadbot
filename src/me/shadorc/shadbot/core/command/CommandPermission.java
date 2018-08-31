package me.shadorc.shadbot.core.command;

public enum CommandPermission {

	USER(0),
	ADMIN(1),
	OWNER(2);

	private final int hierarchy;

	CommandPermission(int hierarchy) {
		this.hierarchy = hierarchy;
	}

	public int getHierarchy() {
		return this.hierarchy;
	}

	public boolean isSuperior(CommandPermission perm) {
		return this.getHierarchy() > perm.getHierarchy();
	}
}