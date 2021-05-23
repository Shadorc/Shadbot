package com.locibot.locibot.command.group;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.groups.entity.DBGroupMember;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

public class Accept extends BaseCmd {
    public Accept() {
        super(CommandCategory.GROUP, CommandPermission.USER_GLOBAL, "accept", "accept a Group invite");
        this.addOption("group_name", "group name", true, ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        if (!DatabaseManager.getGroups().containsGroup(context.getOptionAsString("group_name").orElse(""))) {
            return context.createFollowupMessage("Nice try! But you can't join a group that does not exist!");
        }
        return DatabaseManager.getGroups().getDBGroup(context.getOptionAsString("group_name").orElse("")).flatMap(group -> {
            for (DBGroupMember member : group.getMembers()) {
                if (member.getBean().getId().equals(context.getAuthorId().asLong()) && member.getBean().isInvited() && member.getBean().getAccepted() != 2) {
                    return context.createFollowupMessage("You have accepted the invite! Have fun!").then(group.updateAccept(member.getId(), 1))
                            //inform owner
                            .then(Mono.zip(context.getClient().getUserById(context.getAuthorId()),
                                    context.getClient().getUserById(group.getOwner().getId()))
                                    .flatMap(TupleUtils.function((user, owner) ->
                                            owner.getPrivateChannel().flatMap(privateChannel -> privateChannel.createMessage(
                                                    user.getUsername() + " accepted your invitation to " + group.getGroupName() + "!")))));
                }
            }
            return context.createFollowupMessage("Nice try! But you are not invited to this group...");
        });
    }
}
