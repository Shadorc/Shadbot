package com.shadorc.shadbot.utils;

public enum ExitCode {
    UNKNOWN(-1),
    NORMAL(0),
    RESTART(1),
    FATAL_ERROR(2);

    private final int value;

    ExitCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }
}
