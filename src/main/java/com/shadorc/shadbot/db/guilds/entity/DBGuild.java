package com.shadorc.shadbot.db.guilds.entity;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.db.DatabaseEntity;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.bean.DBGuildBean;
import discord4j.core.object.util.Snowflake;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.shadorc.shadbot.db.guilds.GuildsCollection.LOGGER;

public class DBGuild extends DatabaseEntity<DBGuildBean> {

    public DBGuild(DBGuildBean bean) {
        super(bean);
    }

    public DBGuild(Snowflake id) {
        super(new DBGuildBean(id.asString(), null, null));
    }

    public Snowflake getId() {
        return Snowflake.of(this.getBean().getId());
    }

    public List<DBMember> getMembers() {
        if (this.getBean().getMembers() == null) {
            return Collections.emptyList();
        }

        return this.getBean().getMembers()
                .stream()
                .map(memberBean -> new DBMember(this.getId(), memberBean))
                .collect(Collectors.toUnmodifiableList());
    }

    public Settings getSettings() {
        return new Settings(this.getBean().getSettingsBean());
    }

    public <T> void setSetting(Setting setting, T value) {
        LOGGER.debug("[DBGuild {}] Updating setting {}: {}", this.getId().asLong(), setting, value);

        // TODO: Map Snowflake to String into MongoDB
        Object obj = value;

        // Convert List<Snowflake> to List<String>
        if (value instanceof List && !((List<?>) value).isEmpty() && (((List<?>) value).get(0) instanceof Snowflake)) {
            obj = ((List<Snowflake>) value).stream()
                    .map(Snowflake::asString)
                    .collect(Collectors.toUnmodifiableList());
        }

        // Convert Map<Snowflake, Snowflake> to Map<String, String>
        if (value instanceof Map && !((Map<?, ?>) value).isEmpty()) {
            final List<?> entries = new ArrayList<>(((Map<?, ?>) value).entrySet());
            final List<?> values = new ArrayList<>(((Map<?, ?>) value).values());
            if (entries.get(0) instanceof Snowflake && values.get(0) instanceof Snowflake) {
                obj = ((Map<Snowflake, Snowflake>) value).entrySet()
                        .stream()
                        .collect(Collectors.toMap(
                                entry -> entry.getKey().asString(),
                                entry -> entry.getValue().asString()));
            }
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

        DatabaseManager.getGuilds()
                .getCollection()
                .insertOne(this.toDocument());
    }

    @Override
    public void delete() {
        LOGGER.debug("[DBGuild {}] Deletion", this.getId().asLong());

        DatabaseManager.getGuilds()
                .getCollection()
                .deleteOne(Filters.eq("_id", this.getId().asString()));
    }

    @Override
    public String toString() {
        return "DBGuild{" +
                "bean=" + this.getBean() +
                '}';
    }
}
