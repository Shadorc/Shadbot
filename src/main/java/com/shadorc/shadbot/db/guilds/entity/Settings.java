package com.shadorc.shadbot.db.guilds.entity;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.guilds.bean.SettingsBean;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import reactor.util.annotation.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        return this.getAllowedRoleIds().isEmpty()
                || roles.stream().anyMatch(role -> role.getPermissions().contains(Permission.ADMINISTRATOR))
                || roles.stream().anyMatch(role -> this.getAllowedRoleIds().contains(role.getId()));
    }

    public boolean isCommandAllowed(BaseCmd cmd) {
        return cmd.getNames().stream().noneMatch(this.getBlacklistedCmd()::contains);
    }

    public boolean isTextChannelAllowed(Snowflake channelId) {
        // If no permission has been set OR the text channel is allowed
        return this.getAllowedTextChannelIds().isEmpty() || this.getAllowedTextChannelIds().contains(channelId);
    }

    public boolean isVoiceChannelAllowed(Snowflake channelId) {
        // If no permission has been set OR the voice channel is allowed
        return this.getAllowedVoiceChannelIds().isEmpty() || this.getAllowedVoiceChannelIds().contains(channelId);
    }

    public List<Snowflake> getAllowedTextChannelIds() {
        return this.toSnowflakeList(SettingsBean::getAllowedTextChannelIds);
    }

    public List<Snowflake> getAllowedVoiceChannelIds() {
        return this.toSnowflakeList(SettingsBean::getAllowedVoiceChannelIds);
    }

    public List<Snowflake> getAllowedRoleIds() {
        return this.toSnowflakeList(SettingsBean::getAllowedRoleIds);
    }

    public List<Snowflake> getAutoRoleIds() {
        return this.toSnowflakeList(SettingsBean::getAutoRoleIds);
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
     * @return A map containing message IDs as key and role IDs as value
     */
    public Map<Snowflake, Snowflake> getIamMessages() {
        return Optional.ofNullable(this.bean)
                .map(SettingsBean::getIamMessage)
                .orElse(new HashMap<>())
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        entry -> Snowflake.of(entry.getKey()),
                        entry -> Snowflake.of(entry.getValue())));
    }

    public Optional<String> getJoinMessage() {
        return Optional.ofNullable(this.bean)
                .map(SettingsBean::getJoinMessage);
    }

    public Optional<String> getLeaveMessage() {
        return Optional.ofNullable(this.bean)
                .map(SettingsBean::getLeaveMessage);
    }

    public Optional<Snowflake> getMessageChannelId() {
        return Optional.ofNullable(this.bean)
                .map(SettingsBean::getMessageChannelId)
                .map(Snowflake::of);
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

    private List<Snowflake> toSnowflakeList(Function<SettingsBean, List<String>> mapper) {
        return Optional.ofNullable(this.bean)
                .map(mapper)
                .orElse(new ArrayList<>())
                .stream()
                .map(Snowflake::of)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "Settings{" +
                "bean=" + this.bean +
                '}';
    }
}
