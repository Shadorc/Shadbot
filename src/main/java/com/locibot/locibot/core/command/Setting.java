package com.locibot.locibot.core.command;

public enum Setting {

    DEFAULT_VOLUME,
    ALLOWED_TEXT_CHANNELS,
    ALLOWED_VOICE_CHANNELS,
    ALLOWED_ROLES,
    BLACKLIST,
    AUTO_ROLES,
    IAM_MESSAGES,
    RESTRICTED_CHANNELS,
    RESTRICTED_ROLES,
    AUTO_JOIN_MESSAGE,
    AUTO_LEAVE_MESSAGE,
    LOCALE;

    @Override
    public String toString() {
        return super.name().toLowerCase();
    }

}
