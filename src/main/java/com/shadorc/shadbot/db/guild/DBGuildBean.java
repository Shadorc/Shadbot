package com.shadorc.shadbot.db.guild;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DBGuildBean {

    @JsonProperty("id")
    private long id;
    @JsonProperty("members")
    private List<DBMemberBean> members;
    @JsonProperty("settings")
    private SettingsBean settingsBean;

    public long getId() {
        return this.id;
    }

    public List<DBMemberBean> getMembers() {
        return this.members;
    }

    public SettingsBean getSettingsBean() {
        return this.settingsBean;
    }

    @Override
    public String toString() {
        return "DBGuildBean{" +
                "id=" + this.id +
                ", members=" + this.members +
                ", settingsBean=" + this.settingsBean +
                '}';
    }
}
