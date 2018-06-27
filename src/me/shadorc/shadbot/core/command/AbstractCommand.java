package me.shadorc.shadbot.core.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.core.ratelimiter.RateLimiter;
import reactor.core.publisher.Mono;

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

		RateLimited limiter = this.getClass().getAnnotation(RateLimited.class);
		if(limiter == null) {
			this.rateLimiter = Optional.empty();
		} else {
			this.rateLimiter = Optional.of(new RateLimiter(limiter.max(), limiter.cooldown(), limiter.unit()));
		}
	}

	public abstract Mono<Void> execute(Context context);

	public abstract Mono<EmbedCreateSpec> getHelp(Context context);

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
