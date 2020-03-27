package com.shadorc.shadbot.command.music;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class ShuffleCmd extends BaseCmd {

    public ShuffleCmd() {
        super(CommandCategory.MUSIC, List.of("shuffle"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final GuildMusic guildMusic = context.requireGuildMusic();

        return DiscordUtils.requireVoiceChannel(context)
                .map(voiceChannelId -> {
                    guildMusic.getTrackScheduler().shufflePlaylist();
                    return String.format(Emoji.CHECK_MARK + " Playlist shuffled by **%s**.", context.getUsername());
                })
                .flatMap(message -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(message, channel)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Shuffle current playlist.")
                .build();
    }

}
