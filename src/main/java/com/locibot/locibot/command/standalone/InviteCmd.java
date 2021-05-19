package com.locibot.locibot.command.standalone;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.utils.ShadbotUtil;
import reactor.core.publisher.Mono;

public class InviteCmd extends BaseCmd {

    public InviteCmd() {
        super(CommandCategory.INFO, "invite", "Get an invitation for the bot or for the support server");
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.createFollowupMessage(ShadbotUtil.getDefaultEmbed(
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
