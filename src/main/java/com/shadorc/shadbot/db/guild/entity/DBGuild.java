package com.shadorc.shadbot.db.guild.entity;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.db.DatabaseEntity;
import com.shadorc.shadbot.db.guild.GuildManager;
import com.shadorc.shadbot.db.guild.bean.DBGuildBean;
import com.shadorc.shadbot.utils.LogUtils;
import discord4j.core.object.util.Snowflake;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.shadorc.shadbot.db.guild.GuildManager.LOGGER;

public class DBGuild implements DatabaseEntity {

    private final long id;
    private final List<DBMember> members;
    private final Settings settings;

    public DBGuild(DBGuildBean bean) {
        this.id = bean.getId();
        this.members = bean.getMembers().stream()
                .map(memberBean -> new DBMember(this.getId(), memberBean))
                .collect(Collectors.toList());
        this.settings = new Settings(bean.getSettingsBean());
    }

    public DBGuild(Snowflake id) {
        this.id = id.asLong();
        this.members = new ArrayList<>();
        this.settings = new Settings();
    }

    public Snowflake getId() {
        return Snowflake.of(this.id);
    }

    public List<DBMember> getMembers() {
        return Collections.unmodifiableList(this.members);
    }

    public Settings getSettings() {
        return this.settings;
    }

    public <T> void setSetting(Setting setting, T value) {
        LOGGER.debug("[DBGuild {}] Updating setting {}: {}", this.getId().asLong(), setting, value);
        try {
            final GuildManager gm = GuildManager.getInstance();
            final String response = gm.getTable()
                    .insert(gm.getDatabase().hashMap("id", this.getId().asLong())
                            .with("settings", gm.getDatabase().hashMap(setting.toString(), value)))
                    .optArg("conflict", "update")
                    .run(gm.getConnection())
                    .toString();

            LOGGER.debug("[DBGuild {}] {}", this.getId().asLong(), response);

        } catch (final RuntimeException err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("[DBGuild %d] An error occurred while updating setting.", this.getId().asLong()));
        }
    }

    public void removeSetting(Setting setting) {
        LOGGER.debug("[DBGuild {}] Removing setting {}", this.getId().asLong(), setting);
        try {
            final GuildManager gm = GuildManager.getInstance();
            final String response = gm.requestGuild(this.getId())
                    .replace(guild -> guild.without(gm.getDatabase().hashMap("settings", setting.toString())))
                    .run(gm.getConnection())
                    .toString();

            LOGGER.debug("[DBGuild {}] {}", this.getId().asLong(), response);

        } catch (final RuntimeException err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("[DBGuild %d] An error occurred while removing setting.", this.getId().asLong()));
        }
    }

    public void addMember(DBMember member) {
        // TODO
    }

    public void removeMember(DBMember dbMember) {
        //TODO
    }

    @Override
    public void insert() {
        try {
            LOGGER.debug("[DBGuild {}] Inserting...", this.getId().asLong());

            final GuildManager gm = GuildManager.getInstance();
            final String response = gm.getTable()
                    .insert(gm.getDatabase().hashMap("id", this.getId().asLong()))
                    .run(gm.getConnection());

            LOGGER.debug("[DBGuild {}] {}", this.getId().asLong(), response);

        } catch (final RuntimeException err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("[DBGuild %d] An error occurred during insertion.", this.getId().asLong()));
        }
    }

    @Override
    public void delete() {
        try {
            LOGGER.debug("[DBGuild {}] Deleting...", this.getId().asLong());

            final GuildManager gm = GuildManager.getInstance();
            final String response = gm.requestGuild(this.getId())
                    .delete()
                    .run(gm.getConnection());

            LOGGER.debug("[DBGuild {}] {}", this.getId().asLong(), response);

        } catch (final RuntimeException err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("[DBGuild %d] An error occurred during deletion.", this.getId().asLong()));
        }
    }

    @Override
    public String toString() {
        return "DBGuild{" +
                "id=" + this.id +
                ", members=" + this.members +
                ", settings=" + this.settings +
                '}';
    }
}
