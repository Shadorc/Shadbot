package me.shadorc.shadbot.message;

import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;

public interface MessageListener {

	/**
	 * @param guildId - the guild ID
	 * @param message - the message received
	 * @return true if the message has been intercepted (blocking the execution of other commands), false otherwise
	 */
	boolean intercept(Snowflake guildId, Message message);

}
