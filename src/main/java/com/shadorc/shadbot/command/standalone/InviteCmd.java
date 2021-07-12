package com.shadorc.shadbot.command.standalone;

import com.shadorc.shadbot.core.command.Cmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.utils.ShadbotUtil;
import reactor.core.publisher.Mono;

public class InviteCmd extends Cmd {

    public InviteCmd() {
        super(CommandCategory.INFO, "invite", "Get an invitation for the bot or for the support server");
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.createFollowupMessage(ShadbotUtil.getDefaultLegacyEmbed(
                embed -> embed.setAuthor(context.localize("invite.title"), Config.INVITE_URL, context.getAuthorAvatar())
                        .addField(context.localize("invite.bot"), context.localize("invite.link")
                                .formatted(Config.INVITE_URL), true)
                        .addField(context.localize("invite.support"), context.localize("invite.link")
                                .formatted(Config.SUPPORT_SERVER_URL), true)
                        .addField(context.localize("invite.donation"), context.localize("invite.link")
                                .formatted(Config.PATREON_URL), true)
                        .addField(context.localize("invite.vote"), context.localize("invite.link")
                                .formatted(Config.TOP_GG_URL), true)));
    }

}
