package me.shadorc.discordbot;

import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.utility.Log;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

public class Bot {

	public static void sendMessage(String message, IChannel channel) {
		try {
			if(!message.isEmpty()) {
				RequestBuffer.request(() -> {
					new MessageBuilder(Main.getClient()).withChannel(channel).withContent(message).build();
				});
			}
		} catch (MissingPermissionsException e) {
			Log.error("Missing permissions for channel!");
		} catch (DiscordException e) {
			Log.error(e.getErrorMessage(), e);
		}
	}

	public static void executeCommand(IMessage message, IChannel channel) {
		Log.info("Executing command \"" + message.getContent() + "\" from " + message.getAuthor().getName() + ".");
		new Command(message, channel).execute();
	}
}
