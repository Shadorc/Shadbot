package com.shadorc.shadbot.command.info.support;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.ShadbotUtil;
import reactor.core.publisher.Mono;

public class InviteCmd extends BaseCmd {

    public InviteCmd() {
        super(CommandCategory.INFO, "invite", "Show useful links");
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.createFollowupMessage(ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor("Links", Config.INVITE_URL, context.getAuthorAvatarUrl())
                        .setDescription(
                                """
                                I'm glad you're willing to invite **Shadbot** in your own server, thank you!" +
                                Here are some useful links for you.
                                If you have any questions or issues, **do not hesitate to join the Support Server and ask!**
                                If you want to support Shadbot, you can also follow the **Donation** link to get more information. 
                                Even small donations are really helpful. 
                                """ + Emoji.HEARTS)
                        .addField("Invite", Config.INVITE_URL, false)
                        .addField("Support Server", Config.SUPPORT_SERVER_URL, false)
                        .addField("Donation", Config.PATREON_URL, false)));
    }

}
