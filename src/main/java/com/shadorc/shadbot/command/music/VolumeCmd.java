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
        super(CommandCategory.MUSIC, "volume", "Show or change current volume level");
        this.addOption("volume",
                String.format("Volume to set, must be between 1%% and %d%%", Config.VOLUME_MAX),
                false,
                ApplicationCommandOptionType.INTEGER);
    }

    @Override
    public Mono<?> execute(Context context) {
        final GuildMusic guildMusic = context.requireGuildMusic();

        return DiscordUtil.requireVoiceChannel(context)
                .flatMap(__ -> {
                    final Optional<String> option = context.getOption("volume");
                    final TrackScheduler scheduler = guildMusic.getTrackScheduler();
                    if (option.isEmpty()) {
                        return context.createFollowupMessage(Emoji.SOUND + " (**%s**) Current volume level: **%d%%**",
                                context.getAuthorName(), scheduler.getAudioPlayer().getVolume());
                    }

                    final Integer volume = NumberUtil.toPositiveIntOrNull(option.orElseThrow());
                    if (volume == null) {
                        return Mono.error(new CommandException(String.format("`%s` is not a valid volume.", volume)));
                    }

                    if (volume > Config.VOLUME_MAX) {
                        return Mono.error(new CommandException(
                                String.format("You cannot set the volume higher than %d%%.", Config.VOLUME_MAX)));
                    }

                    scheduler.setVolume(volume);
                    return context.createFollowupMessage(String.format(Emoji.SOUND + " Volume level set to **%s%%** by **%s**.",
                            scheduler.getAudioPlayer().getVolume(), context.getAuthorName()));
                });
    }

}
