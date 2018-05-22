package me.shadorc.shadbot.core.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.ratelimiter.RateLimiter;

public abstract class AbstractCommand {

	private final List<String> names;
	private final String alias;
	private final CommandCategory category;
	private final CommandPermission permission;
	private final Optional<RateLimiter> rateLimiter;

	public AbstractCommand() {
		Command cmdAnnotation = this.getClass().getAnnotation(Command.class);
		this.names = new ArrayList<>(Arrays.asList(cmdAnnotation.names()));
		this.alias = cmdAnnotation.alias();
		this.category = cmdAnnotation.category();
		this.permission = cmdAnnotation.permission();

		RateLimited limiterAnnotation = this.getClass().getAnnotation(RateLimited.class);
		if(limiterAnnotation == null) {
			this.rateLimiter = Optional.empty();
		} else {
			this.rateLimiter = Optional.of(new RateLimiter(limiterAnnotation.max(), limiterAnnotation.cooldown(), limiterAnnotation.unit()));
		}
	}

	public abstract void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException;

	public abstract EmbedCreateSpec getHelp(String prefix);

	public List<String> getNames() {
		return names;
	}

	public String getName() {
		return this.getNames().get(0);
	}

	public String getAlias() {
		return alias;
	}

	public CommandCategory getCategory() {
		return category;
	}

	public CommandPermission getPermission() {
		return permission;
	}

	public Optional<RateLimiter> getRateLimiter() {
		return rateLimiter;
	}

}
