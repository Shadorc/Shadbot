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
        return context.createFollowupMessage(ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor("Links", Config.INVITE_URL, context.getAuthorAvatar())
                        .setDescription(
                                """
                                        Here are some useful links for you.
                                        If you have any questions or issues, **do not hesitate to join the Support Server and ask!**
                                        If you like Shadbot, you can vote for it on **top.gg**, you will unlock the **%s** achievement and the `bass_boost` command! 
                                        If you want to support Shadbot, you can also follow the **Donation** link to get more information. 
                                        Even small donations are really helpful. %s"""
                                        .formatted(Achievement.VOTER.getTitle(), Emoji.HEARTS))
                        .addField("Invite", Config.INVITE_URL, false)
                        .addField("Support Server", Config.SUPPORT_SERVER_URL, false)
                        .addField("Donation", Config.PATREON_URL, false)
                        .addField("Vote", Config.TOP_GG_URL, false)));
    }

}
