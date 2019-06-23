package me.shadorc.shadbot.command.music;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.TrackScheduler;
import me.shadorc.shadbot.music.TrackScheduler.RepeatMode;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.object.help.HelpBuilder;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.Utils;
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
                    RepeatMode mode;
                    if (context.getArg().isPresent()) {
                        mode = Utils.parseEnum(RepeatMode.class, context.getArg().get(),
                                new CommandException(String.format("`%s` is not a valid mode.", context.getArg().get())));
                    }
                    // By default, modifications are made on song repeat mode
                    else {
                        mode = RepeatMode.SONG;
                    }

                    final TrackScheduler scheduler = guildMusic.getTrackScheduler();
                    scheduler.setRepeatMode(scheduler.getRepeatMode() == mode ? RepeatMode.NONE : mode);

                    final Emoji emoji = scheduler.getRepeatMode() == RepeatMode.NONE ? Emoji.PLAY : Emoji.REPEAT;
                    final String playlistRepetition = mode == RepeatMode.PLAYLIST ? "Playlist " : "";
                    final String modeStr = scheduler.getRepeatMode() == RepeatMode.NONE ? "disabled" : "enabled";

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
