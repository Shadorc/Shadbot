package com.shadorc.shadbot.db.guilds.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.util.annotation.Nullable;

import java.util.List;
import java.util.Map;

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
    @JsonProperty("iam_message")
    private Map<String, String> iamMessage;
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
    @JsonProperty("saved_playlists")
    private Map<String, List<String>> savedPlaylists;
    @Nullable
    @JsonProperty("prefix")
    private String prefix;

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
    public Map<String, String> getIamMessage() {
        return this.iamMessage;
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
    public Map<String, List<String>> getSavedPlaylists() {
        return this.savedPlaylists;
    }

    @Nullable
    public String getPrefix() {
        return this.prefix;
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
                ", iamMessage=" + this.iamMessage +
                ", joinMessage='" + this.joinMessage + '\'' +
                ", leaveMessage='" + this.leaveMessage + '\'' +
                ", messageChannelId='" + this.messageChannelId + '\'' +
                ", savedPlaylists=" + this.savedPlaylists +
                ", prefix='" + this.prefix + '\'' +
                '}';
    }
}
