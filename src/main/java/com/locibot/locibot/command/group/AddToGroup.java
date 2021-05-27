package com.locibot.locibot.command.group;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.groups.entity.DBGroup;
import com.locibot.locibot.database.groups.entity.DBGroupMember;
import discord4j.core.object.entity.User;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

public class AddToGroup extends BaseCmd {
    protected AddToGroup() {
        super(CommandCategory.GROUP, "add_member", "add a member to a group");
        this.addOption("group_name", "group name", true, ApplicationCommandOptionType.STRING);
        this.addOption("user", "member name", true, ApplicationCommandOptionType.USER);
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.getOptionAsUser("user").flatMap(user -> {
            String groupName = context.getOptionAsString("group_name").orElse("");
            //check if group does exist
            if (DatabaseManager.getGroups().containsGroup(groupName)) {
                //check if author is the group owner
                return DatabaseManager.getGroups().getDBGroup(groupName).flatMap(group -> {
                    if (context.getAuthorId().equals(group.getOwner().getId()))
                        //add user to group
                        return group.addMember(user).then(context.createFollowupMessage("Member " + user.getUsername() + " got added to group " + group.getGroupName()))
                                //invite added user, if group is scheduled and if invited + accepted + pending < min_required
                                .then(invite(group, user, context));
                    return context.createFollowupMessage("Only the owner of the group can add new members!");
                });
            }
            return context.createFollowupMessage("The group you are looking for does not exist!");
        });
    }

    public Mono<?> invite(DBGroup group, User user, Context context) {
        if (group.getBean().getScheduledTime() != null) {
            int count = 0;
            for (DBGroupMember groupMember : group.getMembers()) {
                if (groupMember.getBean().isInvited() && (groupMember.getBean().getAccepted() == 0 || groupMember.getBean().getAccepted() == 1))
                    count++;
            }
            if (GroupUtil.parseIntToGroupType(group.getBean().getTeamType()).getMin_required() > count) {
                //send invite to user
                return user.getPrivateChannel().flatMap(privateChannel -> privateChannel.createEmbed(GroupUtil.sendInviteMessage(group, user)))
                        //update Database
                        .then(group.updateInvited(user.getId(), true).then(group.updateAccept(user.getId(), 0)))
                        //inform owner
                        .then(context.getClient().getUserById(group.getOwner().getId()).flatMap(owner ->
                                owner.getPrivateChannel().flatMap(privateChannel ->
                                        privateChannel.createMessage("Member " + user.getUsername() + " got invited to group " + group.getGroupName() + " automagically!"))));
            }
        }
        return Mono.empty();
    }
}
