package me.shadorc.shadbot.core.command;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.ratelimiter.RateLimiter;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

public abstract class BaseCmd {

	private final CommandCategory category;
	private final CommandPermission permission;
	private final List<String> names;
	@Nullable
	private final String alias;

	@Nullable
	private RateLimiter rateLimiter;

	public BaseCmd(CommandCategory category, CommandPermission permission, List<String> names, String alias) {
		this.category = category;
		this.permission = permission;
		this.names = new ArrayList<>(names);
		this.alias = alias;
		this.rateLimiter = null;

		if(this.getAlias() != null) {
			this.getNames().add(this.getAlias());
		}
	}

	public BaseCmd(CommandCategory category, CommandPermission permission, List<String> names) {
		this(category, permission, names, null);
	}

	public BaseCmd(CommandCategory category, List<String> names, String alias) {
		this(category, CommandPermission.USER, names, alias);
	}

	public BaseCmd(CommandCategory category, List<String> names) {
		this(category, names, null);
	}

	public abstract Mono<Void> execute(Context context);

	public abstract Consumer<EmbedCreateSpec> getHelp(Context context);

	public CommandCategory getCategory() {
		return this.category;
	}

	public CommandPermission getPermission() {
		return this.permission;
	}

	public List<String> getNames() {
		return this.names;
	}

	public String getName() {
		return this.getNames().get(0);
	}

	@Nullable
	public String getAlias() {
		return this.alias;
	}

	public Optional<RateLimiter> getRateLimiter() {
		return Optional.ofNullable(this.rateLimiter);
	}

	public void setRateLimite(RateLimiter rateLimiter) {
		this.rateLimiter = rateLimiter;
	}

	public void setDefaultRateLimiter() {
		this.setRateLimite(new RateLimiter(3, Duration.ofSeconds(5)));
	}

	public void setGameRateLimiter() {
		this.setRateLimite(new RateLimiter(1, Duration.ofSeconds(5)));
	}

}
