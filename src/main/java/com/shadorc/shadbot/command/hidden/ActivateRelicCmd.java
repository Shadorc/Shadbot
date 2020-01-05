package com.shadorc.shadbot.command.hidden;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.premium.RelicType;
import com.shadorc.shadbot.db.premium.entity.Relic;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.LogUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class ActivateRelicCmd extends BaseCmd {

    public ActivateRelicCmd() {
        super(CommandCategory.HIDDEN, List.of("activate_relic", "activate-relic", "activaterelic"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        final Optional<Relic> relicOpt = DatabaseManager.getPremium().getRelicById(arg);

        if (relicOpt.isEmpty()) {
            return context.getChannel()
                    .flatMap(channel -> DiscordUtils.sendMessage(
                            String.format(Emoji.GREY_EXCLAMATION + " (**%s**) This Relic doesn't exist.", context.getUsername()), channel))
                    .then();
        }

        final Relic relic = relicOpt.get();

        if (relic.getActivation().isPresent()) {
            return context.getChannel()
                    .flatMap(channel -> DiscordUtils.sendMessage(
                            String.format(Emoji.GREY_EXCLAMATION + " (**%s**) This Relic is already activated.", context.getUsername()), channel))
                    .then();
        }

        relic.activate(context.getAuthorId(), relic.getType() == RelicType.GUILD ? context.getGuildId() : null);

        LogUtils.info("{User ID: %d} Relic activated. ID: %s", context.getAuthorId().asLong(), arg);
        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(
                        String.format(Emoji.CHECK_MARK + " (**%s**) Relic successfully activated, enjoy!",
                                context.getUsername()), channel))
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
