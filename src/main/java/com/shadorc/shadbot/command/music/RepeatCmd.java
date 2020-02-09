package com.shadorc.shadbot.command.music;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.music.TrackScheduler;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class RepeatCmd extends BaseCmd {

    public RepeatCmd() {
        super(CommandCategory.MUSIC, List.of("repeat", "loop"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final GuildMusic guildMusic = context.requireGuildMusic();

        return DiscordUtils.requireSameVoiceChannel(context)
                .map(voiceChannelId -> {
                    final TrackScheduler scheduler = guildMusic.getTrackScheduler();
                    final TrackScheduler.RepeatMode oldMode = scheduler.getRepeatMode();
                    final TrackScheduler.RepeatMode newMode = context.getArg()
                            .map(str -> Utils.parseEnum(TrackScheduler.RepeatMode.class, str,
                                    new CommandException(String.format("`%s` is not a valid mode.", str))))
                            .orElse(oldMode == TrackScheduler.RepeatMode.NONE ?
                                    TrackScheduler.RepeatMode.SONG : TrackScheduler.RepeatMode.NONE);

                    if (oldMode == newMode) {
                        return String.format(Emoji.INFO + " Repeat mode already set to %s.",
                                oldMode.toString().toLowerCase());
                    }

                    scheduler.setRepeatMode(newMode);

                    if (newMode == TrackScheduler.RepeatMode.NONE) {
                        return String.format(Emoji.PLAY + " Repetition disabled by **%s**.", context.getUsername());
                    }

                    final StringBuilder strBuilder = new StringBuilder(Emoji.REPEAT.toString() + " ");
                    if (oldMode == TrackScheduler.RepeatMode.PLAYLIST) {
                        strBuilder.append("Playlist repetition disabled. ");
                    } else if (oldMode == TrackScheduler.RepeatMode.SONG) {
                        strBuilder.append("Song repetition disabled. ");
                    }

                    if (newMode == TrackScheduler.RepeatMode.PLAYLIST) {
                        strBuilder.append("Playlist ");
                    } else if (newMode == TrackScheduler.RepeatMode.SONG) {
                        strBuilder.append("Song ");
                    }

                    return strBuilder.append(String.format("repetition enabled by **%s**.", context.getUsername()))
                            .toString();
                })
                .flatMap(message -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(message, channel)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Toggle song/playlist repetition.")
                .setUsage("[none/song/playlist]")
                .addArg("none/song/playlist", "disable repetition or repeat the current song/playlist", true)
                .build();
    }

}
