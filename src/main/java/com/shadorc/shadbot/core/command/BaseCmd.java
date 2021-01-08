package com.shadorc.shadbot.core.command;

import com.shadorc.shadbot.core.ratelimiter.RateLimiter;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.util.Optional;

public abstract class BaseCmd {

    private final CommandCategory category;
    private final CommandPermission permission;
    private final String name;
    private final String description;

    @Nullable
    private RateLimiter rateLimiter;
    private boolean isEnabled;

    protected BaseCmd(CommandCategory category, CommandPermission permission, String name, String description) {
        this.category = category;
        this.permission = permission;
        this.name = name;
        this.description = description;
        this.rateLimiter = null;
        this.isEnabled = true;
    }

    protected BaseCmd(CommandCategory category, String name, String description) {
        this(category, CommandPermission.USER, name, description);
    }

    public ApplicationCommandRequest build(ImmutableApplicationCommandRequest.Builder builder) {
        return builder.build();
    }

    public abstract Mono<?> execute(Context context);

    public CommandCategory getCategory() {
        return this.category;
    }

    public CommandPermission getPermission() {
        return this.permission;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public Optional<RateLimiter> getRateLimiter() {
        return Optional.ofNullable(this.rateLimiter);
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    public void setRateLimiter(@Nullable RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    public void setDefaultRateLimiter() {
        this.setRateLimiter(new RateLimiter(3, Duration.ofSeconds(5)));
    }

    public void setGameRateLimiter() {
        this.setRateLimiter(new RateLimiter(1, Duration.ofSeconds(3)));
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

}
