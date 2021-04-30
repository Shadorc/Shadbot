package com.shadorc.shadbot.core.command;

public enum Setting {

    @Deprecated
    PREFIX, // Removed
    DEFAULT_VOLUME,
    NSFW,
    @Deprecated
    ALLOWED_CHANNELS, // Removed
    @Deprecated
    ALLOWED_TEXT_CHANNELS,  // Removed
    @Deprecated
    ALLOWED_VOICE_CHANNELS, // Removed
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
