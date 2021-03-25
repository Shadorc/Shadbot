package com.shadorc.shadbot.listener.music;

import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.inputs.MessageInputs;
import com.shadorc.shadbot.utils.DiscordUtil;
import com.shadorc.shadbot.utils.NumberUtil;
import com.shadorc.shadbot.utils.StringUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class AudioLoadResultMessageInputs extends MessageInputs {

    private final AudioLoadResultListener listener;

    private AudioLoadResultMessageInputs(GatewayDiscordClient gateway, Duration timeout,
                                         Snowflake channelId, AudioLoadResultListener listener) {
        super(gateway, timeout, channelId);
        this.listener = listener;
    }

    public static AudioLoadResultMessageInputs create(GatewayDiscordClient gateway, Duration timeout,
                                                      Snowflake channelId, AudioLoadResultListener listener) {
        return new AudioLoadResultMessageInputs(gateway, timeout, channelId, listener);
    }

    @Override
    public Mono<Boolean> isValidEvent(MessageCreateEvent event) {
        return Mono.justOrEmpty(MusicManager.getGuildMusic(this.listener.getGuildId()))
                .zipWith(Mono.justOrEmpty(event.getMember()))
                .map(TupleUtils.function((guildMusic, member) -> guildMusic.getDjId().equals(member.getId())));
    }

    @Override
    public Mono<Void> processEvent(MessageCreateEvent event) {
        final Mono<GuildMusic> getGuildMusic = Mono.justOrEmpty(MusicManager.getGuildMusic(this.listener.getGuildId()));

        return getGuildMusic
                .flatMap(guildMusic -> {
                    final String content = event.getMessage().getContent();

                    if ("/cancel".equals(content)) {
                        guildMusic.setWaitingForChoice(false);
                        return guildMusic.getMessageChannel()
                                .flatMap(channel -> DiscordUtil.sendMessage(
                                        Emoji.CHECK_MARK, "**%s** cancelled his choice."
                                                .formatted(event.getMember().orElseThrow().getUsername()), channel))
                                .then(Mono.empty());
                    }

                    // Remove prefix and command name from message content
                    final String contentCleaned = StringUtil.remove(content, "/play");

                    final Set<Integer> choices = new HashSet<>();
                    for (final String choice : contentCleaned.split(",")) {
                        // If the choice is not valid, ignore the message
                        final Integer num = NumberUtil.toIntBetweenOrNull(choice.trim(), 1,
                                Math.min(Config.MUSIC_SEARCHES, this.listener.getResultTracks().size()));
                        if (num == null) {
                            return Mono.empty();
                        }

                        choices.add(num);
                    }

                    choices.forEach(choice -> this.listener.trackLoaded(this.listener.getResultTracks().get(choice - 1)));
                    guildMusic.setWaitingForChoice(false);
                    return Mono.empty();
                });
    }

    @Override
    public boolean takeEventWile(MessageCreateEvent event) {
        return MusicManager.getGuildMusic(this.listener.getGuildId())
                .map(GuildMusic::isWaitingForChoice)
                .orElse(false);
    }

}
