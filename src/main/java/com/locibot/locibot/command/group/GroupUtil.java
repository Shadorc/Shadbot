package com.locibot.locibot.command.group;

import com.locibot.locibot.database.groups.entity.DBGroup;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public abstract class GroupUtil {

    public static GroupType parseIntToGroupType(int type) {
        for (GroupType groupType : GroupType.values()) {
            if (type == groupType.ordinal()) {
                return groupType;
            }
        }
        return GroupType.DEFAULT;
    }

    public static Consumer<EmbedCreateSpec> sendInviteMessage(DBGroup group, User user) {
        String dateString = group.getBean().getScheduledDate();
        String timeString = group.getBean().getScheduledTime();
        if (dateString == null || timeString == null) {
            dateString = "---";
            timeString = "---";
        } else {
            dateString = LocalDate.parse(dateString).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }
        String finalDateString = dateString;
        String finalTimeString = timeString;

        GroupType groupType = parseIntToGroupType(group.getBean().getTeamType());
        String url = groupType.getUrl();

        return embedCreateSpec -> embedCreateSpec.setTitle(groupType.getName() + "-Invite")
                .setColor(Color.RED)
                .setThumbnail(url)
                .setAuthor(user.getUsername(), "", user.getAvatarUrl())
                .addField("Invitation to group: " + group.getBean().getGroupName(), "You got invited from " + user.getUsername() + "!", false)
                .addField("Date", finalDateString, true)
                .addField("Time", finalTimeString, true)
                .addField("Accept/ Decline", "If you are interested to join the "+groupType.getName()+" group, answer with \"/accept " + group.getBean().getGroupName() + "\"\n" +
                        "You can decline the invitation with \"/decline " + group.getBean().getGroupName() + "\".", false);
    }
}
