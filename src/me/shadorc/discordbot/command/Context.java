package me.shadorc.discordbot.command;

import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.Setting;
import me.shadorc.discordbot.Player;
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
	private Player player;

	public Context(MessageReceivedEvent event) {
		this.event = event;

		String[] splitMessage = event.getMessage().getContent().split(" ", 2);
		this.command = splitMessage[0].substring(1).toLowerCase().trim();
		this.arg = splitMessage.length > 1 ? splitMessage[1].trim() : "";
	}

	public Player getPlayer() {
		if(player == null) {
			player = Storage.getPlayer(this.getGuild(), this.getAuthor());
		}
		return player;
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

	public String getPrefix() {
		return Storage.getSetting(this.getGuild(), Setting.PREFIX).toString();
	}

	public String getCommand() {
		return command;
	}

	public String getArg() {
		return arg;
	}

	public boolean hasArg() {
		return !arg.isEmpty();
	}

	public boolean isAuthorAdmin() {
		return event.getAuthor().getPermissionsForGuild(event.getGuild()).contains(Permissions.ADMINISTRATOR);
	}
}
