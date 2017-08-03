package me.shadorc.discordbot.command;

import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.rpg.User;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;

public class Context {

	private final MessageReceivedEvent event;
	private final String command;
	private final String arg;

	public Context(MessageReceivedEvent event) {
		this.event = event;

		String[] splitMessage = event.getMessage().getContent().split(" ", 2);
		this.command = splitMessage[0].substring(1).toLowerCase().trim();
		this.arg = (splitMessage.length > 1) ? splitMessage[1].trim() : null;
	}

	public User getUser() {
		return Storage.getUser(this.getGuild(), this.getAuthor());
	}

	public IUser getAuthor() {
		return event.getAuthor();
	}

	public String getAuthorName() {
		return event.getAuthor().getName();
	}

	public IChannel getChannel() {
		return event.getChannel();
	}

	public IGuild getGuild() {
		return event.getGuild();
	}

	public IMessage getMessage() {
		return event.getMessage();
	}

	public IDiscordClient getClient() {
		return event.getClient();
	}

	public String getCommand() {
		return command;
	}

	public String getArg() {
		return arg;
	}

	public boolean isAuthorAdmin() {
		return event.getAuthor().getPermissionsForGuild(event.getGuild()).contains(Permissions.ADMINISTRATOR);
	}
}
