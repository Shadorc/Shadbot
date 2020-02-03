package com.shadorc.shadbot.command.music;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class ClearCmd extends BaseCmd {

    public ClearCmd() {
        super(CommandCategory.MUSIC, List.of("clear"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        context.requireGuildMusic().getTrackScheduler().clearPlaylist();
        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(
                        String.format(Emoji.CHECK_MARK + " Playlist cleared by **%s**.",
                                context.getUsername()), channel))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Clear current playlist.")
                .build();
    }

}
