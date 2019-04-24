package me.shadorc.shadbot.listener.music;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.CommandInitializer;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.MusicManager;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.object.Inputs;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.StringUtils;
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
        final GuildMusic guildMusic = MusicManager.getMusic(this.listener.getGuildId());
        return Mono.just(guildMusic != null
                && event.getMessage().getContent().isPresent()
                && event.getMessage().getChannelId().equals(guildMusic.getMessageChannelId())
                && event.getMember().map(User::getId).map(guildMusic.getDjId()::equals).orElse(false));
    }

    @Override
    public Mono<Void> processEvent(MessageCreateEvent event) {
        final GuildMusic guildMusic = MusicManager.getMusic(this.listener.getGuildId());

        final String content = event.getMessage().getContent().get();
        final String prefix = Shadbot.getDatabase().getDBGuild(this.listener.getGuildId()).getPrefix();
        if (content.equals(String.format("%scancel", prefix))) {
            guildMusic.setWaitingForChoice(false);
            return guildMusic.getMessageChannel()
                    .flatMap(channel -> DiscordUtils.sendMessage(
                            String.format(Emoji.CHECK_MARK + " **%s** cancelled his choice.",
                                    event.getMember().get().getUsername()), channel))
                    .then();
        }

        // Remove prefix and command names from message content
        String contentCleaned = StringUtils.remove(content, prefix);
        contentCleaned = StringUtils.remove(contentCleaned, CommandInitializer.getCommand("play").getNames().toArray(new String[0]));

        final Set<Integer> choices = new HashSet<>();
        for (final String choice : contentCleaned.split(",")) {
            // If the choice is not valid, ignore the message
            final Integer num = NumberUtils.asIntBetween(choice.trim(), 1, Math.min(Config.MUSIC_SEARCHES, this.listener.getResultTracks().size()));
            if (num == null) {
                return Mono.empty();
            }

            choices.add(num);
        }

        choices
                .forEach(choice -> this.listener.trackLoaded(this.listener.getResultTracks().get(choice - 1)));

        guildMusic.setWaitingForChoice(false);
        return Mono.empty();
    }

    @Override
    public boolean takeEventWile(MessageCreateEvent ignored) {
        final GuildMusic guildMusic = MusicManager.getMusic(this.listener.getGuildId());
        return guildMusic != null && guildMusic.isWaitingForChoice();
    }

}
