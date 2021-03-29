package com.shadorc.shadbot.database.guilds.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shadorc.shadbot.database.Bean;
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
    @Nullable
    @JsonProperty("locale")
    private String locale;

    public DBGuildBean(String id, @Nullable List<DBMemberBean> members, @Nullable SettingsBean settingsBean,
                       @Nullable String locale) {
        this.id = id;
        this.members = members;
        this.settingsBean = settingsBean;
        this.locale = locale;
    }

    public DBGuildBean(String id) {
        this(id, null, null, null);
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

    @Nullable
    public String getLocale() {
        return this.locale;
    }

    @Override
    public String toString() {
        return "DBGuildBean{" +
                "id=" + this.id +
                ", members=" + this.members +
                ", settingsBean=" + this.settingsBean +
                ", locale=" + this.locale +
                '}';
    }
}
