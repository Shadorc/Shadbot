package me.shadorc.discordbot.utility;

import me.shadorc.discordbot.CommandManager;
import me.shadorc.discordbot.Main;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

public class BotUtils {
	
	private static CommandManager cmdManager = new CommandManager();

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

	public static void executeCommand(MessageReceivedEvent event) {
		Log.info("Executing command \"" + event.getMessage().getContent() + "\" from " + event.getMessage().getAuthor().getName() + ".");
		cmdManager.manage(event);
	}
}
