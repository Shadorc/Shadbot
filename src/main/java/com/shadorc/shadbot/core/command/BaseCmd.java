package com.shadorc.shadbot.core.command;

import com.shadorc.shadbot.core.ratelimiter.RateLimiter;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public abstract class BaseCmd {

    private final CommandCategory category;
    private final CommandPermission permission;
    private final String name;
    private final String description;
    private final List<Option> options;

    @Nullable
    private RateLimiter rateLimiter;
    private boolean isEnabled;

    protected BaseCmd(CommandCategory category, CommandPermission permission, String name, String description) {
        this.category = category;
        this.permission = permission;
        this.name = name;
        this.description = description;
        this.options = new LinkedList<>();
        this.rateLimiter = new RateLimiter(3, Duration.ofSeconds(5));
        this.isEnabled = true;
    }

    protected BaseCmd(CommandCategory category, String name, String description) {
        this(category, CommandPermission.USER, name, description);
    }

    public ApplicationCommandRequest build(ImmutableApplicationCommandRequest.Builder builder) {
        for (final Option option : this.options) {
            builder.addOption(ApplicationCommandOptionData.builder()
                    .name(option.getName())
                    .description(option.getDescription())
                    .required(option.isRequired())
                    .type(option.getType())
                    .build());
        }
        return builder.build();
    }

    public abstract Mono<?> execute(Context context);

    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context).build();
    }

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

    public List<Option> getOptions() {
        return Collections.unmodifiableList(this.options);
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

    public void setGameRateLimiter() {
        this.setRateLimiter(new RateLimiter(1, Duration.ofSeconds(3)));
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public void addOption(String name, String description, boolean isRequired, ApplicationCommandOptionType type) {
        this.options.add(new Option(name, description, isRequired, type));
    }

}
