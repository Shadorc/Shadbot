package me.shadorc.shadbot.data;

public enum Setting {
	NSFW("nsfw"),
	ALLOWED_CHANNELS("allowed_channels"),
	PREFIX("prefix"),
	DEFAULT_VOLUME("default_volume"),
	// AUTO_MESSAGE("auto_message"),
	JOIN_MESSAGE("join_message"),
	LEAVE_MESSAGE("leave_message"),
	MESSAGE_CHANNEL_ID("message_channel_id"),
	BLACKLIST("blacklist");

	private final String key;

	Setting(String key) {
		this.key = key;
	}

	@Override
	public String toString() {
		return key;
	}
}
