package me.shadorc.shadbot.utils;

public enum ExitCode {
	UNKNWON(-1),
	NORMAL(0),
	FATAL_ERROR(1),
	RESTART(2),
	RESTART_CLEAN(3);

	private final int value;

	ExitCode(int value) {
		this.value = value;
	}

	public int value() {
		return this.value;
	}
}
