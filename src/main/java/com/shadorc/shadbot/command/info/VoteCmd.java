package com.shadorc.shadbot.command.info;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.users.entity.achievement.Achievement;
import com.shadorc.shadbot.utils.ShadbotUtils;
import reactor.core.publisher.Mono;

public class VoteCmd extends BaseCmd {

    public VoteCmd() {
        super(CommandCategory.INFO, "vote", "If you like me, vote for me on top.gg!");
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.createFollowupMessage(ShadbotUtils.getDefaultEmbed(
                embed -> embed.setAuthor("Vote on top.gg", Config.TOP_GG_URL, context.getAuthorAvatarUrl())
                        .setThumbnail("https://i.imgur.com/4Rf7SlR.png")
                        .setDescription(String.format("If you like me, you can vote for me on **top.gg**!" +
                                        "%n%s" +
                                        "%nYou will unlock the **%s** achievement and the `bass_boost` command!",
                                Config.TOP_GG_URL, Achievement.VOTER.getTitle()))));
    }

}
