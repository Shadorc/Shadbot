package com.locibot.locibot.command.group;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.groups.entity.DBGroup;
import com.locibot.locibot.database.groups.entity.DBGroupMember;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

public class Decline extends BaseCmd {
    public Decline() {
        super(CommandCategory.GROUP, CommandPermission.USER_GLOBAL, "decline", "decline the invitation", ApplicationCommandOptionType.STRING);
        this.addOption("group_name", "group name", true, ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        if (!DatabaseManager.getGroups().containsGroup(context.getOptionAsString("group_name").orElse(""))) {
            return context.createFollowupMessage("Nice try! But you can't decline a invite that does not exist!");
        }
        DBGroup group = DatabaseManager.getGroups().getDBGroup(context.getOptionAsString("group_name").get()).block();
        for (DBGroupMember member : group.getMembers()) {
            if (member.getBean().getId().equals(context.getAuthorId().asLong()) && member.getBean().isInvited()) {
                return context.createFollowupMessage("You have declined the invite! Your group will miss you!").then(group.updateAccept(member.getId(), 2));
            }
        }
        return context.createFollowupMessage("Nice try! But you are not invited to this group...");
    }
}
