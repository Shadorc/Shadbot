package com.locibot.locibot.command.music;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.music.GuildMusic;
import com.locibot.locibot.music.TrackScheduler;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.DiscordUtil;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

public class RepeatCmd extends BaseCmd {

    public RepeatCmd() {
        super(CommandCategory.MUSIC, "repeat", "Toggle song/playlist repetition");
        this.addOption("mode", "none/song/playlist (disable repetition or repeat the current song/playlist)",
                false, ApplicationCommandOptionType.STRING, DiscordUtil.toOptions(TrackScheduler.RepeatMode.class));
    }

    @Override
    public Mono<?> execute(Context context) {
        final GuildMusic guildMusic = context.requireGuildMusic();

        return DiscordUtil.requireVoiceChannel(context)
                .flatMap(__ -> {
                    final TrackScheduler scheduler = guildMusic.getTrackScheduler();
                    final TrackScheduler.RepeatMode oldMode = scheduler.getRepeatMode();
                    final TrackScheduler.RepeatMode newMode =
                            context.getOptionAsEnum(TrackScheduler.RepeatMode.class, "mode")
                                    .orElse(oldMode == TrackScheduler.RepeatMode.NONE ?
                                            TrackScheduler.RepeatMode.SONG : TrackScheduler.RepeatMode.NONE);

                    if (oldMode == newMode) {
                        return context.createFollowupMessage(Emoji.INFO, context.localize("repeat.already.set")
                                .formatted(oldMode.name().toLowerCase()));
                    }

                    scheduler.setRepeatMode(newMode);

                    if (newMode == TrackScheduler.RepeatMode.NONE) {
                        return context.createFollowupMessage(Emoji.PLAY, context.localize("repeat.disabled"));
                    }

                    final StringBuilder strBuilder = new StringBuilder(Emoji.REPEAT + " ");
                    if (oldMode == TrackScheduler.RepeatMode.PLAYLIST) {
                        strBuilder.append(context.localize("repeat.playlist.disabled"));
                    } else if (oldMode == TrackScheduler.RepeatMode.SONG) {
                        strBuilder.append(context.localize("repeat.song.disabled"));
                    }

                    strBuilder.append(' ');

                    if (newMode == TrackScheduler.RepeatMode.PLAYLIST) {
                        strBuilder.append(context.localize("repeat.playlist.enabled"));
                    } else if (newMode == TrackScheduler.RepeatMode.SONG) {
                        strBuilder.append(context.localize("repeat.song.enabled"));
                    }

                    return context.createFollowupMessage(strBuilder.toString());
                });
    }

}
