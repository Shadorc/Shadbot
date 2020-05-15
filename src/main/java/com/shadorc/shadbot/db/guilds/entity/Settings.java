package com.shadorc.shadbot.db.guilds.entity;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.SerializableEntity;
import com.shadorc.shadbot.db.guilds.bean.SettingsBean;
import com.shadorc.shadbot.db.guilds.entity.setting.Iam;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.entity.Role;
import discord4j.rest.util.Permission;
import discord4j.rest.util.Snowflake;

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

    public boolean isCategoryAllowed(Snowflake channelId, CommandCategory category) {
        final Map<Snowflake, Set<CommandCategory>> map = this.getRestrictedCategories();

        // If no permission has been set
        if (map.isEmpty()) {
            return true;
        }
        // If this category has explicitly been allowed in this channel
        if (map.containsKey(channelId) && map.get(channelId).contains(category)) {
            return true;
        }

        return map.values().stream().noneMatch(set -> set.contains(category));
    }

    public Set<Snowflake> getAllowedTextChannelIds() {
        return this.toSnowflakeSet(SettingsBean::getAllowedTextChannelIds);
    }

    public Set<Snowflake> getAllowedVoiceChannelIds() {
        return this.toSnowflakeSet(SettingsBean::getAllowedVoiceChannelIds);
    }

    public Set<Snowflake> getAllowedRoleIds() {
        return this.toSnowflakeSet(SettingsBean::getAllowedRoleIds);
    }

    public Set<Snowflake> getAutoRoleIds() {
        return this.toSnowflakeSet(SettingsBean::getAutoRoleIds);
    }

    public Set<String> getBlacklistedCmd() {
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

    public String getPrefix() {
        return Optional.ofNullable(this.getBean())
                .map(SettingsBean::getPrefix)
                .orElse(Config.DEFAULT_PREFIX);
    }

    public Map<Snowflake, Set<CommandCategory>> getRestrictedCategories() {
        return Optional.ofNullable(this.getBean())
                .map(SettingsBean::getRestrictedCategories)
                .orElse(new HashMap<>())
                .entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> Snowflake.of(entry.getKey()),
                        entry -> entry.getValue()
                                .stream()
                                .map(value -> Utils.parseEnum(CommandCategory.class, value))
                                .collect(Collectors.toSet())));
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
