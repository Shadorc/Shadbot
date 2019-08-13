package com.shadorc.shadbot.command.music;

import com.shadorc.shadbot.Config;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.premium.PremiumManager;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.music.TrackScheduler;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class VolumeCmd extends BaseCmd {

    public VolumeCmd() {
        super(CommandCategory.MUSIC, List.of("volume"), "vol");
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final GuildMusic guildMusic = context.requireGuildMusic();

        return DiscordUtils.requireSameVoiceChannel(context)
                .flatMap(voiceChannelId -> {
                    final TrackScheduler scheduler = guildMusic.getTrackScheduler();
                    if (context.getArg().isEmpty()) {
                        return context.getChannel()
                                .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.SOUND + " (**%s**) Current volume level: **%d%%**",
                                        context.getUsername(), scheduler.getAudioPlayer().getVolume()), channel));
                    }

                    final String arg = context.getArg().get();
                    final Integer volume = NumberUtils.toPositiveIntOrNull(arg);
                    if (volume == null) {
                        return Mono.error(new CommandException(String.format("`%s` is not a valid volume.", arg)));
                    }

                    if (volume > Config.DEFAULT_MAX_VOLUME && !PremiumManager.getInstance().isGuildPremium(context.getGuildId())
                            && !PremiumManager.getInstance().isUserPremium(context.getAuthorId())) {
                        return Mono.error(new CommandException(String.format("You cannot set the volume higher than %d%%. " +
                                "You can set the volume up to %d%% and gain other advantage by contributing " +
                                "to Shadbot. More info here: <%s>",
                                Config.DEFAULT_MAX_VOLUME, Config.PREMIUM_MAX_VOLUME, Config.PATREON_URL)));
                    }

                    scheduler.setVolume(volume > Config.PREMIUM_MAX_VOLUME ? Config.PREMIUM_MAX_VOLUME : volume);
                    return context.getChannel()
                            .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.SOUND + " Volume level set to **%s%%** by **%s**.",
                                    scheduler.getAudioPlayer().getVolume(), context.getUsername()),
                                    channel));
                })
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Show or change current volume level.")
                .addArg("volume", String.format("must be between 0%% and %d%%", Config.DEFAULT_MAX_VOLUME), true)
                .addField("Premium", String.format("Premium users and servers can set the volume up to %d%%", Config.PREMIUM_MAX_VOLUME), false)
                .build();
    }
}
