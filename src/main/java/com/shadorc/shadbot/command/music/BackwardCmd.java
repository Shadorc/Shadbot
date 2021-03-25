package com.shadorc.shadbot.command.music;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.NumberUtil;
import com.shadorc.shadbot.utils.TimeUtil;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

public class BackwardCmd extends BaseCmd {

    public BackwardCmd() {
        super(CommandCategory.MUSIC, "backward", "Fast backward current music a specified amount of time");
        this.addOption("time", "Number of seconds or formatted time (e.g. 72 or 1m12s)",
                true, ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        final GuildMusic guildMusic = context.requireGuildMusic();

        return DiscordUtil.requireVoiceChannel(context)
                .flatMap(__ -> {
                    final String option = context.getOptionAsString("time").orElseThrow();

                    // If the argument is a number of seconds...
                    Long time = NumberUtil.toPositiveLongOrNull(option);
                    if (time == null) {
                        try {
                            // ... else, try to parse it
                            time = TimeUtil.parseTime(option);
                        } catch (final IllegalArgumentException err) {
                            return Mono.error(new CommandException(context.localize("backward.invalid")
                                    .formatted(option)));
                        }
                    }

                    final long newPosition = guildMusic.getTrackScheduler()
                            .changePosition(-TimeUnit.SECONDS.toMillis(time));
                    return context.reply(Emoji.CHECK_MARK, context.localize("backward.message")
                            .formatted(FormatUtil.formatDuration(newPosition)));
                });
    }

}
