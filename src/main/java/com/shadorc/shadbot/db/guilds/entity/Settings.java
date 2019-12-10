package com.shadorc.shadbot.db.guilds.entity;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.SerializableEntity;
import com.shadorc.shadbot.db.guilds.bean.SettingsBean;
import com.shadorc.shadbot.db.guilds.entity.setting.Iam;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Settings extends SerializableEntity<SettingsBean> {

    protected Settings(SettingsBean bean) {
        super(bean);
    }

    protected Settings() {
        super(new SettingsBean());
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
        return Optional.ofNullable(this.getBean())
                .map(SettingsBean::getBlacklist)
                .orElse(new ArrayList<>());
    }

    public Integer getDefaultVol() {
        return Optional.ofNullable(this.getBean())
                .map(SettingsBean::getDefaultVolume)
                .orElse(Config.DEFAULT_VOLUME);
    }

    public List<Iam> getIam() {
        return Optional.ofNullable(this.getBean())
                .map(SettingsBean::getIam)
                .orElse(new ArrayList<>())
                .stream().map(Iam::new)
                .collect(Collectors.toList());
    }

    public Optional<String> getJoinMessage() {
        return Optional.ofNullable(this.getBean())
                .map(SettingsBean::getJoinMessage);
    }

    public Optional<String> getLeaveMessage() {
        return Optional.ofNullable(this.getBean())
                .map(SettingsBean::getLeaveMessage);
    }

    public Optional<Snowflake> getMessageChannelId() {
        return Optional.ofNullable(this.getBean())
                .map(SettingsBean::getMessageChannelId)
                .map(Snowflake::of);
    }

    public Map<String, List<String>> getSavedPlaylists() {
        return Optional.ofNullable(this.getBean())
                .map(SettingsBean::getSavedPlaylists)
                .orElse(new HashMap<>());
    }

    public String getPrefix() {
        return Optional.ofNullable(this.getBean())
                .map(SettingsBean::getPrefix)
                .orElse(Config.DEFAULT_PREFIX);
    }

    private List<Snowflake> toSnowflakeList(Function<SettingsBean, List<String>> mapper) {
        return Optional.ofNullable(this.getBean())
                .map(mapper)
                .orElse(new ArrayList<>())
                .stream()
                .map(Snowflake::of)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "Settings{" +
                "bean=" + this.getBean() +
                '}';
    }
}
