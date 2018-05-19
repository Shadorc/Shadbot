package me.shadorc.shadbot.message;

import discord4j.core.object.entity.Message;

public interface MessageListener {

	/**
	 * @param message - the message received
	 * @return true if the message has been intercepted blocking the execution of other commands, false otherwise
	 */
	boolean intercept(Message message);

}
