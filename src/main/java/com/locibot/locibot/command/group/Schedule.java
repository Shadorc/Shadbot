package com.locibot.locibot.command.group;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.groups.entity.DBGroup;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class Schedule extends BaseCmd {
    protected Schedule() {
        super(CommandCategory.GROUP, "schedule", "schedule a group event");
        this.addOption("team_name", "Team Name", true, ApplicationCommandOptionType.STRING);
        this.addOption("date", "dd.MM.yyyy", true, ApplicationCommandOptionType.STRING);
        this.addOption("time", "hh:mm", true, ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        DBGroup group = DatabaseManager.getGroups().getDBGroup(context.getOptionAsString("team_name").get()).block();
        LocalDate newDate = LocalDate.parse(context.getOptionAsString("date").get(), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        LocalTime newTime = LocalTime.parse(context.getOptionAsString("time").get());

        if (!group.getOwner().getBean().getName().equals(context.getAuthorName())){
            return context.createFollowupMessage("Only the group owner is allowed to create a schedule!");
        }

        group.updateSchedules(newDate, newTime).block();

        //update group
        group = DatabaseManager.getGroups().getDBGroup(context.getOptionAsString("team_name").get()).block();
        DBGroup finalGroup = group;

        group.getMembers().forEach(dbGroupMember -> {
            context.getClient().getUserById(dbGroupMember.getId()).block().getPrivateChannel().flatMap(privateChannel -> privateChannel.createEmbed(getMessage(finalGroup, context))).subscribe();
        });
        return context.createFollowupMessage("Group scheduled!");
    }

    public Consumer<EmbedCreateSpec> getMessage(DBGroup group, Context context) {
        User user = context.getEvent().getClient().getUserById(group.getOwner().getId()).block();
//        LocalDate date = group.getBean().getScheduledDate();
//        LocalTime time = group.getBean().getScheduledTime();
        String dateString = group.getBean().getScheduledDate();
        String timeString = group.getBean().getScheduledTime();
        if (dateString == null || timeString == null){
            dateString = "---";
            timeString = "---";
        } else {
            dateString = LocalDate.parse(dateString).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }


        String finalDateString = dateString;
        String finalTimeString = timeString;
        return embedCreateSpec -> embedCreateSpec.setTitle("Team-Invite")
                .setColor(Color.RED)
                .setThumbnail("https://img.icons8.com/ios-filled/344/placeholder-thumbnail-xml.png")
                .setAuthor(user.getUsername(), "", user.getAvatarUrl())
                .addField("Invitation to " + group.getBean().getGroupName(), "You got invited from " + user.getUsername() + "!", false)
                .addField("Date", finalDateString, true)
                .addField("Time", finalTimeString, true)
                .addField("Accept/ Decline", "If you are interested to join the party, answer with \"/accept " + group.getBean().getGroupName() + "\"\n" +
                        "You can decline the invite with \"/decline " + group.getBean().getGroupName() + "\" :(", false);
    }
}
