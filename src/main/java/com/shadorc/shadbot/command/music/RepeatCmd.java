package com.shadorc.shadbot.command.music;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.music.TrackScheduler;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import com.shadorc.shadbot.utils.EnumUtil;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

public class RepeatCmd extends BaseCmd {

    public RepeatCmd() {
        super(CommandCategory.MUSIC, "repeat", "Toggle song/playlist repetition");
        this.setDefaultRateLimiter();
    }

    @Override
    public ApplicationCommandRequest build(ImmutableApplicationCommandRequest.Builder builder) {
        return builder
                .addOption(ApplicationCommandOptionData.builder()
                        .name("mode")
                        .description("none/song/playlist (disable repetition or repeat the current song/playlist)")
                        .type(ApplicationCommandOptionType.STRING.getValue())
                        .required(false)
                        .build())
                .build();
    }

    @Override
    public Mono<?> execute(Context context) {
        final GuildMusic guildMusic = context.requireGuildMusic();

        return DiscordUtil.requireVoiceChannel(context)
                .map(__ -> {
                    final TrackScheduler scheduler = guildMusic.getTrackScheduler();
                    final TrackScheduler.RepeatMode oldMode = scheduler.getRepeatMode();
                    final TrackScheduler.RepeatMode newMode = context.getOption("mode")
                            .map(str -> EnumUtil.parseEnum(TrackScheduler.RepeatMode.class, str,
                                    new CommandException(String.format("`%s` is not a valid mode.", str))))
                            .orElse(oldMode == TrackScheduler.RepeatMode.NONE ?
                                    TrackScheduler.RepeatMode.SONG : TrackScheduler.RepeatMode.NONE);

                    if (oldMode == newMode) {
                        return String.format(Emoji.INFO + " Repeat mode already set to %s.",
                                oldMode.toString().toLowerCase());
                    }

                    scheduler.setRepeatMode(newMode);

                    if (newMode == TrackScheduler.RepeatMode.NONE) {
                        return String.format(Emoji.PLAY + " Repetition disabled by **%s**.", context.getAuthorName());
                    }

                    final StringBuilder strBuilder = new StringBuilder(Emoji.REPEAT + " ");
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

                    return strBuilder.append(String.format("repetition enabled by **%s**.", context.getAuthorName()))
                            .toString();
                })
                .flatMap(context::createFollowupMessage);
    }

}
