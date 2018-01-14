package me.shadorc.shadbot.command.admin.setting.core;

public enum SettingEnum {

	PREFIX("prefix"),
	VOLUME("volume"),
	NSFW("nsfw"),
	ALLOWED_CHANNELS("allowed_channels"),
	BLACKLIST("blacklist"),
	AUTO_MESSAGE("auto_message"),
	MESSAGE_CHANNEL_ID("message_channel_id"),
	JOIN_MESSAGE("join_message"),
	LEAVE_MESSAGE("leave_message"),
	AUTO_ASSIGN("auto_assign");

	private final String name;

	SettingEnum(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

}
