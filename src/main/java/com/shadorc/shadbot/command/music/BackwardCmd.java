package com.shadorc.shadbot.command.music;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.TimeUtil;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class BackwardCmd extends BaseCmd {

    public BackwardCmd() {
        super(CommandCategory.MUSIC, "backward", "Fast backward current music a specified amount of time");
        this.addOption(option -> option.name("time")
                .description("Number of seconds or formatted time (e.g. 72 or 1m12s)")
                .required(true)
                .type(ApplicationCommandOptionType.STRING.getValue()));
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
                        return Mono.error(new CommandException(context.localize("backward.invalid.time")
                                .formatted(time)));
                    }

                    final long newPosition = guildMusic.getTrackScheduler()
                            .changePosition(-duration.toMillis());
                    return context.createFollowupMessage(Emoji.CHECK_MARK, context.localize("backward.message")
                            .formatted(FormatUtil.formatDuration(newPosition)));
                });
    }

}
