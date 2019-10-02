package com.shadorc.shadbot.data.database;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.shadorc.shadbot.Config;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.setting.Setting;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class DBGuild {

    @JsonProperty("id")
    private final long guildId;
    @JsonProperty("members")
    private final CopyOnWriteArrayList<DBMember> members;
    @JsonProperty("settings")
    public final ConcurrentHashMap<String, Object> settings;

    public DBGuild(Snowflake id) {
        this.guildId = id.asLong();
        this.members = new CopyOnWriteArrayList<>();
        this.settings = new ConcurrentHashMap<>();
    }

    public DBGuild() {
        this(Snowflake.of(0L));
    }

    public Snowflake getId() {
        return Snowflake.of(this.guildId);
    }

    public List<DBMember> getMembers() {
        return Collections.unmodifiableList(this.members);
    }

    public List<Long> getAllowedTextChannels() {
        return this.getListSetting(Setting.ALLOWED_TEXT_CHANNELS, Long.class);
    }

    public List<Long> getAllowedVoiceChannels() {
        return this.getListSetting(Setting.ALLOWED_VOICE_CHANNELS, Long.class);
    }

    public List<Long> getAllowedRoles() {
        return this.getListSetting(Setting.ALLOWED_ROLES, Long.class);
    }

    public List<Long> getAutoRoles() {
        return this.getListSetting(Setting.AUTO_ROLES, Long.class);
    }

    public List<String> getBlacklistedCmd() {
        return this.getListSetting(Setting.BLACKLIST, String.class);
    }

    public Integer getDefaultVol() {
        return Integer.parseInt(Objects.toString(
                this.settings.get(Setting.DEFAULT_VOLUME.toString()),
                Integer.toString(Config.DEFAULT_VOLUME)));
    }

    /**
     * @return A map containing message's ID as key and role's ID as value
     */
    public Map<String, Long> getIamMessages() {
        return (Map<String, Long>) Optional.ofNullable(this.settings.get(Setting.IAM_MESSAGES.toString()))
                .orElse(new HashMap<String, Long>());
    }

    public Optional<String> getJoinMessage() {
        return Optional.ofNullable((String) this.settings.get(Setting.JOIN_MESSAGE.toString()));
    }

    public Optional<String> getLeaveMessage() {
        return Optional.ofNullable((String) this.settings.get(Setting.LEAVE_MESSAGE.toString()));
    }

    public Optional<Long> getMessageChannelId() {
        return Optional.ofNullable((Long) this.settings.get(Setting.MESSAGE_CHANNEL_ID.toString()));
    }

    public Map<String, List<String>> getPlaylists() {
        return (Map<String, List<String>>) Optional.ofNullable(this.settings.get(Setting.SAVED_PLAYLISTS.toString()))
                .orElse(new HashMap<>());
    }

    public String getPrefix() {
        return Objects.toString(
                this.settings.get(Setting.PREFIX.toString()),
                Config.DEFAULT_PREFIX);
    }

    private <T> List<T> getListSetting(Setting setting, Class<T> listClass) {
        return Optional.ofNullable((List<?>) this.settings.get(setting.toString()))
                .orElse(new ArrayList<>())
                .stream()
                .map(listClass::cast)
                .collect(Collectors.toList());
    }

    public boolean hasAllowedRole(List<Role> roles) {
        final List<Long> allowedRoles = this.getAllowedRoles();
        // If the user is an administrator OR no permissions have been set OR the role is allowed
        return allowedRoles.isEmpty()
                || roles.stream().anyMatch(role -> role.getPermissions().contains(Permission.ADMINISTRATOR))
                || roles.stream().anyMatch(role -> allowedRoles.contains(role.getId().asLong()));
    }

    public boolean isCommandAllowed(BaseCmd cmd) {
        final List<String> blacklistedCmd = this.getBlacklistedCmd();
        return cmd.getNames().stream().noneMatch(blacklistedCmd::contains);
    }

    public boolean isTextChannelAllowed(Snowflake channelId) {
        final List<Long> allowedTextChannels = this.getAllowedTextChannels();
        // If no permission has been set OR the text channel is allowed
        return allowedTextChannels.isEmpty() || allowedTextChannels.contains(channelId.asLong());
    }

    public boolean isVoiceChannelAllowed(Snowflake channelId) {
        final List<Long> allowedVoiceChannels = this.getAllowedVoiceChannels();
        // If no permission has been set OR the voice channel is allowed
        return allowedVoiceChannels.isEmpty() || allowedVoiceChannels.contains(channelId.asLong());
    }

    public void setSetting(Setting setting, Object value) {
        this.settings.put(setting.toString(), value);
    }

    public void removeSetting(Setting setting) {
        this.settings.remove(setting.toString());
    }

    public void addMember(DBMember dbMember) {
        this.members.add(dbMember);
    }

    public void removeMember(DBMember dbMember) {
        this.members.remove(dbMember);
    }

    @Override
    public String toString() {
        return String.format("DBGuild [guildId=%s, members=%s, settings=%s]",
                this.guildId, this.members, this.settings);
    }

}
