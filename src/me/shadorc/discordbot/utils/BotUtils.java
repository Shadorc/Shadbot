package me.shadorc.discordbot.utils;

import me.shadorc.discordbot.command.CommandManager;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

public class BotUtils {

	private final static CommandManager CMD_MANAGER = new CommandManager();

	public static void sendMessage(String message, IChannel channel) {
		try {
			if(!message.isEmpty()) {
				RequestBuffer.request(() -> {
					channel.sendMessage(message);
				});
			}
		} catch (MissingPermissionsException e) {
			Log.warn("Missing permissions for channel \"" + channel.getName() + "\" (ID: " + channel.getStringID() + ")");
		} catch (DiscordException e) {
			Log.error("Discord exception while sending message : " + e.getErrorMessage(), e);
		}
	}

	//EmbedBuilder doc : https://discord4j.readthedocs.io/en/latest/Making-embedded-content-using-EmbedBuilder/
	public static void sendEmbed(EmbedObject embed, IChannel channel) {
		try {
			RequestBuffer.request(() -> {
				channel.sendMessage(embed);
			});
		} catch (MissingPermissionsException e) {
			Log.warn("Missing permissions for channel \"" + channel.getName() + "\" (ID: " + channel.getStringID() + ")");
		} catch (DiscordException e) {
			Log.error("Discord exception while sending embed : " + e.getErrorMessage(), e);
		}
	}

	public static void executeCommand(MessageReceivedEvent event) {
		CMD_MANAGER.manage(event);
	}
}
