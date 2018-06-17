package me.shadorc.shadbot.music;

import discord4j.voice.AudioReceiver;

public class MusicReceiver implements AudioReceiver {

	@Override
	public void receive(char sequence, int timestamp, int ssrc, byte[] audio) {
		// No-op
	}

}
