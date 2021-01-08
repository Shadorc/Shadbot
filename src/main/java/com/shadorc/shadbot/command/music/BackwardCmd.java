package com.shadorc.shadbot.command.music;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import com.shadorc.shadbot.utils.TimeUtils;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

public class BackwardCmd extends BaseCmd {

    public BackwardCmd() {
        super(CommandCategory.MUSIC, "backward", "Fast backward current music a specified amount of time");
        this.setDefaultRateLimiter();
    }

    @Override
    public ApplicationCommandRequest build(ImmutableApplicationCommandRequest.Builder builder) {
        return builder
                .addOption(ApplicationCommandOptionData.builder()
                        .name("time")
                        .description("can be seconds or time (e.g. 72 or 1m12s)")
                        .type(ApplicationCommandOptionType.STRING.getValue())
                        .required(true)
                        .build())
                .build();
    }

    @Override
    public Mono<?> execute(Context context) {
        final GuildMusic guildMusic = context.requireGuildMusic();

        return DiscordUtils.requireVoiceChannel(context)
                .flatMap(__ -> {
                    final String option = context.getOption("time").orElseThrow();

                    // If the argument is a number of seconds...
                    Long time = NumberUtils.toPositiveLongOrNull(option);
                    if (time == null) {
                        try {
                            // ... else, try to parse it
                            time = TimeUtils.parseTime(option);
                        } catch (final IllegalArgumentException err) {
                            return Mono.error(new CommandException(String.format("`%s` is not a valid number / time.", option)));
                        }
                    }

                    final long newPosition = guildMusic.getTrackScheduler()
                            .changePosition(-TimeUnit.SECONDS.toMillis(time));
                    return context.createFollowupMessage(Emoji.CHECK_MARK + " (**%s**) New position set to **%s**.",
                            FormatUtils.formatDuration(newPosition), context.getAuthorName());
                });
    }

}
