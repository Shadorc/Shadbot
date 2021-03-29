package com.shadorc.shadbot.core.command;

import com.shadorc.shadbot.core.ratelimiter.RateLimiter;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.*;
import discord4j.rest.service.ApplicationService;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class BaseCmd {

    protected static final Supplier<RateLimiter> DEFAULT_RATELIMITER = () ->
            new RateLimiter(3, Duration.ofSeconds(5));
    protected static final Supplier<RateLimiter> DEFAULT_GAME_RATELIMITER = () ->
            new RateLimiter(1, Duration.ofSeconds(3));

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
        this.options = new ArrayList<>();
        this.rateLimiter = DEFAULT_RATELIMITER.get();
        this.isEnabled = true;
    }

    protected BaseCmd(CommandCategory category, String name, String description) {
        this(category, CommandPermission.USER, name, description);
    }

    public List<ApplicationCommandOptionData> buildOptions() {
        final List<ApplicationCommandOptionData> optionsData = new ArrayList<>();
        for (final Option option : this.options) {
            optionsData.add(ApplicationCommandOptionData.builder()
                    .name(option.getName())
                    .description(option.getDescription())
                    .required(option.isRequired())
                    .type(option.getType())
                    .choices(option.getChoices())
                    .build());
        }
        return optionsData;
    }

    public Mono<ApplicationCommandData> register(ApplicationService applicationService, long applicationId) {
        final ImmutableApplicationCommandRequest request = ApplicationCommandRequest.builder()
                .name(this.getName())
                .description(this.getDescription())
                .addAllOptions(this.buildOptions())
                .build();

        // TODO: Enable for release
        if (true/*this.getPermission().equals(CommandPermission.OWNER)*/) {
            return applicationService.createGuildApplicationCommand(applicationId, Config.OWNER_GUILD_ID, request);
        } else {
            return applicationService.createGlobalApplicationCommand(applicationId, request);
        }
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
        this.setRateLimiter(DEFAULT_GAME_RATELIMITER.get());
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public void addOption(String name, String description, boolean isRequired, ApplicationCommandOptionType type) {
        this.options.add(new Option(name, description, isRequired, type));
    }

    public void addOption(String name, String description, boolean isRequired, ApplicationCommandOptionType type,
                          List<ApplicationCommandOptionChoiceData> choices) {
        this.options.add(new Option(name, description, isRequired, type, choices));
    }

}
