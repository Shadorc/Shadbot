package com.shadorc.shadbot.command.music;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import com.shadorc.shadbot.utils.TimeUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ForwardCmd extends BaseCmd {

    public ForwardCmd() {
        super(CommandCategory.MUSIC, List.of("forward"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final GuildMusic guildMusic = context.requireGuildMusic();
        final String arg = context.requireArg();

        return DiscordUtils.requireVoiceChannel(context)
                .map(ignored -> {
                    // If the argument is a number of seconds...
                    Long num = NumberUtils.toPositiveLongOrNull(arg);
                    if (num == null) {
                        try {
                            // ... else, try to parse it
                            num = TimeUtils.parseTime(arg);
                        } catch (final IllegalArgumentException err) {
                            throw new CommandException(String.format("`%s` is not a valid number / time.", arg));
                        }
                    }

                    final long newPosition = guildMusic.getTrackScheduler()
                            .changePosition(TimeUnit.SECONDS.toMillis(num));
                    return String.format(Emoji.CHECK_MARK + " New position set to **%s** by **%s**.",
                            FormatUtils.formatDuration(newPosition), context.getUsername());
                })
                .flatMap(message -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(message, channel)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Fast forward current song a specified amount of time.")
                .addArg("time", "can be seconds or time (e.g. 72 or 1m12s)", false)
                .build();
    }
}
