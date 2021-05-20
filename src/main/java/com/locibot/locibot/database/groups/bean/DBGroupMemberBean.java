package com.locibot.locibot.database.groups.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.locibot.locibot.database.Bean;
import reactor.util.annotation.Nullable;

public class DBGroupMemberBean implements Bean {
    @JsonProperty("_id")
    private Long id;
    @Nullable
    @JsonProperty("teamName")
    private String name;
    @Nullable
    @JsonProperty("optional")
    private boolean optional;
    @Nullable
    @JsonProperty("invited")
    private boolean invited;
    @Nullable
    @JsonProperty("accepted")
    private int accepted;
    @Nullable
    @JsonProperty("owner")
    private boolean owner;


    public DBGroupMemberBean(Long id, @Nullable String name, boolean optional, boolean invited, int accepted, boolean owner) {
        this.id = id;
        this.name = name;
        this.optional = optional;
        this.invited = invited;
        this.accepted = accepted;
        this.owner = owner;
    }

    public DBGroupMemberBean(Long id, @Nullable String name) {
        this.id = id;
        this.name = name;
    }

    public DBGroupMemberBean(Long id) {
        this(id, null);
    }

    public DBGroupMemberBean() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public boolean isInvited() {
        return invited;
    }

    public void setInvited(boolean invited) {
        this.invited = invited;
    }

    public int getAccepted() {
        return accepted;
    }

    public void setAccepted(int accepted) {
        this.accepted = accepted;
    }

    public boolean isOwner() {
        return owner;
    }

    public void setOwner(boolean owner) {
        this.owner = owner;
    }

    @Override
    public String toString() {
        return "DBMemberBean{" +
                "id=" + this.id +
                ", name=" + this.name +
                ", optional=" + this.optional +
                ", invited=" + this.invited +
                ", accepted=" + this.accepted +
                ", owner=" + this.owner +
                '}';
    }
}
