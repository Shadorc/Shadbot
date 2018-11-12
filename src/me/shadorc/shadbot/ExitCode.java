package me.shadorc.shadbot;

public enum ExitCode {
	UNKNWON(-1),
	NORMAL(0),
	FATAL_ERROR(1),
	RESTART(2);

	private final int value;

	ExitCode(int value) {
		this.value = value;
	}

	public int value() {
		return value;
	}
}
