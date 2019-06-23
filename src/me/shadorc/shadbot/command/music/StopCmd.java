package me.shadorc.shadbot.command.music;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.music.MusicManager;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.object.help.HelpBuilder;
import me.shadorc.shadbot.utils.DiscordUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class StopCmd extends BaseCmd {

    public StopCmd() {
        super(CommandCategory.MUSIC, List.of("stop"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        context.requireGuildMusic();
        MusicManager.getInstance().getConnection(context.getGuildId()).leaveVoiceChannel();
        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.INFO + " Music stopped by **%s**.",
                        context.getUsername()), channel))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Stop music.")
                .build();
    }
}