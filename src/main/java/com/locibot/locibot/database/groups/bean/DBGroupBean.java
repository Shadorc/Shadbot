package com.locibot.locibot.database.groups.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.locibot.locibot.database.Bean;
import reactor.util.annotation.Nullable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class DBGroupBean implements Bean {
    @JsonProperty("_id")
    private String groupName;
    @Nullable
    @JsonProperty("creationDate")
    private String creationDate;
    @Nullable
    @JsonProperty("members")
    private List<DBGroupMemberBean> members;
    @Nullable
    @JsonProperty("scheduledDate")
    private String scheduledDate;
    @Nullable
    @JsonProperty("scheduledTime")
    private String scheduledTime;
    @Nullable
    @JsonProperty("teamType")
    private int teamType;


    public DBGroupBean(String groupName, @Nullable List<DBGroupMemberBean> members, @Nullable String scheduledDate, @Nullable String scheduledTime, int teamType) {
        this.groupName = groupName;
        this.members = members;
        this.creationDate = LocalDate.now().toString();
        this.scheduledDate = scheduledDate;
        this.scheduledTime = scheduledTime;
        this.teamType = teamType;
    }

    public DBGroupBean(String groupName, int teamType) {
        this(groupName, null, "", "", teamType);
    }

    public DBGroupBean(String groupName) {
        this(groupName, null, "", "", 0);
    }

    public DBGroupBean() {
        this.creationDate = LocalDate.now().toString();
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    @Nullable
    public List<DBGroupMemberBean> getMembers() {
        return members;
    }

    public void setMembers(@Nullable List<DBGroupMemberBean> members) {
        this.members = members;
    }

    @Nullable
    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(@Nullable String creationDate) {
        this.creationDate = creationDate;
    }

    @Nullable
    public String getScheduledDate() {
        return scheduledDate;
    }

    @Nullable
    public String getScheduledTime() {
        return scheduledTime;
    }

    public int getTeamType() {
        return teamType;
    }

    public void setTeamType(int teamType) {
        this.teamType = teamType;
    }

    @Override
    public String toString() {
        return "DBGuildBean{" +
                "groupName=" + this.groupName +
                ", members=" + this.members +
                ", creationDate=" + this.creationDate +
                ", scheduledDate=" + this.scheduledDate +
                ", scheduledTime=" + this.scheduledTime +
                ", teamType=" + this.teamType +
                '}';
    }

}
