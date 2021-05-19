package com.locibot.locibot.command.music;

import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.music.GuildMusic;
import com.locibot.locibot.music.TrackScheduler;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.DiscordUtil;
import com.locibot.locibot.utils.NumberUtil;
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
                        return context.createFollowupMessage(Emoji.SOUND, context.localize("volume.current")
                                .formatted(scheduler.getAudioPlayer().getVolume()));
                    }

                    final long percentage = percentageOpt.orElseThrow();
                    if (!NumberUtil.isBetween(percentage, Config.VOLUME_MIN, Config.VOLUME_MAX)) {
                        return Mono.error(new CommandException(context.localize("volume.out.of.range")
                                .formatted(Config.VOLUME_MIN, Config.VOLUME_MAX)));
                    }

                    scheduler.setVolume((int) percentage);
                    return context.createFollowupMessage(Emoji.SOUND, context.localize("volume.message")
                            .formatted(scheduler.getAudioPlayer().getVolume()));
                });
    }

}
