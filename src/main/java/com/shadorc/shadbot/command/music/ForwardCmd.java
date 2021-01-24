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
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

public class ForwardCmd extends BaseCmd {

    public ForwardCmd() {
        super(CommandCategory.MUSIC, "forward", "Fast forward the current music a given amount of time");
        this.setDefaultRateLimiter();
    }

    @Override
    public ApplicationCommandRequest build(ImmutableApplicationCommandRequest.Builder builder) {
        return builder
                .addOption(ApplicationCommandOptionData.builder()
                        .name("time")
                        .description("Can be number of seconds or formatted time (e.g. 72 or 1m12s)")
                        .type(ApplicationCommandOptionType.STRING.getValue())
                        .required(true)
                        .build())
                .build();
    }

    @Override
    public Mono<?> execute(Context context) {
        final GuildMusic guildMusic = context.requireGuildMusic();

        return DiscordUtil.requireVoiceChannel(context)
                .flatMap(__ -> {
                    final String option = context.getOption("time").orElseThrow();

                    // If the argument is a number of seconds...
                    Long time = NumberUtil.toPositiveLongOrNull(option);
                    if (time == null) {
                        try {
                            // ... else, try to parse it
                            time = TimeUtil.parseTime(option);
                        } catch (final IllegalArgumentException err) {
                            return Mono.error(new CommandException(String.format("`%s` is not a valid number / time.", option)));
                        }
                    }

                    final long newPosition = guildMusic.getTrackScheduler()
                            .changePosition(TimeUnit.SECONDS.toMillis(time));
                    return context.createFollowupMessage(Emoji.CHECK_MARK + " (**%s**) New position set to **%s**.",
                            FormatUtil.formatDuration(newPosition), context.getAuthorName());
                });
    }

}
