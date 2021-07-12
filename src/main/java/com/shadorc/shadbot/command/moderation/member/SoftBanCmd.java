package com.shadorc.shadbot.command.moderation.member;

import com.shadorc.shadbot.core.command.GroupCmd;
import discord4j.core.object.entity.Member;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;

public class SoftBanCmd extends RemoveMembersCmd {

    public SoftBanCmd(final GroupCmd groupCmd) {
        super(groupCmd, Permission.BAN_MEMBERS,
                "softban", "Kick a user and delete his messages from the last 7 days");
    }

    @Override
    public Mono<?> action(Member memberToRemove, String reason) {
        return memberToRemove.ban()
                .withDeleteMessageDays(7)
                .withReason(reason)
                .then(memberToRemove.unban());
    }
}
