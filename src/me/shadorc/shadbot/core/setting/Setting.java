package me.shadorc.shadbot.core.setting;

public enum Setting {

    PREFIX,
    DEFAULT_VOLUME,
    NSFW,
    ALLOWED_CHANNELS,
    ALLOWED_TEXT_CHANNELS,
    ALLOWED_VOICE_CHANNELS,
    ALLOWED_ROLES,
    BLACKLIST,
    AUTO_MESSAGE,
    MESSAGE_CHANNEL_ID,
    JOIN_MESSAGE,
    LEAVE_MESSAGE,
    AUTO_ROLES,
    IAM_MESSAGES;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

}
