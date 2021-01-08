/*
package com.shadorc.shadbot.listener.music;

import com.shadorc.shadbot.core.command.CommandManager;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.db.guilds.entity.Settings;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.Inputs;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import com.shadorc.shadbot.utils.StringUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class AudioLoadResultInputs extends Inputs {

    private final AudioLoadResultListener listener;

    private AudioLoadResultInputs(GatewayDiscordClient gateway, Duration timeout,
                                  Snowflake channelId, AudioLoadResultListener listener) {
        super(gateway, timeout, channelId);
        this.listener = listener;
    }

    public static AudioLoadResultInputs create(GatewayDiscordClient gateway, Duration timeout,
                                               Snowflake channelId, AudioLoadResultListener listener) {
        return new AudioLoadResultInputs(gateway, timeout, channelId, listener);
    }

    @Override
    public Mono<Boolean> isValidEvent(MessageCreateEvent event) {
        return Mono.justOrEmpty(MusicManager.getInstance().getGuildMusic(this.listener.getGuildId()))
                .zipWith(Mono.justOrEmpty(event.getMember()))
                .map(TupleUtils.function((guildMusic, member) -> guildMusic.getDjId().equals(member.getId())));
    }

    @Override
    public Mono<Void> processEvent(MessageCreateEvent event) {
        final Mono<GuildMusic> getGuildMusic = Mono.justOrEmpty(MusicManager.getInstance()
                .getGuildMusic(this.listener.getGuildId()));

        final Mono<String> getPrefix = DatabaseManager.getGuilds()
                .getDBGuild(this.listener.getGuildId())
                .map(DBGuild::getSettings)
                .map(Settings::getPrefix);

        return Mono.zip(getGuildMusic, getPrefix)
                .flatMap(TupleUtils.function((guildMusic, prefix) -> {
                    final String content = event.getMessage().getContent();

                    if (content.equals(String.format("%scancel", prefix))) {
                        guildMusic.setWaitingForChoice(false);
                        return guildMusic.getMessageChannel()
                                .flatMap(channel -> DiscordUtils.sendMessage(
                                        String.format(Emoji.CHECK_MARK + " **%s** cancelled his choice.",
                                                event.getMember().orElseThrow().getUsername()), channel))
                                .then(Mono.empty());
                    }

                    final String[] playCmdNames = CommandManager.getInstance()
                            .getCommand("play")
                            .getNames()
                            .toArray(new String[0]);

                    // Remove prefix and command names from message content
                    String contentCleaned = StringUtils.remove(content, prefix);
                    contentCleaned = StringUtils.remove(contentCleaned, playCmdNames);

                    final Set<Integer> choices = new HashSet<>();
                    for (final String choice : contentCleaned.split(",")) {
                        // If the choice is not valid, ignore the message
                        final Integer num = NumberUtils.toIntBetweenOrNull(choice.trim(), 1,
                                Math.min(Config.MUSIC_SEARCHES, this.listener.getResultTracks().size()));
                        if (num == null) {
                            return Mono.empty();
                        }

                        choices.add(num);
                    }

                    choices.forEach(choice -> this.listener.trackLoaded(this.listener.getResultTracks().get(choice - 1)));
                    guildMusic.setWaitingForChoice(false);
                    return Mono.empty();
                }));
    }

    @Override
    public boolean takeEventWile(MessageCreateEvent event) {
        return MusicManager.getInstance()
                .getGuildMusic(this.listener.getGuildId())
                .map(GuildMusic::isWaitingForChoice)
                .orElse(false);
    }

}
*/
