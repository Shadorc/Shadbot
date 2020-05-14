package com.shadorc.shadbot.command.music;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class BoostBassCmd extends BaseCmd {

    private static final int VALUE_MIN = 0;
    private static final int VALUE_MAX = 200;

    public BoostBassCmd() {
        super(CommandCategory.MUSIC, List.of("bass"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        final Integer percentage = NumberUtils.toIntBetweenOrNull(arg, VALUE_MIN, VALUE_MAX);
        if (percentage == null) {
            return Mono.error(new CommandException(
                    String.format("Incorrect value. Must be between **%d** and **%d**.", VALUE_MIN, VALUE_MAX)));
        }

        context.requireGuildMusic()
                .getTrackScheduler()
                .boost(percentage);

        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(
                        String.format(Emoji.CHECK_MARK + " Bass boosted by **%s**.",
                                context.getUsername()), channel))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Enable/disable bass boost.")
                .addArg("percentage",
                        String.format("boost percentage, must be between %d and %d. %d disables bass boost.",
                                VALUE_MIN, VALUE_MAX, VALUE_MIN), false)
                .build();
    }
}
