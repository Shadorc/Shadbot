package com.locibot.locibot.command.music;

import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.music.GuildMusic;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.DiscordUtil;
import com.locibot.locibot.utils.FormatUtil;
import com.locibot.locibot.utils.TimeUtil;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class ForwardCmd extends BaseCmd {

    public ForwardCmd() {
        super(CommandCategory.MUSIC, "forward", "Fast forward the current music a specified amount of time");
        this.addOption("time",
                "Number of seconds or formatted time (e.g. 72 or 1m12s)",
                true,
                ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        final GuildMusic guildMusic = context.requireGuildMusic();

        return DiscordUtil.requireVoiceChannel(context)
                .flatMap(__ -> {
                    final String time = context.getOptionAsString("time").orElseThrow();

                    final Duration duration;
                    try {
                        duration = TimeUtil.parseTime(time);
                    } catch (final IllegalArgumentException err) {
                        return Mono.error(new CommandException(context.localize("forward.invalid.time")
                                .formatted(time)));
                    }

                    final long newPosition = guildMusic.getTrackScheduler()
                            .changePosition(duration.toMillis());
                    return context.createFollowupMessage(Emoji.CHECK_MARK, context.localize("forward.message")
                            .formatted(FormatUtil.formatDuration(newPosition)));
                });
    }

}
