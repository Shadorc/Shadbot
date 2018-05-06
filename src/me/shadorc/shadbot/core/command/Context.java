package me.shadorc.shadbot.core.command;

import java.security.Permissions;
import java.util.List;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import me.shadorc.shadbot.shard.ShadbotShard;
import me.shadorc.shadbot.shard.ShardManager;
import me.shadorc.shadbot.utils.StringUtils;
import sx.blah.discord.api.IShard;
import sx.blah.discord.handle.obj.IUser;

public class Context {

	private final Message message;
	private final String prefix;
	private final String cmdName;
	private final String arg;

	public Context(String prefix, Message message) {
		this.message = message;
		this.prefix = prefix;

		List<String> splittedMsg = StringUtils.split(message.getContent(), 2);
		this.cmdName = splittedMsg.get(0).substring(prefix.length()).toLowerCase();
		this.arg = splittedMsg.size() > 1 ? splittedMsg.get(1) : "";
	}

	public Message getMessage() {
		return message;
	}

	public String getContent() {
		return message.getContent().orElse("");
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

	public DiscordClient getClient() {
		return message.getClient();
	}

	public IUser getOurUser() {
		return message.getClient().getOurUser();
	}

	public ShadbotShard getShadbotShard() {
		return ShardManager.getShadbotShard(this.getShard());
	}

	public IShard getShard() {
		return message.getShard();
	}

	public Guild getGuild() {
		return message.getGuild().block();
	}

	public Channel getChannel() {
		return message.getChannel().block();
	}

	public User getAuthor() {
		return message.getAuthor().block();
	}

	public String getAuthorName() {
		return this.getAuthor().getUsername();
	}

	public CommandPermission getAuthorPermission() {
		if(this.getAuthor().equals(this.getClient().getApplicationInfo().block().getOwner().block())) {
			return CommandPermission.OWNER;
		} else if(this.getGuild() == null) {
			return CommandPermission.ADMIN;
		} else if(this.getAuthor().getPermissionsForGuild(this.getGuild()).contains(Permissions.ADMINISTRATOR)) {
			return CommandPermission.ADMIN;
		} else {
			return CommandPermission.USER;
		}
	}

	public boolean hasArg() {
		return !arg.isEmpty();
	}
}
