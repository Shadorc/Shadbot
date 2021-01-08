/*
package com.shadorc.shadbot.command.admin.member;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public class SoftBanCmd extends RemoveMemberCmd {

    public SoftBanCmd() {
        super("softban", "softbanned", Permission.BAN_MEMBERS);
    }

    @Override
    public Mono<Void> action(Member member, String reason) {
        return member.ban(spec -> spec.setReason(reason).setDeleteMessageDays(7))
                .then(member.unban());
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Ban and instantly unban a user.\nIt's like kicking him but it "
                        + "also deletes his messages from the last 7 days.")
                .addArg("@user", false)
                .addArg("reason", true)
                .build();
    }

}
*/
