package com.shadorc.shadbot.db.guild;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.data.Config;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;

import java.util.*;

public class Settings {

    private final SettingsBean bean;

    protected Settings(SettingsBean bean) {
        this.bean = bean;
    }

    public boolean hasAllowedRole(List<Role> roles) {
        // If the user is an administrator OR no permissions have been set OR the role is allowed
        return this.getAllowedRoles().isEmpty()
                || roles.stream().anyMatch(role -> role.getPermissions().contains(Permission.ADMINISTRATOR))
                || roles.stream().anyMatch(role -> this.getAllowedRoles().contains(role.getId().asLong()));
    }

    public boolean isCommandAllowed(BaseCmd cmd) {
        return cmd.getNames().stream().noneMatch(this.getBlacklistedCmd()::contains);
    }

    public boolean isTextChannelAllowed(Snowflake channelId) {
        // If no permission has been set OR the text channel is allowed
        return this.getAllowedTextChannels().isEmpty() || this.getAllowedTextChannels().contains(channelId.asLong());
    }

    public boolean isVoiceChannelAllowed(Snowflake channelId) {
        // If no permission has been set OR the voice channel is allowed
        return this.getAllowedVoiceChannels().isEmpty() || this.getAllowedVoiceChannels().contains(channelId.asLong());
    }

    public List<Long> getAllowedTextChannels() {
        return Objects.requireNonNullElse(this.bean.getAllowedTextChannels(), new ArrayList<>());
    }

    public List<Long> getAllowedVoiceChannels() {
        return Objects.requireNonNullElse(this.bean.getAllowedVoiceChannels(), new ArrayList<>());
    }

    public List<Long> getAllowedRoles() {
        return Objects.requireNonNullElse(this.bean.getAllowedRoles(), new ArrayList<>());
    }

    public List<Long> getAutoRoles() {
        return Objects.requireNonNullElse(this.bean.getAutoRoles(), new ArrayList<>());
    }

    public List<String> getBlacklistedCmd() {
        return Objects.requireNonNullElse(this.bean.getBlacklist(), new ArrayList<>());
    }

    public Integer getDefaultVol() {
        return Objects.requireNonNullElse(this.bean.getDefaultVolume(), Config.DEFAULT_VOLUME);
    }

    /**
     * @return A map containing message's ID as key and role's ID as value
     */
    public Map<String, Long> getIamMessages() {
        return Objects.requireNonNullElse(this.bean.getIamMessage(), new HashMap<>());
    }

    public Optional<String> getJoinMessage() {
        return Optional.ofNullable(this.bean.getJoinMessage());
    }

    public Optional<String> getLeaveMessage() {
        return Optional.ofNullable(this.bean.getLeaveMessage());
    }

    public Optional<Long> getMessageChannelId() {
        return Optional.ofNullable(this.bean.getMessageChannelId());
    }

    public Map<String, List<String>> getSavedPlaylists() {
        return Objects.requireNonNullElse(this.bean.getSavedPlaylists(), new HashMap<>());
    }

    public String getPrefix() {
        return Objects.requireNonNullElse(this.bean.getPrefix(), Config.DEFAULT_PREFIX);
    }

    @Override
    public String toString() {
        return "Settings{" +
                "bean=" + this.bean +
                '}';
    }
}
