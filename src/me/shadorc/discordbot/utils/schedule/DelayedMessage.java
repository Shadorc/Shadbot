package me.shadorc.discordbot.utils.schedule;

import java.util.Objects;

public class DelayedMessage {

	private final Object message;
	private int count;

	public DelayedMessage(Object object) {
		this.message = object;
		this.count = 0;
	}

	public Object getMessage() {
		return message;
	}

	public int getCount() {
		return count;
	}

	public void incrementCount() {
		this.count++;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null) {
			return false;
		}

		if(!DelayedMessage.class.isAssignableFrom(obj.getClass())) {
			return false;
		}

		return ((DelayedMessage) obj).getMessage().equals(obj);
	}

	@Override
	public int hashCode() {
		return Objects.hash(message, count);
	}
}