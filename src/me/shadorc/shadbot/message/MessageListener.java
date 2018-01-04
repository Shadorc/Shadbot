package me.shadorc.shadbot.message;

import sx.blah.discord.handle.obj.IMessage;

public interface MessageListener {

	/**
	 * @param message - the message received
	 * @return true if the message has been intercepted blocking the execution of other commands, false otherwise
	 */
	boolean intercept(IMessage message);

}
