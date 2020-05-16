package com.shadorc.shadbot.db.guilds.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shadorc.shadbot.db.Bean;
import reactor.util.annotation.Nullable;

import java.util.List;

public class DBGuildBean implements Bean {

    @JsonProperty("_id")
    private String id;
    @Nullable
    @JsonProperty("members")
    private List<DBMemberBean> members;
    @Nullable
    @JsonProperty("settings")
    private SettingsBean settingsBean;

    public DBGuildBean(String id, @Nullable List<DBMemberBean> members, @Nullable SettingsBean settingsBean) {
        this.id = id;
        this.members = members;
        this.settingsBean = settingsBean;
    }

    public DBGuildBean(String id) {
        this(id, null, null);
    }

    public DBGuildBean() {
    }

    public String getId() {
        return this.id;
    }

    @Nullable
    public List<DBMemberBean> getMembers() {
        return this.members;
    }

    @Nullable
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
