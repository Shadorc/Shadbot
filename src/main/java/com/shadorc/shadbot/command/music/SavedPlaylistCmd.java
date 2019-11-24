package com.shadorc.shadbot.command.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.exception.MissingArgumentException;
import com.shadorc.shadbot.listener.music.AudioLoadResultListener;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.music.TrackScheduler;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.StringUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SavedPlaylistCmd extends BaseCmd {

    private enum Action {
        SEE, SAVE, DELETE, LOAD;
    }

    public SavedPlaylistCmd() {
        super(CommandCategory.MUSIC, List.of("saved_playlist", "saved-playlist", "savedplaylist"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        if (!DatabaseManager.getPremium().isGuildPremium(context.getGuildId())) {
            return context.getChannel()
                    .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.LOCK + "  This server is not premium. " +
                            "You can **unlock this feature for this server and gain other advantage** by contributing " +
                            "to Shadbot. More info here: <%s>", Config.PATREON_URL), channel))
                    .then();
        }

        final List<String> args = context.requireArgs(1, 2);

        final Action action = Utils.parseEnum(Action.class, args.get(0),
                new CommandException(String.format("`%s` is not a valid action. %s",
                        args.get(0), FormatUtils.options(Action.class))));

        final Map<String, List<String>> map = DatabaseManager.getGuilds().getDBGuild(context.getGuildId()).getSettings().getSavedPlaylists();

        switch (action) {
            case SEE:
                return this.see(context, map);
            case SAVE:
                return this.save(context, args, map);
            case DELETE:
                return this.delete(context, args, map);
            case LOAD:
                return this.load(context, args, map);
            default:
                return Mono.error(AssertionError::new);
        }
    }

    private Mono<Void> see(Context context, Map<String, List<String>> map) {
        if (map.isEmpty()) {
            return context.getChannel()
                    .flatMap(channel -> DiscordUtils.sendMessage(
                            String.format(Emoji.INFO + " (**%s**) There are no playlists saved. To save a playlist," +
                                            " use `%s%s %s` while listening to music.",
                                    context.getUsername(), context.getPrefix(), this.getName(), Action.SAVE.toString().toLowerCase()), channel))
                    .then();
        }

        final StringBuilder strBuilder = new StringBuilder();

        int index = 1;
        for (final Map.Entry<String, List<String>> entry : map.entrySet()) {
            strBuilder.append(String.format("%d. %s (%s)%n",
                    index, entry.getKey(), StringUtils.pluralOf(entry.getValue().size(), "music")));
            index++;
        }

        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(DiscordUtils.getDefaultEmbed()
                        .andThen(embed -> embed.setAuthor("Saved playlists", null, context.getAvatarUrl())
                                .setThumbnail("https://i.imgur.com/IG3Hj2W.png")
                                .setDescription(strBuilder.toString())), channel))
                .then();
    }

    private Mono<Void> save(Context context, List<String> args, Map<String, List<String>> map) {
        if (args.size() < 2) {
            return Mono.error(new MissingArgumentException());
        }

        if (map.size() >= Config.SAVED_PLAYLIST_SIZE) {
            return Mono.error(new CommandException(String.format("You cannot save more than %d playlists.",
                    Config.SAVED_PLAYLIST_SIZE)));
        }

        final String playlistName = args.get(1);

        if (map.containsKey(playlistName)) {
            return Mono.error(new IllegalArgumentException("There is already a playlist with this name."));
        }

        final TrackScheduler trackScheduler = context.requireGuildMusic().getTrackScheduler();

        final List<AudioTrack> playlist = new ArrayList<>();
        playlist.add(trackScheduler.getAudioPlayer().getPlayingTrack());
        playlist.addAll(trackScheduler.getPlaylist());

        final List<String> urls = playlist.stream()
                .map(AudioTrack::getInfo)
                .map(info -> info.uri)
                .collect(Collectors.toList());

        map.put(playlistName, urls);
        DatabaseManager.getGuilds().getDBGuild(context.getGuildId()).setSetting(Setting.SAVED_PLAYLISTS, map);

        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(
                        String.format(Emoji.CHECK_MARK + " (**%s**) Playlist `%s` saved.", context.getUsername(), playlistName), channel))
                .then();
    }

    private Mono<Void> delete(Context context, List<String> args, Map<String, List<String>> map) {
        if (args.size() < 2) {
            return Mono.error(new MissingArgumentException());
        }

        final String playlistName = args.get(1);

        if (!map.containsKey(playlistName)) {
            return Mono.error(new IllegalArgumentException("There is no playlist with this name."));
        }

        map.remove(playlistName);
        DatabaseManager.getGuilds().getDBGuild(context.getGuildId()).setSetting(Setting.SAVED_PLAYLISTS, map);

        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(
                        String.format(Emoji.CHECK_MARK + " (**%s**) Playlist `%s` deleted.", context.getUsername(), playlistName), channel))
                .then();
    }

    private Mono<Void> load(Context context, List<String> args, Map<String, List<String>> map) {
        if (args.size() < 2) {
            return Mono.error(new MissingArgumentException());
        }

        final GuildMusic guildMusic = context.requireGuildMusic();

        final String playlistName = args.get(1);
        if (!map.containsKey(playlistName)) {
            return Mono.error(new CommandException(String.format("There is no playlist saved with this name. " +
                            "Use `%s%s %s` to see all the playlists saved.",
                    context.getPrefix(), this.getName(), Action.SEE.toString().toLowerCase())));
        }

        for (final String url : map.get(playlistName)) {
            final AudioLoadResultListener resultListener = new AudioLoadResultListener(
                    context.getGuildId(), context.getAuthorId(), url, false);
            guildMusic.addAudioLoadResultListener(resultListener, url);
        }

        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(
                        String.format(Emoji.CHECK_MARK + " (**%s**) Playlist `%s` loaded.", context.getUsername(), playlistName), channel))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Display, save, delete or load saved playlists.")
                .addArg("action", FormatUtils.format(Action.class, "/"), false)
                .addArg("name", "playlist name, must be specified to save, delete or load a playlist", true)
                .addField("Info", String.format("You can save up to %d playlist.", Config.SAVED_PLAYLIST_SIZE), false)
                .build();
    }
}
