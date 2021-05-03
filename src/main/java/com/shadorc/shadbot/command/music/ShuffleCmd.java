package com.shadorc.shadbot.command.music;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
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
