package com.shadorc.shadbot.command.support;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.users.entity.achievement.Achievement;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.ShadbotUtil;
import reactor.core.publisher.Mono;

public class LinksCmd extends BaseCmd {

    public LinksCmd() {
        super(CommandCategory.INFO, "links", "Show useful links");
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.reply(ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor(context.localize("links.title"), Config.INVITE_URL, context.getAuthorAvatar())
                        .setDescription(context.localize("links.description")
                                .formatted(Achievement.VOTER.getTitle(context), Emoji.HEARTS))
                        .addField(context.localize("links.invite"), Config.INVITE_URL, false)
                        .addField(context.localize("links.support.server"), Config.SUPPORT_SERVER_URL, false)
                        .addField(context.localize("links.donation"), Config.PATREON_URL, false)
                        .addField(context.localize("links.vote"), Config.TOP_GG_URL, false)));
    }

}
