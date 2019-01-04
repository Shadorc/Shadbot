package me.shadorc.shadbot.utils.embed.help;

public class Argument {

	private final String name;
	private final String desc;
	private final boolean isFacultative;

	protected Argument(String name, String desc, boolean isFacultative) {
		this.name = name;
		this.desc = desc;
		this.isFacultative = isFacultative;
	}

	protected String getName() {
		return this.name;
	}

	protected String getDesc() {
		return this.desc;
	}

	protected boolean isFacultative() {
		return this.isFacultative;
	}

}
