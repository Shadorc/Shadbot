package me.shadorc.shadbot.command.music;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
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
                .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Playlist cleared by **%s**.",
                        context.getUsername()), channel))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Clear current playlist.")
                .build();
    }

}
