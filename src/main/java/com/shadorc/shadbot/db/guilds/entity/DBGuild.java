package com.shadorc.shadbot.db.guilds.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.db.DatabaseEntity;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.bean.DBGuildBean;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.util.Snowflake;
import org.bson.Document;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.shadorc.shadbot.db.guilds.GuildsCollection.LOGGER;

public class DBGuild implements DatabaseEntity {

    private final DBGuildBean bean;

    public DBGuild(DBGuildBean bean) {
        this.bean = bean;
    }

    public DBGuild(Snowflake id) {
        this.bean = new DBGuildBean(id.asString(), null, null);
    }

    public Snowflake getId() {
        return Snowflake.of(this.bean.getId());
    }

    public List<DBMember> getMembers() {
        if (this.bean.getMembers() == null) {
            return Collections.emptyList();
        }

        return this.bean.getMembers()
                .stream()
                .map(memberBean -> new DBMember(this.getId(), memberBean))
                .collect(Collectors.toUnmodifiableList());
    }

    public Settings getSettings() {
        return new Settings(this.bean.getSettingsBean());
    }

    public <T> void setSetting(Setting setting, T value) {
        LOGGER.debug("[DBGuild {}] Updating setting {}: {}", this.getId().asLong(), setting, value);

        // TODO: This is fucking shit piece of code
        Object obj = value;
        if (value instanceof List && !((List<?>) value).isEmpty() && (((List<?>) value).get(0) instanceof Snowflake)) {
            obj = ((List<Snowflake>) value).stream()
                    .map(Snowflake::asString)
                    .collect(Collectors.toUnmodifiableList());
        }

        DatabaseManager.getGuilds()
                .getCollection()
                .updateOne(
                        Filters.eq("_id", this.getId().asString()),
                        Updates.set(String.format("settings.%s", setting.toString()), obj),
                        new UpdateOptions().upsert(true));
    }

    public void removeSetting(Setting setting) {
        LOGGER.debug("[DBGuild {}] Removing setting {}", this.getId().asLong(), setting);

        DatabaseManager.getGuilds()
                .getCollection()
                .updateOne(
                        Filters.eq("_id", this.getId().asString()),
                        Updates.unset(String.format("settings.%s", setting.toString())));
    }

    @Override
    public void insert() {
        LOGGER.debug("[DBGuild {}] Insertion", this.getId().asLong());

        try {
            DatabaseManager.getGuilds()
                    .getCollection()
                    .insertOne(this.toDocument());
        } catch (final JsonProcessingException err) {
            throw new RuntimeException(err);
        }
    }

    @Override
    public void delete() {
        LOGGER.debug("[DBGuild {}] Deletion", this.getId().asLong());

        DatabaseManager.getGuilds()
                .getCollection()
                .deleteOne(Filters.eq("_id", this.getId().asString()));
    }

    @Override
    public Document toDocument() throws JsonProcessingException {
        return Document.parse(Utils.MAPPER.writeValueAsString(this.bean));
    }

    @Override
    public String toString() {
        return "DBGuild{" +
                "bean=" + this.bean +
                '}';
    }
}
