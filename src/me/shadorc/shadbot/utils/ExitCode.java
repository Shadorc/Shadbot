package me.shadorc.shadbot.utils;

public enum ExitCode {
	FATAL_ERROR(1),
	NORMAL(0),
	RESTART(2),
	UNKNWON(-1);

	private final int value;

	ExitCode(int value) {
		this.value = value;
	}

	public int value() {
		return this.value;
	}
}
