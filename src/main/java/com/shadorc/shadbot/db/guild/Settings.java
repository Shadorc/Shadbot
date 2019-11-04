package com.shadorc.shadbot.db.guild;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.data.Config;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import reactor.util.annotation.Nullable;

import java.util.*;

public class Settings {

    @Nullable
    private final SettingsBean bean;

    protected Settings(@Nullable SettingsBean bean) {
        this.bean = bean;
    }

    protected Settings() {
        this(null);
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
        return Optional.ofNullable(this.bean)
                .map(SettingsBean::getAllowedTextChannels)
                .orElse(new ArrayList<>());
    }

    public List<Long> getAllowedVoiceChannels() {
        return Optional.ofNullable(this.bean)
                .map(SettingsBean::getAllowedVoiceChannels)
                .orElse(new ArrayList<>());
    }

    public List<Long> getAllowedRoles() {
        return Optional.ofNullable(this.bean)
                .map(SettingsBean::getAllowedRoles)
                .orElse(new ArrayList<>());
    }

    public List<Long> getAutoRoles() {
        return Optional.ofNullable(this.bean)
                .map(SettingsBean::getAutoRoles)
                .orElse(new ArrayList<>());
    }

    public List<String> getBlacklistedCmd() {
        return Optional.ofNullable(this.bean)
                .map(SettingsBean::getBlacklist)
                .orElse(new ArrayList<>());
    }

    public Integer getDefaultVol() {
        return Optional.ofNullable(this.bean)
                .map(SettingsBean::getDefaultVolume)
                .orElse(Config.DEFAULT_VOLUME);
    }

    /**
     * @return A map containing message's ID as key and role's ID as value
     */
    public Map<String, Long> getIamMessages() {
        return Optional.ofNullable(this.bean)
                .map(SettingsBean::getIamMessage)
                .orElse(new HashMap<>());
    }

    public Optional<String> getJoinMessage() {
        return Optional.ofNullable(this.bean)
                .map(SettingsBean::getJoinMessage);
    }

    public Optional<String> getLeaveMessage() {
        return Optional.ofNullable(this.bean)
                .map(SettingsBean::getLeaveMessage);
    }

    public Optional<Long> getMessageChannelId() {
        return Optional.ofNullable(this.bean)
                .map(SettingsBean::getMessageChannelId);
    }

    public Map<String, List<String>> getSavedPlaylists() {
        return Optional.ofNullable(this.bean)
                .map(SettingsBean::getSavedPlaylists)
                .orElse(new HashMap<>());
    }

    public String getPrefix() {
        return Optional.ofNullable(this.bean)
                .map(SettingsBean::getPrefix)
                .orElse(Config.DEFAULT_PREFIX);
    }

    @Override
    public String toString() {
        return "Settings{" +
                "bean=" + this.bean +
                '}';
    }
}
