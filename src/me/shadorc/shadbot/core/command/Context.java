package me.shadorc.shadbot.core.command;

import java.util.List;
import java.util.Optional;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.shard.CustomShard;
import me.shadorc.shadbot.shard.ShardManager;
import me.shadorc.shadbot.utils.StringUtils;

public class Context {

	private final Message message;
	private final String prefix;
	private final String cmdName;
	private final Optional<String> arg;

	public Context(String prefix, Message message) {
		this.message = message;
		this.prefix = prefix;

		List<String> splittedMsg = StringUtils.split(this.getContent(), 2);
		this.cmdName = splittedMsg.get(0).substring(prefix.length()).toLowerCase();
		this.arg = Optional.ofNullable(splittedMsg.size() > 1 ? splittedMsg.get(1) : null);
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

	// TODO: Has been changed from "" to Optional
	public Optional<String> getArg() {
		return arg;
	}

	public DiscordClient getClient() {
		return message.getClient();
	}

	// TODO Keep this ?
	public User getSelf() {
		return Shadbot.getSelf().block();
	}

	public CustomShard getShadbotShard() {
		return ShardManager.getShadbotShard(this.getShard());
	}

	public Integer getShardIndex() {
		return message.getClient().getConfig().getShardIndex();
	}

	// TODO: Check everywhere that this is used, has been replaced by Optional
	public Optional<Guild> getGuild() {
		return message.getGuild().blockOptional();
	}

	public Snowflake getGuildId() {
		return this.getGuild().isPresent() ? this.getGuild().get().getId() : null;
	}

	public MessageChannel getChannel() {
		return message.getChannel().block();
	}

	public User getAuthor() {
		return message.getAuthor().block();
	}

	public String getUsername() {
		return this.getAuthor().getUsername();
	}

	public CommandPermission getAuthorPermission() {
		if(this.getAuthor().equals(this.getClient().getApplicationInfo().block().getOwner().block())) {
			return CommandPermission.OWNER;
		} else if(!this.getGuild().isPresent()) {
			return CommandPermission.ADMIN;
		} else if(this.getAuthor().getPermissionsForGuild(this.getGuild()).contains(Permission.ADMINISTRATOR)) {
			return CommandPermission.ADMIN;
		} else {
			return CommandPermission.USER;
		}
	}

	public void requireArg() throws MissingArgumentException {
		if(!this.getArg().isPresent()) {
			throw new MissingArgumentException();
		}
	}

}
