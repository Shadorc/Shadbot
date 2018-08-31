package me.shadorc.shadbot.core.setting;

public enum SettingEnum {

	PREFIX,
	DEFAULT_VOLUME,
	NSFW,
	ALLOWED_CHANNELS,
	ALLOWED_TEXT_CHANNELS,
	ALLOWED_VOICE_CHANNELS,
	BLACKLIST,
	AUTO_MESSAGE,
	MESSAGE_CHANNEL_ID,
	JOIN_MESSAGE,
	LEAVE_MESSAGE,
	AUTO_ROLE,
	PERMISSIONS;

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}

}
