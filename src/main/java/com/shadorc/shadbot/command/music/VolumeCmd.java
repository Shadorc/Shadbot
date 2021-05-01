package com.shadorc.shadbot.command.music;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.music.TrackScheduler;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import com.shadorc.shadbot.utils.NumberUtil;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class VolumeCmd extends BaseCmd {

    public VolumeCmd() {
        super(CommandCategory.MUSIC, "volume", "Show or set current volume level");
        this.addOption(option -> option.name("percentage")
                .description("Volume to set, must be between %d%% and %d%%"
                        .formatted(Config.VOLUME_MIN, Config.VOLUME_MAX))
                .required(false)
                .type(ApplicationCommandOptionType.INTEGER.getValue()));
    }

    @Override
    public Mono<?> execute(Context context) {
        final GuildMusic guildMusic = context.requireGuildMusic();

        return DiscordUtil.requireVoiceChannel(context)
                .flatMap(__ -> {
                    final Optional<Long> percentageOpt = context.getOptionAsLong("percentage");
                    final TrackScheduler scheduler = guildMusic.getTrackScheduler();
                    if (percentageOpt.isEmpty()) {
                        return context.reply(Emoji.SOUND, context.localize("volume.current")
                                .formatted(scheduler.getAudioPlayer().getVolume()));
                    }

                    final long percentage = percentageOpt.orElseThrow();
                    if (!NumberUtil.isBetween(percentage, Config.VOLUME_MIN, Config.VOLUME_MAX)) {
                        return Mono.error(new CommandException(context.localize("volume.out.of.range")
                                .formatted(Config.VOLUME_MIN, Config.VOLUME_MAX)));
                    }

                    scheduler.setVolume((int) percentage);
                    return context.reply(Emoji.SOUND, context.localize("volume.message")
                            .formatted(scheduler.getAudioPlayer().getVolume()));
                });
    }

}
