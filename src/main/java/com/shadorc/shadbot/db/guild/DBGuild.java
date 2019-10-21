package com.shadorc.shadbot.db.guild;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseEntity;
import com.shadorc.shadbot.utils.LogUtils;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;

import java.util.*;

import static com.shadorc.shadbot.db.guild.GuildManager.LOGGER;

@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class DBGuild extends DatabaseEntity {

    @JsonProperty("id")
    private final long guildId;
    @JsonProperty("members")
    private final List<DBMember> members;
    @JsonProperty("settings")
    private final Settings settings;

    protected DBGuild(Snowflake id) {
        this.guildId = id.asLong();
        this.members = Collections.emptyList();
        this.settings = new Settings();
    }

    protected DBGuild() {
        this(Snowflake.of(0L));
    }

    public Snowflake getId() {
        return Snowflake.of(this.guildId);
    }

    public List<DBMember> getMembers() {
        return Collections.unmodifiableList(this.members);
    }

    public List<Long> getAllowedTextChannels() {
        return Objects.requireNonNullElse(this.settings.getAllowedTextChannels(), new ArrayList<>());
    }

    public List<Long> getAllowedVoiceChannels() {
        return Objects.requireNonNullElse(this.settings.getAllowedVoiceChannels(), new ArrayList<>());
    }

    public List<Long> getAllowedRoles() {
        return Objects.requireNonNullElse(this.settings.getAllowedRoles(), new ArrayList<>());
    }

    public List<Long> getAutoRoles() {
        return Objects.requireNonNullElse(this.settings.getAutoRoles(), new ArrayList<>());
    }

    public List<String> getBlacklistedCmd() {
        return Objects.requireNonNullElse(this.settings.getBlacklist(), new ArrayList<>());
    }

    public Integer getDefaultVol() {
        return Objects.requireNonNullElse(this.settings.getDefaultVolume(), Config.DEFAULT_VOLUME);
    }

    /**
     * @return A map containing message's ID as key and role's ID as value
     */
    public Map<String, Long> getIamMessages() {
        return Objects.requireNonNullElse(this.settings.getIamMessage(), new HashMap<>());
    }

    public Optional<String> getJoinMessage() {
        return Optional.ofNullable(this.settings.getJoinMessage());
    }

    public Optional<String> getLeaveMessage() {
        return Optional.ofNullable(this.settings.getLeaveMessage());
    }

    public Optional<Long> getMessageChannelId() {
        return Optional.ofNullable(this.settings.getMessageChannelId());
    }

    public Map<String, List<String>> getSavedPlaylists() {
        return Objects.requireNonNullElse(this.settings.getSavedPlaylists(), new HashMap<>());
    }

    public String getPrefix() {
        return Objects.requireNonNullElse(this.settings.getPrefix(), Config.DEFAULT_PREFIX);
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

    public <T> void setSetting(Setting setting, T value) {
        try {
            LOGGER.debug("[DBGuild {}] Updating setting {}: {}", this.guildId, setting, value);
            final GuildManager gm = GuildManager.getInstance();
            gm.getTable()
                    .insert(gm.getDatabase().hashMap("id", this.guildId)
                            .with("settings", gm.getDatabase().hashMap(setting.toString(), value)))
                    .optArg("conflict", "update")
                    .run(gm.getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("[DBGuild %d] An error occurred while updating setting.", this.guildId));
        }
    }

    public void removeSetting(Setting setting) {
        try {
            LOGGER.debug("[DBGuild {}] Removing setting {}", this.guildId, setting);
            final GuildManager gm = GuildManager.getInstance();
            gm.requestGuild(this.getId())
                    .replace(guild -> guild.without(gm.getDatabase().hashMap("settings", setting.toString())))
                    .run(gm.getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("[DBGuild %d] An error occurred while removing setting.", this.guildId));
        }
    }

    public void removeMember(DBMember dbMember) {
        //TODO
    }

    @Override
    public void insert() {
        try {
            LOGGER.debug("[Guild {}] Inserting...", this.guildId);
            GuildManager.getInstance()
                    .getTable()
                    .insert(GuildManager.getInstance().getDatabase().hashMap("id", this.guildId))
                    .run(GuildManager.getInstance().getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("[DBGuild %d] An error occurred during insertion.", this.guildId));
        }
        LOGGER.debug("[Guild {}] Inserted.", this.guildId);
    }

    @Override
    public void delete() {
        try {
            LOGGER.debug("[Guild {}] Deleting...", this.guildId);
            GuildManager.getInstance()
                    .requestGuild(this.getId())
                    .delete()
                    .run(GuildManager.getInstance().getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("[Guild %d] An error occurred during deletion.", this.guildId));
        }
        LOGGER.debug("[Guild {}] Deleted.", this.guildId);
    }

    @Override
    public String toString() {
        return String.format("DBGuild [guildId=%s, members=%s, settings=%s]",
                this.guildId, this.members, this.settings);
    }

}
