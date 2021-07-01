package com.locibot.locibot.command.group;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

public class Delete extends BaseCmd {

    protected Delete() {
        super(CommandCategory.GROUP, "delete", "delete a group");
        this.addOption("group_name", "group name", true, ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        return Mono.just(context.getOptionAsString("group_name").orElse("")).flatMap(groupName ->
        {
            //check if group does exist
            if (DatabaseManager.getGroups().containsGroup(groupName)) {
                return DatabaseManager.getGroups().getDBGroup(groupName).flatMap(group ->
                {
                    //check if author is owner
                    if (group.getOwner().getId().equals(context.getAuthorId())) {
                        return group.delete().then(context.createFollowupMessage(groupName + " has been deleted!"));
                    }
                    return context.createFollowupMessage("You are not the owner of " + groupName +"!");
                });
            }
            return context.createFollowupMessage(groupName + " does not exist!");
        });
    }
}
