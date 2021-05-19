package com.locibot.locibot.command.music;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.music.GuildMusic;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.DiscordUtil;
import reactor.core.publisher.Mono;

public class ShuffleCmd extends BaseCmd {

    public ShuffleCmd() {
        super(CommandCategory.MUSIC, "shuffle", "Shuffle current playlist");
    }

    @Override
    public Mono<?> execute(Context context) {
        final GuildMusic guildMusic = context.requireGuildMusic();

        return DiscordUtil.requireVoiceChannel(context)
                .flatMap(__ -> {
                    guildMusic.getTrackScheduler().shufflePlaylist();
                    return context.createFollowupMessage(Emoji.CHECK_MARK, context.localize("shuffle.message"));
                });
    }

}
