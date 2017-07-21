package me.shadorc.discordbot;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;

public class Context {
	
	private IMessage message;
	private IChannel channel;
	private String command;
	private String arg;
	
	public Context(MessageReceivedEvent event) {
		this.message = event.getMessage();
		this.channel = event.getChannel();
		this.command = message.getContent().split(" ", 2)[0].replace("/", "").toLowerCase().trim();
		this.arg = message.getContent().contains(" ") ? message.getContent().split(" ", 2)[1].trim() : null;
	}
	
	public IMessage getMessage() {
		return message;
	}
	
	public IChannel getChannel() {
		return channel;
	}
	
	public String getCommand() {
		return command;
	}
	
	public String getArg() {
		return arg;
	}
}
