package me.shadorc.discordbot.message;

import sx.blah.discord.handle.obj.IMessage;

public interface MessageListener {

	void onMessageReceived(IMessage message);

}
