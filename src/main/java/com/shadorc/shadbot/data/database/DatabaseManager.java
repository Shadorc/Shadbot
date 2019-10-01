package com.shadorc.shadbot.data.database;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.data.DatabaseTable;
import com.shadorc.shadbot.utils.ExitCode;
import com.shadorc.shadbot.utils.LogUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.util.Snowflake;

public class DatabaseManager extends DatabaseTable {

    private static DatabaseManager instance;

    static {
        try {
            DatabaseManager.instance = new DatabaseManager();
        } catch (final Exception err) {
            LogUtils.error(err, String.format("An error occurred while initializing %s.", DatabaseManager.class.getSimpleName()));
            System.exit(ExitCode.FATAL_ERROR.getValue());
        }
    }

    public DatabaseManager() {
        super("guild");
    }

    public DBGuild getDBGuild(Snowflake id) {
        try {
            // TODO: What if guild is not present ?
            // TODO: If not present, insert it and return it
            final String guildJson = this.table.get(id.asLong()).toJson().toString();
            return Utils.MAPPER.readValue(guildJson, DBGuild.class);
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err, "An error occurred while getting DBGuild.");
            return null;
        }
    }

    public DBMember getDBMember(Snowflake guildId, Snowflake memberId) {
        try {
            // TODO: What if the member is not present ?
            // TODO: If not present, insert it and return it
            final String memberJson = this.table.get(guildId.asLong())
                    .filter(guild -> guild.hasFields("members"))
                    .filter(DB.hashMap("id", memberId.asLong()))
                    .toJson().toString();
            return Utils.MAPPER.readValue(memberJson, DBMember.class);
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err, "An error occurred while getting DBMember.");
            return null;
        }
    }

    public static DatabaseManager getInstance() {
        return DatabaseManager.instance;
    }

}
