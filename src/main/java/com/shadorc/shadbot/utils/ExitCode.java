package com.shadorc.shadbot.utils;

public enum ExitCode {
    UNKNWON(-1),
    NORMAL(0),
    NORMAL_CLEAN(1),
    RESTART(2),
    RESTART_CLEAN(3),
    FATAL_ERROR(4);

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
