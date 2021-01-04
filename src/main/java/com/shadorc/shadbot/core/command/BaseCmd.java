package com.shadorc.shadbot.core.command;

import com.shadorc.shadbot.core.ratelimiter.RateLimiter;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public abstract class BaseCmd {

    private final CommandCategory category;
    private final CommandPermission permission;
    private final List<String> names;
    @Nullable
    private final String alias;

    @Nullable
    private RateLimiter rateLimiter;
    private boolean isEnabled;

    protected BaseCmd(CommandCategory category, CommandPermission permission, List<String> names, @Nullable String alias) {
        this.category = category;
        this.permission = permission;
        this.names = new ArrayList<>(names);
        this.alias = alias;
        this.rateLimiter = null;
        this.isEnabled = true;

        if (this.alias != null) {
            this.names.add(this.alias);
        }
    }

    protected BaseCmd(CommandCategory category, CommandPermission permission, List<String> names) {
        this(category, permission, names, null);
    }

    protected BaseCmd(CommandCategory category, List<String> names, String alias) {
        this(category, CommandPermission.USER, names, alias);
    }

    protected BaseCmd(CommandCategory category, List<String> names) {
        this(category, names, null);
    }

    public abstract Mono<?> execute(Context context);

    public abstract Consumer<EmbedCreateSpec> getHelp(Context context);

    public CommandCategory getCategory() {
        return this.category;
    }

    public CommandPermission getPermission() {
        return this.permission;
    }

    public List<String> getNames() {
        return Collections.unmodifiableList(this.names);
    }

    public String getName() {
        return this.names.get(0);
    }

    public Optional<String> getAlias() {
        return Optional.ofNullable(this.alias);
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
