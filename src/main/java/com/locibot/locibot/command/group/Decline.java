package com.locibot.locibot.command.group;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.groups.entity.DBGroup;
import com.locibot.locibot.database.groups.entity.DBGroupMember;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.rest.util.ApplicationCommandOptionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import static com.locibot.locibot.command.group.GroupUtil.sendInviteMessage;

public class Decline extends BaseCmd {
    public Decline() {
        super(CommandCategory.GROUP, CommandPermission.USER_GLOBAL, "decline", "decline the invitation", ApplicationCommandOptionType.STRING);
        this.addOption("group_name", "group name", true, ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        if (!DatabaseManager.getGroups().containsGroup(context.getOptionAsString("group_name").orElse(""))) {
            return context.createFollowupMessage("Nice try! But you cannot decline an invitation that does not exist!");
        }
        return DatabaseManager.getGroups().getDBGroup(context.getOptionAsString("group_name").orElse("")).flatMap(group -> {
                    for (DBGroupMember member : group.getMembers()) {
                        if (member.getBean().getId().equals(context.getAuthorId().asLong()) && member.getBean().isInvited()) {
                            //invite next optional member
                            Mono<Message> context1 = inviteNext(context, group, member);
                            if (context1 != null) return context1;
                        }
                    }
                    return context.createFollowupMessage("Nice try! But you are not invited to this group...");
                }
        );
    }

    @Nullable
    private Mono<Message> inviteNext(Context context, DBGroup group, DBGroupMember member) {
        for (DBGroupMember dbGroupMember : group.getMembers()) {
            if (!dbGroupMember.getBean().isInvited() && dbGroupMember.getBean().isOptional()) {
                return context.getEvent().getClient().getUserById(dbGroupMember.getId()).flatMap(user -> {
                            //update database
                            updateDatabase(group, user);
                            //inform user
                            return context.createFollowupMessage("You have declined the invitation!").then(group.updateAccept(member.getId(), 2))
                                    //send invite message to next optional user
                                    .then(user.getPrivateChannel().flatMap(privateChannel -> privateChannel.createEmbed(sendInviteMessage(group, user)))
                                            //inform owner
                                            .then(informOwner(context, group, dbGroupMember, user)));
                        }
                );
            }
        }
        return null;
    }

    @NotNull
    private Mono<Message> informOwner(Context context, DBGroup group, DBGroupMember dbGroupMember, User user) {
        return Mono.zip(context.getClient().getUserById(dbGroupMember.getId()),
                context.getClient().getUserById(group.getOwner().getId()))
                .flatMap(TupleUtils.function((declinedUser, owner) ->
                        owner.getPrivateChannel().flatMap(privateChannel -> privateChannel.createMessage(
                                declinedUser.getUsername() + " declined your invitation to " + group.getGroupName() + "!\n" +
                                        "Next optional member " + user.getUsername() + " got invited automagically"))));
    }

    private void updateDatabase(DBGroup group, User user) {
        group.updateInvited(user.getId(), true);
        group.updateAccept(user.getId(), 0);
    }
}
