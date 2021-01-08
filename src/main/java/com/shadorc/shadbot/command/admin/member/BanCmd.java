/*
package com.shadorc.shadbot.command.admin.member;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public class BanCmd extends RemoveMemberCmd {

    public BanCmd() {
        super("ban", "banned", Permission.BAN_MEMBERS);
    }

    @Override
    public Mono<Void> action(Member member, String reason) {
        return member.ban(spec -> spec.setReason(reason).setDeleteMessageDays(7));
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Ban a user and delete his messages from the last 7 days.")
                .addArg("@user", false)
                .addArg("reason", true)
                .build();
    }

}
*/
