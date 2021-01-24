package com.shadorc.shadbot.command.donator;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.premium.RelicType;
import com.shadorc.shadbot.object.Emoji;
import discord4j.common.util.Snowflake;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class ActivateRelicCmd extends BaseCmd {

    public ActivateRelicCmd() {
        super(CommandCategory.DONATOR, "activate_relic", "Activate a relic");
        this.setDefaultRateLimiter();
    }

    @Override
    public ApplicationCommandRequest build(ImmutableApplicationCommandRequest.Builder builder) {
        return builder
                .addOption(ApplicationCommandOptionData.builder()
                        .name("key")
                        .description("The key to activate")
                        .type(ApplicationCommandOptionType.STRING.getValue())
                        .required(true)
                        .build())
                .build();
    }

    @Override
    public Mono<?> execute(Context context) {
        final String arg = context.getOption("key").orElseThrow();

        return DatabaseManager.getPremium().getRelicById(arg)
                .switchIfEmpty(context.createFollowupMessage(
                        Emoji.GREY_EXCLAMATION + " (**%s**) This Relic doesn't exist.", context.getAuthorName())
                        .then(Mono.empty()))
                .flatMap(relic -> {
                    if (relic.getActivation().isPresent()) {
                        return context.createFollowupMessage(
                                Emoji.GREY_EXCLAMATION + " (**%s**) This Relic is already activated.", context.getAuthorName());
                    }

                    DEFAULT_LOGGER.info("{User ID: {}} Relic activated. ID: {}", context.getAuthorId().asString(), arg);

                    final Optional<Snowflake> guildId = Optional.of(context.getGuildId())
                            .filter(__ -> relic.getType() == RelicType.GUILD);
                    return relic.activate(context.getAuthorId(), guildId.orElse(null))
                            .then(context.createFollowupMessage(
                                    Emoji.CHECK_MARK + " (**%s**) Relic successfully activated, enjoy!", context.getAuthorName()));
                });
    }

}
