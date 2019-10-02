package com.shadorc.shadbot.command.music;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.premium.PremiumManager;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.listener.music.AudioLoadResultListener;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NetUtils;
import com.shadorc.shadbot.utils.StringUtils;
import com.shadorc.shadbot.utils.TextUtils;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

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
        final Snowflake guildId = context.getGuildId();

        return context.getChannel()
                .flatMap(channel -> DiscordUtils.requirePermissions(channel, Permission.CONNECT, Permission.SPEAK)
                        .then(DiscordUtils.requireSameVoiceChannel(context))
                        .zipWith(NetUtils.isValidUrl(arg)
                                .map(isValidUrl -> PlayCmd.getIdentifier(arg, isValidUrl)))
                        .flatMap(tuple -> {
                            final Snowflake voiceChannelId = tuple.getT1();
                            final String identifier = tuple.getT2();

                            final GuildMusic guildMusic = MusicManager.getInstance().getOrCreate(context.getClient(), guildId, voiceChannelId);
                            if (guildMusic.isWaitingForChoice()) {
                                if (guildMusic.getDjId().equals(context.getAuthorId())) {
                                    return Mono.error(new CommandException(String.format("You're already selecting a music. "
                                            + "Enter a number or use `%scancel` to cancel the selection.", context.getPrefix())));
                                }

                                if (identifier.startsWith(AudioLoadResultListener.SC_SEARCH) || identifier.startsWith(AudioLoadResultListener.YT_SEARCH)) {
                                    return context.getClient().getUserById(guildMusic.getDjId())
                                            .map(User::getUsername)
                                            .flatMap(djName -> DiscordUtils.sendMessage(String.format(Emoji.HOURGLASS + " (**%s**) **%s** is "
                                                            + "already selecting a music, please wait for him to finish.",
                                                    context.getUsername(), djName), channel))
                                            .then();
                                }
                            }

                            if (guildMusic.getTrackScheduler().getPlaylist().size() >= Config.DEFAULT_PLAYLIST_SIZE - 1
                                    && !PremiumManager.getInstance().isGuildPremium(guildId)
                                    && !PremiumManager.getInstance().isUserPremium(context.getAuthorId())) {
                                return DiscordUtils.sendMessage(TextUtils.PLAYLIST_LIMIT_REACHED, channel).then();
                            }

                            guildMusic.setMessageChannel(context.getChannelId());

                            final boolean insertFirst = context.getCommandName().endsWith("first");
                            final AudioLoadResultListener resultListener = new AudioLoadResultListener(
                                    guildId, context.getAuthorId(), identifier, insertFirst);

                            guildMusic.addAudioLoadResultListener(resultListener, identifier);

                            return Mono.empty();
                        }));
    }

    private static String getIdentifier(String arg, boolean isValidUrl) {
        // If this is a SoundCloud search...
        if (arg.startsWith("soundcloud ")) {
            return AudioLoadResultListener.SC_SEARCH + StringUtils.remove(arg, "soundcloud ");
        }
        // ... else if the argument is an URL...
        else if (isValidUrl) {
            return arg;
        }
        // ...else, search on YouTube
        else {
            return AudioLoadResultListener.YT_SEARCH + arg;
        }
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Play the music(s) from the url, search terms or playlist.")
                .setFullUsage(String.format("%s%s[first] [soundcloud] <url>", context.getPrefix(), this.getName()))
                .addArg("first", "add the song at the top of the playlist", true)
                .addArg("soundcloud", "search on SoundCloud instead of YouTube", true)
                .setExample(String.format("`%splayfirst soundcloud At Doom's gate`"
                        + "%n`%splay E1M8`", context.getPrefix(), context.getPrefix()))
                .build();
    }
}
