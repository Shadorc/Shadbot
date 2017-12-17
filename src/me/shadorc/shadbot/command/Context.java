package me.shadorc.shadbot.command;

import java.util.List;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.Role;
import me.shadorc.shadbot.utils.StringUtils;
import sx.blah.discord.api.IShard;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;

public class Context {

	private final IMessage message;
	private final String prefix;
	private final String cmdName;
	private final String arg;

	public Context(IMessage message) {
		this.message = message;
		// this.prefix = Storage#getPrefix
		this.prefix = "placeholder";

		List<String> splittedMsg = StringUtils.split(message.getContent(), 2);
		this.cmdName = splittedMsg.get(0).substring(prefix.length()).toLowerCase();
		this.arg = splittedMsg.size() > 1 ? splittedMsg.get(1) : "";
	}

	public IMessage getMessage() {
		return message;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getCommandName() {
		return cmdName;
	}

	public String getArg() {
		return arg;
	}

	public IShard getShard() {
		return message.getShard();
	}

	public IGuild getGuild() {
		return message.getGuild();
	}

	public IChannel getChannel() {
		return message.getChannel();
	}

	public IUser getAuthor() {
		return message.getAuthor();
	}

	public String getAuthorName() {
		return this.getAuthor().getName();
	}

	public Role getAuthorRole() {
		if(this.getAuthor().equals(Shadbot.getOwner())) {
			return Role.OWNER;
		} else if(this.getAuthor().getPermissionsForGuild(this.getGuild()).contains(Permissions.ADMINISTRATOR)) {
			return Role.ADMIN;
		} else {
			return Role.USER;
		}
	}
}
