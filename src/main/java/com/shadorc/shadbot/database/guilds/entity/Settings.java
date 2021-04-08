package com.shadorc.shadbot.database.guilds.entity;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandManager;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.database.SerializableEntity;
import com.shadorc.shadbot.database.guilds.bean.SettingsBean;
import com.shadorc.shadbot.database.guilds.entity.setting.Iam;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Role;
import discord4j.rest.util.Permission;

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
        final Set<Snowflake> allowedRoleIds = this.getAllowedRoleIds();
        // If no permission has been set
        if (allowedRoleIds.isEmpty()) {
            return true;
        }
        //If the user is an administrator OR the role is allowed
        return roles.stream()
                .anyMatch(role -> role.getPermissions().contains(Permission.ADMINISTRATOR)
                        || allowedRoleIds.contains(role.getId()));
    }

    public boolean isCommandAllowed(BaseCmd cmd) {
        final Set<String> blacklistedCmds = this.getBlacklistedCmds();
        // If no blacklisted command has been set
        if (blacklistedCmds.isEmpty()) {
            return true;
        }
        return !blacklistedCmds.contains(cmd.getName());
    }

    @Deprecated
    public boolean isTextChannelAllowed(Snowflake channelId) {
        final Set<Snowflake> allowedTextChannelIds = this.getAllowedTextChannelIds();
        // If no permission has been set
        if (allowedTextChannelIds.isEmpty()) {
            return true;
        }
        return allowedTextChannelIds.contains(channelId);
    }

    @Deprecated
    public boolean isVoiceChannelAllowed(Snowflake channelId) {
        final Set<Snowflake> allowedVoiceChannelIds = this.getAllowedVoiceChannelIds();
        // If no permission has been set
        if (allowedVoiceChannelIds.isEmpty()) {
            return true;
        }
        return allowedVoiceChannelIds.contains(channelId);
    }

    public boolean isCommandAllowedInChannel(BaseCmd cmd, Snowflake channelId) {
        final Map<Snowflake, Set<BaseCmd>> map = this.getRestrictedChannels();
        // If no permission has been set
        if (map.isEmpty()) {
            return true;
        }
        // If this category has explicitly been allowed in this channel
        if (map.containsKey(channelId) && map.get(channelId).contains(cmd)) {
            return true;
        }
        return map.values().stream().noneMatch(set -> set.contains(cmd));
    }

    public boolean isCommandAllowedToRole(BaseCmd cmd, Set<Snowflake> roleIds) {
        final Map<Snowflake, Set<BaseCmd>> map = this.getRestrictedRoles();
        // If no permission has been set
        if (map.isEmpty()) {
            return true;
        }

        for (final Snowflake roleId : roleIds) {
            // If this command has explicitly been allowed to this role
            if (map.containsKey(roleId) && map.get(roleId).contains(cmd)) {
                return true;
            }
        }

        return map.values().stream().noneMatch(set -> set.contains(cmd));
    }

    @Deprecated
    public Set<Snowflake> getAllowedTextChannelIds() {
        return this.toSnowflakeSet(SettingsBean::getAllowedTextChannelIds);
    }

    @Deprecated
    public Set<Snowflake> getAllowedVoiceChannelIds() {
        return this.toSnowflakeSet(SettingsBean::getAllowedVoiceChannelIds);
    }

    public Set<Snowflake> getAllowedRoleIds() {
        return this.toSnowflakeSet(SettingsBean::getAllowedRoleIds);
    }

    public Set<Snowflake> getAutoRoleIds() {
        return this.toSnowflakeSet(SettingsBean::getAutoRoleIds);
    }

    public Set<String> getBlacklistedCmds() {
        return Optional.ofNullable(this.getBean())
                .map(SettingsBean::getBlacklist)
                .map(HashSet::new)
                .orElse(new HashSet<>());
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
                .stream()
                .map(Iam::new)
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

    public Map<Snowflake, Set<BaseCmd>> getRestrictedChannels() {
        return Optional.ofNullable(this.getBean())
                .map(SettingsBean::getRestrictedChannels)
                .orElse(new HashMap<>())
                .entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> Snowflake.of(entry.getKey()),
                        entry -> entry.getValue()
                                .stream()
                                .map(CommandManager::getCommand)
                                .collect(Collectors.toSet())));
    }

    public Map<Snowflake, Set<BaseCmd>> getRestrictedRoles() {
        return Optional.ofNullable(this.getBean())
                .map(SettingsBean::getRestrictedRoles)
                .orElse(new HashMap<>())
                .entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> Snowflake.of(entry.getKey()),
                        entry -> entry.getValue()
                                .stream()
                                .map(CommandManager::getCommand)
                                .collect(Collectors.toSet())));
    }

    @Deprecated
    public Optional<String> getPrefix() {
        return Optional.ofNullable(this.getBean().getPrefix());
    }

    private Set<Snowflake> toSnowflakeSet(Function<SettingsBean, List<String>> mapper) {
        return Optional.ofNullable(this.getBean())
                .map(mapper)
                .map(HashSet::new)
                .orElse(new HashSet<>())
                .stream()
                .map(Snowflake::of)
                .collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return "Settings{" +
                "bean=" + this.getBean() +
                '}';
    }
}
