package me.shadorc.discordbot.message;

import sx.blah.discord.handle.obj.IMessage;

public interface MessageListener {

	/**
	 * @param message - the message received
	 * @return true if the execution of the other commands is blocked, false otherwise
	 */
	boolean onMessageReceived(IMessage message);

}
