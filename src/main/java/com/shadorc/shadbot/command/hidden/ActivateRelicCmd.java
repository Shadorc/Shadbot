package com.shadorc.shadbot.command.hidden;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.premium.RelicType;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.LogUtils;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class ActivateRelicCmd extends BaseCmd {

    public ActivateRelicCmd() {
        super(CommandCategory.HIDDEN, List.of("activate_relic", "activate-relic", "activaterelic"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        return DatabaseManager.getPremium().getRelicById(arg)
                .switchIfEmpty(context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(
                                String.format(Emoji.GREY_EXCLAMATION + " (**%s**) This Relic doesn't exist.",
                                        context.getUsername()), channel))
                        .then(Mono.empty()))
                .flatMap(relic -> {
                    if (relic.getActivation().isPresent()) {
                        return context.getChannel()
                                .flatMap(channel -> DiscordUtils.sendMessage(
                                        String.format(Emoji.GREY_EXCLAMATION + " (**%s**) This Relic is already activated.",
                                                context.getUsername()), channel));
                    }

                    LogUtils.info("{User ID: %d} Relic activated. ID: %s", context.getAuthorId().asLong(), arg);

                    final Snowflake guildId = relic.getType() == RelicType.GUILD ? context.getGuildId() : null;
                    return relic.activate(context.getAuthorId(), guildId)
                            .then(context.getChannel())
                            .flatMap(channel -> DiscordUtils.sendMessage(
                                    String.format(Emoji.CHECK_MARK + " (**%s**) Relic successfully activated, enjoy!",
                                            context.getUsername()), channel));
                })
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Activate a relic.")
                .addArg("key", false)
                .build();
    }
}
