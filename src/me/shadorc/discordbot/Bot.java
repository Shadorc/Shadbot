package me.shadorc.discordbot;

import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

public class Bot {

	public static void sendMessage(String message, IChannel channel) {
		try {
			if(!message.isEmpty()) {
				new MessageBuilder(Main.getClient()).withChannel(channel).withContent(message).build();
			}
		} catch (RateLimitException e) {
			System.err.println("Sending messages too quickly!");
		} catch (MissingPermissionsException e) {
			System.err.println("Missing permissions for channel!");
		} catch (DiscordException e) {
			System.err.println(e.getErrorMessage());
			e.printStackTrace();
		}
	}

	public static void executeCommand(IMessage message, IChannel channel) {
		new Command(message, channel).execute();
	}
}
