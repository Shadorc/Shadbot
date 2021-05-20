package com.shadorc.shadbot.command.moderation.member;

import com.shadorc.shadbot.core.command.GroupCmd;
import discord4j.core.object.entity.Member;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;

public class KickCmd extends RemoveMembersCmd {

    public KickCmd(final GroupCmd groupCmd) {
        super(groupCmd, Permission.KICK_MEMBERS, "kick", "Kick a user");
    }

    @Override
    public Mono<?> action(Member memberToRemove, String reason) {
        return memberToRemove.kick(reason);
    }
}
