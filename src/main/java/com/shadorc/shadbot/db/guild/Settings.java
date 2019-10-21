package com.shadorc.shadbot.db.guild;

import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.util.annotation.Nullable;

import java.util.List;
import java.util.Map;

public class Settings {

    @Nullable
    @JsonProperty("allowed_text_channels")
    private List<Long> allowedTextChannels;
    @Nullable
    @JsonProperty("allowed_voice_channels")
    private List<Long> allowedVoiceChannels;
    @Nullable
    @JsonProperty("allowed_roles")
    private List<Long> allowedRoles;
    @Nullable
    @JsonProperty("auto_roles")
    private List<Long> autoRoles;
    @Nullable
    @JsonProperty("blacklist")
    private List<String> blacklist;
    @Nullable
    @JsonProperty("default_volume")
    private Integer defaultVolume;
    @Nullable
    @JsonProperty("iam_message")
    private Map<String, Long> iamMessage;
    @Nullable
    @JsonProperty("join_message")
    private String joinMessage;
    @Nullable
    @JsonProperty("leave_message")
    private String leaveMessage;
    @Nullable
    @JsonProperty("message_channel_id")
    private Long messageChannelId;
    @Nullable
    @JsonProperty("saved_playlists")
    private Map<String, List<String>> savedPlaylists;
    @Nullable
    @JsonProperty("prefix")
    private String prefix;

    @Nullable
    public List<Long> getAllowedTextChannels() {
        return this.allowedTextChannels;
    }

    @Nullable
    public List<Long> getAllowedVoiceChannels() {
        return this.allowedVoiceChannels;
    }

    @Nullable
    public List<Long> getAllowedRoles() {
        return this.allowedRoles;
    }

    @Nullable
    public List<Long> getAutoRoles() {
        return this.autoRoles;
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
    public Map<String, Long> getIamMessage() {
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
    public Long getMessageChannelId() {
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
}
