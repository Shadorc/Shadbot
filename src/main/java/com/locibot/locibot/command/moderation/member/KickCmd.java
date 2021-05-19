package com.locibot.locibot.command.moderation.member;

import discord4j.core.object.entity.Member;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;

public class KickCmd extends RemoveMembersCmd {

    public KickCmd() {
        super(Permission.KICK_MEMBERS, "kick", "Kick a user");
    }

    @Override
    public Mono<?> action(Member memberToRemove, String reason) {
        return memberToRemove.kick(reason);
    }
}
