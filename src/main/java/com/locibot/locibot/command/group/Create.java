package com.locibot.locibot.command.group;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.groups.GroupsCollection;
import com.locibot.locibot.database.groups.entity.DBGroup;
import com.locibot.locibot.database.groups.entity.DBGroupMember;
import discord4j.common.util.Snowflake;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class Create extends BaseCmd {
    protected Create() {
        super(CommandCategory.FUN, "create_clash", "create a group");
        this.addOption("team_name", "Give us a Name", true, ApplicationCommandOptionType.STRING);
        this.addOption("member1", "User", true, ApplicationCommandOptionType.USER);
        this.addOption("member2", "User", true, ApplicationCommandOptionType.USER);
        this.addOption("member3", "User", true, ApplicationCommandOptionType.USER);
        this.addOption("member4", "User", true, ApplicationCommandOptionType.USER);
        this.addOption("optional_member1", "User", false, ApplicationCommandOptionType.USER);
        this.addOption("optional_member2", "User", false, ApplicationCommandOptionType.USER);
        this.addOption("optional_member3", "User", false, ApplicationCommandOptionType.USER);
        this.addOption("optional_member4", "User", false, ApplicationCommandOptionType.USER);
        this.addOption("optional_member5", "User", false, ApplicationCommandOptionType.USER);
    }

    @Override
    public Mono<?> execute(Context context) {


        String groupName = context.getOptionAsString("team_name").get();
        DBGroup group = new DBGroup(groupName);
        GroupsCollection groupsCollection = DatabaseManager.getGroups();
        Snowflake member1 = context.getOptionAsUser("member1").block().getId();
        DBGroupMember groupMember = new DBGroupMember(member1 ,groupName);
        return context.createFollowupMessage("Created").then(group.insert().then(groupMember.insert()));
    }
}
