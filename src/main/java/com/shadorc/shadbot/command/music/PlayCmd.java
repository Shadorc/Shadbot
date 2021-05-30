package com.shadorc.shadbot.command.music;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.command.MissingPermissionException;
import com.shadorc.shadbot.core.command.Cmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.database.DatabaseManager;
import com.shadorc.shadbot.listener.music.AudioLoadResultListener;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import com.shadorc.shadbot.utils.NetUtil;
import discord4j.core.object.entity.User;
import discord4j.rest.util.ApplicationCommandOptionType;
import discord4j.voice.retry.VoiceGatewayException;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.concurrent.TimeoutException;

import static com.shadorc.shadbot.music.MusicManager.LOGGER;

public class PlayCmd extends Cmd {

    public PlayCmd() {
        super(CommandCategory.MUSIC, "play", "Play the music(s) from the url, search terms or playlist");

        this.addOption(option -> option.name("music")
                .description("The url, search terms or playlist")
                .required(true)
                .type(ApplicationCommandOptionType.STRING.getValue()));
        this.addOption(option -> option.name("first")
                .description("Add the song first in the playlist")
                .required(false)
                .type(ApplicationCommandOptionType.BOOLEAN.getValue()));
        this.addOption(option -> option.name("soundcloud")
                .description("Search on SoundCloud")
                .required(false)
                .type(ApplicationCommandOptionType.BOOLEAN.getValue()));
    }

    @Override
    public Mono<?> execute(Context context) {
        final String query = context.getOptionAsString("music").orElseThrow();
        final boolean playFirst = context.getOptionAsBool("first").orElse(false);
        final boolean isSoundcloud = context.getOptionAsBool("soundcloud").orElse(false);

        return DiscordUtil.requireVoiceChannel(context)
                .flatMap(voiceChannel -> {
                    final String identifier = PlayCmd.getIdentifier(query, isSoundcloud);
                    return MusicManager
                            .getOrCreate(context.getClient(), context.getLocale(), context.getGuildId(), voiceChannel.getId())
                            .flatMap(guildMusic -> PlayCmd.play(context, guildMusic, identifier, playFirst));
                })
                .onErrorMap(err -> {
                    LOGGER.info("{Guild ID: {}} An error occurred while joining a voice channel: {}",
                            context.getGuildId().asString(), err.getMessage());

                    if (!(err instanceof CommandException) && !(err instanceof MissingPermissionException)) {
                        Telemetry.VOICE_CHANNEL_ERROR_COUNTER.labels(err.getClass().getSimpleName()).inc();
                    }

                    if (err instanceof VoiceGatewayException) {
                        return new CommandException(context.localize("play.exception.voice.gateway"));
                    }
                    if (err instanceof TimeoutException) {
                        return new CommandException(context.localize("play.exception.timeout"));
                    }
                    return err;
                });
    }

    private static String getIdentifier(String query, boolean isSoundCloud) {
        // If this is a SoundCloud search...
        if (isSoundCloud) {
            return AudioLoadResultListener.SC_SEARCH + query;
        }
        // ... else if the argument is a valid URL...
        else if (NetUtil.isUrl(query)) {
            return query;
        }
        // ...else, search on YouTube
        else {
            return AudioLoadResultListener.YT_SEARCH + query;
        }
    }

    private static Mono<?> play(Context context, GuildMusic guildMusic, String identifier, boolean playFirst) {
        // Someone is already selecting a music...
        if (guildMusic.isWaitingForChoice()) {
            if (guildMusic.getDjId().equals(context.getAuthorId())) {
                return Mono.error(new CommandException(context.localize("play.self.selecting")));
            }

            if (identifier.startsWith(AudioLoadResultListener.YT_SEARCH) || identifier.startsWith(AudioLoadResultListener.SC_SEARCH)) {
                return guildMusic.getDj()
                        .map(User::getUsername)
                        .flatMap(username -> context.editFollowupMessage(Emoji.HOURGLASS,
                                context.localize("play.other.selecting").formatted(username)));
            }
        }

        return DatabaseManager.getPremium()
                .isPremium(context.getGuildId(), context.getAuthorId())
                .filter(isPremium -> guildMusic.getTrackScheduler().getPlaylist().size() < Config.PLAYLIST_SIZE - 1 || isPremium)
                .doOnNext(__ -> {
                    guildMusic.setMessageChannelId(context.getChannelId());

                    final AudioLoadResultListener resultListener = new AudioLoadResultListener(
                            context, context.getLocale(), context.getGuildId(), context.getAuthorId(), identifier, playFirst);
                    guildMusic.addAudioLoadResultListener(resultListener);
                })
                .switchIfEmpty(context.editFollowupMessage(Emoji.LOCK,
                        context.localize("playlist.limit.reached")
                                .formatted(Config.PLAYLIST_SIZE, Config.PATREON_URL))
                        .then(Mono.empty()));
    }

}
