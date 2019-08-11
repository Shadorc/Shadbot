package com.shadorc.shadbot.utils;

public enum ExitCode {
    UNKNWON(-1),
    NORMAL(0),
    RESTART(1),
    FATAL_ERROR(2);

    private final int value;

    ExitCode(int value) {
        this.value = value;
    }

    public static ExitCode valueOf(int value) {
        for (final ExitCode exitCode : ExitCode.values()) {
            if (exitCode.getValue() == value) {
                return exitCode;
            }
        }
        return ExitCode.UNKNWON;
    }

    public int getValue() {
        return this.value;
    }
}
