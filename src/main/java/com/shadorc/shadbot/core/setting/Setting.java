package com.shadorc.shadbot.core.setting;

public enum Setting {

    PREFIX,
    DEFAULT_VOLUME,
    NSFW,
    ALLOWED_CHANNELS,
    ALLOWED_TEXT_CHANNELS,
    ALLOWED_VOICE_CHANNELS,
    ALLOWED_ROLES,
    BLACKLIST,
    @Deprecated
    AUTO_MESSAGE,
    MESSAGE_CHANNEL_ID,
    @Deprecated
    JOIN_MESSAGE,
    @Deprecated
    LEAVE_MESSAGE,
    AUTO_ROLES,
    IAM_MESSAGES,
    RESTRICTED_CHANNELS,
    RESTRICTED_ROLES,
    AUTO_JOIN_MESSAGE,
    AUTO_LEAVE_MESSAGE;

    @Override
    public String toString() {
        return super.name().toLowerCase();
    }

}
