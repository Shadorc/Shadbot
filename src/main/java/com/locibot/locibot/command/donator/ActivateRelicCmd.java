package com.locibot.locibot.command.donator;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.premium.RelicType;
import com.locibot.locibot.object.Emoji;
import discord4j.common.util.Snowflake;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static com.locibot.locibot.LociBot.DEFAULT_LOGGER;

public class ActivateRelicCmd extends BaseCmd {

    public ActivateRelicCmd() {
        super(CommandCategory.DONATOR, "activate_relic", "Activate a relic");
        this.addOption("relic", "The relic to activate", true, ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        final String arg = context.getOptionAsString("relic").orElseThrow();

        return DatabaseManager.getPremium().getRelicById(arg)
                .switchIfEmpty(context.createFollowupMessage(Emoji.GREY_EXCLAMATION, context.localize("activaterelic.not.found"))
                        .then(Mono.empty()))
                .flatMap(relic -> {
                    if (relic.getActivation().isPresent()) {
                        return context.createFollowupMessage(Emoji.GREY_EXCLAMATION, context.localize("activaterelic.already.activated"));
                    }

                    DEFAULT_LOGGER.info("{User ID: {}} Relic (ID: {}) activated.", context.getAuthorId().asString(), arg);

                    final Optional<Snowflake> guildId = Optional.of(context.getGuildId())
                            .filter(__ -> relic.getType() == RelicType.GUILD);
                    return relic.activate(context.getAuthorId(), guildId.orElse(null))
                            .then(context.createFollowupMessage(Emoji.CHECK_MARK, context.localize("activaterelic.message")));
                });
    }

}
