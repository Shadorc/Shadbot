package me.shadorc.shadbot.command.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class NameCmd extends BaseCmd {

    public NameCmd() {
        super(CommandCategory.MUSIC, List.of("name", "current"), "np");
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final AudioTrackInfo trackInfo = context.requireGuildMusic().getTrackScheduler().getAudioPlayer().getPlayingTrack().getInfo();
        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.MUSICAL_NOTE + " (**%s**) Currently playing: **%s**",
                        context.getUsername(), FormatUtils.trackName(trackInfo)), channel))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Show current music name.")
                .build();
    }
}