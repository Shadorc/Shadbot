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
                    final TrackScheduler.RepeatMode newMode = context.getArg()
                            .map(str -> Utils.parseEnum(TrackScheduler.RepeatMode.class, str,
                                    new CommandException(String.format("`%s` is not a valid mode.", str))))
                            .orElse(TrackScheduler.RepeatMode.SONG);
                    final TrackScheduler.RepeatMode oldMode = scheduler.getRepeatMode();

                    scheduler.setRepeatMode(oldMode == newMode ? TrackScheduler.RepeatMode.NONE : newMode);

                    if (newMode == oldMode) {
                        return String.format(Emoji.PLAY + " Repetition disabled by **%s**.", context.getUsername());
                    }

                    final StringBuilder strBuilder = new StringBuilder(Emoji.REPEAT.toString());
                    if (oldMode == TrackScheduler.RepeatMode.PLAYLIST && newMode == TrackScheduler.RepeatMode.SONG) {
                        strBuilder.append(" Playlist repetition disabled. ");
                    } else if (oldMode == TrackScheduler.RepeatMode.SONG && newMode == TrackScheduler.RepeatMode.PLAYLIST) {
                        strBuilder.append(" Song repetition disabled. ");
                    }

                    strBuilder.append(String.format(" %s repetition enabled by **%s**.",
                            newMode == TrackScheduler.RepeatMode.PLAYLIST ? "Playlist" : "Song",
                            context.getUsername()));
                    return strBuilder.toString();
                })
                .flatMap(message -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(message, channel)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Toggle song/playlist repetition.")
                .setUsage("[song/playlist]")
                .addArg("song/playlist", "repeat the current song/playlist", true)
                .build();
    }

}
