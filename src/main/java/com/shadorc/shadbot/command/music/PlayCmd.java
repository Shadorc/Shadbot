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
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.StringUtils;
import com.shadorc.shadbot.utils.TextUtils;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.util.List;
import java.util.function.Consumer;

public class PlayCmd extends BaseCmd {

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
                                .flatMap(guildMusic -> PlayCmd.play(context, channel, guildMusic, PlayCmd.getIdentifier(arg)))));
    }

    private static String getIdentifier(String arg) {
        // If this is a SoundCloud search...
        if (arg.startsWith("soundcloud ")) {
            return AudioLoadResultListener.SC_SEARCH + StringUtils.remove(arg, "soundcloud ");
        } else {
            try {
                // ... else if the argument is a valid URL...
                new URL(arg);
                return arg;
            } catch (final Exception ignored) {
                // ...else, search on YouTube
                return AudioLoadResultListener.YT_SEARCH + arg;
            }
        }
    }

    private static Mono<Void> play(Context context, MessageChannel channel, GuildMusic guildMusic, String identifier) {
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

        return DatabaseManager.getPremium().isPremium(context.getGuildId(), context.getAuthorId())
                .flatMap(isPremium -> {
                    if (guildMusic.getTrackScheduler().getPlaylist().size() >= Config.PLAYLIST_SIZE - 1 && !isPremium) {
                        return DiscordUtils.sendMessage(TextUtils.PLAYLIST_LIMIT_REACHED, channel).then();
                    }

                    guildMusic.setMessageChannel(context.getChannelId());

                    final boolean insertFirst = context.getCommandName().endsWith("first");
                    final AudioLoadResultListener resultListener = new AudioLoadResultListener(
                            context.getGuildId(), context.getAuthorId(), identifier, insertFirst);

                    guildMusic.addAudioLoadResultListener(resultListener, identifier);

                    return Mono.empty();
                });
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Play the music(s) from the url, search terms or playlist.")
                .setFullUsage(String.format("%s%s[first] [soundcloud] <url>", context.getPrefix(), this.getName()))
                .addArg("first", "add the song at the top of the playlist", true)
                .addArg("soundcloud", "search on SoundCloud instead of YouTube", true)
                .setExample(String.format("`%splayfirst soundcloud At Doom's gate`"
                        + "%n`%splay E1M8`", context.getPrefix(), context.getPrefix()))
                .build();
    }
}
