package me.shadorc.shadbot.utils;

public enum ExitCode {
	NORMAL(0),
	FATAL_ERROR(1),
	RESTART(2);

	private final int value;

	ExitCode(int value) {
		this.value = value;
	}

	public int value() {
		return this.value;
	}
}
