package me.shadorc.discordbot.utility;

import me.shadorc.discordbot.CommandManager;
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
					new MessageBuilder(channel.getClient().getOurUser().getClient()).withChannel(channel).withContent(message).build();
				});
			}
		} catch (MissingPermissionsException e) {
			Log.warn("Missing permissions for channel \"" + channel.getName() + "\" (ID: " + channel.getStringID() + ")");
		} catch (DiscordException e) {
			Log.error(e.getErrorMessage(), e);
		}
	}

	public static void executeCommand(MessageReceivedEvent event) {
		cmdManager.manage(event);
	}
}
