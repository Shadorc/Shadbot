package me.shadorc.shadbot.command.music;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class BackwardCmd extends BaseCmd {

    public BackwardCmd() {
        super(CommandCategory.MUSIC, List.of("backward"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final GuildMusic guildMusic = context.requireGuildMusic();
        final String arg = context.requireArg();

        return DiscordUtils.requireSameVoiceChannel(context)
                .map(voiceChannelId -> {
                    // If the argument is a number of seconds...
                    Long num = NumberUtils.asPositiveLong(arg);
                    if (num == null) {
                        try {
                            // ... else, try to parse it
                            num = TimeUtils.parseTime(arg);
                        } catch (final IllegalArgumentException err) {
                            throw new CommandException(String.format("`%s` is not a valid number / time.", arg));
                        }
                    }

                    final long newPosition = guildMusic.getTrackScheduler().changePosition(-TimeUnit.SECONDS.toMillis(num));
                    return String.format(Emoji.CHECK_MARK + " New position set to **%s** by **%s**.",
                            FormatUtils.shortDuration(newPosition), context.getUsername());
                })
                .flatMap(message -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(message, channel)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Fast backward current song a specified amount of time.")
                .addArg("time", "can be seconds or time (e.g. 72 or 1m12s)", false)
                .build();
    }
}
