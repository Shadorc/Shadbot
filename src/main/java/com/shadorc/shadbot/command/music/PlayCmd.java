package com.shadorc.shadbot.command.music;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.listener.music.AudioLoadResultListener;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NetUtils;
import com.shadorc.shadbot.utils.ShadbotUtils;
import com.shadorc.shadbot.utils.StringUtils;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static com.shadorc.shadbot.music.MusicManager.LOGGER;

public class PlayCmd extends BaseCmd {

    private static final String SC_QUERY = "soundcloud ";

    public PlayCmd() {
        super(CommandCategory.MUSIC, List.of("play", "add", "queue", "playfirst", "addfirst", "queuefirst"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        return DiscordUtils.requireVoiceChannel(context)
                .flatMap(voiceChannel -> context.getChannel()
                        .flatMap(channel -> MusicManager.getInstance()
                                .getOrCreate(context.getClient(), context.getGuildId(), voiceChannel.getId())
                                .flatMap(guildMusic -> this.play(context, channel, guildMusic, this.getIdentifier(arg)))))
                .doOnError(TimeoutException.class, err -> LOGGER.info("{Guild ID: {}} Voice channel connection timed out",
                        context.getGuildId().asLong()))
                .onErrorMap(TimeoutException.class, err -> new CommandException("An error occurred while joining the voice channel, " +
                        "please try again later or in another voice channel."));
    }

    private String getIdentifier(String arg) {
        // If this is a SoundCloud search...
        if (arg.startsWith(SC_QUERY)) {
            return AudioLoadResultListener.SC_SEARCH + StringUtils.remove(arg, SC_QUERY);
        }
        // ... else if the argument is a valid URL...
        else if (NetUtils.isUrl(arg)) {
            return arg;
        }
        // ...else, search on YouTube
        else {
            return AudioLoadResultListener.YT_SEARCH + arg;
        }
    }

    private Mono<Void> play(Context context, MessageChannel channel, GuildMusic guildMusic, String identifier) {
        // Someone is already selecting a music...
        if (guildMusic.isWaitingForChoice()) {
            if (guildMusic.getDjId().equals(context.getAuthorId())) {
                return Mono.error(new CommandException(String.format("You're already selecting a music. "
                        + "Enter a number or use `%scancel` to cancel the selection.", context.getPrefix())));
            }

            if (identifier.startsWith(AudioLoadResultListener.SC_SEARCH) || identifier.startsWith(AudioLoadResultListener.YT_SEARCH)) {
                return context.getClient()
                        .getUserById(guildMusic.getDjId())
                        .map(User::getUsername)
                        .flatMap(djName -> DiscordUtils.sendMessage(String.format(Emoji.HOURGLASS + " (**%s**) **%s** is "
                                        + "already selecting a music, please wait for him to finish.",
                                context.getUsername(), djName), channel))
                        .then();
            }
        }

        return DatabaseManager.getPremium()
                .isPremium(context.getGuildId(), context.getAuthorId())
                .filter(isPremium -> guildMusic.getTrackScheduler().getPlaylist().size() < Config.PLAYLIST_SIZE - 1 || isPremium)
                .doOnNext(ignored -> {
                    final boolean insertFirst = context.getCommandName().endsWith("first");
                    final AudioLoadResultListener resultListener = new AudioLoadResultListener(
                            context.getGuildId(), context.getAuthorId(), identifier, insertFirst);

                    guildMusic.setMessageChannelId(context.getChannelId());
                    guildMusic.addAudioLoadResultListener(resultListener, identifier);
                })
                .switchIfEmpty(DiscordUtils.sendMessage(ShadbotUtils.PLAYLIST_LIMIT_REACHED, channel)
                        .then(Mono.empty()))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Play the music(s) from the url, search terms or playlist.")
                .setFullUsage(String.format("%s%s[first] [soundcloud] <url>", context.getPrefix(), this.getName()))
                .addArg("first", "add the song at the top of the playlist", true)
                .addArg("soundcloud", "search on SoundCloud instead of YouTube", true)
                .setExample(String.format("`%splayfirst soundcloud At Doom's gate`"
                        + "%n`%splay E1M8`", context.getPrefix(), context.getPrefix()))
                .build();
    }
}
