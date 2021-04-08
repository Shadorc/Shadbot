package com.shadorc.shadbot.database.guilds.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shadorc.shadbot.database.Bean;
import com.shadorc.shadbot.database.guilds.bean.setting.IamBean;
import reactor.util.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SettingsBean implements Bean {

    @Nullable
    @Deprecated
    @JsonProperty("allowed_text_channels")
    private List<String> allowedTextChannelIds;
    @Nullable
    @Deprecated
    @JsonProperty("allowed_voice_channels")
    private List<String> allowedVoiceChannelIds;
    @Nullable
    @JsonProperty("allowed_roles")
    private List<String> allowedRoleIds;
    @Nullable
    @JsonProperty("auto_roles")
    private List<String> autoRoleIds;
    @Nullable
    @JsonProperty("blacklist")
    private List<String> blacklist;
    @Nullable
    @JsonProperty("default_volume")
    private Integer defaultVolume;
    @Nullable
    @JsonProperty("iam_messages")
    private List<IamBean> iam;
    @Nullable
    @JsonProperty("join_message")
    private String joinMessage;
    @Nullable
    @JsonProperty("leave_message")
    private String leaveMessage;
    @Nullable
    @JsonProperty("message_channel_id")
    private String messageChannelId;
    @Nullable
    @Deprecated
    @JsonProperty("prefix")
    private String prefix;
    @Nullable
    @JsonProperty("restricted_channels")
    private Map<String, Set<String>> restrictedChannels;
    @Nullable
    @JsonProperty("restricted_roles")
    private Map<String, Set<String>> restrictedRoles;

    public SettingsBean() {

    }

    @Nullable
    @Deprecated
    public List<String> getAllowedTextChannelIds() {
        return this.allowedTextChannelIds;
    }

    @Nullable
    @Deprecated
    public List<String> getAllowedVoiceChannelIds() {
        return this.allowedVoiceChannelIds;
    }

    @Nullable
    public List<String> getAllowedRoleIds() {
        return this.allowedRoleIds;
    }

    @Nullable
    public List<String> getAutoRoleIds() {
        return this.autoRoleIds;
    }

    @Nullable
    public List<String> getBlacklist() {
        return this.blacklist;
    }

    @Nullable
    public Integer getDefaultVolume() {
        return this.defaultVolume;
    }

    @Nullable
    public List<IamBean> getIam() {
        return this.iam;
    }

    @Nullable
    public String getJoinMessage() {
        return this.joinMessage;
    }

    @Nullable
    public String getLeaveMessage() {
        return this.leaveMessage;
    }

    @Nullable
    public String getMessageChannelId() {
        return this.messageChannelId;
    }

    @Nullable
    @Deprecated
    public String getPrefix() {
        return this.prefix;
    }

    @Nullable
    public Map<String, Set<String>> getRestrictedChannels() {
        return this.restrictedChannels;
    }

    @Nullable
    public Map<String, Set<String>> getRestrictedRoles() {
        return this.restrictedRoles;
    }

    @Override
    public String toString() {
        return "SettingsBean{" +
                "allowedTextChannelIds=" + this.allowedTextChannelIds +
                ", allowedVoiceChannelIds=" + this.allowedVoiceChannelIds +
                ", allowedRoleIds=" + this.allowedRoleIds +
                ", autoRoleIds=" + this.autoRoleIds +
                ", blacklist=" + this.blacklist +
                ", defaultVolume=" + this.defaultVolume +
                ", iam=" + this.iam +
                ", joinMessage='" + this.joinMessage + '\'' +
                ", leaveMessage='" + this.leaveMessage + '\'' +
                ", messageChannelId='" + this.messageChannelId + '\'' +
                ", prefix='" + this.prefix + '\'' +
                ", restrictedCategories=" + this.restrictedChannels +
                ", restrictedRoles=" + this.restrictedRoles +
                '}';
    }
}
