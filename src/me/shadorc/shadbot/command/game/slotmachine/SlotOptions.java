package me.shadorc.shadbot.command.game.slotmachine;

import me.shadorc.shadbot.utils.StringUtils;

public enum SlotOptions {

	CHERRIES(250),
	BELL(3000),
	GIFT(100_000);

	private final int gain;

	SlotOptions(int gain) {
		this.gain = gain;
	}

	public int getGain() {
		return this.gain;
	}

	public String getEmoji() {
		return String.format(":%s:", StringUtils.toLowerCase(this));
	}
}