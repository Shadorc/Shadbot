package me.shadorc.discordbot;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

public class Context {

	private IMessage message;
	private IChannel channel;
	private IGuild guild;
	private IDiscordClient client;
	private IUser author;
	private String command;
	private String arg;

	public Context(MessageReceivedEvent event) {
		this.message = event.getMessage();
		this.channel = event.getChannel();
		this.guild = event.getGuild();
		this.client = event.getClient();
		this.author = event.getAuthor();

		String[] splitMessage = message.getContent().split(" ", 2);
		this.command = splitMessage[0].substring(1).toLowerCase().trim();
		this.arg = (splitMessage.length > 1) ? splitMessage[1].trim() : null;
	}

	public IUser getAuthor() {
		return author;
	}

	public IChannel getChannel() {
		return channel;
	}

	public IGuild getGuild() {
		return guild;
	}

	public String getCommand() {
		return command;
	}

	public String getArg() {
		return arg;
	}

	public IDiscordClient getClient() {
		return client;
	}
}
