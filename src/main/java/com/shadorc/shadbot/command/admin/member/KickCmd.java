package com.shadorc.shadbot.command.admin.member;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.help.HelpBuilder;
import discord4j.core.object.entity.Member;
import discord4j.core.object.util.Permission;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public class KickCmd extends RemoveMemberCmd {

    public KickCmd() {
        super("kick", "kicked", Permission.KICK_MEMBERS);
    }

    @Override
    public Mono<Void> action(Member member, String reason) {
        return member.kick(reason);
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Kick user.")
                .addArg("@user", false)
                .addArg("reason", true)
                .build();
    }

}
