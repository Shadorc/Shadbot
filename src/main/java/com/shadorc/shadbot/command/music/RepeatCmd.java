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
                    TrackScheduler.RepeatMode mode;
                    if (context.getArg().isPresent()) {
                        mode = Utils.parseEnum(TrackScheduler.RepeatMode.class, context.getArg().get(),
                                new CommandException(String.format("`%s` is not a valid mode.", context.getArg().get())));
                    }
                    // By default, modifications are made on song repeat mode
                    else {
                        mode = TrackScheduler.RepeatMode.SONG;
                    }

                    final TrackScheduler scheduler = guildMusic.getTrackScheduler();
                    scheduler.setRepeatMode(scheduler.getRepeatMode() == mode ? TrackScheduler.RepeatMode.NONE : mode);

                    final Emoji emoji = scheduler.getRepeatMode() == TrackScheduler.RepeatMode.NONE ? Emoji.PLAY : Emoji.REPEAT;
                    final String playlistRepetition = mode == TrackScheduler.RepeatMode.PLAYLIST ? "Playlist " : "";
                    final String modeStr = scheduler.getRepeatMode() == TrackScheduler.RepeatMode.NONE ? "disabled" : "enabled";

                    return String.format("%s %sRepetition %s by **%s**.", emoji, playlistRepetition, modeStr, context.getUsername());
                })
                .flatMap(message -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(message, channel)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Toggle song/playlist repetition.")
                .setUsage("[song/playlist]")
                .addArg("song/playlist", "repeat the current song/playlist", true)
                .build();
    }

}
