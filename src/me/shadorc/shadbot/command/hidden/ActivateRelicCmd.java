package me.shadorc.shadbot.command.hidden;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.premium.PremiumManager;
import me.shadorc.shadbot.exception.RelicActivationException;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
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

        return context.getChannel()
                .flatMap(channel -> Mono.fromCallable(() -> {
                    PremiumManager.getInstance().activateRelic(context.getGuildId(), context.getAuthorId(), arg);
                    return String.format(Emoji.CHECK_MARK + " (**%s**) Relic successfully activated, enjoy!",
                            context.getUsername());
                })
                        .onErrorResume(RelicActivationException.class,
                                err -> Mono.just(String.format(Emoji.GREY_EXCLAMATION + " (**%s**) %s",
                                        context.getUsername(), err.getMessage())))
                        .flatMap(text -> DiscordUtils.sendMessage(text, channel)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Activate a relic.")
                .addArg("key", false)
                .build();
    }
}
