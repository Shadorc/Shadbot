package me.shadorc.shadbot.command.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
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

public class PauseCmd extends BaseCmd {

    public PauseCmd() {
        super(CommandCategory.MUSIC, List.of("pause", "unpause", "resume"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final AudioPlayer audioPlayer = context.requireGuildMusic().getTrackScheduler().getAudioPlayer();

        return DiscordUtils.requireSameVoiceChannel(context)
                .map(voiceChannelId -> {
                    audioPlayer.setPaused(!audioPlayer.isPaused());
                    if (audioPlayer.isPaused()) {
                        return String.format(Emoji.PAUSE + " Music paused by **%s**.", context.getUsername());
                    } else {
                        return String.format(Emoji.PLAY + " Music resumed by **%s**.", context.getUsername());
                    }
                })
                .flatMap(message -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(message, channel)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Pause current music. Use this command again to resume.")
                .build();
    }
}