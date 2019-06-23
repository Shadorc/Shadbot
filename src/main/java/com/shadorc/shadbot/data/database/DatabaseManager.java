package com.shadorc.shadbot.data.database;

import com.fasterxml.jackson.databind.JavaType;
import com.shadorc.shadbot.data.Data;
import com.shadorc.shadbot.utils.ExitCode;
import com.shadorc.shadbot.utils.LogUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.util.Snowflake;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager extends Data {

    private static DatabaseManager instance;

    static {
        try {
            DatabaseManager.instance = new DatabaseManager();
        } catch (final IOException err) {
            LogUtils.error(err, String.format("An error occurred while initializing %s.", DatabaseManager.class.getSimpleName()));
            System.exit(ExitCode.FATAL_ERROR.value());
        }
    }

    private final Map<Snowflake, DBGuild> guildsMap;

    private DatabaseManager() throws IOException {
        super("database.json", Duration.ofMinutes(15), Duration.ofMinutes(15));

        this.guildsMap = new ConcurrentHashMap<>();
        if (this.getFile().exists()) {
            final JavaType valueType = Utils.MAPPER.getTypeFactory().constructCollectionType(List.class, DBGuild.class);
            final List<DBGuild> guilds = Utils.MAPPER.readValue(this.getFile(), valueType);
            for (final DBGuild guild : guilds) {
                this.guildsMap.put(guild.getId(), guild);
            }
        }
    }

    public Collection<DBGuild> getDBGuilds() {
        return this.guildsMap.values();
    }

    public DBGuild getDBGuild(Snowflake guildId) {
        return this.guildsMap.computeIfAbsent(guildId, DBGuild::new);
    }

    public DBMember getDBMember(Snowflake guildId, Snowflake memberId) {
        final Optional<DBMember> dbMemberOpt = this.getDBGuild(guildId)
                .getMembers()
                .stream()
                .filter(member -> member.getId().equals(memberId))
                .findFirst();

        if (dbMemberOpt.isPresent()) {
            return dbMemberOpt.get();
        }

        final DBMember dbMember = new DBMember(guildId, memberId);
        this.getDBGuild(guildId).addMember(dbMember);
        return dbMember;
    }

    public void removeDBGuild(Snowflake guildId) {
        this.guildsMap.remove(guildId);
    }

    @Override
    public Object getData() {
        return this.guildsMap.values();
    }

    public static DatabaseManager getInstance() {
        return DatabaseManager.instance;
    }

}
