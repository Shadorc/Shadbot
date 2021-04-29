package com.shadorc.shadbot.command.music;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.ratelimiter.RateLimiter;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.NumberUtil;
import discord4j.core.object.entity.Message;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

public class SkipCmd extends BaseCmd {

    public SkipCmd() {
        super(CommandCategory.MUSIC, "skip", "Skip current music and play the next one. " +
                "You can also directly skip to a music in the playlist");
        this.setRateLimiter(new RateLimiter(1, Duration.ofSeconds(1)));
        this.addOption("index", "The index of the music in the playlist to play",
                false, ApplicationCommandOptionType.INTEGER);
    }

    @Override
    public Mono<?> execute(Context context) {
        final GuildMusic guildMusic = context.requireGuildMusic();

        final Mono<Message> sendMessage = context.reply(Emoji.TRACK_NEXT, context.localize("skip.message"));

        final Optional<Long> indexOpt = context.getOptionAsLong("index");
        if (indexOpt.isPresent()) {
            final int playlistSize = guildMusic.getTrackScheduler().getPlaylist().size();
            if (playlistSize == 0) {
                return Mono.error(new CommandException(context.localize("skip.exception.no.playlist")));
            }

            final long index = indexOpt.orElseThrow();
            if (!NumberUtil.isBetween(index, 1, playlistSize)) {
                return Mono.error(new CommandException(context.localize("skip.out.of.index")
                        .formatted(playlistSize)));
            }

            return sendMessage
                    .doOnNext(__ -> {
                        guildMusic.getTrackScheduler().skipTo((int) index);
                        // If the music has been started correctly, we resume it in case the previous music was paused
                        guildMusic.getTrackScheduler().getAudioPlayer().setPaused(false);
                    });
        } else {
            return sendMessage
                    .flatMap(__ -> {
                        // If the music has been started correctly...
                        if (guildMusic.getTrackScheduler().nextTrack()) {
                            // ...we resume it in case the previous music was paused.
                            guildMusic.getTrackScheduler().getAudioPlayer().setPaused(false);
                            return Mono.empty();
                        } else {
                            // there is no more music, this is the end.
                            return guildMusic.end();
                        }
                    });
        }
    }

}
