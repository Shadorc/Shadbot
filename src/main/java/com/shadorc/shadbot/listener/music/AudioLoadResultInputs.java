package com.shadorc.shadbot.listener.music;

import com.shadorc.shadbot.Config;
import com.shadorc.shadbot.core.command.CommandManager;
import com.shadorc.shadbot.data.database.DatabaseManager;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.Inputs;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import com.shadorc.shadbot.utils.StringUtils;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class AudioLoadResultInputs extends Inputs {

    private final AudioLoadResultListener listener;

    public AudioLoadResultInputs(DiscordClient client, Duration timeout, AudioLoadResultListener listener) {
        super(client, timeout);
        this.listener = listener;
    }

    @Override
    public Mono<Boolean> isValidEvent(MessageCreateEvent event) {
        final GuildMusic guildMusic = MusicManager.getInstance().getMusic(this.listener.getGuildId());
        return Mono.just(guildMusic != null
                && event.getMessage().getContent().isPresent()
                && event.getMessage().getChannelId().equals(guildMusic.getMessageChannelId())
                && event.getMember().map(User::getId).map(guildMusic.getDjId()::equals).orElse(false));
    }

    @Override
    public Mono<Void> processEvent(MessageCreateEvent event) {
        final GuildMusic guildMusic = MusicManager.getInstance().getMusic(this.listener.getGuildId());

        final String content = event.getMessage().getContent().orElseThrow();
        final String prefix = DatabaseManager.getInstance().getDBGuild(this.listener.getGuildId()).getPrefix();
        if (content.equals(String.format("%scancel", prefix))) {
            guildMusic.setWaitingForChoice(false);
            return guildMusic.getMessageChannel()
                    .flatMap(channel -> DiscordUtils.sendMessage(
                            String.format(Emoji.CHECK_MARK + " **%s** cancelled his choice.",
                                    event.getMember().orElseThrow().getUsername()), channel))
                    .then();
        }

        // Remove prefix and command names from message content
        String contentCleaned = StringUtils.remove(content, prefix);
        contentCleaned = StringUtils.remove(contentCleaned, CommandManager.getInstance()
                .getCommand("play").getNames().toArray(new String[0]));

        final Set<Integer> choices = new HashSet<>();
        for (final String choice : contentCleaned.split(",")) {
            // If the choice is not valid, ignore the message
            final Integer num = NumberUtils.toIntBetweenOrNull(choice.trim(), 1, Math.min(Config.MUSIC_SEARCHES, this.listener.getResultTracks().size()));
            if (num == null) {
                return Mono.empty();
            }

            choices.add(num);
        }

        choices.forEach(choice -> this.listener.trackLoaded(this.listener.getResultTracks().get(choice - 1)));

        guildMusic.setWaitingForChoice(false);
        return Mono.empty();
    }

    @Override
    public boolean takeEventWile(MessageCreateEvent ignored) {
        final GuildMusic guildMusic = MusicManager.getInstance().getMusic(this.listener.getGuildId());
        return guildMusic != null && guildMusic.isWaitingForChoice();
    }

}
