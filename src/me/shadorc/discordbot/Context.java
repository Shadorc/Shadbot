package me.shadorc.discordbot;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;

public class Context {

	private MessageReceivedEvent event;
	private String command;
	private String arg;

	public Context(MessageReceivedEvent event) {
		this.event = event;

		String[] splitMessage = event.getMessage().getContent().split(" ", 2);
		this.command = splitMessage[0].substring(1).toLowerCase().trim();
		this.arg = (splitMessage.length > 1) ? splitMessage[1].trim() : null;
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
