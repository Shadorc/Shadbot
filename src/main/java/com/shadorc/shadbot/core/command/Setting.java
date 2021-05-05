package com.shadorc.shadbot.core.command;

public enum Setting {

    // TODO Clean-up: Deprecated

    @Deprecated
    PREFIX, // Removed
    DEFAULT_VOLUME,
    NSFW,
    @Deprecated
    ALLOWED_CHANNELS, // Removed
    ALLOWED_TEXT_CHANNELS,
    ALLOWED_VOICE_CHANNELS,
    ALLOWED_ROLES,
    BLACKLIST,
    @Deprecated
    AUTO_MESSAGE, // Removed
    @Deprecated
    MESSAGE_CHANNEL_ID, // Removed
    @Deprecated
    JOIN_MESSAGE, // Replaced by AUTO_JOIN_MESSAGE
    @Deprecated
    LEAVE_MESSAGE, // Replaced by AUTO_LEAVE_MESSAGE
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
