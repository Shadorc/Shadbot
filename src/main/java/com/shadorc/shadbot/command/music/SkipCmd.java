package com.shadorc.shadbot.command.music;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.ratelimiter.RateLimiter;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

public class SkipCmd extends BaseCmd {

    public SkipCmd() {
        super(CommandCategory.MUSIC, List.of("skip", "next"));
        this.setRateLimiter(new RateLimiter(1, Duration.ofSeconds(1)));
    }

    @Override
    public Mono<Void> execute(Context context) {
        final GuildMusic guildMusic = context.requireGuildMusic();

        final Mono<Message> sendMessage = context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.TRACK_NEXT + " Music skipped by **%s**.",
                        context.getUsername()), channel));

        if (context.getArg().isPresent()) {
            final int playlistSize = guildMusic.getTrackScheduler().getPlaylist().size();
            final Integer num = NumberUtils.toIntBetweenOrNull(context.getArg().orElseThrow(), 1, playlistSize);
            if (num == null) {
                return Mono.error(new CommandException(String.format("Number must be between 1 and %d.",
                        playlistSize)));
            }
            return sendMessage
                    .doOnNext(ignored -> {
                        guildMusic.getTrackScheduler().skipTo(num);
                        // If the music has been started correctly, we resume it in case the previous music was paused
                        guildMusic.getTrackScheduler().getAudioPlayer().setPaused(false);
                    })
                    .then();
        } else {
            return sendMessage
                    .flatMap(ignored -> {
                        // If the music has been started correctly
                        if (guildMusic.getTrackScheduler().nextTrack()) {
                            // we resume it in case the previous music was paused.
                            guildMusic.getTrackScheduler().getAudioPlayer().setPaused(false);
                            return Mono.empty();
                        }
                        // else
                        else {
                            // there is no more music, this is the end.
                            return guildMusic.end();
                        }
                    });
        }
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Skip current music and play the next one if it exists."
                        + "\nYou can also directly skip to a music in the playlist by specifying its number.")
                .addArg("num", "the number of the music in the playlist to play", true)
                .build();
    }
}