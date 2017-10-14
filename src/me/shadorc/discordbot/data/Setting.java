package me.shadorc.discordbot.data;

public enum Setting {
	AUTO_MESSAGE("auto_message", false),
	NSFW("nsfw", false),
	ALLOWED_CHANNELS("allowed_channels", true),
	PREFIX("prefix", true),
	DEFAULT_VOLUME("default_volume", true),
	JOIN_MESSAGE("join_message", true),
	LEAVE_MESSAGE("leave_message", true),
	MESSAGE_CHANNEL_ID("message_channel_id", true),
	BLACKLIST("blacklist", true);

	private final String key;
	private final boolean isSaveable;

	Setting(String key, boolean isSaveable) {
		this.key = key;
		this.isSaveable = isSaveable;
	}

	public boolean isSaveable() {
		return isSaveable;
	}

	@Override
	public String toString() {
		return key;
	}
}
