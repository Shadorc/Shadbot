package me.shadorc.shadbot.core.command;

import java.util.List;
import java.util.Optional;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.StringUtils;
import reactor.core.publisher.Mono;

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

	// TODO: Has been changed from "" to Optional, this need to be checked in commands
	public Optional<String> getArg() {
		return arg;
	}

	public DiscordClient getClient() {
		return message.getClient();
	}

	public Mono<User> getSelf() {
		return message.getClient().getSelf();
	}

	public Integer getShardIndex() {
		return message.getClient().getConfig().getShardIndex();
	}

	public Mono<Guild> getGuild() {
		return message.getGuild();
	}

	public Mono<MessageChannel> getChannel() {
		return message.getChannel();
	}

	public Mono<User> getAuthor() {
		return message.getAuthor();
	}

	public Mono<String> getUsername() {
		return this.getAuthor().map(User::getUsername);
	}

	// TODO
	public CommandPermission getAuthorPermission() {
		// if(this.getAuthor().equals(this.getClient().getApplicationInfo().block().getOwner().block())) {
		// return CommandPermission.OWNER;
		// } else if(!this.getGuild().isPresent()) {
		// return CommandPermission.ADMIN;
		// } else if(this.getAuthor().getPermissionsForGuild(this.getGuild()).contains(Permission.ADMINISTRATOR)) {
		// return CommandPermission.ADMIN;
		// } else {
		// return CommandPermission.USER;
		// }
		return CommandPermission.USER;
	}

	public void requireArg() throws MissingArgumentException {
		if(!this.getArg().isPresent()) {
			throw new MissingArgumentException();
		}
	}

}
