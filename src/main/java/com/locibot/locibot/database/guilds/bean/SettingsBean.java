package com.locibot.locibot.database.guilds.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.locibot.locibot.database.Bean;
import com.locibot.locibot.database.guilds.bean.setting.AutoMessageBean;
import com.locibot.locibot.database.guilds.bean.setting.IamBean;
import reactor.util.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SettingsBean implements Bean {

    @Nullable
    @JsonProperty("allowed_text_channels")
    private List<String> allowedTextChannelIds;
    @Nullable
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
    @Deprecated
    @JsonProperty("join_message")
    private String joinMessage;
    @Nullable
    @Deprecated
    @JsonProperty("leave_message")
    private String leaveMessage;
    @Nullable
    @Deprecated
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
    @Nullable
    @JsonProperty("auto_join_message")
    private AutoMessageBean autoJoinMessage;
    @Nullable
    @JsonProperty("auto_leave_message")
    private AutoMessageBean autoLeaveMessage;
    @Nullable
    @JsonProperty("locale")
    private String locale;

    public SettingsBean() {

    }

    @Nullable
    public List<String> getAllowedTextChannelIds() {
        return this.allowedTextChannelIds;
    }

    @Nullable
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
    @Deprecated
    public String getJoinMessage() {
        return this.joinMessage;
    }

    @Nullable
    @Deprecated
    public String getLeaveMessage() {
        return this.leaveMessage;
    }

    @Nullable
    @Deprecated
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

    @Nullable
    public AutoMessageBean getAutoJoinMessage() {
        return this.autoJoinMessage;
    }

    @Nullable
    public AutoMessageBean getAutoLeaveMessage() {
        return this.autoLeaveMessage;
    }

    @Nullable
    public String getLocale() {
        return this.locale;
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
                ", restrictedChannels=" + this.restrictedChannels +
                ", restrictedRoles=" + this.restrictedRoles +
                ", autoJoinMessage=" + this.autoJoinMessage +
                ", autoLeaveMessage=" + this.autoLeaveMessage +
                ", locale=" + this.locale +
                '}';
    }
}
